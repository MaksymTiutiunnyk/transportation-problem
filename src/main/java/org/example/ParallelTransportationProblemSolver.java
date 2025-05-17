package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.*;

public class ParallelTransportationProblemSolver {
    protected final int m, n;
    protected final int[][] cost, allocation;
    protected final int[] supply, demand;
    protected final int[] u, v;
    protected final int[][] delta;
    protected Chain chain;
    protected int minI, minJ;
    protected boolean isCurrentSolutionOptimal;
    protected final int NOT_ALLOCATED = -1, NO_SUPPLY = Integer.MAX_VALUE, NO_DEMAND = Integer.MAX_VALUE, UNDEFINED = Integer.MIN_VALUE;
    protected final int numThreads;
    protected final ExecutorService executor;

    protected ParallelTransportationProblemSolver(TransportationProblem problem) {
        this.m = problem.supply.length;
        this.n = problem.demand.length;
        this.cost = problem.cost;
        this.supply = Arrays.copyOf(problem.supply, m);
        this.demand = Arrays.copyOf(problem.demand, n);
        this.allocation = new int[m][n];
        for (int i = 0; i < m; ++i)
            Arrays.fill(allocation[i], NOT_ALLOCATED);
        this.u = new int[m];
        this.v = new int[n];
        this.delta = new int[m][n];
        this.numThreads = 16;
        executor = Executors.newFixedThreadPool(numThreads);

        if (!isBalanced())
            throw new RuntimeException("Supplies do not match demands");
    }

