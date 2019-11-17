package gg.packetloss.grindstone.helper;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.Collection;

public class CommandResponse implements Response {
    @Override
    public boolean accept(Player player, Collection<Player> recipients, String string) {
        if (!string.matches("\\./.*")) {
            return false;
        }

        // Remove the comment
        String rawCommand = string.replaceFirst("//.*", "");
        // Replace the "./" with "/" and then trim the string
        String command = rawCommand.replaceFirst("\\./", "/").trim();
        // Remove the command, and the comment block, as well as its spaces
        String message = string.replace(rawCommand, "").replaceFirst("// *", "").trim();

        // Send a composite message of the command, a space, and then the comment text
        TextComponent clickableText = new TextComponent("Run Command: " + command);
        clickableText.setColor(ChatColor.DARK_GREEN);
        clickableText.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));

        if (!message.isEmpty()) {
            clickableText.addExtra(" " + message);
        }

        ComponentBuilder clickWrapperBuilder = new ComponentBuilder("");
        clickWrapperBuilder.append(TextComponent.fromLegacyText(getNamePlate()));
        clickWrapperBuilder.append(clickableText);

        BaseComponent[] clickWrapper = clickWrapperBuilder.create();

        recipients.forEach((recipient) -> {
            recipient.sendMessage(clickWrapper);
        });

        return true;
    }
}
