/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.buff;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.*;

@ComponentInformation(friendlyName = "Buff Component", desc = "Manages player buffs.")
public class BuffComponent extends BukkitComponent implements Listener {
    private List<Map<UUID, BuffSet>> playerBuffs = Lists.newArrayList();

    public BuffComponent() {
        super();

        for (int i = 0; i < BuffCategory.values().length; ++i) {
            playerBuffs.add(new HashMap<>());
        }
    }

    @Override
    public void enable() {
        registerCommands(Commands.class);
        CommandBook.registerEvents(this);
    }

    public void clearBuffs(BuffCategory category) {
        playerBuffs.get(category.ordinal()).clear();
    }

    private Map<UUID, BuffSet> getBuffMap(Buff buff) {
        return playerBuffs.get(buff.getCategory().ordinal());
    }

    private BuffSet getBuffSet(Buff buff, Player player) {
        return getBuffMap(buff).computeIfAbsent(player.getUniqueId(), (UUID key) -> new BuffSet(buff.getCategory()));
    }

    public boolean increase(Buff buff, Player player) {
        return getBuffSet(buff, player).increase(buff);
    }

    private void notifyOfNewPower(Buff buff, Player player) {
        int level = getBuffLevel(buff, player).get();
        ChatUtil.sendMessage(player, ChatColor.YELLOW + "You grow stronger, " + ChatColor.BLUE + buff.getFriendlyName()
                + ChatColor.DARK_GREEN + " +" + level);
    }

    public void notifyIncrease(Buff buff, Player player) {
        if (!increase(buff, player)) {
            return;
        }

        notifyOfNewPower(buff, player);
    }

    public boolean decrease(Buff buff, Player player) {
        return getBuffSet(buff, player).decrease(buff);
    }

    public Optional<Integer> getBuffLevel(Buff buff, Player player) {
        BuffSet buffSet = getBuffMap(buff).get(player.getUniqueId());
        if (buffSet == null) {
            return Optional.empty();
        }

        return Optional.of(buffSet.getLevel(buff));
    }

    public boolean fillToLevel(Buff buff, Player player, int targetLevel) {
        int currentValue = getBuffLevel(buff, player).orElse(0);
        if (currentValue >= targetLevel) {
            return false;
        }

        return getBuffSet(buff, player).adjustLevel(buff, targetLevel - currentValue);
    }

    public void notifyFillToLevel(Buff buff, Player player, int targetLevel) {
        if (!fillToLevel(buff, player, targetLevel)) {
            return;
        }

        notifyOfNewPower(buff, player);
    }

    public class Commands {
        @Command(aliases = {"buffs"},
                usage = "", desc = "List player buffs",
                flags = "", min = 0, max = 0)
        public void forecastCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            List<Buff> categorizedBuffs = Lists.newArrayList(Buff.values());
            categorizedBuffs.sort((a, b) -> {
                return a.getCategory().getFriendlyName().compareToIgnoreCase(b.getCategory().getFriendlyName());
            });

            BuffCategory category = null;
            for (Buff buff : categorizedBuffs) {
                Optional<Integer> buffLevel = getBuffLevel(buff, player);
                if (buffLevel.isEmpty()) {
                    continue;
                }

                // If the buff category changed, print that first
                if (buff.getCategory() != category) {
                    category = buff.getCategory();

                    ChatUtil.sendMessage(sender, ChatColor.GOLD + category.getFriendlyName().toUpperCase());
                }

                // Print the buff message
                ChatUtil.sendMessage(sender, "  " + ChatColor.BLUE + buff.getFriendlyName() +
                        ChatColor.DARK_GREEN + " +" + buffLevel.get());
            }

            if (category == null) {
                ChatUtil.sendMessage(player, ChatColor.YELLOW + "No buffs currently enabled.");
            }
        }
    }
}
