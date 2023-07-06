/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer;

import com.google.common.collect.ImmutableList;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandUsageException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.PrayerTriggerEvent;
import gg.packetloss.grindstone.exceptions.InvalidPrayerException;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;

@ComponentInformation(friendlyName = "Prayers", desc = "Let the light (or darkness) be unleashed on thy!")
@Depend(components = {AdminComponent.class})
public class PrayerComponent extends BukkitComponent implements Listener, Runnable {
    @InjectComponent
    private AdminComponent admin;

    private static Map<UUID, ImmutableList<Prayer>> prayers = new HashMap<>();

    @Override
    public void enable() {
        CommandBook.registerEvents(this);

        registerCommands(Commands.class);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(
            CommandBook.inst(),
            this,
            20 * 2,
            11
        );
    }

    private static ImmutableList<Prayer> updatePrayers(UUID playerID, ImmutableList<Prayer> existingPrayers) {
        boolean changed = false;
        for (Prayer prayer : existingPrayers) {
            if (prayer.hasExpired()) {
                changed = true;
                break;
            }
        }

        if (!changed) {
            return existingPrayers;
        }

        ImmutableList.Builder<Prayer> newPrayersBuilder = ImmutableList.builder();
        for (Prayer prayer : existingPrayers) {
            if (prayer.hasExpired()) {
                continue;
            }

            newPrayersBuilder.add(prayer);
        }

        ImmutableList<Prayer> newPrayers = newPrayersBuilder.build();

        if (newPrayers.isEmpty()) {
            prayers.remove(playerID);
        } else {
            prayers.put(playerID, newPrayers);
        }

        return newPrayers;
    }


    public static ImmutableList<Prayer> getPrayers(Player player) {
        UUID playerID = player.getUniqueId();

        ImmutableList<Prayer> activePrayers = prayers.get(playerID);
        if (activePrayers != null) {
            return updatePrayers(playerID, activePrayers);
        }

        return ImmutableList.of();
    }

    public static boolean hasPrayers(Player player) {
        return !getPrayers(player).isEmpty();
    }

    public static void constructPrayer(Player player, boolean isHoly, ImmutableList<PassivePrayerEffect> prayerEffects, long duration) {
        UUID playerID = player.getUniqueId();

        ImmutableList<Prayer> existing = getPrayers(player);

        ImmutableList.Builder<Prayer> newPrayers = ImmutableList.builder();
        newPrayers.addAll(existing);
        newPrayers.add(new Prayer(isHoly, Map.of(PrayerEffectTrigger.PASSIVE, (ImmutableList) prayerEffects), duration));

        prayers.put(playerID, newPrayers.build());
    }

    public static void constructPrayer(Player player, Prayers prayer, long duration) {
        UUID playerID = player.getUniqueId();

        ImmutableList<Prayer> existing = getPrayers(player);

        ImmutableList.Builder<Prayer> newPrayers = ImmutableList.builder();
        newPrayers.addAll(existing);
        newPrayers.add(new Prayer(prayer, duration));

        prayers.put(playerID, newPrayers.build());
    }

    public void clearPrayers(Player player) {
        List<Prayer> existingPrayers = prayers.remove(player.getUniqueId());
        if (existingPrayers == null) {
            return;
        }

        existingPrayers.forEach(prayer -> {
            for (PassivePrayerEffect passiveEffect : prayer.getPassiveEffects()) {
                passiveEffect.strip(player);
            }
        });
    }

    private boolean checkPrayerAllowed(Player player, Prayer prayer) {
        PrayerTriggerEvent event = new PrayerTriggerEvent(player, prayer);
        CommandBook.callEvent(event);
        return event.isCancelled();
    }

    private List<PassivePrayerEffect> getActivePassivePrayers(Player player) {
        List<PassivePrayerEffect> effects = new ArrayList<>();

        for (Prayer prayer : getPrayers(player)) {
            List<PassivePrayerEffect> prayerEffects = prayer.getPassiveEffects();
            if (prayerEffects.isEmpty()) {
                continue;
            }

            // Try to trigger this prayer, if it fails, clear any active effects,
            // and skip application of any new effects.
            if (checkPrayerAllowed(player, prayer)) {
                prayerEffects.forEach(e -> e.strip(player));
                continue;
            }

            effects.addAll(prayerEffects);
        }

        return effects;
    }

