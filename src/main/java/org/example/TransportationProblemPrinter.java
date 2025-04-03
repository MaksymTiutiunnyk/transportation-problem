package org.example;

import java.util.Arrays;

public class TransportationProblemPrinter {
    static public void printAllocation(String message, int[][] allocation) {
        System.out.println(message);
        for (int i = 0; i < allocation[0].length; ++i)
            System.out.println(Arrays.toString(allocation[i]));
    }
}
