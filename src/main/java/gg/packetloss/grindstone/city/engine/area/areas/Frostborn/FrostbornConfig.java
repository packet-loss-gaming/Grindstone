package gg.packetloss.grindstone.city.engine.area.areas.Frostborn;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class FrostbornConfig extends ConfigurationBase {
    @Setting("loot.chance-of-dupe")
    public double chanceOfDupe = 15000;
    @Setting("block-restore.time")
    public int timeToRestore = 4;
}
