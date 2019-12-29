package gg.packetloss.grindstone.guild.state;

import com.google.common.collect.Lists;
import gg.packetloss.grindstone.guild.GuildLevel;
import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.powers.NinjaPower;
import org.bukkit.entity.Arrow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NinjaState extends InternalGuildState {
    private List<NinjaArrow> recentArrows = new ArrayList<>();

    private long nextGrapple = 0;
    private long nextSmokeBomb = 0;
    private long nextArrowBomb = 0;

    public NinjaState(long experience) {
        super(experience);
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
        nextSmokeBomb = System.currentTimeMillis() + 2000;
        nextArrowBomb = Math.max(System.currentTimeMillis() + 1000, nextArrowBomb);
    }

    public boolean canArrowBomb() {
        return nextArrowBomb == 0 || System.currentTimeMillis() >= nextArrowBomb;
    }

    public void arrowBomb() {
        nextArrowBomb = System.currentTimeMillis() + 3000;
    }

    public boolean hasPower(NinjaPower power) {
        return getExperience() >= GuildLevel.getExperienceForLevel(power.getUnlockLevel());
    }

    @Override
    public GuildType getType() {
        return GuildType.NINJA;
    }
}
