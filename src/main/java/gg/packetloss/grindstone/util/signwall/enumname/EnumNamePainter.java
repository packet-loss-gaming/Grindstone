package gg.packetloss.grindstone.util.signwall.enumname;

import gg.packetloss.grindstone.util.signwall.SignWallPainter;
import org.apache.commons.lang.WordUtils;
import org.bukkit.block.Sign;

public class EnumNamePainter<T extends Enum<T>> implements SignWallPainter<T> {
    protected String getTitle(T enumValue) {
        String title = enumValue.name();
        if (title.length() > 15) {
            title = title.substring(0, 15);
        }

        return WordUtils.capitalizeFully(title.replace("_", " "));
    }

    @Override
    public void paint(T value, Sign targetSign) {
        targetSign.setLine(1, getTitle(value));
        targetSign.update();
    }
}
