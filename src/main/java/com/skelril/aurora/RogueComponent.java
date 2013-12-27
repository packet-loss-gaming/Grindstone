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
import com.skelril.aurora.city.engine.PvPComponent;
import com.skelril.aurora.events.PrePrayerApplicationEvent;
import com.skelril.aurora.events.anticheat.RapidHitEvent;
import com.skelril.aurora.events.anticheat.ThrowPlayerEvent;
import com.skelril.aurora.events.custom.item.SpecialAttackEvent;
import com.skelril.aurora.items.specialattack.SpecialAttack;
import com.skelril.aurora.items.specialattack.attacks.melee.fear.Weaken;
import com.skelril.aurora.items.specialattack.attacks.melee.guild.rogue.Nightmare;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EntityDistanceComparator;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.TimeUnit;

@ComponentInformation(friendlyName = "Rogue", desc = "Speed and strength is always the answer.")
@Depend(plugins = {"Pitfall"}, components = {SessionComponent.class, NinjaComponent.class, PvPComponent.class})
public class RogueComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private NinjaComponent ninjaComponent;

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

    public boolean isYLimited(Player player) {

        return sessions.getSession(RogueState.class, player).isYLimited();
    }

    public void limitYVelocity(Player player, boolean limitYVelocity) {

        sessions.getSession(RogueState.class, player).limitYVelocity(limitYVelocity);
    }

    public boolean canBlip(Player player) {

        return sessions.getSession(RogueState.class, player).canBlip();
    }

    public void blip(Player player, double modifier, boolean auto) {

        sessions.getSession(RogueState.class, player).blip();

        server.getPluginManager().callEvent(new ThrowPlayerEvent(player));

        Vector vel = player.getLocation().getDirection();
        vel.multiply(3 * modifier);
        if (auto || isYLimited(player)) {
            vel.setY(Math.min(.8, Math.max(.175, vel.getY())));
        } else {
            vel.setY(Math.min(1.4, Math.max(.175, vel.getY())));
        }
        player.setVelocity(vel);
    }

    public void fakeBlip(Player player) {

        sessions.getSession(RogueState.class, player).blip(3250);
    }

    public boolean canGrenade(Player player) {

        return sessions.getSession(RogueState.class, player).canGrenade();
    }

    public void grenade(Player player) {

        sessions.getSession(RogueState.class, player).grenade();

        for (int i = 0; i < ChanceUtil.getRandom(5) + 4; i++) {
            Snowball snowball = player.launchProjectile(Snowball.class);
            Vector vector = new Vector(ChanceUtil.getRandom(2.0), 1, ChanceUtil.getRandom(2.0));
            snowball.setVelocity(snowball.getVelocity().multiply(vector));
            snowball.setMetadata("rogue-snowball", new FixedMetadataValue(inst, true));
        }
    }

    public void deroguePlayer(Player player) {

        sessions.getSession(RogueState.class, player).setIsRogue(false);

        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent event) {

        Entity e = event.getEntity();
        if (e instanceof Player && event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
            Player player = (Player) e;
            if (isRogue(player)) {
                List<Entity> entities = player.getNearbyEntities(2, 2, 2);

                if (entities.size() < 1) return;

                Collections.sort(entities, new EntityDistanceComparator(player.getLocation()));

                server.getPluginManager().callEvent(new RapidHitEvent(player));

                for (Entity entity : entities) {
                    if (entity.equals(player)) continue;
                    if (entity instanceof LivingEntity) {
                        if (entity instanceof Player && !PvPComponent.allowsPvP(player, (Player) entity)) continue;
                        ((LivingEntity) entity).damage(event.getDamage() * .5, player);
                        event.setCancelled(true);
                        break;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageEntityEvent(EntityDamageByEntityEvent event) {

        Entity damager = event.getDamager();
        boolean wasArrow = false;

        if (damager instanceof Arrow) {
            damager = ((Arrow) event.getDamager()).getShooter();
            wasArrow = true;
        }

        if (event.getEntity() instanceof Player && wasArrow && ChanceUtil.getChance(3)) {

            final Player defender = (Player) event.getEntity();
            if (isRogue(defender) && canBlip(defender) && inst.hasPermission(defender, "aurora.rogue.guild")) {
                final Entity finalDamager = damager;
                server.getScheduler().runTaskLater(inst, new Runnable() {
                    @Override
                    public void run() {

                        defender.teleport(finalDamager, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                        blip(defender, -.5, true);
                    }
                }, 1);
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
        if (p instanceof Snowball && p.hasMetadata("rogue-snowball")) {

            // Create the explosion if no players are around that don't allow PvP
            final Player shooter = (Player) p.getShooter();

            for (Entity entity : p.getNearbyEntities(4, 4, 4)) {
                if (entity.equals(shooter) || !(entity instanceof LivingEntity)) continue;
                if (entity instanceof Player) {
                    final Player defender = (Player) entity;
                    if (!PvPComponent.allowsPvP(shooter, defender)) return;

                    if (inst.hasPermission(defender, "aurora.rogue.guild.master")) {
                        ChatUtil.sendWarning(shooter, defender.getName() + " sends a band of Rogue marauders after you.");
                        for (int i = 1; i < ChanceUtil.getRandom(24) + 20; i++) {
                            server.getScheduler().runTaskLater(inst, new Runnable() {
                                @Override
                                public void run() {
                                    if (defender.getLocation().distanceSquared(shooter.getLocation()) > 2500) {
                                        return;
                                    }
                                    Location l = LocationUtil.findRandomLoc(shooter.getLocation().getBlock(), 3, true, false);
                                    l.getWorld().createExplosion(l.getX(), l.getY(), l.getZ(), 1.75F, true, false);
                                }
                            }, 12 * i);
                        }
                    }
                }
            }

            if (p.hasMetadata("nightmare")) {

                server.getPluginManager().callEvent(new RapidHitEvent(shooter));

                for (Entity e : p.getNearbyEntities(3, 3, 3)) {
                    if (e.isValid() && e instanceof LivingEntity) {
                        if (e instanceof Player && !PvPComponent.allowsPvP(shooter, (Player) e)) continue;
                        shooter.setHealth(Math.min(shooter.getMaxHealth(), shooter.getHealth() + 1));
                        ((LivingEntity) e).setHealth(Math.max(0, ((LivingEntity) e).getHealth() - 1));
                        e.playEffect(EntityEffect.HURT);
                    }
                }
            } else {
                p.getWorld().createExplosion(p.getLocation(), 1.75F);
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
                            if (canBlip(player) && !player.isSneaking()) {
                                blip(player, inst.hasPermission(player, "aurora.rogue.guild") ? 2 : 1, false);
                            }
                        }
                    }, 1);
                    break;
                case RIGHT_CLICK_AIR:
                    if (canGrenade(player) && inst.hasPermission(player, "aurora.rogue.guild")) {
                        grenade(player);
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
                p.setVelocity(p.getVelocity().multiply(new Vector(.35, .25, .35)));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPitfallTrigger(PitfallTriggerEvent event) {

        Entity entity = event.getEntity();

        if (entity instanceof Player) {

            Player player = (Player) entity;

            if (isRogue(player) && inst.hasPermission(player, "aurora.rogue.guild")) {

                blip(player, 1, true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpecialAttack(SpecialAttackEvent event) {

        Player player = event.getPlayer();

        if (isRogue(player)) {
            SpecialAttack attack = event.getSpec();

            if (attack instanceof Weaken) {
                event.setSpec(new Nightmare(attack.getOwner(), attack.getTarget()));
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
                    ((Horse) vehicle).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 20, 2), true);

                    player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 45, 3), true);
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 45, 1), true);
                }
            }
        }
    }

    public class Commands {

        @Command(aliases = {"rogue"}, desc = "Give a player the Rogue power",
                flags = "pl", min = 0, max = 0)
        @CommandPermissions({"aurora.rogue"})
        public void rogue(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");
            if (inst.hasPermission(sender, "aurora.ninja.guild") || ninjaComponent.isNinja((Player) sender)) {
                throw new CommandException("You are a ninja not a rogue!");
            }

            final boolean isRogue = isRogue((Player) sender);

            // Enter Rogue Mode
            roguePlayer((Player) sender);

            // Set flags
            allowConflictingPotions((Player) sender, !args.hasFlag('p'));
            limitYVelocity((Player) sender, args.hasFlag('l'));

            if (!isRogue) {
                ChatUtil.sendNotice(sender, "You gain the power of a rogue warrior!");
            } else {
                ChatUtil.sendNotice(sender, "Rogue flags updated!");
            }
        }

        @Command(aliases = {"derogue"}, desc = "Revoke a player's Rogue power",
                flags = "", min = 0, max = 0)
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

        @Setting("rogue-enabled")
        private boolean isRogue = false;
        @Setting("rogue-y-limited")
        private boolean limitYVelocity = false;
        @Setting("rogue-conflicting-potions")
        private boolean allowConflictingPotions = true;

        private long nextBlip = 0;
        private long nextGrenade = 0;

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

            blip(2250);
        }

        public void blip(long time) {

            nextBlip = System.currentTimeMillis() + time;
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

        public boolean isYLimited() {

            return limitYVelocity;
        }

        public void limitYVelocity(boolean limitYVelocity) {

            this.limitYVelocity = limitYVelocity;
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}