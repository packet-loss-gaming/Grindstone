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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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

    public void setLastArrow(Player player, Arrow arrow) {

        sessions.getSession(NinjaState.class, player).setLastArrow(arrow);
    }

    public Arrow getLastArrow(Player player) {

        Arrow arrow = sessions.getSession(NinjaState.class, player).getLastArrow();
        return arrow != null && arrow.isValid() ? arrow : null;
    }

    public boolean canArrowBomb(Player player) {

        return sessions.getSession(NinjaState.class, player).canArrowBomb();
    }

    public void arrowBomb(final Player player) {
        Arrow arrow = getLastArrow(player);

        if (arrow == null) return;

        sessions.getSession(NinjaState.class, player).arrowBomb();

        for (Entity entity : arrow.getNearbyEntities(7, 7, 7)) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;
            if (entity instanceof Player) {
                final Player defender = (Player) entity;
                if (!PvPComponent.allowsPvP(player, defender)) return;
            }
        }
        arrow.getWorld().createExplosion(arrow.getLocation(), 4);
        arrow.remove();
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
                if (!PvPComponent.allowsPvP(player, (Player) entity)) return;
                modifyCamera = true;
            } else if (entity instanceof Monster) {
                modifyCamera = true;
            }

            oldLoc = entity.getLocation();
        }

        Location k = null;
        for (Entity entity : entities) {
            EntityUtil.forceDamage(entity, 1);
            if (ChanceUtil.getChance(7)) {
                EntityUtil.heal(entity, ChanceUtil.getRandom(3));
            }
            if (entity instanceof Player) {
                if (rogueComponent.isRogue((Player) entity)) {
                    rogueComponent.getState((Player) entity).blip(1250);
                }
                ((Player) entity).addPotionEffect(new PotionEffect(
                        PotionEffectType.BLINDNESS,
                        20 * ChanceUtil.getRandom(3),
                        1
                ));
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
        server.getScheduler().runTaskLater(inst,
                () -> finalK.getWorld().createExplosion(finalK, event.getExplosionPower(), false), event.getDelay());

        if (oldLoc != null) {
            oldLoc.setDirection(modifyCamera ? oldLoc.getDirection().multiply(-1) : player.getLocation().getDirection());
        }
        Location target = event.getTeleportLoc() == null ? oldLoc : event.getTeleportLoc();
        player.teleport(target, PlayerTeleportEvent.TeleportCause.UNKNOWN);
        session.hide(2500);
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
                setLastArrow(player, (Arrow) p);
                // p.setMetadata("ninja-arrow", new FixedMetadataValue(inst, true));
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
                event.setDamage(event.getDamage() * .5);
                break;
            case DROWNING:
                player.setRemainingAir(player.getMaximumAir());
            case FIRE:
                event.setCancelled(true);
                break;
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
                            grapple(player, clicked, face, player.isSneaking() ? 0 : 9);
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

            Set<Player> invisibleNewCount = new HashSet<>();
            Set<Player> visibleNewCount = new HashSet<>();

            Location pLoc = player.getLocation();
            Location k = player.getLocation();

            double sneakRatio = SNEAK_WATCH_DISTANCE_SQ;
            double standRatio = Math.max(sneakRatio, (player.getHealth() / player.getMaxHealth()) * WATCH_DISTANCE_SQ);

            if (player.isFlying()) {
                sneakRatio *= sneakRatio;
                standRatio *= standRatio;
            }

            for (Player otherPlayer : server.getOnlinePlayers()) {
                if (otherPlayer.equals(player)) continue;

                if (otherPlayer.getWorld().equals(player.getWorld()) && canVanish(player)) {

                    // Sets k to the otherPlayer's current location
                    otherPlayer.getLocation(k);

                    double dist = pLoc.distanceSquared(k);

                    if ((player.isSneaking() && dist >= sneakRatio) || dist >= standRatio || !ninjaState.showPlayer()) {
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

            Player player = PlayerUtil.checkPlayer(sender);
            if (inst.hasPermission(player, "aurora.rogue")) {
                throw new CommandException("You are a rogue not a ninja!");
            }

            final boolean isNinja = isNinja(player);

            // Enter Ninja Mode
            ninjaPlayer(player);

            // Set flags
            showToGuild(player, args.hasFlag('g'));
            useVanish(player, !args.hasFlag('i'));
            useTormentArrows(player, !args.hasFlag('t'));

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

        private Arrow lastArrow = null;

        private long nextSeen = 0;
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

        public Arrow getLastArrow() {

            return lastArrow;
        }

        public void setLastArrow(Arrow lastArrow) {

            this.lastArrow = lastArrow;
        }

        public boolean showPlayer() {

            return nextSeen == 0 || System.currentTimeMillis() >= nextSeen;
        }

        public void hide(long time) {

            nextSeen = System.currentTimeMillis() + time;
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

            nextSmokeBomb = System.currentTimeMillis() + 5000;
        }

        public boolean canArrowBomb() {

            return nextArrowBomb == 0 || System.currentTimeMillis() >= nextArrowBomb;
        }

        public void arrowBomb() {

            nextArrowBomb = System.currentTimeMillis() + 10000;
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}