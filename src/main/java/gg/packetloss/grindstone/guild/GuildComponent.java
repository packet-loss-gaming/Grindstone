/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild;

import com.destroystokyo.paper.Title;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.sk89q.commandbook.component.info.InfoComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.chatbridge.ChatBridgeComponent;
import gg.packetloss.grindstone.events.guild.GuildGrantExpEvent;
import gg.packetloss.grindstone.events.guild.GuildLevelUpEvent;
import gg.packetloss.grindstone.guild.base.GuildBase;
import gg.packetloss.grindstone.guild.base.NinjaBase;
import gg.packetloss.grindstone.guild.base.RogueBase;
import gg.packetloss.grindstone.guild.db.PlayerGuildDatabase;
import gg.packetloss.grindstone.guild.db.mysql.MySQLPlayerGuildDatabase;
import gg.packetloss.grindstone.guild.listener.GuildCombatXPListener;
import gg.packetloss.grindstone.guild.listener.NinjaListener;
import gg.packetloss.grindstone.guild.listener.RogueListener;
import gg.packetloss.grindstone.guild.passive.PotionMetabolizer;
import gg.packetloss.grindstone.guild.powers.GuildPower;
import gg.packetloss.grindstone.guild.setting.GuildSettingConverter;
import gg.packetloss.grindstone.guild.state.GuildState;
import gg.packetloss.grindstone.guild.state.InternalGuildState;
import gg.packetloss.grindstone.guild.state.NinjaState;
import gg.packetloss.grindstone.guild.state.RogueState;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.highscore.scoretype.ScoreType;
import gg.packetloss.grindstone.highscore.scoretype.ScoreTypes;
import gg.packetloss.grindstone.util.StringUtil;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldGetQuery;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Guild Management", desc = "Guild core systems and services")
public class GuildComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent admin;
    @InjectComponent
    private HighScoresComponent highScores;
    @InjectComponent
    private ChatBridgeComponent chatBridge;
    @InjectComponent
    private ManagedWorldComponent managedWorld;

    private PlayerGuildDatabase database = new MySQLPlayerGuildDatabase();
    private Map<UUID, InternalGuildState> guildStateMap = new HashMap<>();
    private Map<GuildType, GuildBase> guildBaseMap = new EnumMap<>(GuildType.class);

    private static GuildComponent guildInst;

    public static GuildComponent inst() {
        return guildInst;
    }

    @SuppressWarnings("AccessStaticViaInstance")
    @Override
    public void enable() {
        guildInst = this;

        inst.registerEvents(this);

        inst.registerEvents(new GuildCombatXPListener(this::getState));

        inst.registerEvents(new NinjaListener(this::internalGetState));
        inst.registerEvents(new RogueListener(this::internalGetState));

        server.getScheduler().scheduleSyncRepeatingTask(
                inst,
                new PotionMetabolizer(this::internalGetState),
                20 * 2,
                11
        );

        ComponentCommandRegistrar registrar = CommandBook.getComponentRegistrar();
        registrar.registerTopLevelCommands((commandManager, registration) -> {
            GuildSettingConverter.register(commandManager);

            registrar.registerAsSubCommand("guild", "Guild commands", commandManager, (innerCommandManager, innerRegistration) -> {
                innerRegistration.register(innerCommandManager, GuildCommandsRegistration.builder(), new GuildCommands(this));
            });
        });

        guildBaseMap.put(GuildType.NINJA, new NinjaBase(managedWorld.get(ManagedWorldGetQuery.CITY)));
        guildBaseMap.put(GuildType.ROGUE, new RogueBase(managedWorld.get(ManagedWorldGetQuery.CITY)));
    }

    @Override
    public void disable() {
        for (Map.Entry<UUID, InternalGuildState> entry: guildStateMap.entrySet()) {
            update(Bukkit.getPlayer(entry.getKey()), entry.getValue());
        }
    }

    private InternalGuildState internalGetState(Player player) {
        return guildStateMap.get(player.getUniqueId());
    }

    private GuildState getState(Player player, InternalGuildState internalGuildState) {
        return new GuildState(player, internalGuildState, guildBaseMap.get(internalGuildState.getType()));
    }

    public Optional<GuildState> getState(Player player) {
        InternalGuildState baseState = internalGetState(player);
        if (baseState == null) {
            return Optional.empty();
        }

        return Optional.of(getState(player, baseState));
    }

    private CompletableFuture<Optional<InternalGuildState>> constructGuildState(Player player) {
        CompletableFuture<Optional<InternalGuildState>> future = new CompletableFuture<>();

        server.getScheduler().runTaskAsynchronously(inst, () -> {
            future.complete(database.loadGuild(player.getUniqueId()));
        });

        return future;
    }

    private InternalGuildState constructDefaultGuildState(GuildType type) {
        switch (type) {
            case ROGUE:
                return new RogueState(0);
            case NINJA:
                return new NinjaState(0);
        }
        throw new UnsupportedOperationException();
    }

    private String getGuildName(InternalGuildState state) {
        return state.getType().name().toLowerCase();
    }

    private void announceGuildJoin(Player player, String guildName) {
        String baseMessage = player.getDisplayName() + " has joined the " + guildName + " guild!";
        chatBridge.broadcast(baseMessage);
        Bukkit.broadcast(Text.of(ChatColor.GOLD, baseMessage).build());
    }

    public boolean joinGuild(Player player, GuildType guildType) {
        UUID playerID = player.getUniqueId();

        InternalGuildState currentGuild = guildStateMap.get(playerID);
        if (currentGuild != null) {
            if (currentGuild.getType() == guildType) {
                return false;
            }

            // Sync the current guild
            update(player, currentGuild);
        }

        Optional<InternalGuildState> optNewGuildState = database.loadGuild(playerID, guildType);
        InternalGuildState newGuildState;
        if (optNewGuildState.isPresent()) {
            newGuildState = optNewGuildState.get();

            // Allow admins to switch guilds for free while in admin mode, for testing purposes
            if (!admin.isAdmin(player)) {
                double reduction = Math.min(newGuildState.getExperience() * .1, 1000);
                newGuildState.setExperience(Math.max(0, newGuildState.getExperience() - reduction));
            }
        } else {
            newGuildState = constructDefaultGuildState(guildType);
        }

        // Don't announce if this is an admin change
        if (!admin.isAdmin(player)) {
            announceGuildJoin(player, getGuildName(newGuildState));
        }

        // Sync the new guild
        update(player, newGuildState);
        guildStateMap.put(playerID, newGuildState);

        // Swap powers
        if (currentGuild != null) {
            getState(player, currentGuild).disablePowers();
        }
        getState(player, newGuildState).enablePowers();

        return true;
    }

    private ScoreType getScoreType(InternalGuildState state) {
        switch (state.getType()) {
            case NINJA:
                return ScoreTypes.NINJA_LEVEL;
            case ROGUE:
                return ScoreTypes.ROGUE_LEVEL;
        }

        throw new IllegalStateException();
    }

    private void update(Player player, InternalGuildState state) {
        highScores.update(player, getScoreType(state), (int) state.getExperience());
        database.updateActive(player.getUniqueId(), state);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        constructGuildState(player).thenAccept((optInternalGuildState) -> {
            optInternalGuildState.ifPresent((internalGuildState -> {
                server.getScheduler().runTask(inst, () -> {
                    guildStateMap.put(player.getUniqueId(), internalGuildState);

                    getState(player, internalGuildState).enablePowers();
                });
            }));
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        InternalGuildState internalState = guildStateMap.remove(player.getUniqueId());
        if (internalState == null) {
            return;
        }

        server.getScheduler().runTaskAsynchronously(inst, () -> {
           update(player, internalState);
        });

        getState(player, internalState).disablePowers();
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

    private void showTitleForNewLevel(Player player, String guildName, int newLevel) {
        player.sendTitle(Title.builder().title(
                Text.of(
                        ChatColor.GOLD,
                        "LEVEL UP"
                ).build()
        ).subtitle(
                Text.of(
                        ChatColor.GOLD,
                        StringUtil.toTitleCase(guildName),
                        " Level ",
                        newLevel
                ).build()
        ).build());
    }

    private void announceNewLevel(Player player, String guildName, int newLevel) {
        String baseMessage = player.getDisplayName() + " is now " + guildName + " level " + newLevel + "!";
        chatBridge.broadcast(baseMessage);
        Bukkit.broadcast(Text.of(ChatColor.GOLD, baseMessage).build());
    }

    private void showNewPowers(Player player, InternalGuildState state, int newLevel) {
        List<GuildPower> newPowers = new ArrayList<>();

        for (GuildPower power : state.getType().getPowers()) {
            // Note: We could break once we reach a level past newLevel, but this is relatively
            // cheap, and we could introduce subtle bugs if powers were to even get temporarily out
            // of order.
            if (power.getUnlockLevel() == newLevel) {
                newPowers.add(power);
            }
        }

        if (newPowers.isEmpty()) {
            return;
        }

        player.sendMessage(Text.of(ChatColor.YELLOW, "Powers unlocked:").build());
        for (GuildPower power : newPowers) {
            player.sendMessage(Text.of(
                    ChatColor.YELLOW, " - ",
                    ChatColor.DARK_GREEN, StringUtil.toTitleCase(power.name())
            ).build());
        }
    }

    private void grantExp(Player player, InternalGuildState state, double exp) {
        double currentExp = state.getExperience();

        state.setExperience(currentExp + exp);

        GuildLevel.getNewLevel(currentExp, exp).ifPresent((newLevel) -> {
            CommandBook.callEvent(new GuildLevelUpEvent(player, state.getType(), newLevel));

            String guildName = getGuildName(state);
            showTitleForNewLevel(player, guildName, newLevel);
            announceNewLevel(player, guildName, newLevel);

            showNewPowers(player, state, newLevel);

            update(player, state);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGuildGrantExp(GuildGrantExpEvent event) {
        Player player = event.getPlayer();
        InternalGuildState state = internalGetState(player);

        Validate.notNull(state);
        Validate.isTrue(state.isEnabled());
        Validate.isTrue(state.getType() == event.getGuild());

        grantExp(player, state, event.getGrantedExp());
    }
}
