package org.example;

import java.util.ArrayList;
import java.util.List;
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
            List<Future<ParallelTransportationProblemSolver>> futures = new ArrayList<>();

            for (CornerStrategy strategy : CornerStrategy.values()) {
                futures.add(executor.submit(() -> {
                    long start = System.currentTimeMillis();
                    ParallelTransportationProblemSolver solver = new ParallelTransportationProblemSolver(problem, strategy);
                    final boolean isFirstSolver = solver.solve(done);
                    long time = System.currentTimeMillis() - start;
                    System.out.println("Solving time: " + time + "ms; Start corner: " + strategy.name() + "; Winner: " + isFirstSolver);
                    return isFirstSolver ? solver : null;
                }));
            }

            ParallelTransportationProblemSolver first = null;

            for (Future<ParallelTransportationProblemSolver> f : futures) {
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

