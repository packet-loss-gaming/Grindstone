/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.economic.lottery;

public class LotteryWinner {
    private String name;
    private double amt;

    public LotteryWinner(String name, double amt) {
        this.name = name;
        this.amt = amt;
    }

    public String getName() {
        return name;
    }

    public double getAmt() {
        return amt;
    }
}
