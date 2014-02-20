/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.guilds;

import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuildRank implements Comparable<GuildRank> {

    private String name;
    private int powerLevel;
    private List<String> guildPrivileges = new ArrayList<>();

    public GuildRank(String name, int powerLevel) {

        this.name = name;
        this.powerLevel = powerLevel;
    }

    public String getName() {

        return name;
    }

    public boolean isInRank(GuildPlayer player) {

        return player.getPowerLevel() >= powerLevel;
    }

    public boolean allows(String guildPrivilege) {

        return this.guildPrivileges.contains(guildPrivilege);
    }

    public List<String> getPrivileges() {

        return Collections.unmodifiableList(guildPrivileges);
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
    public int compareTo(GuildRank guildRank) {

        if (this.powerLevel == guildRank.powerLevel) {
            return 0;
        } else if (this.powerLevel > guildRank.powerLevel) {
            return -1;
        } else {
            return 1;
        }
    }
}
