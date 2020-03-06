package gg.packetloss.grindstone.guild;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import gg.packetloss.grindstone.guild.setting.GuildSettingUpdate;
import gg.packetloss.grindstone.guild.state.GuildState;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

import java.util.Optional;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class GuildCommands {
    private GuildComponent component;

    public GuildCommands(GuildComponent component) {
        this.component = component;
    }

    @Command(name = "enable", desc = "Apply guild powers")
    public void guildCmd(Player player) throws CommandException {
        Optional<GuildState> optState = component.getState(player);
        if (optState.isEmpty()) {
            throw new CommandException("You are not in a guild!");
        }

        GuildState state = optState.get();
        if (state.isEnabled()) {
            throw new CommandException("You already have your powers!");
        }

        state.enablePowers();

        if (state.isDisabled()) {
            throw new CommandException("Your powers failed to apply!");
        }
    }

    @Command(name = "setvirtuallevel", desc = "Set virtual level information")
    @CommandPermissions({"aurora.admin.guild.setvirtuallevel"})
    public void guildVirtualLevelCmd(Player player,
                                     @Switch(name = 'c', desc = "Clear") boolean clear,
                                     @Arg(name = "level", desc = "desired level", def = "100") int level) throws CommandException {
        Optional<GuildState> optState = component.getState(player);
        if (optState.isEmpty()) {
            throw new CommandException("You are not in a guild!");
        }

        GuildState state = optState.get();

        if (clear) {
            state.clearVirtualLevel();
            ChatUtil.sendNotice(player, "Virtual level cleared.");
            return;
        }

        state.setVirtualLevel(level);
        ChatUtil.sendNotice(player, "Virtual level set to: " + level);
    }

    @Command(name = "level", desc = "View level information")
    public void guildLevelCmd(Player player,
                              @ArgFlag(name = 'p', desc = "Page of results to return", def = "1") int page) throws CommandException {
        Optional<GuildState> optState = component.getState(player);
        if (optState.isEmpty()) {
            throw new CommandException("You are not in a guild!");
        }

        GuildState state = optState.get();
        state.sendLevelChart(player, page);
    }

    @Command(name = "settings", desc = "View level information")
    public void guildSettingsCmd(Player player,
                                 @Arg(desc = "Setting update", def = "") GuildSettingUpdate setting) throws CommandException {
        Optional<GuildState> optState = component.getState(player);
        if (optState.isEmpty()) {
            throw new CommandException("You are not in a guild!");
        }

        GuildState state = optState.get();
        if (setting == null) {
            state.sendSettings(player);
        } else {
            if (state.updateSetting(setting)) {
                state.sendSettings(player);
            } else {
                ChatUtil.sendError(player, "Setting updated failed.");
            }
        }
    }

    @Command(name = "disable", desc = "Strip guild powers")
    public void deguild(Player player) throws CommandException {
        Optional<GuildState> optState = component.getState(player);
        if (optState.isEmpty()) {
            throw new CommandException("You are not in a guild!");
        }

        GuildState state = optState.get();
        if (state.isDisabled()) {
            throw new CommandException("Your powers have already faded!");
        }

        state.disablePowers();

        if (state.isEnabled()) {
            throw new CommandException("Your powers refuse to leave!");
        }
    }
}
