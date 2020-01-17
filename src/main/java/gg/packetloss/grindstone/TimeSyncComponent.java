package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.util.List;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Time Sync", desc = "Synchronizes time across worlds.")
public class TimeSyncComponent extends BukkitComponent {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());

        server.getScheduler().scheduleSyncRepeatingTask(inst, this::syncTime, 0, 20 * 60);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("leader-world")
        public String leaderWorld = "City";
        @Setting("follower-worlds")
        public List<String> affectedWorlds = List.of("Halzeil");
    }

    private void syncTime() {
        long leaderTime = Bukkit.getWorld(config.leaderWorld).getTime();
        for (String worldName : config.affectedWorlds) {
            Bukkit.getWorld(worldName).setTime(leaderTime);
        }
    }
}
