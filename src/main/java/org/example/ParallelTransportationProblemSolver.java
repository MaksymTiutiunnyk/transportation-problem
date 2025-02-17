package org.example;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class ParallelTransportationProblemSolver extends TransportationProblemSolver {
    private final ForkJoinPool pool = new ForkJoinPool();

    public ParallelTransportationProblemSolver(TransportationProblem problem) {
        super(problem);
    }

    @Override
    protected void computeDelta() {
        pool.invoke(new ComputeDeltaTask(0, m, this));
    }

    private static class ComputeDeltaTask extends RecursiveAction {
        private final int start, end;
        private final ParallelTransportationProblemSolver problem;

        ComputeDeltaTask(int start, int end, ParallelTransportationProblemSolver problem) {
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
}
