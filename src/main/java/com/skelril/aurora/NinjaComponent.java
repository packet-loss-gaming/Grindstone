package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.Pitfall.bukkit.event.PitfallTriggerEvent;
import com.skelril.aurora.city.engine.PvPComponent;
import com.skelril.aurora.events.anticheat.ThrowPlayerEvent;
import com.skelril.aurora.exceptions.PlayerOnlyCommandException;
import com.skelril.aurora.util.*;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.HorseJumpEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.skelril.aurora.util.item.ItemUtil.CustomItems;

@ComponentInformation(friendlyName = "Ninja", desc = "Disappear into the night!")
@Depend(plugins = "Pitfall", components = {SessionComponent.class, RogueComponent.class, PvPComponent.class})
public class NinjaComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private RogueComponent rogueComponent;

    private final int WATCH_DISTANCE = 14;
    private final int WATCH_DISTANCE_SQ = WATCH_DISTANCE * WATCH_DISTANCE;
    private final int SNEAK_WATCH_DISTANCE = 6;
    private final int SNEAK_WATCH_DISTANCE_SQ = SNEAK_WATCH_DISTANCE * SNEAK_WATCH_DISTANCE;

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 5);
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


    public void useTormentArrows(Player player, boolean tormentArrows) {

        sessions.getSession(NinjaState.class, player).usetormentArrows(tormentArrows);
    }

    public boolean hasTormentArrows(Player player) {

        return sessions.getSession(NinjaState.class, player).hasTormentArrows();
    }

    public boolean canSmokeBomb(Player player) {

        return sessions.getSession(NinjaState.class, player).canSmokeBomb();
    }

    public void smokeBomb(final Player player) {

        sessions.getSession(NinjaState.class, player).smokeBomb();

        Location[] locations = new Location[]{
                player.getLocation(),
                player.getEyeLocation()
        };
        EnvironmentUtil.generateRadialEffect(locations, Effect.SMOKE);

        List<Entity> entities = player.getNearbyEntities(4, 4, 4);
        if (entities.isEmpty()) return;

        Collections.sort(entities, new EntityDistanceComparator(player.getLocation()));

        boolean modifyCamera = false;

        Location oldLoc = null;
        Location k = null;
        for (Entity entity : entities) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

            modifyCamera = false;

            if (entity instanceof Player) {
                if (!PvPComponent.allowsPvP(player, (Player) entity)) return;
                ChatUtil.sendWarning((Player) entity, "You hear a strange ticking sound...");
                modifyCamera = true;
            } else if (entity instanceof Monster) {
                modifyCamera = true;
            }

            oldLoc = entity.getLocation();

            k = player.getLocation();
            k.setPitch((float) (ChanceUtil.getRandom(361.0) - 1));
            k.setYaw((float) (ChanceUtil.getRandom(181.0) - 91));

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

        oldLoc.setDirection(modifyCamera ? oldLoc.getDirection().multiply(-1) : player.getLocation().getDirection());
        player.teleport(oldLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    public boolean canGrapple(Player player) {

        return sessions.getSession(NinjaState.class, player).canGrapple();
    }

    public void grapple(final Player player, Block block, double maxClimb) {

        Location k = player.getLocation();

        switch (k.getBlock().getTypeId()) {
            case BlockID.AIR:
                k.setX(k.getBlockX() + .5);
                k.setZ(k.getBlockZ() + .5);
                player.teleport(k, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                break;
        }


        server.getPluginManager().callEvent(new ThrowPlayerEvent(player));

        Vector vel = player.getLocation().getDirection();
        vel.multiply(.5);
        vel.setY(.6);

        int z;
        for (z = 0; block.getY() > player.getLocation().getY(); z++) {
            block = block.getRelative(BlockFace.DOWN);
        }

        Block nextBlock = block.getRelative(BlockFace.UP);
        boolean nextBlockIsSolid = nextBlock.getType().isSolid();

        int i;
        Vector increment = new Vector(0, .1, 0);
        for (i = 0; i < maxClimb && (i < z || block.getType().isSolid() || nextBlockIsSolid); i++) {

            // Determine whether we need to add more velocity
            double ctl = nextBlockIsSolid ? 1 : BlockType.centralTopLimit(block.getTypeId(), block.getData());

            vel.add(ctl > 1 ? increment.clone().multiply(ctl) : increment);

            // Update blocks
            block = nextBlock;
            nextBlock = nextBlock.getRelative(BlockFace.UP);

            // Update boolean
            nextBlockIsSolid = nextBlock.getType().isSolid();
        }

        player.setVelocity(vel);
        player.setFallDistance(0F);

        sessions.getSession(NinjaState.class, player).grapple(i * 200);
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
        if (isNinja(player) && hasTormentArrows(player)) {

            if (p instanceof Arrow) {
                p.setMetadata("ninja-arrow", new FixedMetadataValue(inst, true));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
                event.setDamage(event.getDamage() * .8);
                break;
            case DROWNING:
                player.setRemainingAir(player.getMaximumAir());
            case LAVA:
            case FIRE:
                event.setCancelled(true);
                break;
            case FIRE_TICK:
                player.setFireTicks(0);
                event.setCancelled(true);
                break;
        }
    }

    @EventHandler
    public void onProjectileLand(ProjectileHitEvent event) {

        Projectile p = event.getEntity();
        if (p.getShooter() == null || !(p.getShooter() instanceof Player)) return;
        if (p instanceof Arrow && p.hasMetadata("ninja-arrow") && p.hasMetadata("launch-force")) {
            Object test = p.getMetadata("launch-force").get(0).value();

            if (!(test instanceof Float) || (Float) test < .85) return;

            tormentArrow((Arrow) p);
        }
    }

    private void tormentArrow(Arrow arrow) {
        Player shooter = (Player) arrow.getShooter();
        for (Entity entity : arrow.getNearbyEntities(4, 2, 4)) {
            if (!entity.isValid() || entity.equals(shooter)) continue;
            if (entity instanceof LivingEntity) {
                if (entity instanceof Player && !PvPComponent.allowsPvP((Player) arrow.getShooter(), (Player) entity)) continue;

                if (!shooter.hasLineOfSight(entity)) continue;

                shooter.setHealth(Math.min(shooter.getMaxHealth(), shooter.getHealth() + 1));
                ((LivingEntity) entity).setHealth(Math.max(0, ((LivingEntity) entity).getHealth() - 1));
                entity.playEffect(EntityEffect.HURT);
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

            boolean usingBow = stack != null && stack.getTypeId() == ItemID.BOW;

            switch (event.getAction()) {
                case LEFT_CLICK_BLOCK:
                    if (!canSmokeBomb(player)) break;
                    if (usingBow) {
                        smokeBomb(player);
                    }
                    break;
                case RIGHT_CLICK_BLOCK:
                    // Check cool player specific components
                    if (!canGrapple(player) || usingBow || player.isSneaking()) break;
                    if (EnvironmentUtil.isInteractiveBlock(clicked) || EnvironmentUtil.isShrubBlock(clicked)) break;
                    if (!face.equals(BlockFace.UP) && !face.equals(BlockFace.DOWN) && stack != null) {

                        // Check types
                        Material type = stack.getType();
                        if ((type != Material.AIR && type.isBlock()) || type.isEdible() && player.getFoodLevel() < 20) break;

                        // Check for possible misclick
                        if (EnvironmentUtil.isInteractiveBlock(clicked.getRelative(face))) break;

                        if (LocationUtil.distanceSquared2D(clicked.getLocation().add(.5, 0, .5), player.getLocation()) <= 4) {
                            grapple(player, clicked, 9);
                        }
                    }
                    break;
            }
        }

        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK:
                if (stack != null && ItemUtil.isItem(stack, CustomItems.NINJA_STAR)) {
                    teleport(player);
                    event.setUseInteractedBlock(Event.Result.DENY);
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

            if (isNinja(player) && player.isSneaking()) {
                event.setCancelled(true);
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
            if (player == null) {
                ninjaState.setIsNinja(false);
                continue;
            }

            if (!player.isValid()) continue;

            if (!inst.hasPermission(player, "aurora.ninja")) {
                unninjaPlayer(player);
                continue;
            }

            Set<Player> invisibleNewCount = new HashSet<>();
            Set<Player> visibleNewCount = new HashSet<>();

            Location pLoc = player.getLocation();
            Location k = player.getLocation();

            for (Player otherPlayer : server.getOnlinePlayers()) {
                if (otherPlayer.equals(player)) continue;

                if (otherPlayer.getWorld().equals(player.getWorld()) && canVanish(player)) {

                    // Sets k to the otherPlayer's current location
                    otherPlayer.getLocation(k);

                    double dist = pLoc.distanceSquared(k);

                    if ((player.isSneaking() && dist >= SNEAK_WATCH_DISTANCE_SQ) || dist >= WATCH_DISTANCE_SQ) {
                        if (otherPlayer.canSee(player)
                                && !(guildCanSee(player) && inst.hasPermission(otherPlayer, "aurora.ninja"))
                                && !inst.hasPermission(otherPlayer, "aurora.ninja.master")) {
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

            if (invisibleNewCount.size() > 0) {
                if (invisibleNewCount.size() > 3) {
                    ChatUtil.sendNotice(player, "You are now invisible to multiple players.");
                } else {
                    for (Player aPlayer : invisibleNewCount) {
                        ChatUtil.sendNotice(player, "You are now invisible to " + aPlayer.getDisplayName() + ".");
                    }
                }
            }

            if (visibleNewCount.size() > 0) {
                if (visibleNewCount.size() > 3) {
                    ChatUtil.sendNotice(player, "You are now visible to multiple players.");
                } else {
                    for (Player aPlayer : visibleNewCount) {
                        ChatUtil.sendNotice(player, "You are now visible to " + aPlayer.getDisplayName() + ".");
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

            if (!(sender instanceof Player)) throw new PlayerOnlyCommandException();
            if (inst.hasPermission(sender, "aurora.rogue")) {
                throw new CommandException("You are a rogue not a ninja!");
            }

            final boolean isNinja = isNinja((Player) sender);

            // Enter Ninja Mode
            ninjaPlayer((Player) sender);

            // Set flags
            showToGuild((Player) sender, args.hasFlag('g'));
            useVanish((Player) sender, !args.hasFlag('i'));
            useTormentArrows((Player) sender, !args.hasFlag('t'));

            if (!isNinja) {
                ChatUtil.sendNotice(sender, "You are inspired and become a ninja!");
            } else {
                ChatUtil.sendNotice(sender, "Ninja flags updated!");
            }
        }

        @Command(aliases = {"unninja"}, desc = "Revoke a player's Ninja power",
                flags = "", min = 0, max = 0)
        public void unninja(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new PlayerOnlyCommandException();
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
        @Setting("ninja-torment-arrows")
        private boolean tormentArrows = true;

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

        public boolean hasTormentArrows() {

            return tormentArrows;
        }

        public void usetormentArrows(boolean tormentArrows) {

            this.tormentArrows = tormentArrows;
        }

        public boolean canGrapple() {

            return nextGrapple == 0 || System.currentTimeMillis() >= nextGrapple;
        }

        public void grapple(long time) {

            nextGrapple = System.currentTimeMillis() + 300 + time;
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