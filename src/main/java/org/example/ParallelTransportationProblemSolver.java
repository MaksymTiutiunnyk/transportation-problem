package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ParallelTransportationProblemSolver extends TransportationProblemSolver {
    protected final int numThreads;
    protected final ExecutorService executor;

    public ParallelTransportationProblemSolver(TransportationProblem problem) {
        super(problem);
        this.numThreads = 16;
        executor = Executors.newFixedThreadPool(numThreads);
    }

    public void solve() {
        northwestCornerMethod();
        while (true) {
            isCurrentSolutionOptimal = true;
            computePotentials();
            isCurrentSolutionOptimal = conductDeltaOperations();
            if (isCurrentSolutionOptimal) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                            throw new RuntimeException("Executor did not terminate");
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("Executor did not terminate");
                }
                break;
            }
            buildChain();
            adjustAllocation();
        }
    }

    private static class DeltaTaskResult {
        int minDelta = Integer.MAX_VALUE;
        int minI = -1;
        int minJ = -1;
        boolean isCurrentSolutionOptimal = true;
    }

    protected boolean conductDeltaOperations() {
        int baseChunkSize = m / numThreads;
        int remainder = m % numThreads;
        int currentRow = 0;
        List<Future<DeltaTaskResult>> futures = new ArrayList<>();

        for (int t = 0; t < numThreads; ++t) {
            int rowsInThisChunk = baseChunkSize + (t < remainder ? 1 : 0);
            final int startRow = currentRow;
            final int endRow = startRow + rowsInThisChunk;
            currentRow = endRow;

            futures.add(executor.submit(() -> {
                DeltaTaskResult localResult = new DeltaTaskResult();
                for (int i = startRow; i < endRow; ++i) {
                    for (int j = 0; j < n; ++j) {
                        if (allocation[i][j] == NOT_ALLOCATED) {
                            int deltaVal = cost[i][j] - (u[i] + v[j]);
                            delta[i][j] = deltaVal;

                            if (deltaVal < 0)
                                localResult.isCurrentSolutionOptimal = false;

                            if (deltaVal < localResult.minDelta) {
                                localResult.minDelta = deltaVal;
                                localResult.minI = i;
                                localResult.minJ = j;
                            }
                        } else {
                            delta[i][j] = UNDEFINED;
                        }
                    }
                }
                return localResult;
            }));
        }

        int minDelta = Integer.MAX_VALUE;
        try {
            for (Future<DeltaTaskResult> future : futures) {
                DeltaTaskResult result = future.get();
                if (!result.isCurrentSolutionOptimal) isCurrentSolutionOptimal = false;
                if (result.minDelta < minDelta) {
                    minDelta = result.minDelta;
                    minI = result.minI;
                    minJ = result.minJ;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return isCurrentSolutionOptimal;
    }

    protected void adjustAllocation() {
        int minValue = chain.getMinValue();
        int chainSize = chain.chain.size();
        int baseChunkSize = chainSize / numThreads;
        int remainder = chainSize % numThreads;
        int currentRow = 0;

        List<Future<Void>> futures = new ArrayList<>();

        for (int t = 0; t < numThreads; ++t) {
            int rowsInThisChunk = baseChunkSize + (t < remainder ? 1 : 0);
            int startRow = currentRow;
            int endRow = startRow + rowsInThisChunk;
            currentRow = endRow;

            futures.add(executor.submit(() -> {
                for (int idx = startRow; idx < endRow; ++idx) {
                    ChainElement element = chain.chain.get(idx);
                    int i = element.i;
                    int j = element.j;

                    if (element.sign == Sign.NEGATIVE) {
                        if (allocation[i][j] - minValue == 0)
                            allocation[i][j] = NOT_ALLOCATED;
                        else
                            allocation[i][j] -= minValue;
                    } else {
                        if (allocation[i][j] == NOT_ALLOCATED)
                            allocation[i][j] += minValue + 1;
                        else
                            allocation[i][j] += minValue;
                    }
                }
                return null;
            }));
        }

        try {
            for (Future<Void> future : futures)
                future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        int degenerateCount = degenerateCount();
        if (degenerateCount > 0) {
            ArrayList<ChainElement> degenerateElements = new ArrayList<>();
            for (ChainElement chainElement : chain.chain) {
                if (allocation[chainElement.i][chainElement.j] == NOT_ALLOCATED)
                    degenerateElements.add(chainElement);
            }
            degenerateElements.sort((a, b) -> cost[b.i][b.j] - cost[a.i][a.j]);
            for (int i = 0; i < degenerateCount; ++i)
                allocation[degenerateElements.get(i).i][degenerateElements.get(i).j] = 0;
        }
    }
}
