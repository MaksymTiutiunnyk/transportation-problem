package org.example;

public class TransportationProblem {
    final int[][] cost;
    final int[] supply;
    final int[] demand;

    TransportationProblem(int[][] cost, int[] supply, int[] demand) {
        this.cost = cost;
        this.supply = supply;
        this.demand = demand;
    }
}
