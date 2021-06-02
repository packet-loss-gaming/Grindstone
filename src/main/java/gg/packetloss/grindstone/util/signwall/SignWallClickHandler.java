/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.signwall;

import org.bukkit.entity.Player;

public interface SignWallClickHandler<T> {
    public T handleLeftClick(Player player, T value);
    public default T handleRightClick(Player player, T value) {
        return handleLeftClick(player, value);
    }

    public default boolean allowNavigation() {
        return true;
    }
}
