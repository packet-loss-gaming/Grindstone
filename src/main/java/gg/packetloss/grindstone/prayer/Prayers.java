/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gg.packetloss.grindstone.prayer.effect.passive.PotionPrayerEffect;
import gg.packetloss.grindstone.prayer.effect.passive.PotionPurgePrayerEffect;
import gg.packetloss.grindstone.util.StringUtil;
import org.bukkit.ChatColor;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;


public enum Prayers {
    // Unholy Prayers
    FIRE(35, false, ImmutableList.of(PrayerEffects.FIRE)),
    STARVATION(35, false, ImmutableList.of(PrayerEffects.STARVATION)),
    INVENTORY(35, false, ImmutableList.of(PrayerEffects.INVENTORY)),
    RACKET(35, false, ImmutableList.of(PrayerEffects.RACKET)),
    SMOKE(35, false, ImmutableList.of(PrayerEffects.SMOKE)),
    TNT(35, false, ImmutableList.of(PrayerEffects.TNT)),
    WALK(35, false, ImmutableList.of(new PotionPrayerEffect(PotionEffectType.SLOW, 2))),
    SLAP(35, false, ImmutableList.of(PrayerEffects.SLAP)),
    BUTTER_FINGERS(35, false, ImmutableList.of(PrayerEffects.BUTTER_FINGERS)),
    ARROW(35, false, ImmutableList.of(PrayerEffects.ARROW)),
    ZOMBIE(35, false, ImmutableList.of(PrayerEffects.ZOMBIE_SWARM)),
    DOOM(
        35,
        false,
        ImmutableList.of(
            PrayerEffects.SLAP,
            new PotionPrayerEffect(PotionEffectType.POISON, 2),
            PrayerEffects.TNT_FAKE
        )
    ),
    POISON(35, false, ImmutableList.of(new PotionPrayerEffect(PotionEffectType.POISON, 2))),
    BLINDNESS(35, false, ImmutableList.of(new PotionPrayerEffect(PotionEffectType.BLINDNESS, 1))),
    MUSHROOM(35, false, ImmutableList.of(new PotionPrayerEffect(PotionEffectType.CONFUSION, 1))),
    CANNON(35, false, ImmutableList.of(PrayerEffects.CANNON)),
    MERLIN(
        35,
        false,
        ImmutableList.of(
            PrayerEffects.FIRE,
            PrayerEffects.SMOKE,
            PrayerEffects.BUTTER_FINGERS,
            new PotionPrayerEffect(PotionEffectType.CONFUSION, 1)
        )
    ),
    ROCKET(35, false, ImmutableList.of(PrayerEffects.ROCKET)),
    GLASS_BOX(35, false, ImmutableList.of(PrayerEffects.GLASS_BOX)),
    DEADLY_POTION(35, false, ImmutableList.of(PrayerEffects.DEADLY_POTION)),

