package org.example;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

public class ParallelTransportationProblemSolverSolver extends TransportationProblemSolver {
    private final ForkJoinPool pool = new ForkJoinPool();

    public ParallelTransportationProblemSolverSolver(int[][] cost, int[] supply, int[] demand) {
        super(cost, supply, demand);
    }

    @Override
    protected void computePotentials() {
        Arrays.fill(u, Double.NaN);
        Arrays.fill(v, Double.NaN);
        u[0] = 0;

        boolean updated;
        do {
            updated = false;
            updated = pool.invoke(new ComputePotentialsTask(0, m, updated));
        } while (updated);
    }

    private class ComputePotentialsTask extends RecursiveTask<Boolean> {
        private final int startRow;
        private final int endRow;
        private final boolean updated;

        public ComputePotentialsTask(int startRow, int endRow, boolean updated) {
            this.startRow = startRow;
            this.endRow = endRow;
            this.updated = updated;
        }

        @Override
        protected Boolean compute() {
            boolean localUpdated = updated;
            for (int i = startRow; i < endRow; i++) {
                for (int j = 0; j < n; j++) {
                    if (allocation[i][j] > 0) {
                        if (!Double.isNaN(u[i]) && Double.isNaN(v[j])) {
                            v[j] = cost[i][j] - u[i];
                            localUpdated = true;
                        } else if (!Double.isNaN(v[j]) && Double.isNaN(u[i])) {
                            u[i] = cost[i][j] - v[j];
                            localUpdated = true;
                        }
                    }
                }
            }
            return localUpdated;
        }
    }

    @Override
    protected void computeDelta() {
        pool.invoke(new ComputeDeltaTask(0, m, this));
    }

    private static class ComputeDeltaTask extends RecursiveAction {
        private final int start, end;
        private final ParallelTransportationProblemSolverSolver problem;

        ComputeDeltaTask(int start, int end, ParallelTransportationProblemSolverSolver problem) {
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

    @Override
    protected boolean isOptimal() {
        return pool.invoke(new IsOptimalTask(0, m, this));
    }

    private static class IsOptimalTask extends RecursiveTask<Boolean> {
        private final int start, end;
        private final ParallelTransportationProblemSolverSolver problem;

        IsOptimalTask(int start, int end, ParallelTransportationProblemSolverSolver problem) {
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
}
