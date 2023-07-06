/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class HotSpringArena extends AbstractRegionedArena implements GenericArena, Listener {
    private AdminComponent adminComponent;

    List<Player> playerL = new ArrayList<>();

    public HotSpringArena(World world, ProtectedRegion region, AdminComponent adminComponent) {

        super(world, region);
        this.adminComponent = adminComponent;

        CommandBook.registerEvents(this);
    }

    @Override
    public void run() {

        smoke();
        effect();
    }

    @Override
    public void disable() {

        // No disable code
    }

    @Override
    public String getId() {

        return getRegion().getId();
    }

    @Override
    public void equalize() {

        // Do nothing
    }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.GENERIC;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {

        if (playerL.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    public void smoke() {

        try {
            if (!isEmpty()) {
                BlockVector3 min = getRegion().getMinimumPoint();
                BlockVector3 max = getRegion().getMaximumPoint();

                int minX = min.getBlockX();
                int minY = min.getBlockY();
                int minZ = min.getBlockZ();
                int maxX = max.getBlockX();
                int maxZ = max.getBlockZ();

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        int sizeY = getWorld().getHighestBlockYAt(x, z) - minY;

                        for (int y = sizeY; y > 0; y--) {
                            Block block = getWorld().getBlockAt(x, y + minY, z);

                            if (EnvironmentUtil.isWater(block) && ChanceUtil.getChance(200)) {
                                getWorld().playEffect(block.getLocation(), Effect.ENDER_SIGNAL, 1);
                                if (EnvironmentUtil.hasThunderstorm(getWorld()) && ChanceUtil.getChance(50)) {
                                    getWorld().spawn(block.getLocation(), Zombie.class);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            CommandBook.logger().warning("The region: " + getId() + " does not exists in the world: " + getWorld().getName() + ".");
        }
    }

    public void effect() {

        for (final Player player : getContained(Player.class)) {
            try {

                player.removePotionEffect(PotionEffectType.CONFUSION);
                player.removePotionEffect(PotionEffectType.BLINDNESS);
                player.removePotionEffect(PotionEffectType.WEAKNESS);
                player.removePotionEffect(PotionEffectType.POISON);
                player.removePotionEffect(PotionEffectType.SLOW);

                player.removePotionEffect(PotionEffectType.REGENERATION);
                player.removePotionEffect(PotionEffectType.SPEED);
                player.removePotionEffect(PotionEffectType.JUMP);

                int divinityMod = 20;
                if (player.hasPermission("aurora.tome.divinity")) {
                    divinityMod *= 2;
                    ChatUtil.sendNotice(player, "The gods double the effectiveness of the spring.");
                }

                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, divinityMod * 300, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, divinityMod * 180, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, divinityMod * 180, 1));

                Block downward = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                if (downward.getType() != Material.LAPIS_BLOCK) {
                    downward = downward.getRelative(BlockFace.DOWN);
                    if (downward.getType() != Material.LAPIS_BLOCK) {
                        continue;
                    }
                }

                // TODO Fix this

                playerL.add(player);

                //AntiCheatAPI.exemptPlayer(player, CheckType.WATER_WALK);
                //AntiCheatAPI.exemptPlayer(player, CheckType.FLY);
                //AntiCheatAPI.exemptPlayer(player, CheckType.SNEAK);

                IntegratedRunnable runnable = new IntegratedRunnable() {
                    @Override
                    public boolean run(int times) {

                        if (!player.isValid()) return true;

                        Block downward = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                        if (downward.getType().isSolid() && player.getLocation().getBlockY() > 70) {
                            return true;
                        }

                        GeneralPlayerUtil.takeFlightSafely(player);

                        CommandBook.callEvent(new ThrowPlayerEvent(player));
                        Vector vector = player.getVelocity();
                        vector.add(new Vector(0, 1.2, 0));
                        player.setVelocity(vector);

                        player.setFallDistance(0);
                        return false;
                    }

                    @Override
                    public void end() {

                        playerL.remove(player);

                        if (!player.isValid()) return;

                        // AntiCheatAPI.unexemptPlayer(player, CheckType.WATER_WALK);
                        // AntiCheatAPI.unexemptPlayer(player, CheckType.FLY);
                        // AntiCheatAPI.unexemptPlayer(player, CheckType.SNEAK);
                    }
                };

                TimedRunnable timedRunnable = new TimedRunnable(runnable, 1);
                BukkitTask task = Bukkit.getScheduler().runTaskTimer(CommandBook.inst(), timedRunnable, 0, 15);
                timedRunnable.setTask(task);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
