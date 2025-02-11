package org.example;

public class Main {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        TransportationProblemGenerator transportationProblemGenerator = new TransportationProblemGenerator(10000, 10000, 10, 100, 100, 1);
        long endTime = System.currentTimeMillis();
        System.out.println("Time taken for generation: " + (endTime - startTime) + "ms");

//        int[][] cost = {
//                {3, 1, 4, 8},
//                {5, 1, 1, 4},
//                {9, 2, 5, 4},
//                {1, 9, 7, 3}
//        };
//        int[] supply = {26, 9, 28, 15};
//        int[] demand = {73, 1, 3, 1};

        int[][] cost = transportationProblemGenerator.cost;
        int[] supply = transportationProblemGenerator.supply;
        int[] demand = transportationProblemGenerator.demand;

//        for (int i = 0; i < cost.length; i++) {
//            System.out.println(Arrays.toString(cost[i]));
//        }
//        System.out.println(Arrays.toString(supply));
//        System.out.println(Arrays.toString(demand));

        startTime = System.currentTimeMillis();
        TransportationProblemSolver sequentialTransportationProblemSolver = new SequentialTransportationProblemSolverSolver(cost, supply, demand);
        sequentialTransportationProblemSolver.solve();
        endTime = System.currentTimeMillis();
//        sequentialTransportProblem.printSolution();
        System.out.println("Time taken for sequential transport problem: " + (endTime - startTime) + "ms");

        TransportationProblemSolver parallelTransportationProblemSolver = new ParallelTransportationProblemSolverSolver(cost, supply, demand);
        startTime = System.currentTimeMillis();
        parallelTransportationProblemSolver.solve();
        endTime = System.currentTimeMillis();
//        parallelTransportProblem.printSolution();
        System.out.println("Time taken for parallel transport problem: " + (endTime - startTime) + "ms");

        System.out.println(SolutionValidator.compareSolutions(sequentialTransportationProblemSolver.getAllocation(), parallelTransportationProblemSolver.getAllocation()));
    }
}