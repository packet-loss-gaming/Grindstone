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
import com.skelril.aurora.events.PrePrayerApplicationEvent;
import com.skelril.aurora.events.anticheat.ThrowPlayerEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
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

    private List<Snowball> snowBalls = new ArrayList<>();

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

    public boolean allowsConflictingPotions(Player player) {

        return sessions.getSession(RogueState.class, player).allowsConflictingPotions();
    }

    public void allowConflictingPotions(Player player, boolean allowConflictingPotions) {

        sessions.getSession(RogueState.class, player).allowConflictingPotions(allowConflictingPotions);
    }

    public boolean canBlip(Player player) {

        return sessions.getSession(RogueState.class, player).canBlip();
    }

    public void blip(Player player, double modifier) {

        sessions.getSession(RogueState.class, player).blip();

        server.getPluginManager().callEvent(new ThrowPlayerEvent(player));

        Vector vel = player.getLocation().getDirection();
        vel.multiply(3 * modifier);
        vel.setY(Math.min(.8, Math.max(.175, vel.getY())));
        player.setVelocity(vel);

        player.getWorld().playSound(player.getLocation(), Sound.GHAST_SCREAM, .2F, 0);
    }

    public void fakeBlip(Player player) {

        sessions.getSession(RogueState.class, player).blip();
    }

    public boolean canGrenade(Player player) {

        return sessions.getSession(RogueState.class, player).canGrenade();
    }

    public void grendade(Player player) {

        sessions.getSession(RogueState.class, player).grenade();

        for (int i = 0; i < ChanceUtil.getRandom(5) + 4; i++) {
            Snowball snowball = player.launchProjectile(Snowball.class);
            Vector vector = new Vector(ChanceUtil.getRandom(2), 1, ChanceUtil.getRandom(2));
            snowball.setVelocity(snowball.getVelocity().multiply(vector));
            snowBalls.add(snowball);
        }
    }

    public void deroguePlayer(Player player) {

        RogueState session = sessions.getSession(RogueState.class, player);
        session.setIsRogue(false);

        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageEntityEvent(EntityDamageByEntityEvent event) {

        Entity damager = event.getDamager();
        boolean wasArrow = false;

        if (event.getDamager() instanceof Arrow) {
            damager = ((Arrow) event.getDamager()).getShooter();
            wasArrow = true;
        }

        if (event.getEntity() instanceof Player && wasArrow && ChanceUtil.getChance(3)) {

            Player defender = (Player) event.getEntity();
            if (isRogue(defender) && canBlip(defender) && inst.hasPermission(defender, "aurora.rogue.guild")) {
                defender.teleport(damager, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                blip(defender, -1);
            }
        }

        if (damager instanceof Player && isRogue((Player) damager)) {

            fakeBlip((Player) damager);
        }
    }

    @EventHandler
    public void onProjectileLand(ProjectileHitEvent event) {

        Projectile p = event.getEntity();
        if (p.getShooter() == null || !(p.getShooter() instanceof Player)) return;
        if (isRogue((Player) p.getShooter())) {

            if (p instanceof Snowball && snowBalls.contains(p)) {
                p.getWorld().createExplosion(p.getLocation(), 2.75F);
                snowBalls.remove(p);
            }
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {

        final Player player = event.getPlayer();
        ItemStack stack = player.getItemInHand();

        if (isRogue(player) && stack != null && ItemUtil.isSword(stack.getTypeId())) {
            switch (event.getAction()) {
                case LEFT_CLICK_AIR:
                    server.getScheduler().runTaskLater(inst, new Runnable() {
                        @Override
                        public void run() {
                            if (canBlip(player)) {
                                blip(player, inst.hasPermission(player, "aurora.rogue.guild") ? 2 : 1);
                            }
                        }
                    }, 1);
                    break;
                case RIGHT_CLICK_AIR:
                    if (canGrenade(player) && inst.hasPermission(player, "aurora.rogue.guild")) {
                        grendade(player);
                    }
                    break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBowFire(EntityShootBowEvent event) {

        if (event.getEntity() instanceof Player) {
            final Player player = (Player) event.getEntity();

            if (isRogue(player)) {
                Entity p = event.getProjectile();
                p.setVelocity(p.getVelocity().multiply(new Vector(1, .25, 1)));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPitfallTrigger(PitfallTriggerEvent event) {

        Entity entity = event.getEntity();

        if (entity instanceof Player) {

            Player player = (Player) entity;

            if (isRogue(player) && inst.hasPermission(player, "aurora.rogue.guild")) {

                blip(player, 1);
            }
        }
    }

    private static Set<Integer> blockedEffects = new HashSet<>();

    static {
        blockedEffects.add(PotionEffectType.INCREASE_DAMAGE.getId());
        blockedEffects.add(PotionEffectType.SPEED.getId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrePrayerApplicationEvent event) {

        Player player = event.getPlayer();
        if (isRogue(player) && inst.hasPermission(player, "aurora.rogue.guild")) {
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
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 45, 6), true);

                Entity vehicle = player.getVehicle();
                if (vehicle != null && vehicle instanceof Horse) {
                    ((Horse) vehicle).removePotionEffect(PotionEffectType.SPEED);
                    ((Horse) vehicle).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 2));

                    player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 45, 3), true);
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 45, 1), true);
                }
            }
        }
    }

    public class Commands {

        @Command(aliases = {"rogue"}, desc = "Give a player the Rogue power",
                flags = "p", min = 0, max = 0)
        @CommandPermissions({"aurora.rogue"})
        public void rogue(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");
            if (inst.hasPermission(sender, "aurora.ninja.guild") || ninjaComponent.isNinja((Player) sender)) {
                throw new CommandException("You are a ninja not a rogue!");
            }

            if (inst.hasPermission(sender, "aurora.rogue.guild")) {
                allowConflictingPotions((Player) sender, !args.hasFlag('p'));
            } else if (args.getFlags().size() > 0) {
                ChatUtil.sendError(sender, "You must be a member of the ninja guild to use flags.");
            }

            roguePlayer(PlayerUtil.matchSinglePlayer(sender, sender.getName()));
            ChatUtil.sendNotice(sender, "You gain the power of a rogue warrior!");
        }

        @Command(aliases = {"derogue"}, desc = "Revoke a player's Rogue power",
                flags = "", min = 0, max = 0)
        @CommandPermissions({"aurora.rogue"})
        public void derogue(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");
            if (!isRogue((Player) sender)) {
                throw new CommandException("You are not a rogue!");
            }

            deroguePlayer(PlayerUtil.matchSinglePlayer(sender, sender.getName()));
            ChatUtil.sendNotice(sender, "You return to your weak existence.");
        }
    }

    // Rogue Session
    private static class RogueState extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        private boolean isRogue = false;
        private long nextBlip = 0;
        private long nextGrenade = 0;
        private boolean allowConflictingPotions = true;

        protected RogueState() {

            super(MAX_AGE);
        }

        public boolean isRogue() {

            return isRogue;
        }

        public void setIsRogue(boolean isRogue) {

            this.isRogue = isRogue;
        }

        public boolean canBlip() {

            return nextBlip == 0 || System.currentTimeMillis() >= nextBlip;
        }

        public void blip() {

            nextBlip = System.currentTimeMillis() + 1500;
        }

        public boolean canGrenade() {

            return nextGrenade == 0 || System.currentTimeMillis() >= nextGrenade;
        }

        public void grenade() {

            nextGrenade = System.currentTimeMillis() + 3500;
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