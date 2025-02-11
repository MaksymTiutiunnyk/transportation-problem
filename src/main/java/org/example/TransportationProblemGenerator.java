package org.example;

import java.util.Random;

public class TransportationProblemGenerator {
    int[][] cost;
    int[] supply;
    int[] demand;

    TransportationProblemGenerator(int m, int n, int maxCost, int maxSupply, int maxDemand, long seed) {
        Random rand = new Random(seed);
        cost = new int[m][n];
        supply = new int[m];
        demand = new int[n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                cost[i][j] = rand.nextInt(maxCost) + 1;
            }
        }

        int totalSupply = 0, totalDemand = 0;
        for (int i = 0; i < m; i++) {
            supply[i] = rand.nextInt(maxSupply - 1) + 1;
            totalSupply += supply[i];
        }
        for (int j = 0; j < n; j++) {
            demand[j] = rand.nextInt(maxDemand - 1) + 1; // Мінімум 1
            totalDemand += demand[j];
        }

        int balance = totalSupply - totalDemand;
        while (balance != 0) {
            if (balance > 0) {
                int i = rand.nextInt(m);
                if (supply[i] > 1) {
                    supply[i]--;
                    balance--;
                }
            } else {
                int j = rand.nextInt(n);
                if (demand[j] > 1) {
                    demand[j]--;
                    balance++;
                }
            }
        }
    }
}