    // Holy Prayers
    ANTIFIRE(15, true, ImmutableList.of(new PotionPrayerEffect(PotionEffectType.FIRE_RESISTANCE, 2))),
    FIREBALL(75, true, ImmutableList.of(PrayerEffects.THROWN_FIREBALL)),
    HEALTH(15, true, ImmutableList.of(new PotionPrayerEffect(PotionEffectType.REGENERATION, 1))),
    SPEED(20, true, ImmutableList.of(new PotionPrayerEffect(PotionEffectType.SPEED, 2))),
    POWER(
        30,
        true,
        ImmutableList.of(
            PrayerEffects.INFINITE_FOOD,
            new PotionPrayerEffect(PotionEffectType.REGENERATION, 0),
            new PotionPrayerEffect(PotionEffectType.INCREASE_DAMAGE, 0),
            new PotionPrayerEffect(PotionEffectType.DAMAGE_RESISTANCE, 0),
            new PotionPrayerEffect(PotionEffectType.WATER_BREATHING, 0),
            new PotionPrayerEffect(PotionEffectType.FIRE_RESISTANCE, 0),
            new PotionPurgePrayerEffect(PotionEffectType.CONFUSION),
            new PotionPurgePrayerEffect(PotionEffectType.BLINDNESS),
            new PotionPurgePrayerEffect(PotionEffectType.WEAKNESS),
            new PotionPurgePrayerEffect(PotionEffectType.POISON),
            new PotionPurgePrayerEffect(PotionEffectType.SLOW)
        )
    ),
    GOD(
        60,
        true,
        ImmutableList.of(
            PrayerEffects.THROWN_FIREBALL,
            PrayerEffects.INFINITE_FOOD,
            new PotionPrayerEffect(PotionEffectType.INVISIBILITY, 0),
            new PotionPrayerEffect(PotionEffectType.REGENERATION, 1),
            new PotionPrayerEffect(PotionEffectType.INCREASE_DAMAGE, 1),
            new PotionPrayerEffect(PotionEffectType.DAMAGE_RESISTANCE, 1),
            new PotionPrayerEffect(PotionEffectType.WATER_BREATHING, 0),
            new PotionPrayerEffect(PotionEffectType.FIRE_RESISTANCE, 0),
            new PotionPurgePrayerEffect(PotionEffectType.CONFUSION),
            new PotionPurgePrayerEffect(PotionEffectType.BLINDNESS),
            new PotionPurgePrayerEffect(PotionEffectType.WEAKNESS),
            new PotionPurgePrayerEffect(PotionEffectType.POISON),
            new PotionPurgePrayerEffect(PotionEffectType.SLOW)
        )
    ),
    NIGHT_VISION(30, true, ImmutableList.of(new PotionPrayerEffect(PotionEffectType.NIGHT_VISION, 1, 20 * 15))),
    FLASH(
        40,
        true,
        ImmutableList.of(
            PrayerEffects.INFINITE_FOOD,
            new PotionPrayerEffect(PotionEffectType.SPEED, 6)
        )
    ),
    INVISIBILITY(30, true, ImmutableList.of(new PotionPrayerEffect(PotionEffectType.INVISIBILITY, 1))),
    DEADLYDEFENSE(75, true, ImmutableList.of(PrayerEffects.DEADLY_DEFENSE)),
    DIGGYDIGGY(30, true, ImmutableList.of(new PotionPrayerEffect(PotionEffectType.FAST_DIGGING, 1))),
    HULK(
        50,
        true,
        ImmutableList.of(
            PrayerEffects.INFINITE_FOOD,
            new PotionPrayerEffect(PotionEffectType.INCREASE_DAMAGE, 1),
            new PotionPrayerEffect(PotionEffectType.DAMAGE_RESISTANCE, 0)
        )
    ),
    HEALTH_BOOST(15, true, ImmutableList.of(new PotionPrayerEffect(PotionEffectType.HEALTH_BOOST, 4))),
    ABSORPTION(15, true, ImmutableList.of(new PotionPrayerEffect(PotionEffectType.ABSORPTION, 4)));

    private final int levelCost;
    private final boolean isHoly;
    private final ImmutableMap<PrayerEffectTrigger, ImmutableList<PrayerEffect>> effects;

    private Prayers(int levelCost, boolean isHoly, ImmutableList<PrayerEffect> effects) {
        this.levelCost = levelCost;
        this.isHoly = isHoly;

        // Create a working set map
        EnumMap<PrayerEffectTrigger, ArrayList<PrayerEffect>> tmpEffects = new EnumMap<>(PrayerEffectTrigger.class);
        for (PrayerEffectTrigger trigger : PrayerEffectTrigger.values()) {
            tmpEffects.put(trigger, new ArrayList<>());
        }

        for (PrayerEffect effect : effects) {
            tmpEffects.get(effect.getTrigger()).add(effect);
        }

        // Create an optimized immutable map
        ImmutableMap.Builder<PrayerEffectTrigger, ImmutableList<PrayerEffect>> effectsBuilder = ImmutableMap.builder();
        for (Map.Entry<PrayerEffectTrigger, ArrayList<PrayerEffect>> entry : tmpEffects.entrySet()) {
            effectsBuilder.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
        }
        this.effects = effectsBuilder.build();
    }

    public int getLevelCost() {
        return levelCost;
    }

    public boolean isHoly() {
        return isHoly;
    }

    public boolean isUnholy() {
        return !isHoly();
    }

    protected ImmutableMap<PrayerEffectTrigger, ImmutableList<PrayerEffect>> getEffects() {
        return effects;
    }

    public String getFormattedName() {
        return StringUtil.toUppercaseTitle(name());
    }

    public ChatColor getChatColor() {
        if (isHoly()) {
            return ChatColor.BLUE;
        } else {
            return ChatColor.RED;
        }
    }

    public String getPermissionName() {
        return name().toLowerCase().replaceAll("_", "");
    }
}
