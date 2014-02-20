/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.guilds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Guild {

    private final String guildName;
    private List<GuildPlayer> players = new ArrayList<>();
    private List<GuildRank> ranks = new ArrayList<>();
    private ConcurrentHashMap<String, GenericGuildEffect> guildEffects = new ConcurrentHashMap<>();

    public Guild(String guildName) {

        this.guildName = guildName;
    }

    public String getGuildName() {

        return guildName;
    }

    public void addGuildEffects(GenericGuildEffect... guildEffects) {

        for (GenericGuildEffect guildEffect : guildEffects) {
            this.guildEffects.put(guildEffect.getName(), guildEffect);
        }
    }

    public GenericGuildEffect getGuildEffect(String effectName) {

        return guildEffects.get(effectName);
    }

    public void addGuildPlayers(GuildPlayer... guildPlayers) {

        Collections.addAll(this.players, guildPlayers);
    }

    public void run(OperationFrequency frequency) {

        for (GenericGuildEffect guildEffect : guildEffects.values()) {

            if (!guildEffect.check(frequency)) continue;

            guildEffect.run();
        }
    }
}
