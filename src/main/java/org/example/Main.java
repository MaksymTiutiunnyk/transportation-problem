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
        TransportationProblem transportationProblem = TransportationProblemGenerator.generate(500, 500, 10, 100, 100, 1);

        long startTime, endTime, totalSequential = 0, totalParallel = 0;
        int numIterations = 1; // for performance measuring it should be changed to 20
        TransportationProblemSolver transportationProblemSolver = null;
        ParallelTransportationProblemSolver parallelTransportationProblemSolver = null;

        for (int i = 0; i <= numIterations; ++i) {
            startTime = System.currentTimeMillis();
            transportationProblemSolver = new TransportationProblemSolver(transportationProblem);
            transportationProblemSolver.solve();
            endTime = System.currentTimeMillis();

            if (i > 0)
                totalSequential += endTime - startTime;
        }

        System.out.println("Time taken for sequential transportation problem: " + (totalSequential / numIterations) + "ms");
//        TransportationProblemPrinter.printAllocation("Optimal solution: ", transportationProblemSolver.getAllocation());
        System.out.println("Total cost of delivery: " + transportationProblemSolver.getCost());

        for (int i = 0; i <= numIterations; ++i) {
            startTime = System.currentTimeMillis();
            parallelTransportationProblemSolver = new ParallelTransportationProblemSolver(transportationProblem);
            parallelTransportationProblemSolver.solve();
            endTime = System.currentTimeMillis();

            if (i > 0)
                totalParallel += endTime - startTime;
        }
        System.out.println("Time taken for parallel transportation problem: " + (totalParallel / numIterations) + "ms");
//        TransportationProblemPrinter.printAllocation("Optimal solution: ", parallelTransportationProblemSolver.getAllocation());
        System.out.println("Total cost of delivery: " + parallelTransportationProblemSolver.getCost());

        final boolean areEqual = SolutionValidator.compareSolutions(transportationProblemSolver.getAllocation(), parallelTransportationProblemSolver.getAllocation());
        if (areEqual)
            System.out.println("Solutions are equal");
        else
            System.out.println("Solutions are not equal");

        System.out.println("Acceleration: " + (double) totalSequential / (double) totalParallel);
    }
}