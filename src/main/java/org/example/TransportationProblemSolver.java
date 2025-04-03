package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class TransportationProblemSolver {
    protected final int m, n;
    protected final int[][] cost, allocation;
    protected final int[] supply, demand;
    protected final int[] u, v;
    protected final int[][] delta;
    protected Chain chain;
    protected int minI, minJ;
    protected final int NOT_ALLOCATED = -1, NO_SUPPLY = Integer.MAX_VALUE, NO_DEMAND = Integer.MAX_VALUE, UNDEFINED = Integer.MIN_VALUE;

    protected TransportationProblemSolver(TransportationProblem problem) {
        this.m = problem.supply.length;
        this.n = problem.demand.length;
        this.cost = problem.cost;
        this.supply = Arrays.copyOf(problem.supply, m);
        this.demand = Arrays.copyOf(problem.demand, n);
        this.allocation = new int[m][n];
        for (int i = 0; i < m; i++)
            Arrays.fill(allocation[i], NOT_ALLOCATED);
        this.u = new int[m];
        this.v = new int[n];
        this.delta = new int[m][n];

        if (!isBalanced())
            throw new RuntimeException("Supplies do not match demands");
    }

    public void solve() {
        northwestCornerMethod();
        while (true) {
            computePotentials();
            computeDelta();
            if (isOptimal())
                break;
            buildChain();
            adjustAllocation();
        }
    }

    private void northwestCornerMethod() {
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

    protected void computePotentials() {
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

    protected void computeDelta() {
        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < n; ++j) {
                if (allocation[i][j] == NOT_ALLOCATED)
                    delta[i][j] = cost[i][j] - (u[i] + v[j]);
                else
                    delta[i][j] = UNDEFINED;
            }
        }
    }

    protected boolean isOptimal() {
        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < n; ++j) {
                if (delta[i][j] != UNDEFINED && delta[i][j] < 0)
                    return false;
            }
        }
        return true;
    }

    private void buildChain() {
        boolean isSearchInColumn = true;
        boolean[][] visited = new boolean[m][n];
        chain = new Chain(); // create a new chain on each iteration

        defineMinDeltaIndexes();

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

    protected void defineMinDeltaIndexes() {
        int minDelta = Integer.MAX_VALUE;
        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < n; ++j) {
                if (delta[i][j] != UNDEFINED && delta[i][j] < minDelta) {
                    minDelta = delta[i][j];
                    minI = i;
                    minJ = j;
                }
            }
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
                if (allocation[chainElement.i][chainElement.j] - minValue == 0)
                    allocation[chainElement.i][chainElement.j] = NOT_ALLOCATED;
                else
                    allocation[chainElement.i][chainElement.j] -= minValue;
            }
            else {
                if (allocation[chainElement.i][chainElement.j] == NOT_ALLOCATED)
                    allocation[chainElement.i][chainElement.j] += minValue + 1;
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
