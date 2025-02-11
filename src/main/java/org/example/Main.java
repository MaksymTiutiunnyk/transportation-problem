package org.example;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        DataGenerator dataGenerator = new DataGenerator(10000, 10000, 10, 100, 100);
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

        int[][] cost = dataGenerator.cost;
        int[] supply = dataGenerator.supply;
        int[] demand = dataGenerator.demand;

//        for (int i = 0; i < cost.length; i++) {
//            System.out.println(Arrays.toString(cost[i]));
//        }
//        System.out.println(Arrays.toString(supply));
//        System.out.println(Arrays.toString(demand));

        startTime = System.currentTimeMillis();
        TransportProblem tp = new TransportProblem(cost, supply, demand);
        tp.solve();
        endTime = System.currentTimeMillis();
//        tp.printSolution();
        System.out.println("Time taken for transport problem: " + (endTime - startTime) + "ms");

        startTime = System.currentTimeMillis();
        ParallelTransportProblem ptp = new ParallelTransportProblem(cost, supply, demand);
        ptp.solve();
        endTime = System.currentTimeMillis();
//        ptp.printSolution();
        System.out.println("Time taken for parallel transport problem: " + (endTime - startTime) + "ms");

        System.out.println(SolutionValidator.compareSolutions(tp.getAllocation(), ptp.getAllocation()));
    }
}

//[7, 10, 5]
//        [4, 2, 1]
//        [9, 6, 6]
//        [95, 73, 45]
//        [89, 47, 77]
//Оптимальний розподіл:
//        [89, 0, 6]
//        [0, 2, 71]
//        [0, 45, 0]
//Time taken for transport problem: 3ms
//Оптимальний розподіл:
//        [89, 0, 6]
//        [0, 47, 26]
//        [0, 0, 45]