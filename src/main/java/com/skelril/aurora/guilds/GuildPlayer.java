/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.guilds;

import org.apache.commons.lang.Validate;

public class GuildPlayer implements Comparable<GuildPlayer> {

    private String name;
    private int powerLevel;

    public GuildPlayer(String name, int powerLevel) {

        this.name = name;
        this.powerLevel = powerLevel;
    }

    public String getName() {

        return name;
    }

    public int getPowerLevel() {

        return powerLevel;
    }

    public void setPowerLevel(int powerLevel) {

        Validate.isTrue(powerLevel > 0, "\'powerLevel\' must be a positive integer greater than 0");
        this.powerLevel = powerLevel;
    }

    public void incrementPowerLevel(int amount) {

        Validate.isTrue(amount > 0, "\'amount\' must be a positive integer greater than 0");
        powerLevel += amount;
    }

    @Override
    public int compareTo(GuildPlayer guildPlayer) {

        if (this.powerLevel == guildPlayer.powerLevel) {
            return 0;
        } else if (this.powerLevel > guildPlayer.powerLevel) {
            return -1;
        } else {
            return 1;
        }
    }
}
