/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.InfoComponent;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.skelril.Pitfall.bukkit.event.PitfallTriggerEvent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.city.engine.combat.PvPComponent;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.events.guild.RogueBlipEvent;
import gg.packetloss.grindstone.events.guild.RogueGrenadeEvent;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.guild.rogue.Nightmare;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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

import java.util.Collections;
import java.util.List;
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

    private static final float DEFAULT_SPEED = .2F;

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 11);
    }

    // Player Management
    public RogueState roguePlayer(Player player) {

        RogueState rogueState = getState(player);

        if (rogueState.isRogue()) return rogueState;

        rogueState.setIsRogue(true);

        double multiplier = inst.hasPermission(player, "aurora.rogue.master") ? 2.5 : 2;

        player.setWalkSpeed((float) (DEFAULT_SPEED * multiplier));
        return rogueState;
    }

    public RogueState getState(Player player) {
        return sessions.getSession(RogueState.class, player);
    }

    public boolean isRogue(Player player) {
        return getState(player).isRogue();
    }

    public void blip(Player player, double modifier, boolean auto) {

        RogueBlipEvent event = new RogueBlipEvent(player, modifier, auto);
        server.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        modifier = event.getModifier();

        RogueState rogueSession = getState(player);
        rogueSession.blip();

        server.getPluginManager().callEvent(new ThrowPlayerEvent(player));

        Vector vel = player.getLocation().getDirection();
        vel.multiply(3 * modifier * Math.max(.1, player.getFoodLevel() / 20.0));
        if (auto || rogueSession.isYLimited()) {
            vel.setY(Math.min(.8, Math.max(.175, vel.getY())));
        } else {
            vel.setY(Math.min(1.4, vel.getY()));
        }
        player.setVelocity(vel);
    }

    public void stallBlip(RogueState session) {
        session.blip(12000);
    }

    public void grenade(Player player) {

        RogueGrenadeEvent event = new RogueGrenadeEvent(player, ChanceUtil.getRandom(5) + 4);
        server.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        getState(player).grenade();

        for (int i = event.getGrenadeCount(); i > 0; --i) {
            Snowball snowball = player.launchProjectile(Snowball.class);
            Vector vector = new Vector(ChanceUtil.getRandom(2.0), 1, ChanceUtil.getRandom(2.0));
            snowball.setVelocity(snowball.getVelocity().multiply(vector));
            snowball.setMetadata("rogue-snowball", new FixedMetadataValue(inst, true));
        }
    }

    public void deroguePlayer(Player player) {

        RogueState rogueState = getState(player);
        rogueState.setIsRogue(false);
        player.setWalkSpeed(DEFAULT_SPEED);
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

    private static EDBEExtractor<LivingEntity, LivingEntity, Projectile> extractor = new EDBEExtractor<>(
            LivingEntity.class,
            LivingEntity.class,
            Projectile.class
    );

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamageEntityEvent(EntityDamageByEntityEvent event) {

        CombatantPair<LivingEntity, LivingEntity, Projectile> result = extractor.extractFrom(event);

        if (result == null) return;

        final LivingEntity attacker = result.getAttacker();
        final LivingEntity defender = result.getDefender();

        if (result.hasProjectile()) {
            Projectile projectile = result.getProjectile();
            if (projectile.hasMetadata("nightmare")) {
                event.setCancelled(true);
                return;
            }
            if (defender instanceof Player && ChanceUtil.getChance(3)) {
                RogueState rogueSession = getState((Player) defender);
                if (rogueSession.isRogue() && rogueSession.canBacklash() && rogueSession.canBlip()) {
                    if (attacker instanceof Player && !PvPComponent.allowsPvP((Player) attacker, (Player) defender)) return;
                    server.getScheduler().runTaskLater(inst, () -> {
                        defender.teleport(attacker, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                        blip((Player) defender, -.5, true);
                    }, 1);
                }
            }
        } else if (attacker instanceof Player && isRogue((Player) attacker)) {
            event.setDamage((event.getDamage() + 10) * 1.2);
        }
    }

    @EventHandler
    public void onProjectileLand(ProjectileHitEvent event) {

        Projectile p = event.getEntity();
        if (p.getShooter() == null || !(p.getShooter() instanceof LivingEntity)) return;
        if (p instanceof Snowball && p.hasMetadata("rogue-snowball")) {

            // Create the explosion if no players are around that don't allow PvP
            final LivingEntity shooter = (LivingEntity) p.getShooter();
            if (p.hasMetadata("nightmare")) {

                if (shooter instanceof Player) {
                    server.getPluginManager().callEvent(new RapidHitEvent((Player) shooter));
                }

                for (Entity entity : p.getNearbyEntities(3, 3, 3)) {
                    if (!entity.isValid() || entity.equals(shooter) || !(entity instanceof LivingEntity)) continue;

                    if (entity instanceof Player) {
                        if (((Player) entity).getGameMode().equals(GameMode.CREATIVE)) continue;
                        if (shooter instanceof Player) {
                            if (!PvPComponent.allowsPvP((Player) shooter, (Player) entity)) continue;
                        }
                    }

                    if (((LivingEntity) entity).getHealth() < 2) continue;

                    EntityUtil.heal(shooter, 1);
                    EntityUtil.forceDamage(entity, 1);
                    entity.playEffect(EntityEffect.HURT);
                }
            } else {

                for (Entity entity : p.getNearbyEntities(4, 4, 4)) {
                    if (entity.equals(shooter) || !(entity instanceof LivingEntity)) continue;
                    if (entity instanceof Player) {
                        final Player defender = (Player) entity;
                        if (shooter instanceof Player) {
                            if (!PvPComponent.allowsPvP((Player) shooter, defender)) return;
                        }

                        if (getState(defender).isTraitorProtected()) {
                            if (shooter instanceof Player) {
                                ChatUtil.sendWarning(
                                        (Player) shooter,
                                        defender.getName() + " sends a band of Rogue marauders after you."
                                );
                            }
                            for (int i = ChanceUtil.getRandom(24) + 20; i > 0; --i) {
                                server.getScheduler().runTaskLater(inst, () -> {
                                    if (defender.getLocation().distanceSquared(shooter.getLocation()) > 2500) {
                                        return;
                                    }
                                    Location l = LocationUtil.findRandomLoc(shooter.getLocation().getBlock(), 3, true, false);
                                    l.getWorld().createExplosion(l.getX(), l.getY(), l.getZ(), 1.75F, true, false);
                                }, 12 * i);
                            }
                        }
                    }
                }

                p.getWorld().createExplosion(p.getLocation(), 1.75F);
            }
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {

        final Player player = event.getPlayer();
        ItemStack stack = player.getItemInHand();

        RogueState rogueSession = sessions.getSession(RogueState.class, player);
        if (rogueSession.isRogue() && stack != null
                && ((!rogueSession.hasActionItem() && ItemUtil.isSword(stack.getTypeId()))
                || (rogueSession.hasActionItem() && stack.getTypeId() == rogueSession.getActionItem()))) {
            switch (event.getAction()) {
                case LEFT_CLICK_AIR:
                    server.getScheduler().runTaskLater(inst, () -> {
                        if (rogueSession.canBlip() && !player.isSneaking()) {
                            blip(player, 2, false);
                        }
                    }, 1);
                    break;
                case RIGHT_CLICK_AIR:
                    if (rogueSession.canGrenade()) {
                        grenade(player);
                    }
                    break;
                case RIGHT_CLICK_BLOCK:
                    if (rogueSession.canGrenade() && rogueSession.getGrenadeSafety()) {
                        Block clicked = event.getClickedBlock();
                        BlockFace face = event.getBlockFace();

                        // Never throw grenades if the clicked block was interactive, or could've been a misclick
                        // of a nearby interactive block.
                        if (EnvironmentUtil.isMaybeInteractiveBlock(clicked, face)) {
                            break;
                        }

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

            RogueState rogueSession = getState(player);
            if (rogueSession.isRogue()) {

                Entity p = event.getProjectile();
                p.setVelocity(p.getVelocity().multiply(.9));

                stallBlip(rogueSession);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPitfallTrigger(PitfallTriggerEvent event) {

        Entity entity = event.getEntity();

        if (entity instanceof Player) {

            Player player = (Player) entity;

            if (isRogue(player)) {
                blip(player, 1, true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpecialAttack(SpecialAttackEvent event) {

        Player player = event.getPlayer();

        if (isRogue(player)) {
            SpecialAttack attack = event.getSpec();

            if (event.getContext().equals(SpecType.MELEE) && ChanceUtil.getChance(14)) {
                event.setSpec(new Nightmare(attack.getOwner(), attack.getTarget()));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWhoisLookup(InfoComponent.PlayerWhoisEvent event) {

        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (!inst.hasPermission(player, "aurora.rogue")) return;
            event.addWhoisInformation("Rogue Mode", isRogue(player) ? "Enabled" : "Disabled");
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
            if (player == null) {
                continue;
            }

            if (!player.isValid()) continue;

            if (!inst.hasPermission(player, "aurora.rogue")) {
                deroguePlayer(player);
                continue;
            }

            Entity vehicle = player.getVehicle();
            if (vehicle instanceof Horse) {
                ((Horse) vehicle).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 20, 2), true);
            }
        }
    }

    public class Commands {

        @Command(aliases = {"rogue"}, desc = "Give a player the Rogue power",
                flags = "lgtbl:", min = 0, max = 0)
        @CommandPermissions({"aurora.rogue"})
        public void rogue(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);
            if (inst.hasPermission(player, "aurora.ninja")) {
                throw new CommandException("You are a ninja not a rogue!");
            }

            final boolean isRogue = isRogue(player);

            // Enter Rogue Mode
            RogueState rogueSession = roguePlayer(player);

            // Set flags
            rogueSession.limitYVelocity(args.hasFlag('l'));
            rogueSession.setBacklash(!args.hasFlag('b'));
            rogueSession.setTraitorProtection(args.hasFlag('t') && inst.hasPermission(player, "aurora.rogue.master"));
            rogueSession.setGrenadeSafety(!args.hasFlag('g'));

            if (args.hasFlag('l')) {
                rogueSession.setActionItem(args.getFlagInteger('l'));
            } else {
                rogueSession.resetActionItem();
            }

            if (!isRogue) {
                ChatUtil.sendNotice(player, "You gain the power of a rogue warrior!");
            } else {
                ChatUtil.sendNotice(player, "Rogue flags updated!");
            }
        }

        @Command(aliases = {"derogue"}, desc = "Revoke a player's Rogue power",
                flags = "", min = 0, max = 0)
        public void derogue(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);
            if (!isRogue(player)) {
                throw new CommandException("You are not a rogue!");
            }

            deroguePlayer(player);
            ChatUtil.sendNotice(player, "You return to your weak existence.");
        }
    }

    // Rogue Session
    public static class RogueState extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        @Setting("rogue-enabled")
        private boolean isRogue = false;
        @Setting("rogue-y-limited")
        private boolean limitYVelocity = false;
        @Setting("rogue-backlash")
        private boolean rogueBacklash = true;
        @Setting("rogue-traitor")
        private boolean rogueTraitor = false;

        @Setting("rogue-action-item")
        private int rogueActionItem = -1;

        @Setting("rogue-grenade-safety")
        private boolean grenadeSafety = true;

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

        public boolean hasActionItem() {
            return rogueActionItem != -1;
        }

        public int getActionItem() {
            return rogueActionItem;
        }

        public void setActionItem(int rogueActionItem) {
            this.rogueActionItem = rogueActionItem;
        }

        public void resetActionItem() {
            rogueActionItem = -1;
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

        public boolean canBacklash() {

            return rogueBacklash;
        }

        public void setBacklash(boolean enabled) {

            this.rogueBacklash = enabled;
        }

        public boolean isTraitorProtected() {

            return rogueTraitor;
        }

        public void setTraitorProtection(boolean rogueTraitor) {

            this.rogueTraitor = rogueTraitor;
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

        public boolean getGrenadeSafety() {
            return grenadeSafety;
        }

        public void setGrenadeSafety(boolean grenadeSafety) {
            this.grenadeSafety = grenadeSafety;
        }
    }
}