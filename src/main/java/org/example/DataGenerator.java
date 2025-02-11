package org.example;

import java.util.Random;

public class DataGenerator {
    int[][] cost;
    int[] supply;
    int[] demand;

    DataGenerator(int m, int n, int maxCost, int maxSupply, int maxDemand) {
        Random rand = new Random();
        cost = new int[m][n];
        supply = new int[m];
        demand = new int[n];

        // Генерація вартостей перевезень
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                cost[i][j] = rand.nextInt(maxCost) + 1;
            }
        }

        // Початкове випадкове заповнення supply та demand
        int totalSupply = 0, totalDemand = 0;
        for (int i = 0; i < m; i++) {
            supply[i] = rand.nextInt(maxSupply - 1) + 1; // Мінімум 1
            totalSupply += supply[i];
        }
        for (int j = 0; j < n; j++) {
            demand[j] = rand.nextInt(maxDemand - 1) + 1; // Мінімум 1
            totalDemand += demand[j];
        }

        // Балансування supply і demand
        int balance = totalSupply - totalDemand;
        while (balance != 0) {
            if (balance > 0) {
                // Зменшуємо випадковий supply, якщо можливо
                int i = rand.nextInt(m);
                if (supply[i] > 1) { // Забезпечуємо, що не буде нулів
                    supply[i]--;
                    balance--;
                }
            } else {
                // Зменшуємо випадковий demand, якщо можливо
                int j = rand.nextInt(n);
                if (demand[j] > 1) { // Забезпечуємо, що не буде нулів
                    demand[j]--;
                    balance++;
                }
            }
        }
    }
}
