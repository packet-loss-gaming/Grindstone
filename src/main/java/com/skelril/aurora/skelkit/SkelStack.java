package com.skelril.aurora.skelkit;

public class SkelStack {

    int type;
    int data;
    int amount;

    public SkelStack(int type) {

        this(type, 0, 1);
    }

    public SkelStack(int type, int amount) {

        this(type, 0, amount);
    }

    public SkelStack(int type, int data, int amount) {

        this.type = type;
        this.data = data;
        this.amount = amount;
    }

    public int getType() {

        return type;
    }

    public int getData() {

        return data;
    }

    public int getAmount() {

        return amount;
    }
}
