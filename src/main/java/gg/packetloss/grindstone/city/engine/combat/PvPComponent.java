/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.combat;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldguard.bukkit.protection.events.DisallowedPVPEvent;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackPreDamageEvent;
import gg.packetloss.grindstone.exceptions.UnsupportedPrayerException;
import gg.packetloss.grindstone.homes.HomeManagerComponent;
import gg.packetloss.grindstone.prayer.Prayer;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.util.bridge.WorldEditBridge.toBlockVec3;

@ComponentInformation(friendlyName = "PvP", desc = "Skelril PvP management.")
@Depend(components = {SessionComponent.class, PrayerComponent.class}, plugins = "WorldGuard")
public class PvPComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private static SessionComponent sessions;
    @InjectComponent
    private PrayerComponent prayers;

    private static List<PvPScope> pvpLimitors = new ArrayList<>();

    @Override
    public void enable() {
        registerCommands(Commands.class);
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    public class Commands {
        @Command(aliases = {"pvp"},
                usage = "", desc = "Toggle PvP",
                flags = "s", min = 0, max = 0)
        public void pvpCmd(CommandContext args, CommandSender sender) throws CommandException {

            PlayerUtil.checkPlayer(sender);

            PvPSession session = sessions.getSession(PvPSession.class, sender);

            if (session.recentlyHit()) {
                throw new CommandException("You have been hit recently and cannot toggle PvP!");
            }

            if (!args.hasFlag('s') || !session.hasPvPOn()) {
                session.setPvP(!session.hasPvPOn());
                ChatUtil.sendNotice(sender, "Global PvP has been: " + (session.hasPvPOn() ? "enabled" : "disabled") + ".");

                session.useSafeSpots(!args.hasFlag('s'));
            } else {
                if (session.useSafeSpots()) {
                    session.useSafeSpots(!args.hasFlag('s'));
                } else {
                    session.useSafeSpots(args.hasFlag('s'));
                }
            }

            if (session.hasPvPOn()) {
                ChatUtil.sendNotice(sender, "Safe spots are: " + (session.useSafeSpots() ? "enabled" : "disabled") + ".");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {

        final Player player = event.getPlayer();
        PvPSession session = sessions.getSession(PvPSession.class, player);

        if (session.punishNextLogin()) {
            try {
                Prayer[] targetPrayers = new Prayer[]{
                        PrayerComponent.constructPrayer(player, PrayerType.GLASSBOX, 1000 * 60 * 3),
                        PrayerComponent.constructPrayer(player, PrayerType.STARVATION, 1000 * 60 * 3),
                };

                prayers.influencePlayer(player, targetPrayers);

                server.getScheduler().runTaskLater(inst,
                        () -> ChatUtil.sendWarning(player,
                                "You ran from a fight, the Giant Chicken does not approve!"), 1
                );
            } catch (UnsupportedPrayerException ignored) {
            }
        }
        session.wasKicked(false);
    }


    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {

        PvPSession session = sessions.getSession(PvPSession.class, event.getPlayer());

        if (session.recentlyHit()) {
            session.wasKicked(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        PvPSession session = sessions.getSession(PvPSession.class, event.getPlayer());

        if (session.recentlyHit() || session.punishNextLogin()) {
            session.punishNextLogin(true);
            return;
        }

        session.setPvP(false);
    }

    private static EDBEExtractor<Player, Player, Projectile> extractor = new EDBEExtractor<>(
            Player.class,
            Player.class,
            Projectile.class
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {

        CombatantPair<Player, Player, Projectile> result = extractor.extractFrom(event);

        if (result == null) return;

        sessions.getSession(PvPSession.class, result.getAttacker()).updateHit();
        sessions.getSession(PvPSession.class, result.getDefender()).updateHit();
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpecialAttackPreDamage(SpecialAttackPreDamageEvent event) {
        if (!(event.getAttacker() instanceof Player)) {
            return;
        }

        if (!(event.getDefender() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getAttacker();
        Player defender = (Player) event.getDefender();

        if (!allowsPvP(attacker, defender)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Optional<Player> optCreator = ExplosionStateFactory.getExplosionCreator();
        if (optCreator.isEmpty()) {
            return;
        }

        Player attacker = optCreator.get();
        Player defender = (Player) event.getEntity();

        if (!allowsPvP(attacker, defender)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        PvPSession pSession = sessions.getSession(PvPSession.class, player);
        pSession.resetHit();

        if (pSession.punishNextLogin()) {
            pSession.punishNextLogin(false);
        }

        Player killer = player.getKiller();
        if (killer != null) {
            sessions.getSession(PvPSession.class, killer).resetHit();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvPBlock(DisallowedPVPEvent event) {

        if (allowsPvP(event.getAttacker(), event.getDefender(), false)) event.setCancelled(true);
    }

    public static void registerScope(PvPScope scope) {
        pvpLimitors.add(scope);
    }

    public static void removeScope(PvPScope scope) {
        pvpLimitors.remove(scope);
    }

    public static boolean allowsPvP(Player attacker, Player defender) {

        return allowsPvP(attacker, defender, true);
    }

    private static boolean checkSafeZone(ApplicableRegionSet regions, Player attacker, Player defender) {
        for (ProtectedRegion region : regions) {
            if (HomeManagerComponent.isPlayerHouse(region, attacker) || HomeManagerComponent.isPlayerHouse(region, defender)) {
                return false;
            }
        }
        return true;
    }

    public static boolean allowsPvP(Player attacker, Player defender, boolean checkRegions) {
        if (attacker.equals(defender)) {
            return true;
        }

        PvPSession attackerSession = sessions.getSession(PvPSession.class, attacker);
        PvPSession defenderSession = sessions.getSession(PvPSession.class, defender);

        for (PvPScope scope : pvpLimitors) {
            if (!scope.checkFor(attacker, defender)) return false;
        }

        if (attackerSession.hasPvPOn() && defenderSession.hasPvPOn()) {
            if (attackerSession.useSafeSpots() || defenderSession.useSafeSpots()) {
                RegionManager manager = WorldGuardBridge.getManagerFor(attacker.getWorld());

                if (!checkSafeZone(manager.getApplicableRegions(toBlockVec3(attacker.getLocation())), attacker, defender)) {
                    return false;
                }

                if (!checkSafeZone(manager.getApplicableRegions(toBlockVec3(defender.getLocation())), attacker, defender)) {
                    return false;
                }
            }
            return true;
        } else if (checkRegions) {
            RegionManager manager = WorldGuardBridge.getManagerFor(attacker.getWorld());
            ApplicableRegionSet attackerApplicable = manager.getApplicableRegions(toBlockVec3(attacker.getLocation()));
            ApplicableRegionSet defenderApplicable = manager.getApplicableRegions(toBlockVec3(defender.getLocation()));

            return attackerApplicable.queryValue(WorldGuardBridge.wrap(attacker), Flags.PVP) == StateFlag.State.ALLOW &&
                    defenderApplicable.queryValue(WorldGuardBridge.wrap(attacker), Flags.PVP) == StateFlag.State.ALLOW;
        }
        return false;
    }
}
