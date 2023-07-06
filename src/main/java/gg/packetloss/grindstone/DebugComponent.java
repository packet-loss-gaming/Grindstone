/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.bosses.DebugCow;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

@ComponentInformation(friendlyName = "ADebug", desc = "Debug tools")
public class DebugComponent extends BukkitComponent {
    private DebugCow debugCowBoss;

    @Override
    public void enable() {
        debugCowBoss = new DebugCow();

        //CommandBook.registerEvents(new InventoryCorruptionFixer());
        //CommandBook.registerEvents(new BlockDebug());

        registerCommands(DebugCowCmd.class);
        registerCommands(TurboPigCmd.class);
        registerCommands(DamageSimulateCmd.class);
        //registerCommands(FoodInfo.class);
        //registerCommands(ChunkLighter.class);
        //registerCommands(LocationDebug.class);

        // Bug fixes

        // Fixes an issue where potion effects are not removed from players on death
        //CommandBook.registerEvents(new PotionDeathFix());
        //CommandBook.registerEvents(new ItemSpawnPrinter());
        //CommandBook.registerEvents(new DamageSys());
    }

    public class DebugCowCmd {
        @Command(aliases = {"debugcow"}, desc = "Create a debug cow",
                usage = "[health]", flags = "", min = 0, max = 1)
        @CommandPermissions("aurora.debug.debugcow")
        public void debugCowCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            double health = args.getDouble(0, 100000);

            Cow cow = player.getWorld().spawn(player.getLocation(), Cow.class);
            debugCowBoss.bind(cow, health);
        }
    }

    public class TurboPigCmd {
        @Command(aliases = {"turbopig"}, desc = "Create a turbo pig",
                usage = "[speed]", flags = "", min = 0, max = 1)
        @CommandPermissions("aurora.debug.turbopig")
        public void turboPigCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            double speed = args.getDouble(0, 1);

            Pig pig = player.getWorld().spawn(player.getLocation(), Pig.class);
            pig.setSaddle(true);
            pig.setRemoveWhenFarAway(true);

            EntityUtil.setMovementSpeed(pig, speed);
        }
    }

    public class DamageSimulateCmd {
        @Command(aliases = {"simulatedamage"}, desc = "Simulate damage on the currently held item")
        @CommandPermissions("aurora.debug.simulation.damage")
        public void simulateDamageCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);

            ItemStack is = player.getInventory().getItemInHand();
            is.setDurability((short) Math.max(0, is.getData().getItemType().getMaxDurability() - 20));
            player.setItemInHand(is);
            ChatUtil.sendNotice(player, "Damage simulated!");
        }
    }

    public class FoodInfo {

        @Command(aliases = {"foodstats"}, desc = "Report hunger info",
                flags = "", min = 0, max = 0)
        @CommandPermissions("aurora.debug.foodstats")
        public void myLocCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);
            ChatUtil.sendNotice(player, "Food level: " + player.getFoodLevel());
            ChatUtil.sendNotice(player, "Sat. level: " + player.getSaturation());
            ChatUtil.sendNotice(player, "Exh. level: " + player.getExhaustion());
        }
    }

    public class LocationDebug {

        @Command(aliases = {"myloc"}, desc = "Get your location",
                flags = "", min = 0, max = 0)
        public void myLocCmd(CommandContext args, CommandSender sender) throws CommandException {

            Location l = PlayerUtil.checkPlayer(sender).getLocation();
            ChatUtil.sendNotice(sender, "X: " + l.getX() + ", Y:" + l.getY() + ", Z: " + l.getZ());
            ChatUtil.sendNotice(sender, "Pitch: " + l.getPitch() + ", Yaw: " + l.getYaw());
        }
    }

    private class PotionDeathFix implements Listener {

        @EventHandler
        public void onRespawn(final PlayerRespawnEvent event) {
            Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
                Player player = event.getPlayer();

                for (PotionEffect next : player.getActivePotionEffects()) {
                    player.addPotionEffect(new PotionEffect(next.getType(), 0, 0), true);
                }
            }, 1);
        }
    }

    private class ItemSpawnPrinter implements Listener {

        @EventHandler
        public void onItemSpawn(final ItemSpawnEvent event) {
            Location l = event.getEntity().getLocation();
            ChatUtil.sendDebug("X: " + l.getX() + ", Y:" + l.getY() + ", Z: " + l.getZ());
        }
    }

    private class DamageSys implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {

            double damage = event.getDamage(EntityDamageEvent.DamageModifier.BASE);
            double origDamage = event.getOriginalDamage(EntityDamageEvent.DamageModifier.BASE);

            if (damage == origDamage) return;

            for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values()) {
                if (modifier == EntityDamageEvent.DamageModifier.BASE) continue;
                if (!event.isApplicable(modifier)) continue;

                event.setDamage(modifier, event.getDamage(modifier) * (damage / origDamage));
            }
        }
    }
}
