package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.Pitfall.bukkit.event.PitfallTriggerEvent;
import com.skelril.aurora.events.PrePrayerApplicationEvent;
import com.skelril.aurora.events.anticheat.ThrowPlayerEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EntityDistanceComparator;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

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

    private final int WATCH_DISTANCE = 14;
    private final int WATCH_DISTANCE_SQ = WATCH_DISTANCE * WATCH_DISTANCE;
    private final int SNEAK_WATCH_DISTANCE = 6;
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

    public void useVanish(Player player, boolean vanish) {

        sessions.getSession(NinjaState.class, player).useVanish(vanish);
    }

    public boolean canVanish(Player player) {

        return sessions.getSession(NinjaState.class, player).canVanish();
    }


    public void usePoisonArrows(Player player, boolean poisonArrows) {

        sessions.getSession(NinjaState.class, player).usePoisonArrows(poisonArrows);
    }

    public boolean hasPoisonArrows(Player player) {

        return sessions.getSession(NinjaState.class, player).hasPoisonArrows();
    }

    public boolean allowsConflictingPotions(Player player) {

        return sessions.getSession(NinjaState.class, player).allowsConflictingPotions();
    }

    public void allowConflictingPotions(Player player, boolean allowConflictingPotions) {

        sessions.getSession(NinjaState.class, player).allowConflictingPotions(allowConflictingPotions);
    }

    public boolean canSmokeBomb(Player player) {

        return sessions.getSession(NinjaState.class, player).canSmokeBomb();
    }

    public void smokeBomb(Player player) {

        sessions.getSession(NinjaState.class, player).smokeBomb();

        server.getPluginManager().callEvent(new ThrowPlayerEvent(player));

        Location[] locations = new Location[]{
                player.getLocation(),
                player.getEyeLocation()
        };
        EnvironmentUtil.generateRadialEffect(locations, org.bukkit.Effect.SMOKE);

        List<Entity> entities = player.getNearbyEntities(4, 4, 4);
        if (entities.isEmpty()) return;

        Collections.sort(entities, new EntityDistanceComparator(player.getLocation()));

        Location oldLoc = null;
        Location k = null;
        for (Entity entity : entities) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

            oldLoc = entity.getLocation();

            k = player.getLocation();
            k.setPitch((float) (ChanceUtil.getRandom(361.0) - 181));
            k.setYaw((float) (ChanceUtil.getRandom(361.0) - 181));

            if (entity instanceof Player) {
                ChatUtil.sendWarning((Player) entity, "You hear a strange ticking sound...");
            }

            entity.teleport(k, PlayerTeleportEvent.TeleportCause.UNKNOWN);
        }

        if (oldLoc == null || k == null) return;

        // Offset by 1 so that the bomb is not messed up by blocks
        if (k.getBlock().getType() != Material.AIR) {
            k.add(0, 1, 0);
        }

        final Location finalK = k;
        server.getScheduler().runTaskLater(inst, new Runnable() {
            @Override
            public void run() {
                finalK.getWorld().createExplosion(finalK, 3, false);
            }
        }, 30);

        oldLoc.setDirection(player.getLocation().getDirection());
        player.teleport(oldLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    public boolean canGrapple(Player player) {

        return sessions.getSession(NinjaState.class, player).canGrapple();
    }

    public void grapple(Player player, double modifier) {

        sessions.getSession(NinjaState.class, player).grapple();

        server.getPluginManager().callEvent(new ThrowPlayerEvent(player));

        Vector vel = player.getLocation().getDirection();
        vel.multiply(.5);
        vel.setY(modifier);
        player.setVelocity(vel);
    }

    public void teleport(Player player) {

        Location[] locations = new Location[]{
                player.getLocation(),
                player.getEyeLocation()
        };
        EnvironmentUtil.generateRadialEffect(locations, org.bukkit.Effect.SMOKE);

        ItemUtil.removeItemOfName(player, ItemUtil.Guild.Ninja.makeStar(1), 1, false);
        player.teleport(new Location(Bukkit.getWorld("City"), 150.0001, 45, -443.0001, -180, 0));
    }

    public void unninjaPlayer(Player player) {

        NinjaState session = sessions.getSession(NinjaState.class, player);
        session.setIsNinja(false);

        player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);

        for (Player otherPlayer : server.getOnlinePlayers()) {
            // Show Yourself!
            if (otherPlayer != player) otherPlayer.showPlayer(player);
        }
    }

    @EventHandler
    public void onProjectileLaunch(EntityShootBowEvent event) {

        Entity e = event.getProjectile();
        Projectile p = e instanceof Projectile ? (Projectile) e : null;
        if (p == null || p.getShooter() == null || !(p.getShooter() instanceof Player)) return;

        Player player = (Player) p.getShooter();
        if (isNinja(player) && hasPoisonArrows(player) && inst.hasPermission(player, "aurora.ninja.guild")) {

            if (p instanceof Arrow) {
                arrowForce.put((Arrow) p, event.getForce());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (!isNinja(player)) return;

        switch (event.getCause()) {
            case FALL:
                event.setDamage(event.getDamage() * .5);
                break;
            case PROJECTILE:
            case ENTITY_ATTACK:
                event.setDamage(event.getDamage() * Math.min(1, ChanceUtil.getRandom(2.0) - .4));
                break;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageEntityEvent(EntityDamageByEntityEvent event) {

        Projectile p = null;

        if (event.getDamager() instanceof Arrow) {
            p = (Projectile) event.getDamager();
        }

        if (p == null) return;
        Entity shooter = p.getShooter();
        if (shooter == null || !(shooter instanceof Player)) return;
        if (p instanceof Arrow && arrowForce.containsKey(p)) {
            poisonArrow((Arrow) p, arrowForce.get(p));
            arrowForce.remove(p);

            double diff = (((Player) shooter).getMaxHealth() - ((Player) shooter).getHealth());
            event.setDamage(Math.max(event.getDamage(), event.getDamage() * diff * .35));
        }
    }

    @EventHandler
    public void onProjectileLand(ProjectileHitEvent event) {

        Projectile p = event.getEntity();
        if (p.getShooter() == null || !(p.getShooter() instanceof Player)) return;
        if (p instanceof Arrow && arrowForce.containsKey(p)) {
            poisonArrow((Arrow) p, arrowForce.get(p));
            arrowForce.remove(p);
        }
    }

    private void poisonArrow(Arrow arrow, float force) {
        int duration = (int) (20 * ((7 * force) + 3));
        for (Entity entity : arrow.getNearbyEntities(4, 2, 4)) {
            if (!ChanceUtil.getChance(3) || entity.equals(arrow.getShooter())) continue;
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.POISON, duration, 1), true);
                if (entity instanceof Player) {
                    ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration, 4), true);
                }
            }
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {

        final Player player = event.getPlayer();
        ItemStack stack = player.getItemInHand();

        if (isNinja(player)) {
            Block clicked = event.getClickedBlock();
            if (clicked == null) return;
            BlockFace face = event.getBlockFace();
            switch (event.getAction()) {
                case LEFT_CLICK_BLOCK:
                    if (!canSmokeBomb(player)) break;
                    if (stack != null && stack.getTypeId() == ItemID.BOW) {
                        smokeBomb(player);
                    }
                    break;
                case RIGHT_CLICK_BLOCK:
                    if (stack != null && ItemUtil.matchesFilter(stack, ChatColor.BLACK + "Ninja Star")) {
                        teleport(player);
                        event.setUseInteractedBlock(Event.Result.DENY);
                        break;
                    }
                    if (!canGrapple(player) || EnvironmentUtil.isInteractiveBlock(clicked)) break;
                    if (!face.equals(BlockFace.UP) && !face.equals(BlockFace.DOWN)
                            && (stack == null || stack.getType().equals(Material.AIR) || !stack.getType().isBlock())) {
                        if (clicked.getLocation().distanceSquared(player.getLocation()) <= 4) {
                            grapple(player, player.isSneaking() ? 1.5 : 1);
                        }
                    }
                    break;
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
                    if (otherPlayer.getWorld().equals(player.getWorld()) && canVanish(player)) {
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
                flags = "gtpi", min = 0, max = 0)
        @CommandPermissions({"aurora.ninja"})
        public void ninja(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");
            if (inst.hasPermission(sender, "aurora.rogue.guild") || rogueComponent.isRogue((Player) sender)) {
                throw new CommandException("You are a rogue not a ninja!");
            }

            final boolean isNinja = isNinja((Player) sender);

            ninjaPlayer((Player) sender);
            showToGuild((Player) sender, args.hasFlag('g'));
            useVanish((Player) sender, !args.hasFlag('i'));
            usePoisonArrows((Player) sender, !args.hasFlag('t'));
            allowConflictingPotions((Player) sender, !args.hasFlag('p'));

            if (!isNinja) {
                ChatUtil.sendNotice(sender, "You are inspired and become a ninja!");
            } else {
                ChatUtil.sendNotice(sender, "Ninja flags updated!");
            }
        }

        @Command(aliases = {"unninja"}, desc = "Revoke a player's Ninja power",
                flags = "", min = 0, max = 0)
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

        @Setting("ninja-enabled")
        private boolean isNinja = false;
        @Setting("ninja-vanish")
        private boolean useVanish = true;
        @Setting("ninja-show-to-guild")
        private boolean showToGuild = false;
        @Setting("ninja-toxic-arrows")
        private boolean toxicArrows = true;
        @Setting("ninja-conflicting-potions")
        private boolean allowConflictingPotions = true;

        private long nextGrapple = 0;
        private long nextSmokeBomb = 0;

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

        public boolean canVanish() {

            return useVanish;
        }

        public void useVanish(boolean useVanish) {

            this.useVanish = useVanish;
        }

        public boolean hasPoisonArrows() {

            return toxicArrows;
        }

        public void usePoisonArrows(boolean explosiveArrows) {

            this.toxicArrows = explosiveArrows;
        }

        public boolean allowsConflictingPotions() {

            return allowConflictingPotions;
        }

        public void allowConflictingPotions(boolean allowConflictingPotions) {

            this.allowConflictingPotions = allowConflictingPotions;
        }

        public boolean canGrapple() {

            return nextGrapple == 0 || System.currentTimeMillis() >= nextGrapple;
        }

        public void grapple() {

            nextGrapple = System.currentTimeMillis() + 1200;
        }

        public boolean canSmokeBomb() {

            return nextSmokeBomb == 0 || System.currentTimeMillis() >= nextSmokeBomb;
        }

        public void smokeBomb() {

            nextSmokeBomb = System.currentTimeMillis() + 2750;
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}