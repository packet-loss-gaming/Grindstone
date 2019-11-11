package gg.packetloss.grindstone.city.engine.area.areas.Spleef;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.city.engine.area.PersistentArena;
import gg.packetloss.grindstone.exceptions.UnknownPluginException;
import gg.packetloss.grindstone.util.APIUtil;
import gg.packetloss.grindstone.util.database.IOUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.util.player.PlayerState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.util.item.ItemUtil.NO_ARMOR;

@ComponentInformation(friendlyName = "Spleef", desc = "Spleef game implementation")
@Depend(components = {AdminComponent.class}, plugins = {"WorldGuard"})
public class SpleefArea extends BukkitComponent implements Runnable, PersistentArena {
    protected final CommandBook inst = CommandBook.inst();
    protected final Logger log = inst.getLogger();
    protected final Server server = CommandBook.server();

    protected SpleefConfig config;

    @InjectComponent
    protected AdminComponent admin;

    protected HashMap<UUID, PlayerState> playerState = new HashMap<>();

    protected List<SpleefAreaInstance> spleefInstances = new ArrayList<>();

    private void reloadConfig() {
        World world = server.getWorlds().get(0);

        try {
            WorldGuardPlugin WG = APIUtil.getWorldGuard();

            configure(config);
            for (String regionName : config.arenas) {
                RegionManager manager = WG.getRegionManager(world);
                spleefInstances.add(new SpleefAreaInstance(this, world, manager, regionName));
            }
        } catch (UnknownPluginException ignored) {
            log.info("WorldGuard could not be found!");
        }
    }

    @Override
    public void enable() {
        config = new SpleefConfig();

        server.getScheduler().runTaskLater(inst, () -> {
            reloadConfig();
            reloadData();
        }, 1);

        //noinspection AccessStaticViaInstance
        inst.registerEvents(new SpleefListener(this));

        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 0, 20 * 2);
    }

    @Override
    public void reload() {
        super.reload();
        reloadConfig();
    }

    @Override
    public void disable() {
        writeData(false);
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
        return playerState.containsKey(player.getUniqueId());
    }

    private void addPlayer(Player player) {
        PlayerState state = GeneralPlayerUtil.makeComplexState(player);
        state.setOwnerName(player.getName());
        state.setLocation(player.getLocation());
        playerState.put(player.getUniqueId(), state);

        player.getInventory().clear();
        player.getInventory().setArmorContents(NO_ARMOR);

        player.getInventory().addItem(new ItemStack(Material.DIAMOND_SPADE));
    }

    private void removePlayer(Player player) {
        PlayerState state = playerState.remove(player.getUniqueId());

        // Clear Player
        player.getInventory().clear();
        player.getInventory().setArmorContents(NO_ARMOR);

        // Restore the contents
        player.getInventory().setArmorContents(state.getArmourContents());
        player.getInventory().setContents(state.getInventoryContents());
        player.setHealth(Math.min(player.getMaxHealth(), state.getHealth()));
        player.setFoodLevel(state.getHunger());
        player.setSaturation(state.getSaturation());
        player.setExhaustion(state.getExhaustion());
        player.setLevel(state.getLevel());
        player.setExp(state.getExperience());
        player.updateInventory();
    }

    @Override
    public void run() {
        List<Player> allPlayers = new ArrayList<>();

        for (SpleefAreaInstance region : spleefInstances) {
            Collection<Player> regionPlayers = region.run();
            allPlayers.addAll(regionPlayers);
        }

        for (Player player : allPlayers) {
            if (!playerState.containsKey(player.getUniqueId())) {
                addPlayer(player);
            }
        }

        for (Player player : server.getOnlinePlayers()) {
            if (playerState.containsKey(player.getUniqueId()) && !allPlayers.contains(player)) {
                removePlayer(player);
            }
        }

        writeData(true);
    }

    @Override
    public void writeData(boolean doAsync) {
        Runnable run = () -> {
            IOUtil.toBinaryFile(getWorkingDir(), "respawns", playerState);
        };

        if (doAsync) {
            server.getScheduler().runTaskAsynchronously(inst, run);
        } else {
            run.run();
        }
    }

    @Override
    public void reloadData() {
        File playerStateFile = new File(getWorkingDir().getPath() + "/respawns.dat");
        if (playerStateFile.exists()) {
            Object playerStateFileO = IOUtil.readBinaryFile(playerStateFile);
            if (playerStateFileO instanceof HashMap) {
                //noinspection unchecked
                playerState = (HashMap<UUID, PlayerState>) playerStateFileO;
                log.info("Loaded: " + playerState.size() + " respawn records for Spleef.");
            } else {
                log.warning("Invalid block record file encountered: " + playerStateFile.getName() + "!");
                log.warning("Attempting to use backup file...");
                playerStateFile = new File(getWorkingDir().getPath() + "/old-" + playerStateFile.getName());
                if (playerStateFile.exists()) {
                    playerStateFileO = IOUtil.readBinaryFile(playerStateFile);
                    if (playerStateFileO instanceof HashMap) {
                        //noinspection unchecked
                        playerState = (HashMap<UUID, PlayerState>) playerStateFileO;
                        log.info("Backup file loaded successfully!");
                        log.info("Loaded: " + playerState.size() + " respawn records for Spleef.");
                    } else {
                        log.warning("Backup file failed to load!");
                    }
                }
            }
        }
    }

    public File getWorkingDir() {
        return new File(inst.getDataFolder() + "/area/spleef/");
    }

}
