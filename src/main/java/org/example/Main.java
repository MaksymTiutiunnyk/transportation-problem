package org.example;

public class Main {
    public static void main(String[] args) {
//        A normal test case (you can comment in lines with printing an optimal solution and a total cost)
//        int[][] cost = {
//                {3, 1, 4, 8},
//                {5, 1, 1, 4},
//                {9, 2, 5, 4},
//                {1, 9, 7, 3}
//        };
//        int[] supply = {26, 9, 28, 15};
//        int[] demand = {73, 1, 3, 1};
//        TransportationProblem transportationProblem = new TransportationProblem(cost, supply, demand);

//        A degenerate test case (you can comment in lines with printing an optimal solution and a total cost)
//        int[][] cost = {
//                {8, 2, 6},
//                {10, 9, 9},
//                {7, 10, 7}
//        };
//        int[] supply = {100, 120, 80};
//        int[] demand = {100, 100, 100};
//        TransportationProblem transportationProblem = new TransportationProblem(cost, supply, demand);

//      Automatically generated (do not comment in lines with printing an optimal solution and a total cost if the problem is huge)
        TransportationProblem transportationProblem = TransportationProblemGenerator.generate(10, 100, 10, 100, 100, 1);

        long startTime = System.currentTimeMillis();
        TransportationProblemSolver transportationProblemSolver = new TransportationProblemSolver(transportationProblem);
        transportationProblemSolver.solve();
        long endTime = System.currentTimeMillis();

        System.out.println("Time taken for sequential transport problem: " + (endTime - startTime) + "ms");
//        TransportationProblemPrinter.printAllocation("Optimal solution: ", transportationProblemSolver.getAllocation());
//        System.out.println("Total cost of delivery: " + transportationProblemSolver.getCost());

        startTime = System.currentTimeMillis();
        ParallelTransportationProblemSolver parallelTransportationProblemSolver = new ParallelTransportationProblemSolver(transportationProblem);
        parallelTransportationProblemSolver.solve();
        endTime = System.currentTimeMillis();
        System.out.println("Time taken for parallel transport problem: " + (endTime - startTime) + "ms");

        final boolean areEqual = SolutionValidator.compareSolutions(transportationProblemSolver.getAllocation(), parallelTransportationProblemSolver.getAllocation());
        if (areEqual)
            System.out.println("Solutions are equal");
        else
            System.out.println("Solutions are not equal");
    }
}