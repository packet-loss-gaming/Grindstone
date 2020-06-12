package gg.packetloss.grindstone.city.engine.area.areas.RogueChaser;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class RogueChaserConfig extends ConfigurationBase {
    @Setting("chased.speed")
    public double chasedSpeed = 1.0;
    @Setting("chased.chance-of-jump")
    public double chanceOfJump = 100;
    @Setting("xp.player-count-multiplier")
    public int playerCountXpModifier = 15;
    @Setting("xp.base-exp")
    public int baseXp = 65;
    @Setting("xp.min-exp")
    public int minXp = 30;
}
