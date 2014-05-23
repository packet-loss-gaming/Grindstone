/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.economic.lottery;

import com.skelril.aurora.util.player.GenericWealthStore;

import java.util.List;

/**
 * Author: Turtle9598
 */
public interface LotteryTicketDatabase {

    public boolean load();

    public boolean save();

    public void addTickets(String playerName, int count);

    public int getTickets(String playerName);

    public void clearTickets();

    public int getTicketCount();

    public List<GenericWealthStore> getTickets();
}
