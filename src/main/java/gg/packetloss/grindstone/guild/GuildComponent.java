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
import gg.packetloss.grindstone.events.guild.GuildLevelUpEvent;
import gg.packetloss.grindstone.guild.db.PlayerGuildDatabase;
import gg.packetloss.grindstone.guild.db.mysql.MySQLPlayerGuildDatabase;
import gg.packetloss.grindstone.guild.listener.NinjaListener;
import gg.packetloss.grindstone.guild.listener.RogueListener;
import gg.packetloss.grindstone.guild.passive.PotionMetabolizer;
import gg.packetloss.grindstone.guild.setting.GuildSettingConverter;
import gg.packetloss.grindstone.guild.state.GuildState;
import gg.packetloss.grindstone.guild.state.InternalGuildState;
import gg.packetloss.grindstone.guild.state.NinjaState;
import gg.packetloss.grindstone.guild.state.RogueState;
import gg.packetloss.grindstone.util.StringUtil;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Guild Management", desc = "Guild core systems and services")
public class GuildComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent admin;

    private PlayerGuildDatabase database = new MySQLPlayerGuildDatabase();
    private Map<UUID, InternalGuildState> guildStateMap = new HashMap<>();

    private static GuildComponent guildInst;

    public static GuildComponent inst() {
        return guildInst;
    }

    @SuppressWarnings("AccessStaticViaInstance")
    @Override
    public void enable() {
        guildInst = this;

        inst.registerEvents(this);

        inst.registerEvents(new NinjaListener(this::internalGetState));
        inst.registerEvents(new RogueListener(this::internalGetState));

        server.getScheduler().scheduleSyncRepeatingTask(
                inst,
                new PotionMetabolizer(this::internalGetState),
                20 * 2,
                11
        );

        ComponentCommandRegistrar registrar = CommandBook.getComponentRegistrar();
        GuildSettingConverter.register(registrar);
        registrar.registerTopLevelCommands((commandManager, registration) -> {
            registrar.registerAsSubCommand("guild", "Guild commands", commandManager, (innerCommandManager, innerRegistration) -> {
                innerRegistration.register(innerCommandManager, GuildCommandsRegistration.builder(), new GuildCommands(this));
            });
        });
    }

    @Override
    public void disable() {
        for (Map.Entry<UUID, InternalGuildState> entry: guildStateMap.entrySet()) {
            database.updateActive(entry.getKey(), entry.getValue());
        }
    }

    private InternalGuildState internalGetState(Player player) {
        return guildStateMap.get(player.getUniqueId());
    }

    public Optional<GuildState> getState(Player player) {
        InternalGuildState baseState = internalGetState(player);
        if (baseState == null) {
            return Optional.empty();
        }

        return Optional.of(new GuildState(player, baseState));
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

    public boolean joinGuild(Player player, GuildType guildType) {
        UUID playerID = player.getUniqueId();

        InternalGuildState currentGuild = guildStateMap.get(playerID);
        if (currentGuild != null) {
            if (currentGuild.getType() == guildType) {
                return false;
            }

            // Sync the current guild
            database.updateActive(playerID, currentGuild);
        }

        Optional<InternalGuildState> optNewGuildState = database.loadGuild(playerID, guildType);
        InternalGuildState newGuildState;
        if (optNewGuildState.isPresent()) {
            newGuildState = optNewGuildState.get();

            // Allow admins to switch guilds for free while in admin mode, for testing purposes
            if (!admin.isAdmin(player)) {
                newGuildState.setExperience(newGuildState.getExperience() * .9);
            }
        } else {
            newGuildState = constructDefaultGuildState(guildType);
        }

        // Sync the new guild
        database.updateActive(playerID, newGuildState);
        guildStateMap.put(playerID, newGuildState);

        // Swap powers
        if (currentGuild != null) {
            new GuildState(player, currentGuild).disablePowers();
        }
        new GuildState(player, newGuildState).enablePowers();

        return true;
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        constructGuildState(player).thenAccept((optInternalGuildState) -> {
            optInternalGuildState.ifPresent((internalGuildState -> {
                server.getScheduler().runTask(inst, () -> {
                    guildStateMap.put(player.getUniqueId(), internalGuildState);

                    new GuildState(player, internalGuildState).enablePowers();
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
           database.updateActive(player.getUniqueId(), internalState);
        });

        new GuildState(player, internalState).disablePowers();
    }

    private void grantExp(Player player, InternalGuildState state, double exp) {
        double currentExp = state.getExperience();

        state.setExperience(currentExp + exp);

        GuildLevel.getNewLevel(currentExp, exp).ifPresent((newLevel) -> {
            CommandBook.callEvent(new GuildLevelUpEvent(player, state.getType(), newLevel));

            player.sendTitle(Title.builder().title(
                    Text.of(
                            ChatColor.GOLD,
                            "LEVEL UP"
                    ).build()
            ).subtitle(
                    Text.of(
                            ChatColor.GOLD,
                            StringUtil.toTitleCase(state.getType().name()),
                            " Level ",
                            newLevel
                    ).build()
            ).build());
        });
    }

    private static EDBEExtractor<Player, LivingEntity, Arrow> extractor = new EDBEExtractor<>(
            Player.class,
            LivingEntity.class,
            Arrow.class
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() < 1) {
            return;
        }

        CombatantPair<Player, LivingEntity, Arrow> result = extractor.extractFrom(event);
        if (result == null) return;

        final Player attacker = result.getAttacker();
        InternalGuildState state = internalGetState(attacker);
        if (state == null) {
            return;
        }

        if (!state.isEnabled()) {
            return;
        }

        double maxDamage = Math.min(500, Math.min(result.getDefender().getMaxHealth(), event.getFinalDamage()));
        grantExp(attacker, state, maxDamage * .1);
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
}
