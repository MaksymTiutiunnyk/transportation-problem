package org.example;

import java.util.ArrayList;

class Chain {
    ArrayList<ChainElement> chain = new ArrayList<>();

    public void add(ChainElement chainElement) {
        if (columnContainsPlus(chainElement.j) || rowContainsPlus(chainElement.i)) {
            chainElement.setSign(Sign.NEGATIVE);
            chain.add(chainElement);
            return;
        }
        chain.add(chainElement);
    }

    private boolean columnContainsPlus(int column) {
        for (ChainElement chainElement : chain) {
            if (chainElement.j == column && chainElement.sign == Sign.POSITIVE)
                return true;
        }
        return false;
    }

    private boolean rowContainsPlus(int row) {
        for (ChainElement chainElement : chain) {
            if (chainElement.i == row && chainElement.sign == Sign.POSITIVE)
                return true;
        }
        return false;
    }

    public int getMinValue() {
        int minValue = Integer.MAX_VALUE;

        for (ChainElement chainElement : chain) {
            if (chainElement.sign == Sign.NEGATIVE && chainElement.allocation < minValue)
                minValue = chainElement.allocation;
        }

        return minValue;
    }

    public boolean isClosed() {
        if (chain.size() < 4)
            return false;

        ChainElement first = chain.getFirst();
        ChainElement last = chain.getLast();

        if (first.i != last.i && first.j != last.j)
            return false;

        for (int k = 0; k < chain.size() - 1; ++k) {
            ChainElement current = chain.get(k);
            ChainElement next = chain.get(k + 1);

            if (current.i != next.i && current.j != next.j)
                return false;
        }

        return true;
    }
}
