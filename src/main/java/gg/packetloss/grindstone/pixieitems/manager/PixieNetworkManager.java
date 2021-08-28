/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.manager;

import gg.packetloss.grindstone.pixieitems.PixieSinkVariant;
import gg.packetloss.grindstone.pixieitems.TransactionBroker;
import gg.packetloss.grindstone.pixieitems.db.PixieNetworkDetail;
import gg.packetloss.grindstone.util.task.promise.TaskFuture;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PixieNetworkManager {
    TaskFuture<Optional<PixieNetworkDetail>> createNetwork(UUID namespace, String name, Location origin);
    TaskFuture<Optional<PixieNetworkDetail>> selectNetwork(UUID namespace, String name);
    TaskFuture<Optional<PixieNetworkDetail>> selectNetwork(int networkID);
    TaskFuture<List<PixieNetworkDetail>> selectNetworks(UUID namespace);

    TaskFuture<NewSourceResult> addSource(int networkID, Block block);
    TaskFuture<NewSinkResult> addSink(int networkID, Block block, PixieSinkVariant variant);

    boolean maybeExpandChest(Block block);

    TaskFuture<Void> removeContainer(Location... locations);

    Optional<Integer> getNetworkFromSourceContainers(Block... blocks);

    void sourceItems(TransactionBroker broker, int networkID, Inventory inventory);

    void handleChunkLoad(Chunk chunk);
    void handleChunkUnload(Chunk chunk);
}
