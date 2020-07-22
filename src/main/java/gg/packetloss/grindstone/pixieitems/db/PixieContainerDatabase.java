/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.db;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PixieContainerDatabase {
    boolean addSource(int networkID, Location... locations);
    boolean addSink(int networkID, Set<String> itemNames, Location... locations);

    Optional<Integer> removeContainer(int networkID, Location... locations);

    Optional<PixieChestDetail> getDetailsAtLocation(Location location);
    Optional<Collection<Integer>> getNetworksInLocations(Location... locations);

    Optional<Collection<Integer>> getNetworksInChunk(Chunk chunk);
    Optional<Collection<PixieNetworkDefinition>> getChestsInNetworks(List<Integer> networks);
}
