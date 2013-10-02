package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.skelril.Pitfall.bukkit.event.PitfallTriggerEvent;
import com.skelril.aurora.events.PrePrayerApplicationEvent;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.HorseJumpEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Ninja", desc = "Disappear into the night!")
@Depend(plugins = "Pitfall", components = {SessionComponent.class, RogueComponent.class})
public class NinjaComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private RogueComponent rogueComponent;

    private Map<Arrow, Float> arrowForce = new HashMap<>();

    private final int WATCH_DISTANCE = 10;
    private final int WATCH_DISTANCE_SQ = WATCH_DISTANCE * WATCH_DISTANCE;
    private final int SNEAK_WATCH_DISTANCE = 3;
    private final int SNEAK_WATCH_DISTANCE_SQ = SNEAK_WATCH_DISTANCE * SNEAK_WATCH_DISTANCE;

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 11);
    }

    // Player Management
    public void ninjaPlayer(Player player) {

        sessions.getSession(NinjaState.class, player).setIsNinja(true);
    }

    public boolean isNinja(Player player) {

        return sessions.getSession(NinjaState.class, player).isNinja();
    }

    public void showToGuild(Player player, boolean showToGuild) {

        sessions.getSession(NinjaState.class, player).showToGuild(showToGuild);
    }

    public boolean guildCanSee(Player player) {

        return sessions.getSession(NinjaState.class, player).guildCanSee();
    }

    public void useExplosiveArrows(Player player, boolean explosiveArrows) {

        sessions.getSession(NinjaState.class, player).useExplosiveArrows(explosiveArrows);
    }

    public boolean hasExplosiveArrows(Player player) {

        return sessions.getSession(NinjaState.class, player).hasExplosiveArrows();
    }

    public boolean allowsConflictingPotions(Player player) {

        return sessions.getSession(NinjaState.class, player).allowsConflictingPotions();
    }

    public void allowConflictingPotions(Player player, boolean allowConflictingPotions) {

        sessions.getSession(NinjaState.class, player).allowConflictingPotions(allowConflictingPotions);
    }

    public void unninjaPlayer(Player player) {

        NinjaState session = sessions.getSession(NinjaState.class, player);
        session.setIsNinja(false);
        session.showToGuild(false);

        player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);

        for (final Player otherPlayer : server.getOnlinePlayers()) {
            // Show Yourself!
            if (otherPlayer != player)
                otherPlayer.showPlayer(player);
        }
    }

    @EventHandler
    public void onProjectileLaunch(EntityShootBowEvent event) {

        Entity e = event.getProjectile();
        Projectile p = e instanceof Projectile ? (Projectile) e : null;
        if (p == null || p.getShooter() == null || !(p.getShooter() instanceof Player)) return;

        Player player = (Player) p.getShooter();
        if (isNinja(player) && hasExplosiveArrows(player) && inst.hasPermission(player, "aurora.ninja.guild")) {

            if (p instanceof Arrow) {
                arrowForce.put((Arrow) p, event.getForce());
            }
        }
    }

    @EventHandler
    public void onProjectileLand(ProjectileHitEvent event) {

        Projectile p = event.getEntity();
        if (p.getShooter() == null || !(p.getShooter() instanceof Player)) return;
        if (isNinja((Player) p.getShooter())) {

            if (p instanceof Arrow && arrowForce.containsKey(p)) {
                p.getWorld().createExplosion(p.getLocation(), 3F * arrowForce.get(p), true);
                arrowForce.remove(p);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHorseJump(HorseJumpEvent event) {

        Entity passenger = event.getEntity().getPassenger();
        if (passenger != null && passenger instanceof Player && isNinja((Player) passenger)) {
            event.setPower(event.getPower() * 1.37F);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        for (final Player otherPlayer : server.getOnlinePlayers()) {
            if (otherPlayer != player) otherPlayer.showPlayer(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPitfallEvent(PitfallTriggerEvent event) {

        Entity entity = event.getEntity();

        if (entity instanceof Player) {
            Player player = (Player) entity;

            if (isNinja(player) && player.isSneaking() && inst.hasPermission(player, "aurora.ninja.guild")) {
                event.setCancelled(true);
            }
        }
    }

    private static Set<Integer> blockedEffects = new HashSet<>();

    static {
        blockedEffects.add(PotionEffectType.FIRE_RESISTANCE.getId());
        blockedEffects.add(PotionEffectType.WATER_BREATHING.getId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrePrayerApplicationEvent event) {

        Player player = event.getPlayer();
        if (isNinja(player) && inst.hasPermission(player, "aurora.ninja.guild")) {
            Iterator<PotionEffect> it = event.getCause().getEffect().getPotionEffects().iterator();
            while (it.hasNext()) {
                if (blockedEffects.contains(it.next().getType().getId())) {
                    if (!allowsConflictingPotions(player)) {
                        it.remove();
                    } else {
                        event.setCancelled(true);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void run() {

        for (NinjaState ninjaState : sessions.getSessions(NinjaState.class).values()) {
            if (!ninjaState.isNinja()) {
                continue;
            }

            Player player = ninjaState.getPlayer();

            // Stop this from breaking if the player isn't here
            if (player == null || !player.isOnline() || player.isDead()) continue;

            if (inst.hasPermission(player, "aurora.ninja.guild")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 45, 0), true);
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 45, 1), true);

                Entity vehicle = player.getVehicle();
                if (vehicle != null && vehicle instanceof Horse) {
                    ((Horse) vehicle).removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                    ((Horse) vehicle).removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
                    ((Horse) vehicle).addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 60, 1));
                    ((Horse) vehicle).addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 60, 1));
                }
            }

            Set<Player> invisibleNewCount = new HashSet<>();
            Set<Player> visibleNewCount = new HashSet<>();

            for (Player otherPlayer : server.getOnlinePlayers()) {
                if (otherPlayer != player) {
                    if (otherPlayer.getWorld().equals(player.getWorld())) {
                        if (player.getLocation().distanceSquared(otherPlayer.getLocation()) >= WATCH_DISTANCE_SQ
                                || (player.getLocation().distanceSquared(otherPlayer.getLocation()) >=
                                SNEAK_WATCH_DISTANCE_SQ
                                && player.isSneaking())) {
                            if (otherPlayer.canSee(player)
                                    && !(guildCanSee(player) && otherPlayer.hasPermission("aurora.ninja.guild"))
                                    && !inst.hasPermission(otherPlayer, "aurora.ninja.guild.master")) {
                                otherPlayer.hidePlayer(player);
                                invisibleNewCount.add(otherPlayer);
                            }
                        } else {
                            if (!otherPlayer.canSee(player)) {
                                otherPlayer.showPlayer(player);
                                visibleNewCount.add(otherPlayer);
                            }
                        }
                    } else {
                        if (!otherPlayer.canSee(player)) {
                            otherPlayer.showPlayer(player);
                            visibleNewCount.add(otherPlayer);
                        }
                    }
                }
            }

            if (invisibleNewCount.size() > 0) {
                if (invisibleNewCount.size() > 3) {
                    ChatUtil.sendNotice(player, "You are now invisible to multiple players.");
                } else {
                    for (Player playerThatCanNotSeePlayer : invisibleNewCount) {
                        ChatUtil.sendNotice(player, "You are now invisible to "
                                + playerThatCanNotSeePlayer.getDisplayName() + ".");
                    }
                }
            }

            if (visibleNewCount.size() > 0) {
                if (visibleNewCount.size() > 3) {
                    ChatUtil.sendNotice(player, "You are now visible to multiple players.");
                } else {
                    for (Player playerThatCanSeePlayer : visibleNewCount) {
                        ChatUtil.sendNotice(player, "You are now visible to "
                                + playerThatCanSeePlayer.getDisplayName() + ".");
                    }
                }
            }
        }
    }

    public class Commands {

        @Command(aliases = {"ninja"}, desc = "Give a player the Ninja power",
                flags = "gxp", min = 0, max = 0)
        @CommandPermissions({"aurora.ninja"})
        public void ninja(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");
            if (inst.hasPermission(sender, "aurora.rogue.guild") || rogueComponent.isRogue((Player) sender)) {
                throw new CommandException("You are a rogue not a ninja!");
            }

            ninjaPlayer((Player) sender);
            if (inst.hasPermission(sender, "aurora.ninja.guild")) {
                showToGuild((Player) sender, args.hasFlag('g'));
                useExplosiveArrows((Player) sender, !args.hasFlag('x'));
                allowConflictingPotions((Player) sender, !args.hasFlag('p'));
            } else if (args.getFlags().size() > 0) {
                ChatUtil.sendError(sender, "You must be a member of the ninja guild to use flags.");
            }
            ChatUtil.sendNotice(sender, "You are inspired and become a ninja!");
        }

        @Command(aliases = {"unninja"}, desc = "Revoke a player's Ninja power",
                flags = "", min = 0, max = 0)
        @CommandPermissions({"aurora.ninja"})
        public void unninja(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");
            if (!isNinja((Player) sender)) {
                throw new CommandException("You are not a ninja!");
            }

            unninjaPlayer((Player) sender);

            ChatUtil.sendNotice(sender, "You return to your previous boring existence.");
        }
    }

    // Ninja Session
    private static class NinjaState extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        private boolean isNinja = false;
        private boolean showToGuild = false;
        private boolean explosiveArrows = true;
        private boolean allowConflictingPotions = true;

        protected NinjaState() {

            super(MAX_AGE);
        }

        public boolean isNinja() {

            return isNinja;
        }

        public void setIsNinja(boolean isNinja) {

            this.isNinja = isNinja;
        }

        public boolean guildCanSee() {

            return showToGuild;
        }

        public void showToGuild(boolean showToGuild) {

            this.showToGuild = showToGuild;
        }

        public boolean hasExplosiveArrows() {

            return explosiveArrows;
        }

        public void useExplosiveArrows(boolean explosiveArrows) {

            this.explosiveArrows = explosiveArrows;
        }

        public boolean allowsConflictingPotions() {

            return allowConflictingPotions;
        }

        public void allowConflictingPotions(boolean allowConflictingPotions) {

            this.allowConflictingPotions = allowConflictingPotions;
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}