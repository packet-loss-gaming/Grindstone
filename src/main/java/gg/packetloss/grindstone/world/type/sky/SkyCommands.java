package gg.packetloss.grindstone.world.type.sky;

import com.sk89q.commandbook.command.argument.SinglePlayerTarget;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

@CommandContainer
public class SkyCommands {
    private final SkyWorldCoreComponent component;

    public SkyCommands(SkyWorldCoreComponent component) {
        this.component = component;
    }

    @Command(name = "invite", desc = "Invite a player for a temporary visit to the sky world")
    public void inviteCmd(Player sender, @Arg(desc = "the player to invite") SinglePlayerTarget target) {
        if (!component.hasAccess(sender)) {
            ChatUtil.sendError(sender, "You must have access to the sky world to invite players.");
            return;
        }

        if (!component.isSkyWorld(sender.getWorld())) {
            ChatUtil.sendError(sender, "You must be in the sky world to invite players.");
            return;
        }

        Player targetPlayer = target.get();
        if (component.isForbiddenFromEntering(targetPlayer)) {
            ChatUtil.sendError(sender, targetPlayer.getDisplayName() + " is banned from entry by administrator.");
            return;
        }

        component.registerInvite(sender, targetPlayer);

        ChatUtil.sendNotice(targetPlayer, "You've been invited to " + sender.getWorld().getName() + ".");
        targetPlayer.sendMessage(Text.of(
                TextAction.Hover.showText(Text.of("Click to accept.")),
                TextAction.Click.runCommand("/sky accept"),
                ChatColor.YELLOW,
                "Click to ",
                Text.of(ChatColor.BLUE, "ACCEPT"),
                "."
        ).build());

        ChatUtil.sendNotice(sender, "Invite sent.");
    }

    @Command(name = "accept", desc = "Accept an invite to the sky world")
    public void acceptCmd(Player sender) {
        component.acceptInvite(sender).thenAccept(accepted -> {
            if (accepted) {
                ChatUtil.sendNotice(sender, "Welcome to " + sender.getWorld().getName() + ".");
                return;
            }

            ChatUtil.sendError(sender, "Your invite is no longer valid.");
        });
    }
}
