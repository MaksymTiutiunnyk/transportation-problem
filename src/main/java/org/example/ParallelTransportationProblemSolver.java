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
        private final ParallelTransportationProblemSolver problemSolver;

        ComputeDeltaTask(int start, int end, ParallelTransportationProblemSolver problemSolver) {
            this.start = start;
            this.end = end;
            this.problemSolver = problemSolver;
        }

        @Override
        protected void compute() {
            if (end - start <= 2) {
                for (int i = start; i < end; i++) {
                    for (int j = 0; j < problemSolver.n; j++) {
                        if (problemSolver.allocation[i][j] == 0) {
                            problemSolver.delta[i][j] = problemSolver.cost[i][j] - (int) (problemSolver.u[i] + problemSolver.v[j]);
                        } else {
                            problemSolver.delta[i][j] = Integer.MAX_VALUE;
                        }
                    }
                }
            } else {
                int mid = (start + end) / 2;
                ComputeDeltaTask left = new ComputeDeltaTask(start, mid, problemSolver);
                ComputeDeltaTask right = new ComputeDeltaTask(mid, end, problemSolver);
                invokeAll(left, right);
            }
        }
    }
}
