/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.signwall;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;

import java.util.Arrays;

public interface SignWallPainter<ValueType> {
    public default void paintFirst(int numElementsPreceding, Sign targetSign) {
        targetSign.setLine(1, getHighlightColor(numElementsPreceding) + "<<");
        targetSign.update();
    }

    public void paint(ValueType value, Sign targetSign);

    public default void paintEmpty(Sign targetSign) {
        Arrays.fill(targetSign.getLines(), "");
        targetSign.update();
    }

    public default void paintLast(int numElementsSucceeding, Sign targetSign) {
        targetSign.setLine(1, getHighlightColor(numElementsSucceeding) + ">>");
        targetSign.update();
    }

    private String getHighlightColor(int numElements) {
        return numElements == 0 ? "" : ChatColor.BLUE.toString();
    }
}
