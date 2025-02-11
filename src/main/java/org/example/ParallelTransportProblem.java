package org.example;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

public class ParallelTransportProblem {
    private final int m;
    private final int n;
    private final int[][] cost;
    private final int[][] allocation;
    private final boolean[][] visited;
    private final int[] supply;
    private final int[] demand;
    private final double[] u;
    private final double[] v;
    private final int[][] delta;
    private final ForkJoinPool pool = new ForkJoinPool();

    public ParallelTransportProblem(int[][] cost, int[] supply, int[] demand) {
        this.m = supply.length;
        this.n = demand.length;
        this.cost = cost;
        this.supply = Arrays.copyOf(supply, m);
        this.demand = Arrays.copyOf(demand, n);
        this.allocation = new int[m][n];
        this.u = new double[m];
        this.v = new double[n];
        this.delta = new int[m][n];
        this.visited = new boolean[m][n];
        for (int i = 0; i < m; i++) {
            Arrays.fill(visited[i], false);
        }
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

//    private void computePotentials() {
//        Arrays.fill(u, Double.NaN);
//        Arrays.fill(v, Double.NaN);
//        u[0] = 0;
//
//        boolean updated;
//        do {
//            updated = false;
//            pool.invoke(new ComputePotentialsTask(0, m, this));
//        } while (updated);
//    }
//
//    private static class ComputePotentialsTask extends RecursiveAction {
//        private final int start, end;
//        private final ParallelTransportProblem problem;
//
//        ComputePotentialsTask(int start, int end, ParallelTransportProblem problem) {
//            this.start = start;
//            this.end = end;
//            this.problem = problem;
//        }
//
//        @Override
//        protected void compute() {
//            if (end - start <= 2) {
//                for (int i = start; i < end; i++) {
//                    for (int j = 0; j < problem.n; j++) {
//                        if (problem.allocation[i][j] > 0) {
//                            if (!Double.isNaN(problem.u[i]) && Double.isNaN(problem.v[j])) {
//                                problem.v[j] = problem.cost[i][j] - problem.u[i];
//                            } else if (!Double.isNaN(problem.v[j]) && Double.isNaN(problem.u[i])) {
//                                problem.u[i] = problem.cost[i][j] - problem.v[j];
//                            }
//                        }
//                    }
//                }
//            } else {
//                int mid = (start + end) / 2;
//                ComputePotentialsTask left = new ComputePotentialsTask(start, mid, problem);
//                ComputePotentialsTask right = new ComputePotentialsTask(mid, end, problem);
//                invokeAll(left, right);
//            }
//        }
//    }

    private void computeDelta() {
        pool.invoke(new ComputeDeltaTask(0, m, this));
    }

    private static class ComputeDeltaTask extends RecursiveAction {
        private final int start, end;
        private final ParallelTransportProblem problem;

        ComputeDeltaTask(int start, int end, ParallelTransportProblem problem) {
            this.start = start;
            this.end = end;
            this.problem = problem;
        }

        @Override
        protected void compute() {
            if (end - start <= 2) {
                for (int i = start; i < end; i++) {
                    for (int j = 0; j < problem.n; j++) {
                        if (problem.allocation[i][j] == 0) {
                            problem.delta[i][j] = problem.cost[i][j] - (int) (problem.u[i] + problem.v[j]);
                        } else {
                            problem.delta[i][j] = Integer.MAX_VALUE;
                        }
                    }
                }
            } else {
                int mid = (start + end) / 2;
                ComputeDeltaTask left = new ComputeDeltaTask(start, mid, problem);
                ComputeDeltaTask right = new ComputeDeltaTask(mid, end, problem);
                invokeAll(left, right);
            }
        }
    }

    private boolean isOptimal() {
        return pool.invoke(new IsOptimalTask(0, m, this));
    }

    private static class IsOptimalTask extends RecursiveTask<Boolean> {
        private final int start, end;
        private final ParallelTransportProblem problem;

        IsOptimalTask(int start, int end, ParallelTransportProblem problem) {
            this.start = start;
            this.end = end;
            this.problem = problem;
        }

        @Override
        protected Boolean compute() {
            if (end - start <= 2) {
                for (int i = start; i < end; i++) {
                    for (int j = 0; j < problem.n; j++) {
                        if (problem.delta[i][j] < 0) {
                            return false;
                        }
                    }
                }
                return true;
            } else {
                int mid = (start + end) / 2;
                IsOptimalTask left = new IsOptimalTask(start, mid, problem);
                IsOptimalTask right = new IsOptimalTask(mid, end, problem);
                left.fork();
                boolean rightResult = right.compute();
                boolean leftResult = left.join();
                return leftResult && rightResult;
            }
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
