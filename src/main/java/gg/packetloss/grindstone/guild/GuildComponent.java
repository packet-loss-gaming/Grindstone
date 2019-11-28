package gg.packetloss.grindstone.guild;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.InfoComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.guild.listener.NinjaListener;
import gg.packetloss.grindstone.guild.listener.RogueListener;
import gg.packetloss.grindstone.guild.passive.PotionMetabolizer;
import gg.packetloss.grindstone.guild.state.GuildState;
import gg.packetloss.grindstone.guild.state.InternalGuildState;
import gg.packetloss.grindstone.guild.state.NinjaState;
import gg.packetloss.grindstone.guild.state.RogueState;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Guild Management", desc = "Guild core systems and services")
public class GuildComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private Map<UUID, InternalGuildState> guildStateMap = new HashMap<>();

    @SuppressWarnings("AccessStaticViaInstance")
    @Override
    public void enable() {
        inst.registerEvents(this);

        inst.registerEvents(new NinjaListener(this::internalGetState));
        inst.registerEvents(new RogueListener(this::internalGetState));

        server.getScheduler().scheduleSyncRepeatingTask(
                inst,
                new PotionMetabolizer(this::internalGetState),
                20 * 2,
                11
        );

        registerCommands(Commands.class);
    }

    private InternalGuildState internalGetState(Player player) {
        return guildStateMap.get(player.getUniqueId());
    }

    public boolean inGuild(GuildType guild, Player player) {
        switch (guild) {
            case ROGUE:
                if (player.hasPermission("aurora.rogue")) {
                    return true;
                }
                break;
            case NINJA:
                if (player.hasPermission("aurora.ninja")) {
                    return true;
                }
                break;
        }

        return false;
    }

    public Optional<GuildType> getGuild(Player player) {
        for (GuildType guildType : GuildType.values()) {
            if (inGuild(guildType, player)) {
                return Optional.of(guildType);
            }
        }

        return Optional.empty();
    }

    public Optional<GuildState> getState(Player player) {
        InternalGuildState baseState = guildStateMap.get(player.getUniqueId());
        if (baseState == null) {
            return Optional.empty();
        }

        return Optional.of(new GuildState(player, baseState));
    }

    private GuildState constructGuildState(Player player, GuildType type) {
        UUID playerID = player.getUniqueId();

        switch (type) {
            case NINJA:
                guildStateMap.putIfAbsent(playerID, new NinjaState());
            case ROGUE:
                guildStateMap.putIfAbsent(playerID, new RogueState());
        }

        return new GuildState(player, guildStateMap.get(playerID));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        getGuild(player).ifPresent((guildType) -> {
            constructGuildState(player, guildType).enablePowers();
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        getState(player).ifPresent(GuildState::disablePowers);
    }

    @EventHandler(ignoreCancelled = true)
    public void onWhoisLookup(InfoComponent.PlayerWhoisEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();

            getState(player).ifPresent((guild) -> {
               event.addWhoisInformation("Guild", guild.getType().name());
               event.addWhoisInformation("Guild Powers Enabled", guild.isEnabled());
            });
        }
    }

    public class Commands {
        @Command(aliases = {"guild"}, desc = "Apply guild powers",
                flags = "", min = 0, max = 0)
        public void guildCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            Optional<GuildState> optState = getState(player);
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

        @Command(aliases = {"deguild", "unguild"}, desc = "Strip guild powers",
                flags = "", min = 0, max = 0)
        public void deguild(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            Optional<GuildState> optState = getState(player);
            if (optState.isEmpty()) {
                throw new CommandException("You are not in a guild!");
            }

            GuildState state = optState.get();
            if (state.isEnabled()) {
                throw new CommandException("Your powers have already faded!");
            }

            state.disablePowers();

            if (state.isEnabled()) {
                throw new CommandException("Your powers refuse to leave!");
            }
        }
    }
}
