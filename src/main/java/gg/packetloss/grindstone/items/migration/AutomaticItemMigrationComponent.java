/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.migration;

import com.destroystokyo.paper.loottable.LootableInventory;
import com.google.gson.reflect.TypeToken;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePopEvent;
import gg.packetloss.grindstone.items.migration.migrations.CustomItemModelMigration;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.util.persistence.SingleFileFilesystemStateHelper;
import org.bukkit.Chunk;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.io.IOException;

@ComponentInformation(friendlyName = "Automatic Item Migration", desc = "Automatic Item Updates")
@Depend(components = {AdminComponent.class, PlayerStateComponent.class})
public class AutomaticItemMigrationComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private AdminComponent admin;
    @InjectComponent
    private PlayerStateComponent playerState;

    private static final MigrationManager manager = new MigrationManager();

    static {
        manager.add(new CustomItemModelMigration());
    }

    private AutomaticMigrationState state = new AutomaticMigrationState();
    private SingleFileFilesystemStateHelper<AutomaticMigrationState> stateHelper;

    @Override
    public void enable() {
        try {
            stateHelper = new SingleFileFilesystemStateHelper<>("item-migration.json", new TypeToken<>() { });
            stateHelper.load().ifPresent(loadedState -> state = loadedState);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (state.isOutOfDate()) {
            state.updateRevision();
        }

        CommandBook.registerEvents(this);
    }

    @Override
    public void disable() {
        try {
            stateHelper.save(state);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean processInventory(Inventory inv) {
        boolean[] updated = { false };
        for (int i = 0; i < inv.getSize(); ++i) {
            int finalI = i;
            manager.applyUpdates(inv.getItem(i)).ifPresent((updatedItem) -> {
                updated[0] = true;
                inv.setItem(finalI, updatedItem);
            });
        }
        return updated[0];
    }

    private void processChunk(Chunk chunk) {
        BlockState[] entities = chunk.getTileEntities();
        for (BlockState entity : entities) {
            if (!(entity instanceof Container)) {
                continue;
            }

            if (entity instanceof LootableInventory) {
                if (((LootableInventory) entity).hasLootTable()) {
                    continue;
                }
            }

            try {
                if (processInventory(((Container) entity).getSnapshotInventory())) {
                    entity.update();
                }
            } catch (Throwable t) {
                CommandBook.logger().severe("Failed to migrate tile entity: " + entity.getLocation().toString());
                t.printStackTrace();
            }

        }

        for (Entity entity : chunk.getEntities()) {
            // I'm not sure that this causes a problem, but let's not tempt fate
            if (entity instanceof Player) {
                continue;
            }

            if (entity instanceof Item) {
                manager.applyUpdates(((Item) entity).getItemStack()).ifPresent(((Item) entity)::setItemStack);
                continue;
            }

            if (entity instanceof InventoryHolder) {
                processInventory(((InventoryHolder) entity).getInventory());
            }

            if (entity instanceof LivingEntity) {
                EntityEquipment equipment = ((LivingEntity) entity).getEquipment();
                if (equipment == null) {
                    continue;
                }

                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    manager.applyUpdates(equipment.getItem(slot)).ifPresent(item -> equipment.setItem(slot, item));
                }
            }
        }

        state.markChunkProcessed(chunk);
    }

    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        if (event.isNewChunk()) {
            state.markChunkProcessed(chunk);
            return;
        }

        if (state.isChunkProcessed(chunk)) {
            return;
        }

        processChunk(chunk);
    }

    private void processPlayer(Player player) {
        processInventory(player.getInventory());
        processInventory(player.getEnderChest());

        state.markPlayerProcessed(player);
    }

    private boolean hasTempKind(Player player) {
        try {
            return playerState.hasTempKind(player) || admin.isAdmin(player);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (state.isPlayerProcessed(player)) {
            return;
        }

        // Wait for the PlayerStatePopEvent
        if (hasTempKind(player)) {
            return;
        }

        processPlayer(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerStatePop(PlayerStatePopEvent event) {
        if (!event.getKind().isTemporary()) {
            return;
        }

        Player player = event.getPlayer();
        if (state.isPlayerProcessed(player)) {
            return;
        }

        processPlayer(player);
    }
}
