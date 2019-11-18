package gg.packetloss.bukkittext;

import org.bukkit.ChatColor;

class TextChatColor implements TextStreamPart {
    protected ChatColor color;

    protected TextChatColor(ChatColor color) {
        this.color = color;
    }
}
