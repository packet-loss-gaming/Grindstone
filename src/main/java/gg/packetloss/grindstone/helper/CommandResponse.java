package gg.packetloss.grindstone.helper;

import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.bukkittext.TextBuilder;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;

public class CommandResponse implements Response {
    @Override
    public boolean accept(Player player, Collection<Player> recipients, String string) {
        if (!string.matches("\\./.*")) {
            return false;
        }

        // Replace the "./" with "/" and then trim the string
        String command = string.replaceFirst("\\./", "/").trim();

        // Send a composite message of the command, a space, and then the comment text
        TextBuilder builder = Text.builder();
        builder.append(getNamePlate());
        builder.append(Text.of(
                ChatColor.DARK_GREEN, "Run Command: ",
                Text.of(ChatColor.BLUE, command),
                TextAction.Click.suggestCommand(command)
        ));

        BaseComponent[] clickWrapper = builder.build();
        recipients.forEach((recipient) -> {
            recipient.sendMessage(clickWrapper);
        });

        return true;
    }
}
