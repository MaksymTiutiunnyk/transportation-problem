package org.example;

import java.util.Arrays;

public class SequentialTransportationProblemSolverSolver extends TransportationProblemSolver {
    public SequentialTransportationProblemSolverSolver(int[][] cost, int[] supply, int[] demand) {
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

    @Override
    protected boolean isOptimal() {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (delta[i][j] < 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