    private List<InteractTriggeredPrayerEffect> getActiveInteractivePrayers(Player player) {
        List<InteractTriggeredPrayerEffect> effects = new ArrayList<>();

        for (Prayer prayer : getPrayers(player)) {
            List<InteractTriggeredPrayerEffect> prayerEffects = prayer.getInteractiveEffects();
            if (prayerEffects.isEmpty()) {
                continue;
            }

            // Try to trigger this prayer, if it fails, skip triggering.
            if (checkPrayerAllowed(player, prayer)) {
                continue;
            }

            effects.addAll(prayerEffects);
        }

        return effects;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (hasPrayers(player)) {
            long count = getPrayers(player).stream().filter(Prayer::isUnholy).count();

            if (count > 1) {
                ChatUtil.sendNotice(player, ChatColor.GOLD, "The curses have been lifted!");
            } else if (count > 0) {
                ChatUtil.sendNotice(player, ChatColor.GOLD, "The curse has been lifted!");
            }

            clearPrayers(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        for (InteractTriggeredPrayerEffect prayerEffect : getActiveInteractivePrayers(player)) {
            prayerEffect.trigger(event, player);
        }
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (PassivePrayerEffect prayerEffect : getActivePassivePrayers(player)) {
                prayerEffect.trigger(player);
            }
        }
    }

    public Prayers getPrayerByString(String prayer) throws InvalidPrayerException {

        try {
            return Prayers.valueOf(prayer.trim().toUpperCase());
        } catch (Exception e) {
            throw new InvalidPrayerException();
        }
    }

    public class Commands {

        @Command(aliases = {"pray", "pr"},
                usage = "<player> <prayer>", desc = "Pray for something to happen to the player",
                flags = "csl", min = 0, max = 2)
        public void prayerCmd(CommandContext args, CommandSender sender) throws CommandException {

            String playerString;
            String prayerString;
            Player player;

            if (args.argsLength() < 2) {
                if (args.hasFlag('l')) {
                    int quantity = 0;
                    StringBuilder sb = new StringBuilder();
                    sb.append(ChatColor.YELLOW).append("Valid prayers: ");
                    for (Prayers prayer : Prayers.values()) {
                        if (prayer.isHoly()) {
                            if (!sender.hasPermission("aurora.pray.holy." + prayer.getPermissionName())) {
                                continue;
                            }
                        } else {
                            if (!sender.hasPermission("aurora.pray.unholy." + prayer.getPermissionName())) {
                                continue;
                            }
                        }
                        if (quantity > 0) sb.append(ChatColor.GRAY).append(", ");
                        sb.append(prayer.getChatColor());
                        sb.append(prayer.getFormattedName());
                        sb.append(ChatColor.DARK_GRAY).append(" (");
                        sb.append(ChatColor.DARK_AQUA).append(prayer.getLevelCost());
                        sb.append(ChatColor.DARK_GRAY).append(")");
                        quantity++;
                    }
                    sb.append(ChatColor.YELLOW).append(".");
                    ChatUtil.sendNotice(sender, sb.toString());
                    return;
                } else {
                    throw new CommandUsageException("Too few arguments.", "/pray [csl] <player> <prayer>");
                }
            } else {
                playerString = args.getString(0);
                prayerString = args.getString(1).toLowerCase();
                player = InputUtil.PlayerParser.matchSinglePlayer(sender, playerString);
            }

            // Check for valid nameType
            try {
                if (player.getName().equals(sender.getName()) && !sender.hasPermission("aurora.tome.divinity")) {
                    player.getWorld().strikeLightningEffect(player.getLocation());
                    throw new CommandException("The gods don't take kindly to using their power on yourself.");
                }

                Prayers prayerType = getPrayerByString(prayerString);

                if (prayerType.isUnholy()) {
                    CommandBook.inst().checkPermission(sender, "aurora.pray.unholy." + prayerString);
                    ChatUtil.sendNotice(sender, ChatColor.DARK_RED + "The player: " + player.getDisplayName()
                            + " has been smited!");
                } else {
                    CommandBook.inst().checkPermission(sender, "aurora.pray.holy." + prayerString);
                    ChatUtil.sendNotice(sender, ChatColor.GOLD + "The player: " + player.getDisplayName()
                            + " has been blessed!");
                }

                if (sender instanceof Player && !admin.isAdmin((Player) sender)) {
                    Player senderP = (Player) sender;

                    int newL = senderP.getLevel() - prayerType.getLevelCost();
                    if (newL < 0) {
                        throw new CommandException("You do not have enough levels to use that prayer.");
                    }
                    senderP.setLevel(newL);
                }

                if (args.hasFlag('c') && sender.hasPermission("aurora.pray.clear")) clearPrayers(player);

                constructPrayer(player, prayerType, TimeUnit.MINUTES.toMicros(15));

            } catch (InvalidPrayerException ex) {
                throw new CommandException("That is not a valid prayer!");
            }
        }
    }

}