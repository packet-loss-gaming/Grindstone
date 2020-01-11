package gg.packetloss.grindstone.guild;

import com.sk89q.minecraft.util.commands.CommandException;
import gg.packetloss.grindstone.guild.state.GuildState;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.ArgFlag;

import java.util.Optional;

@CommandContainer
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
