package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.skelril.Pitfall.bukkit.event.PitfallTriggerEvent;
import com.skelril.aurora.events.CreepSpeakEvent;
import com.skelril.aurora.events.PrayerApplicationEvent;
import com.skelril.aurora.events.ThrowPlayerEvent;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ComponentInformation(friendlyName = "Rogue", desc = "Speed and strength is always the answer.")
@Depend(plugins = {"Pitfall"}, components = {SessionComponent.class, NinjaComponent.class})
public class RogueComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private NinjaComponent ninjaComponent;

    private Map<Player, Player> attacked = new HashMap<>();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 11);
    }

    // Player Management
    public void roguePlayer(Player player) {

        sessions.getSession(RogueState.class, player).setIsRogue(true);
    }

    public boolean isRogue(Player player) {

        return sessions.getSession(RogueState.class, player).isRogue();
    }

    public void setSilentMode(Player player, boolean bool) {

        sessions.getSession(RogueState.class, player).setSilentMode(bool);
    }

    public boolean isOnSilent(Player player) {

        return sessions.getSession(RogueState.class, player).isOnSilent();
    }

    public void showToGuild(Player player) {

        sessions.getSession(RogueState.class, player).showToGuild(true);
    }

    public boolean isShownToGuild(Player player) {

        return sessions.getSession(RogueState.class, player).guildCanSee();
    }

    public void hideFromGuild(Player player) {

        sessions.getSession(RogueState.class, player).showToGuild(false);
    }

    public void deroguePlayer(Player player) {

        RogueState session = sessions.getSession(RogueState.class, player);
        session.setIsRogue(false);
        session.showToGuild(false);

        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);

        showPlayer(player);
    }

    public void hidePlayer(Player player) {

        sessions.getSession(RogueState.class, player).setIsVisible(false);

        for (Player otherPlayer : player.getWorld().getPlayers()) {
            // Hide Yourself!
            if (!inst.hasPermission(otherPlayer, "aurora.rogue.guild.master")
                    && otherPlayer != attacked.get(player)
                    && otherPlayer != player
                    && otherPlayer.canSee(player)
                    && !(isShownToGuild(player) && otherPlayer.hasPermission("aurora.rogue.guild"))) {

                otherPlayer.hidePlayer(player);
            }
        }
    }

    public boolean isVisible(Player player) {

        return sessions.getSession(RogueState.class, player).isVisible();
    }

    public void showPlayer(Player player) {

        sessions.getSession(RogueState.class, player).setIsVisible(true);

        for (Player otherPlayer : server.getOnlinePlayers()) {
            // Show Yourself!
            if (otherPlayer != player && !otherPlayer.canSee(player)) otherPlayer.showPlayer(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSprint(PlayerToggleSprintEvent event) {

        Player player = event.getPlayer();

        // On Sneak
        if (isRogue(player) && event.isSprinting()) {
            if (attacked.containsKey(player) && attacked.get(player) == null) return;
            // Hide and tell the player
            hidePlayer(player);

            if (!isOnSilent(player)) {
                ChatUtil.sendNotice(player, "You disappear!");
            }
        }

        // On Leave Sneak
        if (isRogue(player) && !event.isSprinting()) {
            // Show and tell the player
            showPlayer(player);
            if (attacked.containsKey(player) && attacked.get(player) == null) return;

            if (!isOnSilent(player)) {
                ChatUtil.sendNotice(player, "You appear!");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBowFire(EntityShootBowEvent event) {

        if (event.getEntity() instanceof Player) {
            final Player player = (Player) event.getEntity();

            if (isRogue(player) && event.getForce() > .85) {
                showPlayer(player);
                ChatUtil.sendWarning(player, "Your bow fire allows everyone to see you!");
                attacked.put(player, null);
                // Create a new delayed task to remove the player after 35 seconds
                server.getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {

                    @Override
                    public void run() {

                        attacked.remove(player);
                    }
                }, (20 * 15)); // Multiply seconds by 20 to convert to ticks
            }
        }
    }

    // Stop Mobs from targeting Rogue
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {

        Entity entity = event.getEntity();
        Entity targetEntity = event.getTarget();

        if (entity instanceof Player || !(targetEntity instanceof Player)) return;

        Player player = (Player) targetEntity;

        if ((!isVisible(player) || (isRogue(player) && inst.hasPermission(player, "aurora.rogue.guild")))
                && !player.getWorld().isThundering()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreepSpeak(CreepSpeakEvent event) {

        Player player = event.getPlayer();

        if (!isVisible(player) || (isRogue(player) && inst.hasPermission(player, "aurora.rogue.guild"))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        for (Player otherPlayer : player.getWorld().getPlayers()) {
            // ReHide players
            if (!isVisible(otherPlayer)) {
                player.hidePlayer(otherPlayer);
            } else {
                player.showPlayer(otherPlayer);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {

        Entity attackingEntity = event.getDamager();
        Entity defendingEntity = event.getEntity();

        // Basic Check
        if (!(attackingEntity instanceof Player) || !(defendingEntity instanceof Player)) return;

        // Move these down here to ensure we don't throw errors
        final Player attacker = (Player) attackingEntity;
        final Player defender = (Player) defendingEntity;
        if (isVisible(attacker)) return;

        // Show the player who is being attacked who attacked him
        if (defender != attacked.get(attacker)) {
            defender.showPlayer(attacker);
            ChatUtil.sendWarning(attacker, "Your blow allows your enemy to see you!");
            attacked.put(attacker, defender);

            // Create a new delayed task to remove the player after 15 seconds
            server.getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {

                @Override
                public void run() {

                    attacked.remove(attacker);
                }
            }, (20 * 7)); // Multiply seconds by 20 to convert to ticks
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPitfallTrigger(PitfallTriggerEvent event) {

        Entity entity = event.getEntity();

        if (entity instanceof Player) {

            Player player = (Player) entity;

            if (isRogue(player) && inst.hasPermission(player, "aurora.rogue.guild")) {

                server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
                Vector vel = player.getLocation().getDirection();
                vel.multiply(3);
                vel.setY(Math.min(.8, Math.max(.175, vel.getY())));
                player.setVelocity(vel);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        if (isRogue(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void run() {

        for (RogueState rogueState : sessions.getSessions(RogueState.class).values()) {
            if (!rogueState.isRogue()) {
                continue;
            }

            Player player = rogueState.getPlayer();

            // Stop this from breaking if the player isn't here
            if (player == null || !player.isOnline() || player.isDead()) {
                continue;
            }

            if (inst.hasPermission(player, "aurora.rogue.guild")) {
                player.removePotionEffect(PotionEffectType.SPEED);
                player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 600, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 600, 6));
            }
        }
    }

    public class Commands {

        @Command(aliases = {"rogue"}, desc = "Give a player the Rogue power",
                flags = "gs", min = 0, max = 0)
        @CommandPermissions({"aurora.rogue"})
        public void rogue(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");
            if (inst.hasPermission(sender, "aurora.ninja.guild") || ninjaComponent.isNinja((Player) sender)) {
                throw new CommandException("You are a ninja not a rogue!");
            }

            roguePlayer(PlayerUtil.matchSinglePlayer(sender, sender.getName()));
            if (args.hasFlag('g') && inst.hasPermission(sender, "aurora.rogue.guild")) {
                showToGuild((Player) sender);
            } else if (!args.hasFlag('g')) {
                hideFromGuild((Player) sender);
            } else {
                ChatUtil.sendError(sender, "You must be a member of the rogue guild to use this flag.");
            }
            setSilentMode((Player) sender, args.hasFlag('s'));
            ChatUtil.sendNotice(sender, "You gain the power of a rogue warrior!");
        }

        @Command(aliases = {"derogue"}, desc = "Revoke a player's Rogue power",
                flags = "", min = 0, max = 0)
        @CommandPermissions({"aurora.rogue"})
        public void derogue(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");
            if (inst.hasPermission(sender, "aurora.ninja.guild")) {
                throw new CommandException("You are a ninja not a rogue!");
            }

            deroguePlayer(PlayerUtil.matchSinglePlayer(sender, sender.getName()));
            ChatUtil.sendNotice(sender, "You return to your weak existence.");
        }
    }

    // Rogue Session
    private static class RogueState extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        private boolean isVisible = true;
        private boolean isRogue = false;
        private boolean showToGuild = false;
        private boolean silentMode = false;

        protected RogueState() {

            super(MAX_AGE);
        }

        public boolean isVisible() {

            return isVisible;
        }

        public void setIsVisible(boolean isVisible) {

            this.isVisible = isVisible;
        }

        public boolean isRogue() {

            return isRogue;
        }

        public void setIsRogue(boolean isRogue) {

            this.isRogue = isRogue;
        }

        public boolean guildCanSee() {

            return showToGuild;
        }

        public void showToGuild(boolean showToGuild) {

            this.showToGuild = showToGuild;
        }

        public boolean isOnSilent() {

            return silentMode;
        }

        public void setSilentMode(boolean silentMode) {

            this.silentMode = silentMode;
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}