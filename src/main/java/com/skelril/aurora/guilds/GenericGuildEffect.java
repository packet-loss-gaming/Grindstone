/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.guilds;

public abstract class GenericGuildEffect {

    protected final Guild master;
    protected final String name;
    protected final OperationFrequency frequency;

    public GenericGuildEffect(Guild master, String name, OperationFrequency frequency) {

        this.master = master;
        this.name = name;
        this.frequency = frequency;
    }

    public String getName() {

        return name;
    }

    public boolean check(OperationFrequency frequency) {

        return this.frequency.getLevel() >= frequency.getLevel();
    }

    public abstract void run();
}
