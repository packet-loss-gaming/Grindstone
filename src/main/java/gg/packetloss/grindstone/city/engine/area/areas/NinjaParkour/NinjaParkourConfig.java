package gg.packetloss.grindstone.city.engine.area.areas.NinjaParkour;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class NinjaParkourConfig extends ConfigurationBase {
    @Setting("columns.count")
    public int columnCount = 10;
    @Setting("columns.min-range")
    public int columnMinRange = 1;
    @Setting("columns.max-range")
    public int columnMaxRange = 5;
}