    public void solve() throws ExecutionException, InterruptedException {
        northwestCornerMethod();
        while (true) {
            isCurrentSolutionOptimal = true;
            computePotentials();
            isCurrentSolutionOptimal = conductDeltaOperations();
            if (isCurrentSolutionOptimal) {
                executor.shutdown();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        throw new RuntimeException("Executor did not terminate");
                    }
                }
                break;
            }
            buildChain();
            adjustAllocation();
        }
    }

    public void northwestCornerMethod() {
        int i = 0, j = 0;
        while (i < m && j < n) {
            int currentAllocation = Math.min(supply[i], demand[j]);
            allocation[i][j] = (currentAllocation == NO_SUPPLY) ? NOT_ALLOCATED : currentAllocation;

            if (supply[i] == demand[j]) {
                supply[i] = NO_SUPPLY;
                demand[j] = 0;
                ++i;
            } else {
                supply[i] -= currentAllocation;
                demand[j] -= currentAllocation;
                if (supply[i] == 0) supply[i] = NO_SUPPLY;
                if (demand[j] == 0) demand[j] = NO_DEMAND;
                if (supply[i] == NO_SUPPLY) ++i;
                else ++j;
            }
        }
    }

    public void computePotentials() {
        Arrays.fill(u, UNDEFINED);
        Arrays.fill(v, UNDEFINED);
        u[0] = 0;

        boolean updated;
        do {
            updated = false;
            for (int i = 0; i < m; ++i) {
                for (int j = 0; j < n; ++j) {
                    if (allocation[i][j] != NOT_ALLOCATED) {
                        if (u[i] != UNDEFINED && v[j] == UNDEFINED) {
                            v[j] = cost[i][j] - u[i];
                            updated = true;
                        } else if (v[j] != UNDEFINED && u[i] == UNDEFINED) {
                            u[i] = cost[i][j] - v[j];
                            updated = true;
                        }
                    }
                }
            }
        } while (updated);
    }

    private static class DeltaTaskResult {
        int minDelta = Integer.MAX_VALUE;
        int minI = -1;
        int minJ = -1;
        boolean isCurrentSolutionOptimal = true;
    }

    public boolean conductDeltaOperations() throws InterruptedException, ExecutionException {
        int baseChunkSize = m / numThreads;
        int remainder = m % numThreads;
        int currentRow = 0;
        List<Future<DeltaTaskResult>> futures = new ArrayList<>();

        for (int t = 0; t < numThreads; ++t) {
            int rowsInThisChunk = baseChunkSize + (t < remainder ? 1 : 0);
            final int startRow = currentRow;
            final int endRow = startRow + rowsInThisChunk;
            currentRow = endRow;

            futures.add(executor.submit(() -> {
                DeltaTaskResult localResult = new DeltaTaskResult();
                for (int i = startRow; i < endRow; ++i) {
                    for (int j = 0; j < n; ++j) {
                        if (allocation[i][j] == NOT_ALLOCATED) {
                            int deltaVal = cost[i][j] - (u[i] + v[j]);
                            delta[i][j] = deltaVal;

                            if (deltaVal < 0) {
                                localResult.isCurrentSolutionOptimal = false;
                            }

                            if (deltaVal < localResult.minDelta) {
                                localResult.minDelta = deltaVal;
                                localResult.minI = i;
                                localResult.minJ = j;
                            }
                        } else {
                            delta[i][j] = UNDEFINED;
                        }
                    }
                }
                return localResult;
            }));
        }

        // Combine results
        int minDelta = Integer.MAX_VALUE;

        for (Future<DeltaTaskResult> future : futures) {
            DeltaTaskResult result = future.get();
            if (!result.isCurrentSolutionOptimal) isCurrentSolutionOptimal = false;
            if (result.minDelta < minDelta) {
                minDelta = result.minDelta;
                minI = result.minI;
                minJ = result.minJ;
            }
        }

        return isCurrentSolutionOptimal;
    }


    public void buildChain() {
        boolean isSearchInColumn = true;
        boolean[][] visited = new boolean[m][n];
        chain = new Chain(); // create a new chain on each iteration

        Stack<ChainElement> stack = new Stack<>();
        final ChainElement firstElement = new ChainElement(minI, minJ, cost[minI][minJ], allocation[minI][minJ]);
        stack.push(firstElement);
        chain.add(firstElement);

        while (!chain.isClosed()) {
            isSearchInColumn = !isSearchInColumn;

            ChainElement currentElement = stack.peek();
            visited[currentElement.i][currentElement.j] = true;

            int nearestIndex = findNearestIndex(currentElement, isSearchInColumn, visited);

            if (nearestIndex != -1) {
                ChainElement newElement = isSearchInColumn
                        ? new ChainElement(nearestIndex, currentElement.j, cost[nearestIndex][currentElement.j], allocation[nearestIndex][currentElement.j])
                        : new ChainElement(currentElement.i, nearestIndex, cost[currentElement.i][nearestIndex], allocation[currentElement.i][nearestIndex]);

                stack.push(newElement);
                chain.add(newElement);
            } else
                chain.chain.remove(stack.pop());
        }
    }

    public int findNearestIndex(ChainElement element, boolean searchInColumn, boolean[][] visited) {
        int minDistance = Integer.MAX_VALUE;
        int nearestIndex = -1;
        int fixedIndex = searchInColumn ? element.j : element.i;

        if (searchInColumn) {
            for (int i = 0; i < m; ++i) {
                if (allocation[i][fixedIndex] != NOT_ALLOCATED && i != element.i && !visited[i][fixedIndex]) {
                    int distance = Math.abs(i - element.i);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestIndex = i;
                    }
                }
            }
        } else {
            for (int j = 0; j < n; ++j) {
                if (allocation[fixedIndex][j] != NOT_ALLOCATED && j != element.j && !visited[fixedIndex][j]) {
                    int distance = Math.abs(j - element.j);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestIndex = j;
                    }
                }
            }
        }
        return nearestIndex;
    }

    public void adjustAllocation() throws InterruptedException, ExecutionException {
        int minValue = chain.getMinValue();
        int chainSize = chain.chain.size();
        int baseChunkSize = chainSize / numThreads;
        int remainder = chainSize % numThreads;
        int currentRow = 0;

        List<Future<Void>> futures = new ArrayList<>();

        for (int t = 0; t < numThreads; ++t) {
            int rowsInThisChunk = baseChunkSize + (t < remainder ? 1 : 0);
            int startRow = currentRow;
            int endRow = startRow + rowsInThisChunk;
            currentRow = endRow;

            futures.add(executor.submit(() -> {
                for (int idx = startRow; idx < endRow; ++idx) {
                    ChainElement element = chain.chain.get(idx);
                    int i = element.i;
                    int j = element.j;

                    if (element.sign == Sign.NEGATIVE) {
                        if (allocation[i][j] - minValue == 0)
                            allocation[i][j] = NOT_ALLOCATED;
                        else
                            allocation[i][j] -= minValue;
                    } else {
                        if (allocation[i][j] == NOT_ALLOCATED)
                            allocation[i][j] += minValue + 1;
                        else
                            allocation[i][j] += minValue;
                    }
                }
                return null;
            }));
        }

        for (Future<Void> future : futures)
            future.get();

        int degenerateCount = degenerateCount();
        if (degenerateCount > 0) {
            ArrayList<ChainElement> degenerateElements = new ArrayList<>();
            for (ChainElement chainElement : chain.chain) {
                if (allocation[chainElement.i][chainElement.j] == NOT_ALLOCATED)
                    degenerateElements.add(chainElement);
            }
            degenerateElements.sort((a, b) -> cost[b.i][b.j] - cost[a.i][a.j]);
            for (int i = 0; i < degenerateCount; ++i)
                allocation[degenerateElements.get(i).i][degenerateElements.get(i).j] = 0;
        }
    }

    public int[][] getAllocation() {
        return allocation;
    }

    public int getCost() {
        int totalCost = 0;
        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < n; ++j) {
                if (allocation[i][j] > 0)
                    totalCost += allocation[i][j] * cost[i][j];
            }
        }
        return totalCost;
    }

    private boolean isBalanced() {
        int supplySum = 0, demandSum = 0;
        for (int i = 0; i < m; ++i)
            supplySum += supply[i];

        for (int i = 0; i < n; ++i)
            demandSum += demand[i];

        return supplySum == demandSum;
    }

    protected int degenerateCount() {
        int basisCount = 0;

        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < n; ++j) {
                if (allocation[i][j] != NOT_ALLOCATED)
                    ++basisCount;
            }
        }
        return m + n - 1 - basisCount;
    }
}
