/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild;

import com.destroystokyo.paper.Title;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.info.InfoComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.chatbridge.ChatBridgeComponent;
import gg.packetloss.grindstone.data.DatabaseComponent;
import gg.packetloss.grindstone.events.guild.GuildGrantExpEvent;
import gg.packetloss.grindstone.events.guild.GuildLevelUpEvent;
import gg.packetloss.grindstone.guild.base.GuildBase;
import gg.packetloss.grindstone.guild.base.NinjaBase;
import gg.packetloss.grindstone.guild.base.RogueBase;
import gg.packetloss.grindstone.guild.db.PlayerGuildDatabase;
import gg.packetloss.grindstone.guild.db.sql.SQLPlayerGuildDatabase;
import gg.packetloss.grindstone.guild.listener.GuildCombatXPListener;
import gg.packetloss.grindstone.guild.listener.NinjaListener;
import gg.packetloss.grindstone.guild.listener.RogueListener;
import gg.packetloss.grindstone.guild.passive.PotionMetabolizer;
import gg.packetloss.grindstone.guild.powers.GuildPower;
import gg.packetloss.grindstone.guild.setting.GuildSettingConverter;
import gg.packetloss.grindstone.guild.state.*;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.highscore.scoretype.ScoreType;
import gg.packetloss.grindstone.highscore.scoretype.ScoreTypes;
import gg.packetloss.grindstone.util.PluginTaskExecutor;
import gg.packetloss.grindstone.util.StringUtil;
import gg.packetloss.grindstone.util.task.promise.TaskFuture;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldGetQuery;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.math.BigInteger;
import java.util.*;

@ComponentInformation(friendlyName = "Guild Management", desc = "Guild core systems and services")
@Depend(components = {
    AdminComponent.class, DatabaseComponent.class, HighScoresComponent.class,
    ChatBridgeComponent.class, ManagedWorldComponent.class
})
public class GuildComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private AdminComponent admin;
    @InjectComponent
    private HighScoresComponent highScores;
    @InjectComponent
    private ChatBridgeComponent chatBridge;
    @InjectComponent
    private ManagedWorldComponent managedWorld;

    private final PlayerGuildDatabase database = new SQLPlayerGuildDatabase();
    private final Map<UUID, InternalGuildState> guildStateMap = new HashMap<>();
    private final Map<GuildType, GuildBase> guildBaseMap = new EnumMap<>(GuildType.class);

    private static GuildComponent guildInst;

    public static GuildComponent inst() {
        return guildInst;
    }

    @Override
    public void enable() {
        guildInst = this;

        CommandBook.registerEvents(this);

        CommandBook.registerEvents(new GuildCombatXPListener(this::getState));

        CommandBook.registerEvents(new NinjaListener(this::internalGetState));
        CommandBook.registerEvents(new RogueListener(this::internalGetState));

        Bukkit.getScheduler().scheduleSyncRepeatingTask(
                CommandBook.inst(),
                new PotionMetabolizer(this::internalGetState),
                20 * 2,
                11
        );

        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            GuildSettingConverter.register(registrar);

            registrar.registerAsSubCommand("guild", "Guild commands", (guildRegistrar) -> {
                guildRegistrar.register(GuildCommandsRegistration.builder(), new GuildCommands(this));
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

    private TaskFuture<Optional<InternalGuildState>> constructGuildState(Player player) {
        return TaskFuture.asyncTask(() -> {
            return database.loadGuild(player.getUniqueId());
        });
    }

    private InternalGuildState constructDefaultGuildState(GuildType type) {
        return switch (type) {
            case ROGUE -> new RogueState(0, new RogueStateSettings());
            case NINJA -> new NinjaState(0, new NinjaStateSettings());
        };
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        constructGuildState(player).thenAccept((optInternalGuildState) -> {
            optInternalGuildState.ifPresent((internalGuildState -> {
                guildStateMap.put(player.getUniqueId(), internalGuildState);
                getState(player, internalGuildState).enablePowers();
            }));
        });
    }

    private void update(Player player, InternalGuildState state) {
        highScores.update(player, getScoreType(state), BigInteger.valueOf((long) state.getExperience()));
        database.updateActive(player.getUniqueId(), state);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        InternalGuildState internalState = guildStateMap.remove(player.getUniqueId());
        if (internalState == null) {
            return;
        }

        PluginTaskExecutor.submitAsync(() -> {
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
        ).fadeIn(10).stay(20).fadeOut(10).build());
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
