package org.example;

public class SequentialTransportationProblemSolver extends TransportationProblemSolver {
    public SequentialTransportationProblemSolver(TransportationProblem problem) {
        super(problem);
    }

    @Override
    protected void computeDelta() {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (allocation[i][j] == 0) {
                    delta[i][j] = cost[i][j] - (int) (u[i] + v[j]);
                } else {
                    delta[i][j] = Integer.MAX_VALUE;
                }
            }
        }
    }
}
