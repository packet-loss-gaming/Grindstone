package gg.packetloss.grindstone.guild.state;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.guild.GuildPowersDisableEvent;
import gg.packetloss.grindstone.events.guild.GuildPowersEnableEvent;
import gg.packetloss.grindstone.guild.GuildType;
import org.bukkit.entity.Player;

public class GuildState {
    private Player player;
    private InternalGuildState state;

    public GuildState(Player player, InternalGuildState state) {
        this.player = player;
        this.state = state;
    }

    public boolean isEnabled() {
        return state.isEnabled();
    }

    public boolean isDisabled() {
        return !isEnabled();
    }

    public GuildType getType() {
        return state.getType();
    }

    public boolean enablePowers() {
        GuildPowersEnableEvent event = new GuildPowersEnableEvent(player, state.getType());
        CommandBook.server().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        state.setEnabled(true);
        return true;
    }

    public boolean disablePowers() {
        GuildPowersDisableEvent event = new GuildPowersDisableEvent(player, state.getType());
        CommandBook.server().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        state.setEnabled(false);
        return true;
    }
}
