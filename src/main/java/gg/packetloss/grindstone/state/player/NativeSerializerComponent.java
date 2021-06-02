/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.player;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.hackbook.ItemSerializer;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ComponentInformation(friendlyName = "Native Serializer", desc = "Native object serialization")
public class NativeSerializerComponent extends BukkitComponent {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private Path migrationFile;
    private Path itemStorageDir;

    @Override
    public void enable() {
        String nativeDirectory = inst.getDataFolder().getPath() + "/native/";
        String objectStorageDirectory = nativeDirectory + "objects/";

        try {
            migrationFile = Paths.get(nativeDirectory, "migrate");
            itemStorageDir = Files.createDirectories(Paths.get(objectStorageDirectory, "items"));

            upgradeOutOfDate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<ItemStack> deserializeFrom(Path file, boolean migrate) throws IOException {
        byte[] content = Files.readAllBytes(file);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            return ItemSerializer.fromInputStream(inputStream, migrate);
        }
    }

    private void serializeTo(Path file, List<ItemStack> items) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ItemSerializer.writeToOutputStream(items, outputStream);

            Files.write(
                    file,
                    outputStream.toByteArray(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        }
    }

    private void performUpgrade() throws IOException {
        List<Path> files = Files.find(itemStorageDir, 1, (path, ignored) -> {
            return path.getFileName().toString().endsWith(".dat");
        }).collect(Collectors.toList());

        for (Path file : files) {
            log.info("Migrating " + file.getFileName());

            serializeTo(file, deserializeFrom(file, true));
        }
    }

    private void upgradeOutOfDate() throws IOException {
        if (Files.exists(migrationFile)) {
            log.info("Migration file found, initializing migration");
            performUpgrade();
            Files.delete(migrationFile);
            log.info("Migration completed");
        }
    }

    private Path getItemPath(UUID key) {
        return itemStorageDir.resolve(key.toString() + ".dat");
    }

    public void writeItems(UUID key, List<ItemStack> items) throws IOException {
        serializeTo(getItemPath(key), items);
    }

    public List<ItemStack> readItems(UUID key) throws IOException {
        return deserializeFrom(getItemPath(key), false);
    }

    public void removeItems(UUID key) throws IOException {
        Files.delete(getItemPath(key));
    }
}
