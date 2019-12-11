package gg.packetloss.grindstone.util.signwall.flag;

import gg.packetloss.grindstone.util.flag.BooleanFlagState;
import gg.packetloss.grindstone.util.signwall.SignWallPainter;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;

public class BooleanFlagPainter<T extends Enum<T>> implements SignWallPainter<BooleanFlagState<T>> {
    public String getTitle(T flag) {
        String title = flag.toString();
        if (title.length() > 15) {
            title = title.substring(0, 15);
        }

        return WordUtils.capitalizeFully(title.replace("_", " "));
    }

    @Override
    public void paint(BooleanFlagState<T> value, Sign targetSign) {
        targetSign.setLine(1, getTitle(value.getFlag()));
        targetSign.setLine(2, value.isEnabled() ? ChatColor.DARK_GREEN + "Enabled" : ChatColor.RED + "Disabled");
        targetSign.update();
    }
}
