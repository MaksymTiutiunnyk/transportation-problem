package org.example;

public class Main {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        TransportationProblem transportationProblem = TransportationProblemGenerator.generate(1000, 10000, 10, 100, 100, 1);
        long endTime = System.currentTimeMillis();
        System.out.println("Time taken for generation: " + (endTime - startTime) + "ms");

        // на цих даних тестував послідовний алгоритм на правильність роботи
//        int[][] cost = {
//                {3, 1, 4, 8},
//                {5, 1, 1, 4},
//                {9, 2, 5, 4},
//                {1, 9, 7, 3}
//        };
//        int[] supply = {26, 9, 28, 15};
//        int[] demand = {73, 1, 3, 1};
//        TransportationProblem transportationProblem = new TransportationProblem(cost, supply, demand);

        startTime = System.currentTimeMillis();
        TransportationProblemSolver sequentialTransportationProblemSolver = new SequentialTransportationProblemSolver(transportationProblem);
        sequentialTransportationProblemSolver.solve();
        endTime = System.currentTimeMillis();
        System.out.println("Time taken for sequential transport problem: " + (endTime - startTime) + "ms");

        startTime = System.currentTimeMillis();
        TransportationProblemSolver parallelTransportationProblemSolver = new ParallelTransportationProblemSolver(transportationProblem);
        parallelTransportationProblemSolver.solve();
        endTime = System.currentTimeMillis();
        System.out.println("Time taken for parallel transport problem: " + (endTime - startTime) + "ms");

        final boolean areEqual = SolutionValidator.compareSolutions(sequentialTransportationProblemSolver.getAllocation(), parallelTransportationProblemSolver.getAllocation());

        if (areEqual) {
            System.out.println("Solutions are equal");
        } else {
            System.out.println("Solutions are not equal");
        }
    }
}