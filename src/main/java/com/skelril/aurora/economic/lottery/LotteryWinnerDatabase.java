/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.economic.lottery;

import java.util.List;

/**
 * Author: Turtle9598
 */
public interface LotteryWinnerDatabase {

    public boolean load();

    public boolean save();

    public void addWinner(String name, double amount);

    public List<LotteryWinner> getRecentWinner(int limit);
}
