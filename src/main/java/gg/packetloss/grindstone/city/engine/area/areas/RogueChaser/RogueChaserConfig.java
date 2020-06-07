package gg.packetloss.grindstone.city.engine.area.areas.RogueChaser;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class RogueChaserConfig extends ConfigurationBase {
    @Setting("chased.speed")
    public double chasedSpeed = 1.0;
    @Setting("chased.chance-of-jump")
    public double chanceOfJump = 100;
}
