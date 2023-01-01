/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.util.task.promise.TaskFuture;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class ChatUtil {

    public enum MessageType {
        NOTICE(ChatColor.YELLOW),
        ERROR(ChatColor.RED),
        WARNING(ChatColor.RED);

        private final ChatColor color;
        MessageType(ChatColor color) {
            this.color = color;
        }

        public ChatColor getColor() {
            return color;
        }
    }

    public static void message(CommandSender sender, MessageType type, Object... args) {
        sender.sendMessage(Text.of(type.getColor(), Arrays.asList(args)).build());
    }

    public static <T extends CommandSender> void message(Iterable<T> senders, MessageType type, Object... args) {
        var built = Text.of(type.getColor(), Arrays.asList(args)).build();
        for (CommandSender sender : senders) {
            sender.sendMessage(built);
        }
    }

    public static void sendNotice(CommandSender sender, Object... args) {
        message(sender, MessageType.NOTICE, args);
    }

    public static <T extends CommandSender> void sendNotice(Iterable<T> senders, Object... args) {
        message(senders, MessageType.NOTICE, args);
    }

    public static void sendWarning(CommandSender sender, Object... args) {
        message(sender, MessageType.WARNING, args);
    }

    public static <T extends CommandSender> void sendWarning(Iterable<T> senders, Object... args) {
        message(senders, MessageType.WARNING, args);
    }

    public static void sendError(CommandSender sender, Object... args) {
        message(sender, MessageType.ERROR, args);
    }

    public static <T extends CommandSender> void sendError(Iterable<T> senders, Object... args) {
        message(senders, MessageType.ERROR, args);
    }

    public static void sendDebug(Text messageText) {
        Text debugText = Text.of(ChatColor.BLACK, "[", ChatColor.DARK_RED, "DEBUG", ChatColor.BLACK, "] ", messageText);
        BaseComponent[] builtDebugText = debugText.build();

        Bukkit.getOnlinePlayers().forEach((player) -> {
            if (player.hasPermission("aurora.debug")) {
                player.sendMessage(builtDebugText);
            }
        });
    }

    public static void sendDebug(Object... objects) {
        sendDebug(Text.of(objects));
    }

    public static void sendAdminNotice(Text messageText) {
        Text debugText = Text.of(ChatColor.BLACK, "[", ChatColor.DARK_RED, "ADMIN", ChatColor.BLACK, "] ", messageText);
        BaseComponent[] builtAdminMessage = debugText.build();

        CommandBook.logger().info(messageText.toString());
        Bukkit.getOnlinePlayers().forEach((player) -> {
            if (player.hasPermission("aurora.admin.adminmode")) {
                player.sendMessage(builtAdminMessage);
            }
        });
    }

    public static void sendAdminNotice(Object... objects) {
        sendAdminNotice(Text.of(objects));
    }

    public static TaskFuture<Void> sendStaggered(CommandSender sender, Iterable<Text> lines) {
        TaskFuture<Void> future = new TaskFuture<>();

        Iterator<Text> it = lines.iterator();
        int i = 0;
        while (it.hasNext()) {
            Text line = it.next();

            if (i == 0) {
                sender.sendMessage(line.build());
            } else {
                boolean hasNext = it.hasNext();
                CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
                    sender.sendMessage(line.build());
                    if (!hasNext) {
                        future.complete(null);
                    }
                }, i * 20 * 3);
            }

            ++i;
        }

        return future;
    }

    public static void message(CommandSender sender, MessageType type, String message) {
        if (sender == null) return;
        sender.sendMessage(type.getColor() + message);
    }

    public static void message(Collection<? extends CommandSender> targets, MessageType type, String message) {
        for (CommandSender target : targets) {
            message(target, type, message);
        }
    }

    public static void sendNotice(String playerName, String notice) {
        try {
            sendNotice(InputUtil.PlayerParser.matchPlayerExactly(null, playerName), notice);
        } catch (CommandException ignored) {
        }
    }

    public static void sendNotice(CommandSender sender, String notice) {
        message(sender, MessageType.NOTICE, notice);
    }

    public static void sendNotice(Collection<? extends CommandSender> senders, String notice) {
        message(senders, MessageType.NOTICE, notice);
    }

    public static void sendNotice(CommandSender sender, ChatColor chatColor, String notice) {
        sender.sendMessage(chatColor + notice.replace("%p%", chatColor.toString()));
    }

    public static void sendNotice(Collection<? extends CommandSender> senders, ChatColor chatColor, String notice) {
        senders.forEach(s -> sendNotice(s, chatColor, notice));
    }

    public static void sendError(CommandSender sender, String error) {
        message(sender, MessageType.ERROR, error);
    }

    public static void sendError(Collection<? extends CommandSender> senders, String error) {
        message(senders, MessageType.ERROR, error);
    }

    public static void sendWarning(CommandSender sender, String warning) {
        message(sender, MessageType.WARNING, warning);
    }

    public static void sendWarning(Collection<? extends CommandSender> senders, String warning) {
        message(senders, MessageType.WARNING, warning);
    }

    public static final DecimalFormat WHOLE_NUMBER_FORMATTER = new DecimalFormat("#,###");
    public static final DecimalFormat ONE_DECIMAL_FORMATTER = new DecimalFormat("#,###.#");
    public static final DecimalFormat TWO_DECIMAL_FORMATTER = new DecimalFormat("#,##0.00");

    public static String makeCountString(int value, String currencyName) {
        return makeCountString(ChatColor.YELLOW, WHOLE_NUMBER_FORMATTER.format(value), currencyName);
    }

    public static String makeCountString(double value, String currencyName) {

        return makeCountString(ChatColor.YELLOW, String.valueOf(value), currencyName);
    }

    public static String makeCountString(String value, String currencyName) {

        return makeCountString(ChatColor.YELLOW, value, currencyName);
    }

    public static String makeCountString(ChatColor color, int value, String currencyName) {

        return ChatColor.WHITE + String.valueOf(value) + color + currencyName;
    }

    public static String makeCountString(ChatColor color, double value, String currencyName) {

        return ChatColor.WHITE + String.valueOf(value) + color + currencyName;
    }

    public static String makeCountString(ChatColor color, String value, String currencyName) {

        return ChatColor.WHITE + value + color + currencyName;
    }

    public static char loonyCharacter() {
        switch (ChanceUtil.getRandom(7)) {
            case 7:
                return '?';
            case 6:
                return '&';
            case 5:
                return '!';
            case 4:
                return '#';
            case 3:
                return '@';
            case 2:
                return '%';
            default:
                return '$';
        }
    }

    public static ChatColor loonyColor() {
        switch (ChanceUtil.getRandom(6)) {
            case 6:
                return ChatColor.RED;
            case 5:
                return ChatColor.GREEN;
            case 4:
                return ChatColor.BLUE;
            case 3:
                return ChatColor.AQUA;
            case 2:
                return ChatColor.YELLOW;
            case 1:
                return ChatColor.DARK_PURPLE;
            default:
                return ChatColor.WHITE;
        }
    }

    public static String loonizeWord(String word) {
        return loonizeWord(word, true);
    }

    public static String loonizeWord(String word, boolean useColor) {
        StringBuilder loonyFilteredString = new StringBuilder();
        if (useColor) {
            for (int f = 0; f < word.length(); f++) {
                loonyFilteredString.append(ChatUtil.loonyColor()).append(ChatUtil.loonyCharacter());
            }
        } else {
            for (int f = 0; f < word.length(); f++) {
                loonyFilteredString.append(ChatUtil.loonyCharacter());
            }
        }

        return loonyFilteredString.toString();
    }

    public static String runeizeString(String string) {

        StringBuilder out = new StringBuilder();

        char[] chars = string.toCharArray();
        boolean nextAllowed = true;

        for (int i = 0; i < chars.length; i++) {

            if (!Character.isAlphabetic(chars[i])) {
                nextAllowed = true;
                continue;
            }

            if ((i == 0 || Character.isSpaceChar(chars[i - 1])) && chars[i] == 'X') {
                nextAllowed = true;
                continue;
            }

            if (!nextAllowed) {
                chars[i] = Character.toLowerCase(chars[i]);
            }
            nextAllowed = false;
        }

        for (char character : chars) {
            out.append(character);
        }

        return out.toString();
    }

    public static String toString(Vector vector) {
        if (vector == null) {
            return "NONE";
        }

        return vector.getX() + ", " + vector.getY() + ", " + vector.getZ();
    }
}
