package gg.packetloss.grindstone.util;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class CommandBlocker {
    private List<String> whitelist;

    public CommandBlocker(List<String> whitelist) {
        this.whitelist = whitelist;
    }

    public boolean allowsCommand(String command) {
        command = command.toLowerCase();

        for (String cmd : whitelist) {
            if (command.startsWith("/" + cmd)) {
                return true;
            }
        }

        return false;
    }

    public void handle(PlayerCommandPreprocessEvent event) {
        if (!allowsCommand(event.getMessage())) {
            ChatUtil.sendError(event.getPlayer(), "Command blocked.");
            event.setCancelled(true);
        }
    }
}
