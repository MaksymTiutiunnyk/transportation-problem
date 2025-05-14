package org.example;

import java.util.*;

public class ParallelTransportationProblemSolver {
    protected final int m, n;
    protected final int[][] cost, allocation;
    protected final List<Location> allocated = new ArrayList<>();
    protected final List<Location> notAllocated = new ArrayList<>();
    protected final int[] supply, demand;
    protected final int[] u, v;
    protected final int[][] delta;
    protected Chain chain;
    protected int minI, minJ;
    protected boolean isCurrentSolutionOptimal;
    protected final int NOT_ALLOCATED = -1, NO_SUPPLY = Integer.MAX_VALUE, NO_DEMAND = Integer.MAX_VALUE, UNDEFINED = Integer.MIN_VALUE;

    protected ParallelTransportationProblemSolver(TransportationProblem problem) {
        this.m = problem.supply.length;
        this.n = problem.demand.length;
        this.cost = problem.cost;
        this.supply = Arrays.copyOf(problem.supply, m);
        this.demand = Arrays.copyOf(problem.demand, n);
        this.allocation = new int[m][n];
        for (int i = 0; i < m; ++i)
            Arrays.fill(allocation[i], NOT_ALLOCATED);
        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < n; ++j)
                notAllocated.add(new Location(i, j));
        }
        this.u = new int[m];
        this.v = new int[n];
        this.delta = new int[m][n];

        if (!isBalanced())
            throw new RuntimeException("Supplies do not match demands");
    }

    public void solve() {
        northwestCornerMethod();
        while (true) {
            isCurrentSolutionOptimal = true;
            computePotentials();
            conductDeltaOperations();
            if (isCurrentSolutionOptimal)
                break;
            buildChain();
            adjustAllocation();
        }
    }

    protected void northwestCornerMethod() {
        int i = 0, j = 0;
        while (i < m && j < n) {
            int currentAllocation = Math.min(supply[i], demand[j]);

            if (currentAllocation != NO_SUPPLY) {
                allocation[i][j] = currentAllocation;
                int finalI = i;
                int finalJ = j;
                notAllocated.remove(notAllocated.stream().filter((element) -> element.i == finalI && element.j == finalJ).findFirst().get());
                allocated.add(new Location(i, j));
            }

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

    protected void computePotentials() {
        Arrays.fill(u, UNDEFINED);
        Arrays.fill(v, UNDEFINED);
        u[0] = 0;

        List<Integer>[] rows = new List[m];
        List<Integer>[] cols = new List[n];
        for (int i = 0; i < m; ++i) rows[i] = new ArrayList<>();
        for (int j = 0; j < n; ++j) cols[j] = new ArrayList<>();

        for (Location loc : allocated) {
            rows[loc.i].add(loc.j);
            cols[loc.j].add(loc.i);
        }

        Deque<Integer> supplyQueue = new ArrayDeque<>();
        Deque<Integer> demandQueue = new ArrayDeque<>();
        supplyQueue.add(0);

        while (!supplyQueue.isEmpty()) {

            while (!supplyQueue.isEmpty()) {
                int i = supplyQueue.removeFirst();
                for (int j : rows[i]) {
                    if (v[j] == UNDEFINED) {
                        v[j] = cost[i][j] - u[i];
                        demandQueue.addLast(j);
                    }
                }
            }

            while (!demandQueue.isEmpty()) {
                int j = demandQueue.removeFirst();
                for (int i : cols[j]) {
                    if (u[i] == UNDEFINED) {
                        u[i] = cost[i][j] - v[j];
                        supplyQueue.addLast(i);
                    }
                }
            }
        }
    }

    protected void conductDeltaOperations() {
        int minDelta = Integer.MAX_VALUE;
        for (Location location : notAllocated) {
            delta[location.i][location.j] = cost[location.i][location.j] - (u[location.i] + v[location.j]);

            if (delta[location.i][location.j] < 0)
                isCurrentSolutionOptimal = false;

            if (delta[location.i][location.j] < minDelta) {
                minDelta = delta[location.i][location.j];
                minI = location.i;
                minJ = location.j;
            }
        }
        for (Location location : allocated)
            delta[location.i][location.j] = UNDEFINED;
    }

    private void buildChain() {
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

    protected int findNearestIndex(ChainElement element, boolean searchInColumn, boolean[][] visited) {
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

    protected void adjustAllocation() {
        int minValue = chain.getMinValue();

        for (ChainElement chainElement : chain.chain) {
            if (chainElement.sign == Sign.NEGATIVE) {
                if (allocation[chainElement.i][chainElement.j] - minValue == 0) {
                    allocation[chainElement.i][chainElement.j] = NOT_ALLOCATED;
                    allocated.remove(allocated.stream().filter((element) -> element.i == chainElement.i && element.j == chainElement.j).findFirst().get());
                    notAllocated.add(new Location(chainElement.i, chainElement.j));
                }
                else
                    allocation[chainElement.i][chainElement.j] -= minValue;
            } else {
                if (allocation[chainElement.i][chainElement.j] == NOT_ALLOCATED) {
                    allocation[chainElement.i][chainElement.j] += minValue + 1;
                    notAllocated.remove(notAllocated.stream().filter((element) -> element.i == chainElement.i && element.j == chainElement.j).findFirst().get());
                    allocated.add(new Location(chainElement.i, chainElement.j));
                }
                else
                    allocation[chainElement.i][chainElement.j] += minValue;
            }
        }
        int degenerateCount = degenerateCount();
        if (degenerateCount > 0) {
            ArrayList<ChainElement> degenerateElements = new ArrayList<>();
            for (ChainElement chainElement : chain.chain) {
                if (allocation[chainElement.i][chainElement.j] == NOT_ALLOCATED)
                    degenerateElements.add(chainElement);
            }
            degenerateElements.sort((chainElement1, chainElement2) -> cost[chainElement2.i][chainElement2.j] - cost[chainElement1.i][chainElement1.j]);
            for (int i = 0; i < degenerateCount; ++i) {
                allocation[degenerateElements.get(i).i][degenerateElements.get(i).j] = 0;
                int finalI = i;
                notAllocated.remove(notAllocated.stream().filter((element) -> element.i == degenerateElements.get(finalI).i && element.j == degenerateElements.get(finalI).j).findFirst().get());
                allocated.add(new Location(degenerateElements.get(finalI).i, degenerateElements.get(finalI).j));
            }
        }
    }

    static class Location {
        public int i, j;

        public Location(int i, int j) {
            this.i = i;
            this.j = j;
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
