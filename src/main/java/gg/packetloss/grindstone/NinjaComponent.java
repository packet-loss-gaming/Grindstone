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
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.skelril.Pitfall.bukkit.event.PitfallTriggerEvent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.city.engine.combat.PvPComponent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.events.guild.NinjaGrappleEvent;
import gg.packetloss.grindstone.events.guild.NinjaSmokeBombEvent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.HorseJumpEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

    public void addArrow(Player player, Arrow arrow) {

        sessions.getSession(NinjaState.class, player).addArrow(arrow);
    }

    public List<Arrow> getRecentArrows(Player player) {

        return sessions.getSession(NinjaState.class, player).getRecentArrows();
    }

    public boolean canArrowBomb(Player player) {

        return sessions.getSession(NinjaState.class, player).canArrowBomb();
    }

    private void handleArrowBombArrow(Player player, Arrow arrow) {
        if (arrow instanceof TippedArrow) {
            AreaEffectCloud effectCloud = arrow.getWorld().spawn(arrow.getLocation(), AreaEffectCloud.class);
            effectCloud.setBasePotionData(((TippedArrow) arrow).getBasePotionData());
            effectCloud.setDuration(20 * 10);
            effectCloud.setSource(arrow.getShooter());
        } else {
            float launchForce = arrow.getMetadata("launch-force").get(0).asFloat();

            ExplosionStateFactory.createPvPExplosion(
                    player,
                    arrow.getLocation(), Math.max(2, 4 * launchForce),
                    false,
                    true
            );
        }

        arrow.remove();
    }

    public void arrowBomb(Player player) {
        List<Arrow> arrows = getRecentArrows(player);

        if (arrows.isEmpty()) return;

        sessions.getSession(NinjaState.class, player).arrowBomb();

        for (Arrow arrow : arrows) {
            handleArrowBombArrow(player, arrow);
        }
    }

    public boolean canSmokeBomb(Player player) {

        return sessions.getSession(NinjaState.class, player).canSmokeBomb();
    }

    public void smokeBomb(final Player player) {

        List<Entity> entities = player.getNearbyEntities(4, 4, 4);
        entities.sort(new EntityDistanceComparator(player.getLocation()));

        Location oldLoc = null;
        for (Entity entity : entities) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;
            oldLoc = entity.getLocation();
        }

        if (oldLoc == null) return;

        NinjaSmokeBombEvent event = new NinjaSmokeBombEvent(player, 3, 30, entities, oldLoc, player.getLocation());
        server.getPluginManager().callEvent(event);
        if (event.isCancelled() || entities.isEmpty()) return;

        NinjaState session = sessions.getSession(NinjaState.class, player);
        session.smokeBomb();

        entities.sort(new EntityDistanceComparator(player.getLocation()));

        oldLoc = null;
        boolean modifyCamera = false;
        for (Entity entity : entities) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

            modifyCamera = false;

            if (entity instanceof Player) {
                if (!PvPComponent.allowsPvP(player, (Player) entity)) continue;
                modifyCamera = true;
            } else if (entity instanceof Monster) {
                modifyCamera = true;
            }

            oldLoc = entity.getLocation();
        }

        Location k = null;
        for (Entity entity : entities) {
            EntityUtil.forceDamage(entity, 1);
            EntityUtil.heal(player, 1);

            if (entity instanceof Player) {
                if (rogueComponent.isRogue((Player) entity)) {
                    rogueComponent.getState((Player) entity).blip(event.getDelayInMills());
                }

                ChatUtil.sendWarning(entity, "You hear a strange ticking sound...");
            }

            k = event.getTargetLoc();
            k.setPitch((float) (ChanceUtil.getRandom(361.0) - 1));
            k.setYaw((float) (ChanceUtil.getRandom(181.0) - 91));

            entity.teleport(k, PlayerTeleportEvent.TeleportCause.UNKNOWN);
        }

        if (k == null) return;

        Location[] locations = new Location[]{
                player.getLocation(),
                player.getEyeLocation()
        };
        EnvironmentUtil.generateRadialEffect(locations, Effect.SMOKE);

        // Offset by 1 so that the bomb is not messed up by blocks
        if (k.getBlock().getType() != Material.AIR) {
            k.add(0, 1, 0);
        }

        final Location finalK = k;
        server.getScheduler().runTaskLater(inst, () -> {
            ExplosionStateFactory.createPvPExplosion(
                    player,
                    finalK,
                    event.getExplosionPower(),
                    false,
                    true
            );
        }, event.getDelay());

        if (oldLoc != null) {
            oldLoc.setDirection(modifyCamera ? oldLoc.getDirection().multiply(-1) : player.getLocation().getDirection());
        }
        Location target = event.getTeleportLoc() == null ? oldLoc : event.getTeleportLoc();
        player.teleport(target, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    public boolean canGrapple(Player player) {

        return sessions.getSession(NinjaState.class, player).canGrapple();
    }

    public void grapple(final Player player, Block block, BlockFace clickedFace, double maxClimb) {

        NinjaGrappleEvent event = new NinjaGrappleEvent(player, maxClimb);
        server.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

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

        int playerY = player.getLocation().getBlockY();
        int z;
        for (z = 0; block.getY() >= playerY; z++) {
            if (block.getY() == playerY) {
                if (EnvironmentUtil.isWater(block.getRelative(BlockFace.DOWN))) {
                    vel.setY(vel.getY() * 2);
                }
                break;
            }
            block = block.getRelative(BlockFace.DOWN);
        }

        Block nextBlock = block.getRelative(BlockFace.UP);
        boolean nextBlockIsSolid = nextBlock.getType().isSolid();

        int i;
        Vector increment = new Vector(0, .1, 0);
        for (i = 0; i < event.getMaxClimb() && (i < z || block.getType().isSolid() || nextBlockIsSolid); i++) {

            // Determine whether we need to add more velocity
            double ctl = nextBlockIsSolid ? 1 : BlockType.centralTopLimit(block.getTypeId(), block.getData());

            if (EnvironmentUtil.isWater(block.getRelative(clickedFace))) {
                ctl *= 2;
            }

            vel.add(ctl > 1 ? increment.clone().multiply(ctl) : increment);

            // Update blocks
            block = nextBlock;
            nextBlock = nextBlock.getRelative(BlockFace.UP);

            // Update boolean
            nextBlockIsSolid = nextBlock.getType().isSolid();
        }

        player.setVelocity(vel);
        player.setFallDistance(0F);

        sessions.getSession(NinjaState.class, player).grapple(Math.max(i, 3) * 200);
    }

    public void teleport(Player player) {

        Location[] locations = new Location[]{
                player.getLocation(),
                player.getEyeLocation()
        };
        EnvironmentUtil.generateRadialEffect(locations, org.bukkit.Effect.SMOKE);

        ItemUtil.removeItemOfName(player, CustomItemCenter.build(CustomItems.NINJA_STAR), 1, false);
        player.teleport(new Location(Bukkit.getWorld("City"), 150.0001, 45, -443.0001, -180, 0));
    }

    public void unninjaPlayer(Player player) {

        NinjaState session = sessions.getSession(NinjaState.class, player);
        session.setIsNinja(false);

        for (Player otherPlayer : server.getOnlinePlayers()) {
            // Show Yourself!
            if (otherPlayer != player) otherPlayer.showPlayer(inst, player);
        }
    }

    @EventHandler
    public void onProjectileLaunch(EntityShootBowEvent event) {
        Entity e = event.getProjectile();
        Projectile p = e instanceof Projectile ? (Projectile) e : null;
        if (p == null || p.getShooter() == null || !(p.getShooter() instanceof Player)) return;

        Player player = (Player) p.getShooter();
        if (isNinja(player) && p instanceof Arrow) {
            addArrow(player, (Arrow) p);

            if (ChanceUtil.getChance(4)) {
                event.setConsumeArrow(false);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {

        if (event instanceof EntityDamageByEntityEvent) {
            entityDamage((EntityDamageByEntityEvent) event);
            return;
        }

        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        if (!isNinja(player)) return;

        switch (event.getCause()) {
            case FALL:
                event.setDamage(event.getDamage() * .8);
                break;
            case FIRE:
            case FIRE_TICK:
                player.setFireTicks(0);
                event.setCancelled(true);
                break;
        }
    }

    private static EDBEExtractor<Player, LivingEntity, Arrow> extractor = new EDBEExtractor<>(
            Player.class,
            LivingEntity.class,
            Arrow.class
    );

    public void entityDamage(EntityDamageByEntityEvent event) {

        CombatantPair<Player, LivingEntity, Arrow> result = extractor.extractFrom(event);

        if (result == null) return;

        final Player attacker = result.getAttacker();

        if (isNinja(attacker) && result.hasProjectile()) {
            EntityDamageEvent lastEvent = attacker.getLastDamageCause();
            if (lastEvent != null) {
                double lastDamage = lastEvent.getFinalDamage();
                double scale = ((attacker.getMaxHealth() - attacker.getHealth()) / attacker.getMaxHealth());
                EntityUtil.heal(attacker, ChanceUtil.getRandom(scale * lastDamage));
            }
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {

        final Player player = event.getPlayer();
        ItemStack stack = player.getItemInHand();

        if (isNinja(player)) {
            Block clicked = event.getClickedBlock();
            BlockFace face = event.getBlockFace();

            boolean usingBow = stack.getType() == Material.BOW;

            switch (event.getAction()) {
                case LEFT_CLICK_AIR:
                    if (!canArrowBomb(player)) break;
                    if (usingBow) {
                        arrowBomb(player);
                    }
                    break;
                case LEFT_CLICK_BLOCK:
                    if (!canSmokeBomb(player)) break;
                    if (usingBow) {
                        smokeBomb(player);
                    }
                    break;
                case RIGHT_CLICK_BLOCK: {
                    if (!canGrapple(player)) break;

                    boolean isClimbableItem = stack.getType() == Material.AIR || ItemUtil.isSword(stack.getTypeId());
                    if (!isClimbableItem) break;

                    // Do not attempt to climb shrubs.
                    if (EnvironmentUtil.isShrubBlock(clicked)) {
                        break;
                    }
                    if (!face.equals(BlockFace.UP) && !face.equals(BlockFace.DOWN)) {
                        // Never climb if the clicked block was interactive, or could've been a misclick
                        // of a nearby interactive block.
                        if (EnvironmentUtil.isMaybeInteractiveBlock(clicked, face)) {
                            break;
                        }

                        // Check if the block is higher than the player
                        if (clicked.getLocation().getY() < player.getLocation().getY()) break;

                        // Check the 2D distance between the block and the player
                        if (LocationUtil.distanceSquared2D(clicked.getLocation().add(.5, 0, .5), player.getLocation()) <= 4) {
                            // If the player is sneaking treat this as a "slowing" option,
                            // if not sneaking, treat this as a proper grapple.
                            grapple(player, clicked, face, player.isSneaking() ? 0 : 12);
                        }
                    }
                    break;
                }
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
        if (passenger instanceof Player && isNinja((Player) passenger)) {
            event.setPower(event.getPower() * 1.37F);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        for (final Player otherPlayer : server.getOnlinePlayers()) {
            if (otherPlayer != player) otherPlayer.showPlayer(inst, player);
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

    @EventHandler(ignoreCancelled = true)
    public void onWhoisLookup(InfoComponent.PlayerWhoisEvent event) {

        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (!inst.hasPermission(player, "aurora.ninja")) return;
            event.addWhoisInformation("Ninja Mode", isNinja(player) ? "Enabled" : "Disabled");
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
        }
    }

    public class Commands {

        @Command(aliases = {"ninja"}, desc = "Give a player the Ninja power",
                flags = "", min = 0, max = 0)
        @CommandPermissions({"aurora.ninja"})
        public void ninja(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);
            if (inst.hasPermission(player, "aurora.rogue")) {
                throw new CommandException("You are a rogue not a ninja!");
            }

            final boolean isNinja = isNinja(player);

            // Enter Ninja Mode
            ninjaPlayer(player);

            if (!isNinja) {
                ChatUtil.sendNotice(player, "You are inspired and become a ninja!");
            } else {
                ChatUtil.sendNotice(player, "Ninja flags updated!");
            }
        }

        @Command(aliases = {"unninja"}, desc = "Revoke a player's Ninja power",
                flags = "", min = 0, max = 0)
        public void unninja(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);
            if (!isNinja(player)) {
                throw new CommandException("You are not a ninja!");
            }

            unninjaPlayer(player);

            ChatUtil.sendNotice(player, "You return to your previous boring existence.");
        }
    }

    private static class NinjaArrow {
        private final WeakReference<Arrow> arrowRef;
        private final long creationTimeStamp;

        public NinjaArrow(Arrow arrow) {
            this.arrowRef = new WeakReference<>(arrow);
            this.creationTimeStamp = System.currentTimeMillis();
        }

        public long getCreationTimeStamp() {
            return creationTimeStamp;
        }

        public Optional<Arrow> getIfStillRelevant(long lastArrowTime) {
            if (lastArrowTime - creationTimeStamp >= TimeUnit.SECONDS.toMillis(5)) {
                return Optional.empty();
            }

            Arrow arrow = arrowRef.get();
            if (arrow == null) {
                return Optional.empty();
            }

            if (!arrow.isValid()) {
                return Optional.empty();
            }

            return Optional.of(arrow);
        }
    }

    // Ninja Session
    private static class NinjaState extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        @Setting("ninja-enabled")
        private boolean isNinja = false;

        private List<NinjaArrow> recentArrows = new ArrayList<>();

        private long nextGrapple = 0;
        private long nextSmokeBomb = 0;
        private long nextArrowBomb = 0;

        protected NinjaState() {

            super(MAX_AGE);
        }

        public boolean isNinja() {

            return isNinja;
        }

        public void setIsNinja(boolean isNinja) {

            this.isNinja = isNinja;
        }

        public List<Arrow> getRecentArrows() {
            if (recentArrows.isEmpty()) {
                return new ArrayList<>();
            }

            long lastArrowTime = recentArrows.get(recentArrows.size() - 1).getCreationTimeStamp();
            List<Arrow> arrows = recentArrows.stream()
                    .map(arrow -> arrow.getIfStillRelevant(lastArrowTime))
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

            recentArrows.clear();

            return arrows;
        }

        public void addArrow(Arrow lastArrow) {
            recentArrows.add(new NinjaArrow(lastArrow));
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

            nextSmokeBomb = System.currentTimeMillis() + 2000;
        }

        public boolean canArrowBomb() {

            return nextArrowBomb == 0 || System.currentTimeMillis() >= nextArrowBomb;
        }

        public void arrowBomb() {

            nextArrowBomb = System.currentTimeMillis() + 3000;
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}