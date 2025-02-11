package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public abstract class TransportationProblemSolver {
    protected final int m;
    protected final int n;
    protected final int[][] cost;
    protected final int[][] allocation;
    protected final int[] supply;
    protected final int[] demand;
    protected final double[] u;
    protected final double[] v;
    protected final int[][] delta;

    protected TransportationProblemSolver(int[][] cost, int[] supply, int[] demand) {
        this.m = supply.length;
        this.n = demand.length;
        this.cost = cost;
        this.supply = Arrays.copyOf(supply, m);
        this.demand = Arrays.copyOf(demand, n);
        this.allocation = new int[m][n];
        this.u = new double[m];
        this.v = new double[n];
        this.delta = new int[m][n];
    }

    protected abstract void computePotentials();
    protected abstract void computeDelta();
    protected abstract boolean isOptimal();

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

    private void adjustAllocation() {
        int minI = 0, minJ = 0, minDelta = Integer.MAX_VALUE;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (delta[i][j] < minDelta) {
                    minDelta = delta[i][j];
                    minI = i;
                    minJ = j;
                }
            }
        }

        Chain chain = new Chain();
        findCycle(chain, new ChainElement(minI, minJ, cost[minI][minJ], allocation[minI][minJ]));

        int minValue = chain.getMinValue();
        for (ChainElement chainElement : chain.chain) {
            if (chainElement.sign == -1)
                allocation[chainElement.i][chainElement.j] -= minValue;
            else
                allocation[chainElement.i][chainElement.j] += minValue;
        }
    }

    private void findCycle(Chain chain, ChainElement firstElement) {
        boolean isSearchInColumn = true;
        ChainElement currentElement;
        int col;
        int row;
        int nearestCol;
        int nearestRow;
        int minDistance;
        boolean[][] visited = new boolean[m][n];

        for (int i = 0; i < m; i++) {
            Arrays.fill(visited[i], false);
        }

        Stack<ChainElement> stack = new Stack<>();
        stack.push(firstElement);
        chain.add(firstElement);

        while (!chain.isClosed()) {
            isSearchInColumn = !isSearchInColumn;

            currentElement = stack.peek();
            visited[currentElement.i][currentElement.j] = true;

            col = currentElement.j;
            row = currentElement.i;
            nearestCol = -1;
            nearestRow = -1;
            minDistance = Integer.MAX_VALUE;

            if (!isSearchInColumn) {
                for (int j = 0; j < n; j++) {
                    if (allocation[row][j] > 0 && j != col && !visited[row][j]) {
                        int distance = Math.abs(j - col);
                        if (distance < minDistance) {
                            minDistance = distance;
                            nearestCol = j;
                        }
                    }
                }

                if (nearestCol != -1) {
                    ChainElement newElement = new ChainElement(row, nearestCol, cost[row][nearestCol], allocation[row][nearestCol]);
                    stack.push(newElement);
                    chain.add(newElement);
                } else {
                    chain.chain.remove(stack.pop());
                }
                continue;
            }

            for (int i = 0; i < m; i++) {
                if (allocation[i][col] > 0 && i != row && !visited[i][col]) {
                    int distance = Math.abs(i - row);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestRow = i;
                    }
                }
            }

            if (nearestRow != -1) {
                ChainElement newElement = new ChainElement(nearestRow, col, cost[nearestRow][col], allocation[nearestRow][col]);
                chain.add(newElement);
                stack.push(newElement);
            } else {
                chain.chain.remove(stack.pop());
            }
        }
    }

    public void solve() {
        northwestCornerMethod();
        while (true) {
            computePotentials();
            computeDelta();
            if (isOptimal()) {
                break;
            }
            adjustAllocation();
        }
    }

    public void printSolution() {
        System.out.println("Оптимальний розподіл:");
        for (int i = 0; i < m; i++) {
            System.out.println(Arrays.toString(allocation[i]));
        }
    }

    public int[][] getAllocation() {
        return allocation;
    }
}

class ChainElement {
    int i, j, cost, allocation, sign = 1;

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
            if (chainElement.j == column && chainElement.sign == 1) {
                return true;
            }
        }
        return false;
    }

    private boolean rowContainsPlus(int row) {
        for (ChainElement chainElement : chain) {
            if (chainElement.i == row && chainElement.sign == 1) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return chain.size();
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
        if (chain.size() < 4) {
            return false;
        }

        ChainElement first = chain.getFirst();
        ChainElement last = chain.getLast();

        if (first.i != last.i && first.j != last.j) {
            return false;
        }

        for (int k = 0; k < chain.size() - 1; k++) {
            ChainElement current = chain.get(k);
            ChainElement next = chain.get(k + 1);

            if (current.i != next.i && current.j != next.j) {
                return false;
            }
        }

        return true;
    }
}