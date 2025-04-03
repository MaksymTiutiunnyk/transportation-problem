package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class ParallelTransportationProblemSolver extends TransportationProblemSolver {
    private final ForkJoinPool pool = new ForkJoinPool();

    protected ParallelTransportationProblemSolver(TransportationProblem problem) {
        super(problem);
    }

//  no acceleration, no slowing down
    @Override
    protected void computeDelta() {
        pool.invoke(new ComputeDeltaTask(0, m, this));
    }

    private class ComputeDeltaTask extends RecursiveAction {
        private final int start, end;
        private final ParallelTransportationProblemSolver problemSolver;

        ComputeDeltaTask(int start, int end, ParallelTransportationProblemSolver problemSolver) {
            this.start = start;
            this.end = end;
            this.problemSolver = problemSolver;
        }

        @Override
        protected void compute() {
            int THRESHOLD = 10;
            if (end - start <= THRESHOLD) {
                for (int i = start; i < end; ++i) {
                    for (int j = 0; j < problemSolver.n; ++j) {
                        if (problemSolver.allocation[i][j] == NOT_ALLOCATED)
                            problemSolver.delta[i][j] = problemSolver.cost[i][j] - (problemSolver.u[i] + problemSolver.v[j]);
                        else
                            problemSolver.delta[i][j] = UNDEFINED;
                    }
                }
            } else {
                int mid = (start + end) / 2;
                ComputeDeltaTask left = new ComputeDeltaTask(start, mid, problemSolver);
                ComputeDeltaTask right = new ComputeDeltaTask(mid, end, problemSolver);
                invokeAll(left, right); // tried to compute one of the tasks right here - no changes
            }
        }
    }

//  extreme slowing down (tried Fork/Join - no changes)
    @Override
    protected int findNearestIndex(ChainElement element, boolean searchInColumn, boolean[][] visited) {
        int fixedIndex = searchInColumn ? element.j : element.i;
        ResultWrapper result = new ResultWrapper();
        int numThreads = Runtime.getRuntime().availableProcessors();
        if (searchInColumn) {
            List<SearchInColumnTask> tasks = new ArrayList<>();

            int chunkSize = (m + numThreads - 1) / numThreads;

            for (int t = 0; t < numThreads; ++t) {
                int start = t * chunkSize;
                int end = Math.min(start + chunkSize, m);
                SearchInColumnTask task = new SearchInColumnTask(start, end, element, fixedIndex, visited, result);
                tasks.add(task);
                task.start();
            }

            for (SearchInColumnTask task : tasks) {
                try {
                    task.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return result.nearestIndex;
        }

        List<SearchInRowTask> tasks = new ArrayList<>();

        int chunkSize = (n + numThreads - 1) / numThreads;

        for (int t = 0; t < numThreads; ++t) {
            int start = t * chunkSize;
            int end = Math.min(start + chunkSize, n);
            SearchInRowTask task = new SearchInRowTask(start, end, element, fixedIndex, visited, result);
            tasks.add(task);
            task.start();
        }

        for (SearchInRowTask task : tasks) {
            try {
                task.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return result.nearestIndex;
    }

    private class SearchInColumnTask extends Thread {
        private final int start, end, fixedIndex;
        private final ChainElement element;
        private final boolean[][] visited;
        private final ResultWrapper result;

        public SearchInColumnTask(int start, int end, ChainElement element, int fixedIndex, boolean[][] visited, ResultWrapper result) {
            this.start = start;
            this.end = end;
            this.element = element;
            this.fixedIndex = fixedIndex;
            this.visited = visited;
            this.result = result;
        }

        @Override
        public void run() {
            int localMinDistance = Integer.MAX_VALUE;
            int localNearestIndex = -1;

            for (int i = start; i < end; ++i) {
                if (allocation[i][fixedIndex] != NOT_ALLOCATED && i != element.i && !visited[i][fixedIndex]) {
                    int distance = Math.abs(i - element.i);
                    if (distance < localMinDistance) {
                        localMinDistance = distance;
                        localNearestIndex = i;
                    }
                }
            }

            synchronized (result) {
                if (localMinDistance < result.minDistance) {
                    result.minDistance = localMinDistance;
                    result.nearestIndex = localNearestIndex;
                }
            }
        }
    }

    private class SearchInRowTask extends Thread {
        private final int start, end, fixedIndex;
        private final ChainElement element;
        private final boolean[][] visited;
        private final ResultWrapper result;

        public SearchInRowTask(int start, int end, ChainElement element, int fixedIndex, boolean[][] visited, ResultWrapper result) {
            this.start = start;
            this.end = end;
            this.element = element;
            this.fixedIndex = fixedIndex;
            this.visited = visited;
            this.result = result;
        }

        @Override
        public void run() {
            int localMinDistance = Integer.MAX_VALUE;
            int localNearestIndex = -1;

            for (int j = start; j < end; ++j) {
                if (allocation[fixedIndex][j] != NOT_ALLOCATED && j != element.j && !visited[fixedIndex][j]) {
                    int distance = Math.abs(j - element.j);
                    if (distance < localMinDistance) {
                        localMinDistance = distance;
                        localNearestIndex = j;
                    }
                }
            }

            synchronized (result) {
                if (localMinDistance < result.minDistance) {
                    result.minDistance = localMinDistance;
                    result.nearestIndex = localNearestIndex;
                }
            }
        }
    }

    private static class ResultWrapper {
        private int minDistance = Integer.MAX_VALUE;
        private int nearestIndex = -1;
    }

//  I've tried to parallelize this method earlier, so it's also included, but it changes nothing
    @Override
    protected void adjustAllocation() {
        int minValue = chain.getMinValue();

        pool.invoke(new AdjustAllocationTask(chain.chain, minValue, allocation, 0, chain.chain.size()));

//      it's also possible to parallelize this part, but it's quite small and won't change anything
        int degenerateCount = degenerateCount();
        if (degenerateCount > 0) {
            ArrayList<ChainElement> degenerateElements = new ArrayList<>();
            for (ChainElement chainElement : chain.chain) {
                if (allocation[chainElement.i][chainElement.j] == NOT_ALLOCATED)
                    degenerateElements.add(chainElement);
            }
            degenerateElements.sort((chainElement1, chainElement2) -> cost[chainElement2.i][chainElement2.j] - cost[chainElement1.i][chainElement1.j]);
            for (int i = 0; i < degenerateCount; ++i)
                allocation[degenerateElements.get(i).i][degenerateElements.get(i).j] = 0;
        }
    }

    private class AdjustAllocationTask extends RecursiveAction {
        private final List<ChainElement> chain;
        private final int minValue;
        private final int[][] allocation;
        private final int start, end;

        AdjustAllocationTask(List<ChainElement> chain, int minValue, int[][] allocation, int start, int end) {
            this.chain = chain;
            this.minValue = minValue;
            this.allocation = allocation;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            int THRESHOLD = 10;
            if (end - start <= THRESHOLD) {
                for (int i = start; i < end; ++i) {
                    ChainElement chainElement = chain.get(i);
                    if (chainElement.sign == Sign.NEGATIVE) {
                        if (allocation[chainElement.i][chainElement.j] - minValue == 0)
                            allocation[chainElement.i][chainElement.j] = NOT_ALLOCATED;
                        else
                            allocation[chainElement.i][chainElement.j] -= minValue;
                    }
                    else {
                        if (allocation[chainElement.i][chainElement.j] == NOT_ALLOCATED)
                            allocation[chainElement.i][chainElement.j] += minValue + 1;
                        else
                            allocation[chainElement.i][chainElement.j] += minValue;
                    }
                }
            } else {
                int mid = (start + end) / 2;
                AdjustAllocationTask left = new AdjustAllocationTask(chain, minValue, allocation, start, mid);
                AdjustAllocationTask right = new AdjustAllocationTask(chain, minValue, allocation, mid, end);
                invokeAll(left, right);
            }
        }
    }
}
