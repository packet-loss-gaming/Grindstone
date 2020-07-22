/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.db;

import org.bukkit.Location;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PixieNetworkDatabase {
    Optional<PixieNetworkDetail> createNetwork(UUID namespace, String name, Location origin);
    Optional<PixieNetworkDetail> selectNetwork(UUID namespace, String name);
    Optional<PixieNetworkDetail> selectNetwork(int networkID);
    List<PixieNetworkDetail> selectNetworks(UUID namespace);
}
