/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class WorldLevelConfig extends ConfigurationBase {
    @Setting("demonic-ashes.per-y-chunk")
    public double demonicAshesPerYChunk = 25;
    @Setting("mobs.drop-table.item-count.max")
    public int mobsDropTableItemCountMax = 20;
    @Setting("mobs.drop-table.item-count.per-level")
    public double mobsDropTableItemCountPerLevel = .25;
    @Setting("mobs.drop-table.type-modifiers.default")
    public double mobsDropTableTypeModifiersDefault = 1;
    @Setting("mobs.drop-table.type-modifiers.creeper")
    public double mobsDropTableTypeModifiersCreeper = 2.5;
    @Setting("mobs.drop-table.type-modifiers.endermite")
    public double mobsDropTableTypeModifiersEndermite = .1;
    @Setting("mobs.drop-table.type-modifiers.silverfish")
    public double mobsDropTableTypeModifiersSilverfish = 1.5;
    @Setting("mobs.drop-table.type-modifiers.wither")
    public double mobsDropTableTypeModifiersWither = 5;
    @Setting("mobs.drop-table.sacrifice-value")
    public int mobsDropTableSacrificeValue = 256;
    @Setting("mobs.silverfish.level-enabled-at")
    public int mobsSilverfishLevelEnabledAt = 5;
    @Setting("mobs.silverfish.max-chance")
    public int mobsSilverfishMaxChance = 12;
    @Setting("mobs.silverfish.base-chance")
    public int mobsSilverfishBaseChance = 250;
    @Setting("mobs.silverfish.initial-silverfish-chance")
    public int mobsSilverfishInitialSilverfishChance = 3;
    @Setting("ores.per-level")
    public double oresPerLevel = 3;
    @Setting("adjustment.death-penalty")
    public double adjustmentDeathPenalty = .5;
}