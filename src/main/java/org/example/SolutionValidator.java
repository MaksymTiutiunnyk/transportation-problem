package org.example;

public class SolutionValidator {
    public static boolean compareSolutions(int[][] alloc1, int[][] alloc2) {
        if (alloc1.length != alloc2.length || alloc1[0].length != alloc2[0].length) {
            return false;
        }
        for (int i = 0; i < alloc1.length; i++) {
            for (int j = 0; j < alloc1[0].length; j++) {
                if (alloc1[i][j] != alloc2[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }
}
