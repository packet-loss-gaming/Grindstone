package gg.packetloss.grindstone.city.engine.area.areas.Spleef;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.guild.state.GuildState;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.util.item.ItemUtil.NO_ARMOR;

@ComponentInformation(friendlyName = "Spleef", desc = "Spleef game implementation")
@Depend(components = {GuildComponent.class, PlayerStateComponent.class}, plugins = {"WorldGuard"})
public class SpleefArea extends BukkitComponent implements Runnable {
    protected final CommandBook inst = CommandBook.inst();
    protected final Logger log = inst.getLogger();
    protected final Server server = CommandBook.server();

    protected SpleefConfig config;

    @InjectComponent
    protected GuildComponent guilds;
    @InjectComponent
    protected PlayerStateComponent playerState;

    protected List<SpleefAreaInstance> spleefInstances = new ArrayList<>();

    private void reloadConfig() {
        World world = server.getWorlds().get(0);

        configure(config);
        for (String regionName : config.arenas) {
            RegionManager manager = WorldGuardBridge.getManagerFor(world);
            spleefInstances.add(new SpleefAreaInstance(this, world, manager, regionName));
        }
    }

    @Override
    public void enable() {
        config = new SpleefConfig();

        reloadConfig();

        //noinspection AccessStaticViaInstance
        inst.registerEvents(new SpleefListener(this));

        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 0, 5);
    }

    @Override
    public void reload() {
        super.reload();
        reloadConfig();
    }

    public boolean anyContains(Location location) {
        for (SpleefAreaInstance area : spleefInstances) {
            if (area.contains(location)) {
                return true;
            }
        }

        return false;
    }

    public boolean isUsingArenaTools(Player player) {
        return playerState.hasValidStoredState(PlayerStateKind.SPLEEF, player);
    }

    private void addPlayer(Player player) {
        try {
            playerState.pushState(PlayerStateKind.SPLEEF, player);

            guilds.getState(player).ifPresent(GuildState::disablePowers);
            GeneralPlayerUtil.takeFlightSafely(player);

            player.setFoodLevel(20);
            player.setSaturation(20F);
            player.setExhaustion(0);

            player.getInventory().clear();
            player.getInventory().setArmorContents(NO_ARMOR);

            player.getInventory().addItem(new ItemStack(Material.DIAMOND_SHOVEL));
        } catch (IOException | ConflictingPlayerStateException ex) {
            ex.printStackTrace();
        }
    }

    private void removePlayer(Player player) {
        try {
            playerState.popState(PlayerStateKind.SPLEEF, player);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        List<Player> allPlayers = new ArrayList<>();

        for (SpleefAreaInstance region : spleefInstances) {
            Collection<Player> regionPlayers = region.run();
            allPlayers.addAll(regionPlayers);
        }

        for (Player player : allPlayers) {
            if (!isUsingArenaTools(player)) {
                addPlayer(player);
            }
        }

        for (Player player : server.getOnlinePlayers()) {
            if (isUsingArenaTools(player) && !allPlayers.contains(player)) {
                removePlayer(player);
            }
        }
    }
}
