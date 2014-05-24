/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.economic.store;

public class ItemTransaction {

    private final String player;
    private final String item;
    private final int amount;

    public ItemTransaction(String player, String item, int amount) {
        this.player = player;
        this.item = item;
        this.amount = amount;
    }

    public String getPlayer() {
        return player;
    }

    public String getItem() {
        return item;
    }

    public int getAmount() {
        return amount;
    }
}
