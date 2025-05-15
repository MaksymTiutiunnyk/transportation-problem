package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

enum CornerStrategy {
    NORTHWEST, NORTHEAST, SOUTHWEST, SOUTHEAST
}

public class ParallelTransportationSolver {
    public static ParallelTransportationProblemSolver solveParallel(TransportationProblem problem) throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
            AtomicBoolean done = new AtomicBoolean(false);
            List<Callable<ParallelTransportationProblemSolver>> tasks = new ArrayList<>();

            for (CornerStrategy strategy : CornerStrategy.values()) {
                tasks.add(() -> {
                    long start = System.currentTimeMillis();
                    ParallelTransportationProblemSolver solver = new ParallelTransportationProblemSolver(problem, strategy);
                    final boolean isFirstSolver = solver.solve(done);
                    long time = System.currentTimeMillis() - start;
                    System.out.println("Solving time: " + time + "ms; Start corner: " + strategy.name() + "; Winner: " + isFirstSolver);
                    return isFirstSolver ? solver : null;
                });
            }

            ParallelTransportationProblemSolver first = null;
            List<Future<ParallelTransportationProblemSolver>> results = executor.invokeAll(tasks);

            for (Future<ParallelTransportationProblemSolver> f : results) {
                ParallelTransportationProblemSolver result = f.get();
                if (result != null) {
                    first = result;
                    break;
                }
            }
            return first;
        }
    }
}

