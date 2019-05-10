/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.environment.FrostBiteEvent;
import gg.packetloss.grindstone.jail.JailComponent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.logging.Logger;


@ComponentInformation(friendlyName = "Biome Management", desc = "Biome Management component.")
@Depend(plugins = {"Vault"}, components = {JailComponent.class, AdminComponent.class})
public class BiomeManagementComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private JailComponent jailComponent;
    @InjectComponent
    private AdminComponent adminComponent;

    private LocalConfiguration config;

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        config = configure(new LocalConfiguration());
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 20 * 8);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("enable-frozen")
        public boolean enableFrozen = false;

        @Setting("main-world")
        public String mainWorld = "City";
    }

    @Override
    public void run() {

        try {
            // Attempt to execute Frost Bite
            //noinspection CodeBlock2Expr
            Bukkit.getWorld(config.mainWorld).getPlayers().forEach((t) -> {
                //darkWind(t);
                //noinspection Convert2MethodRef
                frostBite(t);
            });
        } catch (Exception e) {
            log.warning("Please verify the world: " + config.mainWorld + " exists.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {

        if (event.getSource().getTypeId() == BlockID.MYCELIUM && event.getBlock().getBiome() != Biome.MUSHROOM_ISLAND) {
            event.setCancelled(true);
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
            if (player.getLocation().getBlock().getBiome().equals(Biome.SWAMPLAND)) {

                if (inst.hasPermission(player, "aurora.biomeeffects.override.darkwind")) {
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                } else if (player.getInventory().contains(ItemID.ROTTEN_FLESH)) {
                    player.getInventory().removeItem(new ItemStack(ItemID.ROTTEN_FLESH, 1));
                } else {
                    ChatUtil.sendWarning(player, "A dark wind passes over you.");
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 600, 1));
                }
            } else if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                player.removePotionEffect(PotionEffectType.BLINDNESS);
            }
        } catch (Exception e) {
            log.warning("The Dark Wind could not be executed for the player: " + player.getName() + ".");
        }
    }
}
