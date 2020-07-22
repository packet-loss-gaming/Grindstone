/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.block;

import com.google.gson.Gson;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.exceptions.UnstorableBlockStateException;
import org.bukkit.Server;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Block State", desc = "Block state management")
public class BlockStateComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private Path statesDir;

    private Gson gson = new Gson();

    private BlockStateCollection[] states = new BlockStateCollection[BlockStateKind.values().length];

    @Override
    public void enable() {
        try {
            Path baseDir = Path.of(inst.getDataFolder().getPath(), "state");
            statesDir = Files.createDirectories(baseDir.resolve("states/blocks"));

            loadBlockStates();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @Override
    public void disable() {
        saveBlockStates();
    }

    private Path getStateFile(BlockStateKind kind) {
        return statesDir.resolve(kind.name().toLowerCase() + ".json");
    }

    private void loadBlockStates() {
        for (BlockStateKind kind : BlockStateKind.values()) {
            Path stateFile = getStateFile(kind);
            if (!Files.exists(stateFile)) {
                states[kind.ordinal()] = new BlockStateCollection();
                continue;
            }

            try (BufferedReader reader = Files.newBufferedReader(stateFile)) {
                states[kind.ordinal()] = gson.fromJson(reader, BlockStateCollection.class);
            } catch (IOException e) {
                log.warning("Failed to load block records for: " + kind.name());
                e.printStackTrace();
            }
        }
    }

    private void saveBlockState(BlockStateKind kind) {
        BlockStateCollection blockStateCollection = states[kind.ordinal()];
        if (!blockStateCollection.isDirty()) {
            return;
        }

        Path stateFile = getStateFile(kind);

        try (BufferedWriter writer = Files.newBufferedWriter(
                stateFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(gson.toJson(blockStateCollection));
            blockStateCollection.resetDirtyFlag();
        } catch (IOException e) {
            log.warning("Failed to load write records for: " + kind.name());
            e.printStackTrace();
        }
    }

    private void saveBlockStates() {
        for (BlockStateKind kind : BlockStateKind.values()) {
            saveBlockState(kind);
        }
    }

    public void pushBlock(BlockStateKind kind, Player player, BlockState blockState) throws UnstorableBlockStateException {
        states[kind.ordinal()].push(BlockStateRecordTranslator.constructFrom(player, blockState));
    }

    public void pushAnonymousBlock(BlockStateKind kind, BlockState blockState) throws UnstorableBlockStateException {
        states[kind.ordinal()].push(BlockStateRecordTranslator.constructFrom(blockState));
    }

    public void dropAllBlocks(BlockStateKind kind) {
        states[kind.ordinal()].dropAll();
    }

    public void popAllBlocks(BlockStateKind kind) {
        states[kind.ordinal()].popAll(BlockStateRecordTranslator::restore);
    }

    public void popBlocksOlderThan(BlockStateKind kind, long maxAge) {
        long currentTime = System.currentTimeMillis();

        states[kind.ordinal()].popWhere(
                (record) ->  currentTime - record.getCreationTime() >= maxAge,
                BlockStateRecordTranslator::restore
        );
    }

    private boolean recordMatchesPlayer(BlockStateRecord record, Player player) {
        Optional<UUID> owner = record.getOwner();
        if (owner.isEmpty()) {
            return false;
        }

        return owner.get().equals(player.getUniqueId());
    }

    public void popBlocksCreatedBy(BlockStateKind kind, Player player) {
        states[kind.ordinal()].popWhere(
                (record) -> recordMatchesPlayer(record, player),
                BlockStateRecordTranslator::restore
        );
    }

    public void popBlocksWhere(BlockStateKind kind, Predicate<BlockStateRecord> predicate) {
        states[kind.ordinal()].popWhere(predicate, BlockStateRecordTranslator::restore);
    }

    public boolean hasPlayerBrokenBlocks(BlockStateKind kind, Player player) {
        return states[kind.ordinal()].hasRecordMatching((record) -> recordMatchesPlayer(record, player));
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        server.getScheduler().runTask(CommandBook.inst(), this::saveBlockStates);
    }
}
