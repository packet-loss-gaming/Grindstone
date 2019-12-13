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

@ComponentInformation(friendlyName = "Native Serializer", desc = "Native object serialization")
public class NativeSerializerComponent extends BukkitComponent {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private Path itemStorageDir;

    @Override
    public void enable() {
        String objectStorageDirectory = inst.getDataFolder().getPath() + "/native/objects/";

        try {
            itemStorageDir = Files.createDirectories(Paths.get(objectStorageDirectory, "items"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Path getItemPath(UUID key) {
        return itemStorageDir.resolve(key.toString() + ".dat");
    }

    public void writeItems(UUID key, List<ItemStack> items) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ItemSerializer.writeToOutputStream(items, outputStream);

            Files.write(
                    getItemPath(key),
                    outputStream.toByteArray(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        }
    }

    public List<ItemStack> readItems(UUID key) throws IOException {
        byte[] content = Files.readAllBytes(getItemPath(key));
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            return ItemSerializer.fromInputStream(inputStream);
        }
    }

    public void removeItems(UUID key) throws IOException {
        Files.delete(getItemPath(key));
    }
}
