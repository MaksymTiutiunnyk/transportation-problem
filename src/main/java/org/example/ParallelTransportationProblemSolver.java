package org.example;

import java.util.concurrent.atomic.AtomicBoolean;

public class ParallelTransportationProblemSolver extends TransportationProblemSolver {
    private final CornerStrategy strategy;

    public ParallelTransportationProblemSolver(TransportationProblem problem, CornerStrategy strategy) {
        super(problem);
        this.strategy = strategy;
    }

    public boolean solve(AtomicBoolean done) {
        cornerMethod();
        while (!done.get()) {
            isCurrentSolutionOptimal = true;
            computePotentials();
            isCurrentSolutionOptimal = conductDeltaOperations();
            if (isCurrentSolutionOptimal) {
                done.set(true);
                return true;
            }
            buildChain();
            adjustAllocation();
        }
        return false;
    }

    protected void cornerMethod() {
        int i = (strategy == CornerStrategy.SOUTHWEST || strategy == CornerStrategy.SOUTHEAST) ? m - 1 : 0;
        int j = (strategy == CornerStrategy.NORTHEAST || strategy == CornerStrategy.SOUTHEAST) ? n - 1 : 0;
        int iStep = (i == 0) ? 1 : -1;
        int jStep = (j == 0) ? 1 : -1;

        while (i >= 0 && i < m && j >= 0 && j < n) {
            int currentAllocation = Math.min(supply[i], demand[j]);
            allocation[i][j] = (currentAllocation == NO_SUPPLY) ? NOT_ALLOCATED : currentAllocation;

            if (supply[i] == demand[j]) {
                supply[i] = NO_SUPPLY;
                demand[j] = 0;
                i += iStep;
            } else {
                supply[i] -= currentAllocation;
                demand[j] -= currentAllocation;
                if (supply[i] == 0) supply[i] = NO_SUPPLY;
                if (demand[j] == 0) demand[j] = NO_DEMAND;
                if (supply[i] == NO_SUPPLY) i += iStep;
                else j += jStep;
            }
        }
    }
}
