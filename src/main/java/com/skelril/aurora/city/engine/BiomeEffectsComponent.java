package com.skelril.aurora.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.FrostBiteEvent;
import com.skelril.aurora.jail.JailComponent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Biome Effects", desc = "Biome Effects.")
@Depend(plugins = {"Vault"}, components = {JailComponent.class, AdminComponent.class})
public class BiomeEffectsComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private JailComponent jailComponent;
    @InjectComponent
    private AdminComponent adminComponent;

    private static Permission permission = null;
    private LocalConfiguration config;

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        setupPermissions();
        config = configure(new LocalConfiguration());
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 20 * 8);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("enable-swamp")
        public boolean enableSwamp = false;
        @Setting("enable-frozen")
        public boolean enableFrozen = false;
        @Setting("secret-chest-location")
        private String secretChestLocationString = "228,45,-232,City";

        public Location secretChestLocation = LocationUtil.parseLocation(secretChestLocationString);
        public String mainWorld = secretChestLocation.getWorld() != null
                                  ? secretChestLocation.getWorld().getName() : "null";
    }

    @Override
    public void run() {

        try {
            for (Player player : Bukkit.getWorld(config.mainWorld).getPlayers()) {
                frostBite(player); // Attempt to execute Frost Bite
                darkWind(player); // Attempt to execute the Dark Wind

            }
        } catch (Exception e) {
            log.warning("Please verify the world: " + config.mainWorld + " exists.");
        }
    }

    @EventHandler
    public void onChestInteract(PlayerInteractEvent event) {

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        if (block != null && block.getTypeId() == BlockID.CHEST
                && LocationUtil.isLocNearby(block.getLocation(), config.secretChestLocation, 1)) {
            permission.playerAdd((World) null, player.getName(), "aurora.biomeeffects.override.darkwind");
            ChatUtil.sendNotice(player, "");

        }
    }

    private void frostBite(Player player) {

        try {
            ItemStack held = player.getItemInHand();
            if (config.enableFrozen
                    && EnvironmentUtil.isFrozenBiome(player.getEyeLocation().getBlock().getBiome())
                    && (held == null || held.getTypeId() != BlockID.TORCH)
                    && !adminComponent.isAdmin(player)
                    && !jailComponent.isJailed(player)) {

                int damage = 5 - player.getEyeLocation().getBlock().getLightLevel();

                FrostBiteEvent event = new FrostBiteEvent(player, damage);
                server.getPluginManager().callEvent(event);

                damage = event.getDamage();

                if (event.isCancelled() || damage <= 0) return;
                player.damage(ChanceUtil.getChance(damage) ? damage * ChanceUtil.getRandom(4) : damage);
                ChatUtil.sendWarning(player, "The cold temperatures eat away at you.");
            }
        } catch (Exception e) {
            log.warning("Frost Bite could not be executed for the player: " + player.getName() + ".");
        }
    }

    private void darkWind(Player player) {

        try {
            if (config.enableSwamp
                    && player.getLocation().getBlock().getBiome().equals(Biome.SWAMPLAND)) {

                if (inst.hasPermission(player, "aurora.biomeeffects.override.darkwind")) {
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                } else if (player.getInventory().contains(ItemID.ROTTEN_FLESH)) {
                    player.getInventory().removeItem(new ItemStack(ItemID.ROTTEN_FLESH, 1));
                } else {
                    ChatUtil.sendWarning(player, "A dark wind passes over you.");
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 600, 1));
                }
            } else if (config.enableSwamp && player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                player.removePotionEffect(PotionEffectType.BLINDNESS);
            }
        } catch (Exception e) {
            log.warning("The Dark Wind could not be executed for the player: " + player.getName() + ".");
        }
    }

    private boolean setupPermissions() {

        RegisteredServiceProvider<Permission> permissionProvider = server.getServicesManager().getRegistration(net
                .milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }
}
