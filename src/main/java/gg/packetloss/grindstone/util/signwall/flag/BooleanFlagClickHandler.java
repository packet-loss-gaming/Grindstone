/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.signwall.flag;

import gg.packetloss.grindstone.util.flag.BooleanFlagState;
import gg.packetloss.grindstone.util.signwall.SignWallClickHandler;
import org.bukkit.entity.Player;

public class BooleanFlagClickHandler<T extends Enum<T>> implements SignWallClickHandler<BooleanFlagState<T>> {
    @Override
    public BooleanFlagState<T> handleLeftClick(Player player, BooleanFlagState<T> value) {
        value.toggle();
        return value;
    }
}
