/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.warps;

import gg.packetloss.grindstone.util.NamespaceConstants;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface WarpDatabase {
    List<WarpPoint> getWarpsInNamespace(UUID namespace);

    default List<WarpPoint> getGlobalWarps() {
        return getWarpsInNamespace(NamespaceConstants.GLOBAL);
    }

    Optional<WarpPoint> getWarp(WarpQualifiedName qualifiedName);

    /**
     * @return the previous warp point if it exists
     */
    Optional<WarpPoint> setWarp(WarpQualifiedName qualifiedName, Location loc);

    /**
     * @return the deleted warp if it existed
     */
    Optional<WarpPoint> destroyWarp(WarpQualifiedName qualifiedName);

    boolean load();
    boolean save();
}
