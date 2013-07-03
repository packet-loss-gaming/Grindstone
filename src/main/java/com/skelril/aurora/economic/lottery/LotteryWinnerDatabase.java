package com.skelril.aurora.economic.lottery;

import java.util.List;

/**
 * Author: Turtle9598
 */
public interface LotteryWinnerDatabase extends Iterable<String> {

    public boolean load();

    public boolean save(int max);

    public void addWinner(String name);

    public void addWinner(String name, long time);

    public List<String> getRecentWinner();

    public List<String> getRecentWinner(int limit);
}
