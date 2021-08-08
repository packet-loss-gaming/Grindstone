/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.state;

import com.google.common.collect.Lists;
import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.powers.NinjaPower;
import org.bukkit.entity.Arrow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NinjaState extends InternalGuildState {
    private final List<NinjaArrow> recentArrows = new ArrayList<>();

    private long nextGrapple = 0;
    private long nextSmokeBomb = 0;
    private long nextArrowBomb = 0;

    private final NinjaStateSettings settings;

    public NinjaState(long experience, NinjaStateSettings settings) {
        super(experience);
        this.settings = settings;
    }

    public List<Arrow> getRecentArrows() {
        if (recentArrows.isEmpty()) {
            return new ArrayList<>();
        }

        long lastArrowTime = recentArrows.get(recentArrows.size() - 1).getCreationTimeStamp();
        List<Arrow> arrows = recentArrows.stream()
                .map(arrow -> arrow.getIfStillRelevant(lastArrowTime))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        recentArrows.clear();

        if (arrows.size() > 0 && !hasPower(NinjaPower.MULTI_ARROW_BOMBS)) {
            return Lists.newArrayList(arrows.get(arrows.size() - 1));
        }

        return arrows;
    }

    public void addArrow(Arrow lastArrow) {
        recentArrows.add(new NinjaArrow(lastArrow));
    }

    public boolean canGrapple() {
        return nextGrapple == 0 || System.currentTimeMillis() >= nextGrapple;
    }

    public void grapple(long time) {
        nextGrapple = System.currentTimeMillis() + 300 + time;
    }

    public boolean canSmokeBomb() {
        return nextSmokeBomb == 0 || System.currentTimeMillis() >= nextSmokeBomb;
    }

    public void smokeBomb() {
        nextSmokeBomb = System.currentTimeMillis() + 4750;
    }

    public boolean canArrowBomb() {
        return nextArrowBomb == 0 || System.currentTimeMillis() >= nextArrowBomb;
    }

    public void arrowBomb(int numArrows) {
        nextArrowBomb = System.currentTimeMillis() + 1750 + (1250 * numArrows);
    }

    public boolean hasPower(NinjaPower power) {
        return hasLevelForPower(power);
    }

    @Override
    public GuildType getType() {
        return GuildType.NINJA;
    }

    @Override
    public NinjaStateSettings getSettings() {
        return settings;
    }
}
