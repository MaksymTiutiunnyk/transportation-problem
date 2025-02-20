package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public abstract class TransportationProblemSolver {
    protected final int m, n;
    protected final int[][] cost, allocation;
    protected final int[] supply, demand;
    protected final double[] u, v;
    protected final int[][] delta;
    private Chain chain;
    private int minI, minJ;

    protected TransportationProblemSolver(TransportationProblem problem) {
        this.m = problem.supply.length;
        this.n = problem.demand.length;
        this.cost = problem.cost;
        this.supply = Arrays.copyOf(problem.supply, m);
        this.demand = Arrays.copyOf(problem.demand, n);
        this.allocation = new int[m][n];
        this.u = new double[m];
        this.v = new double[n];
        this.delta = new int[m][n];

        if (!isSolvable())
            throw new RuntimeException("Supplies do not match demands");
    }

    protected abstract void computeDelta();

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

    public int[][] getAllocation() {
        return allocation;
    }

    private boolean isSolvable() {
        int supplySum = 0, demandSum = 0;
        for (int i = 0; i < m; i++)
            supplySum += supply[i];

        for (int i = 0; i < n; i++)
            demandSum += demand[i];

        return supplySum == demandSum;
    }

    private void northwestCornerMethod() {
        int i = 0, j = 0;
        while (i < m && j < n) {
            int value = Math.min(supply[i], demand[j]);
            allocation[i][j] = value;
            supply[i] -= value;
            demand[j] -= value;
            if (supply[i] == 0) i++;
            if (demand[j] == 0) j++;
        }
    }

    private void computePotentials() {
        Arrays.fill(u, Double.NaN);
        Arrays.fill(v, Double.NaN);
        u[0] = 0;

        boolean updated;
        do {
            updated = false;
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    if (allocation[i][j] > 0) {
                        if (!Double.isNaN(u[i]) && Double.isNaN(v[j])) {
                            v[j] = cost[i][j] - u[i];
                            updated = true;
                        } else if (!Double.isNaN(v[j]) && Double.isNaN(u[i])) {
                            u[i] = cost[i][j] - v[j];
                            updated = true;
                        }
                    }
                }
            }
        } while (updated);
    }

    private boolean isOptimal() {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (delta[i][j] < 0)
                    return false;
            }
        }
        return true;
    }

    private void defineMinDeltaIndexes() {
        int minDelta = Integer.MAX_VALUE;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (delta[i][j] < minDelta) {
                    minDelta = delta[i][j];
                    minI = i;
                    minJ = j;
                }
            }
        }
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

    private int findNearestIndex(ChainElement element, boolean searchInColumn, boolean[][] visited) {
        int minDistance = Integer.MAX_VALUE;
        int nearestIndex = -1;
        int fixedIndex = searchInColumn ? element.j : element.i;

        if (searchInColumn) {
            for (int i = 0; i < m; i++) {
                if (allocation[i][fixedIndex] > 0 && i != element.i && !visited[i][fixedIndex]) {
                    int distance = Math.abs(i - element.i);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestIndex = i;
                    }
                }
            }
        } else {
            for (int j = 0; j < n; j++) {
                if (allocation[fixedIndex][j] > 0 && j != element.j && !visited[fixedIndex][j]) {
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

    private void adjustAllocation() {
        int minValue = chain.getMinValue();
        for (ChainElement chainElement : chain.chain) {
            if (chainElement.sign == -1)
                allocation[chainElement.i][chainElement.j] -= minValue;
            else
                allocation[chainElement.i][chainElement.j] += minValue;
        }
    }
}


class ChainElement {
    int i, j, cost, allocation;
    int sign = 1; // 1 is positive (+), -1 is negative (-)

    ChainElement(int i, int j, int cost, int allocation) {
        this.i = i;
        this.j = j;
        this.cost = cost;
        this.allocation = allocation;
    }

    public void setSign(int sign) {
        this.sign = sign;
    }
}

class Chain {
    ArrayList<ChainElement> chain = new ArrayList<>();

    public void add(ChainElement chainElement) {
        if (columnContainsPlus(chainElement.j) || rowContainsPlus(chainElement.i)) {
            chainElement.setSign(-1);
            chain.add(chainElement);
            return;
        }
        chain.add(chainElement);
    }

    private boolean columnContainsPlus(int column) {
        for (ChainElement chainElement : chain) {
            if (chainElement.j == column && chainElement.sign == 1)
                return true;
        }
        return false;
    }

    private boolean rowContainsPlus(int row) {
        for (ChainElement chainElement : chain) {
            if (chainElement.i == row && chainElement.sign == 1)
                return true;
        }
        return false;
    }

    public int getMinValue() {
        int minValue = Integer.MAX_VALUE;

        for (ChainElement chainElement : chain) {
            if (chainElement.sign == -1 && chainElement.allocation < minValue)
                minValue = chainElement.allocation;
        }

        return minValue;
    }

    public boolean isClosed() {
        if (chain.size() < 4)
            return false;

        ChainElement first = chain.getFirst();
        ChainElement last = chain.getLast();

        if (first.i != last.i && first.j != last.j)
            return false;

        for (int k = 0; k < chain.size() - 1; k++) {
            ChainElement current = chain.get(k);
            ChainElement next = chain.get(k + 1);

            if (current.i != next.i && current.j != next.j)
                return false;
        }

        return true;
    }
}