package org.example;

enum Sign {POSITIVE, NEGATIVE}

class ChainElement {
    int i, j, cost, allocation;
    Sign sign = Sign.POSITIVE;

    ChainElement(int i, int j, int cost, int allocation) {
        this.i = i;
        this.j = j;
        this.cost = cost;
        this.allocation = allocation;
    }

    public void setSign(Sign sign) {
        this.sign = sign;
    }
}
