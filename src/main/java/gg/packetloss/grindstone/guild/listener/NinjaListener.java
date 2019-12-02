package gg.packetloss.grindstone.guild.listener;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.skelril.Pitfall.bukkit.event.PitfallTriggerEvent;
import gg.packetloss.grindstone.city.engine.combat.PvPComponent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.events.guild.*;
import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.state.InternalGuildState;
import gg.packetloss.grindstone.guild.state.NinjaState;
import gg.packetloss.grindstone.guild.state.RogueState;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NinjaListener implements Listener {
    private static final double DEFAULT_MAX_HEALTH = 20;

    private Function<Player, InternalGuildState> internalStateLookup;

    public NinjaListener(Function<Player, InternalGuildState> internalStateLookup) {
        this.internalStateLookup = internalStateLookup;
    }

    private Optional<NinjaState> getState(Player player) {
        InternalGuildState internalState = internalStateLookup.apply(player);
        if (internalState instanceof NinjaState && internalState.isEnabled()) {
            return Optional.of((NinjaState) internalState);
        }
        return Optional.empty();
    }

    private Optional<RogueState> getRogueState(Player player) {
        InternalGuildState internalState = internalStateLookup.apply(player);
        if (internalState.isEnabled() && internalState instanceof RogueState) {
            return Optional.of((RogueState) internalState);
        }
        return Optional.empty();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPowersEnable(GuildPowersEnableEvent event) {
        if (event.getGuild() != GuildType.NINJA) {
            return;
        }

        Player player = event.getPlayer();
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(DEFAULT_MAX_HEALTH * 1.3);

        ChatUtil.sendNotice(player, "You are inspired and become a ninja!");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPowersDisable(GuildPowersDisableEvent event) {
        if (event.getGuild() != GuildType.NINJA) {
            return;
        }

        Player player = event.getPlayer();
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(DEFAULT_MAX_HEALTH);

        ChatUtil.sendNotice(player, "You return to your previous boring existence.");
    }

    @EventHandler
    public void onProjectileLaunch(EntityShootBowEvent event) {
        Entity e = event.getProjectile();
        Projectile p = e instanceof Projectile ? (Projectile) e : null;
        if (p == null || p.getShooter() == null || !(p.getShooter() instanceof Player)) return;

        Player player = (Player) p.getShooter();
        if (p instanceof Arrow) {
            Optional<NinjaState> optState = getState(player);
            if (optState.isEmpty()) {
                return;
            }

            NinjaState state = optState.get();
            state.addArrow((Arrow) p);

            if (ChanceUtil.getChance(4)) {
                event.setConsumeArrow(false);
            }
        }
    }

    private static EDBEExtractor<Player, LivingEntity, Arrow> extractor = new EDBEExtractor<>(
            Player.class,
            LivingEntity.class,
            Arrow.class
    );

    private void entityDamage(EntityDamageByEntityEvent event) {
        CombatantPair<Player, LivingEntity, Arrow> result = extractor.extractFrom(event);

        if (result == null) return;

        final Player attacker = result.getAttacker();
        if (getState(attacker).isEmpty()) {
            return;
        }

        if (!result.hasProjectile()) {
            return;
        }

        EntityDamageEvent lastEvent = attacker.getLastDamageCause();
        if (lastEvent != null) {
            double lastDamage = lastEvent.getFinalDamage();
            double scale = ((attacker.getMaxHealth() - attacker.getHealth()) / attacker.getMaxHealth());
            EntityUtil.heal(attacker, ChanceUtil.getRandom(scale * lastDamage));
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
        if (getState(player).isEmpty()) return;

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

    public void arrowBomb(Player player, NinjaState state) {
        List<Arrow> arrows = state.getRecentArrows();
        if (arrows.isEmpty()) return;

        NinjaArrowBombEvent event = new NinjaArrowBombEvent(
                player,
                arrows
        );

        CommandBook.server().getPluginManager().callEvent(event);
        if (event.isCancelled() || arrows.isEmpty()) return;

        state.arrowBomb();

        for (Arrow arrow : arrows) {
            handleArrowBombArrow(player, arrow);
        }
    }

    private List<Entity> refineSmokeBombTargets(Player player, List<Entity> entities) {
        return entities.stream().filter(e -> {
            if (e instanceof Player) {
                return PvPComponent.allowsPvP(player, (Player) e);
            }

            return true;
        }).sorted(new EntityDistanceComparator(player.getLocation())).collect(Collectors.toList());
    }

    private Optional<Location> getSmokeBombOldLoc(Player player, List<Entity> entities) {
        Entity targetEntity = null;

        for (Entity entity : entities) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

            targetEntity = entity;
        }

        if (targetEntity == null) {
            return Optional.empty();
        }

        Location targetLoc = targetEntity.getLocation();
        targetLoc.setDirection(targetLoc.toVector().subtract(player.getLocation().toVector()));
        return Optional.of(targetLoc);
    }

    public void smokeBomb(final Player player, NinjaState state) {

        List<Entity> entities = player.getNearbyEntities(4, 4, 4);
        entities = refineSmokeBombTargets(player, entities);

        Optional<Location> optSmokeBombLoc = getSmokeBombOldLoc(player, entities);
        if (optSmokeBombLoc.isEmpty()) {
            return;
        }

        NinjaSmokeBombEvent event = new NinjaSmokeBombEvent(
                player,
                3,
                30,
                entities,
                optSmokeBombLoc.get(),
                player.getLocation()
        );

        CommandBook.server().getPluginManager().callEvent(event);
        if (event.isCancelled() || entities.isEmpty()) return;

        state.smokeBomb();

        Location targetLoc = event.getTargetLoc();
        int totalHealed = 0;
        for (Entity entity : entities) {
            EntityUtil.forceDamage(entity, 1);
            totalHealed += 1;

            if (entity instanceof Player) {
                getRogueState((Player) entity).ifPresent(RogueState::stallBlip);

                ChatUtil.sendWarning(entity, "You hear a strange ticking sound...");
            }

            Location newEntityLoc = targetLoc.clone();

            newEntityLoc.setPitch((float) (ChanceUtil.getRandom(361.0) - 1));
            newEntityLoc.setYaw((float) (ChanceUtil.getRandom(181.0) - 91));

            entity.teleport(newEntityLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
        }
        EntityUtil.heal(player, ChanceUtil.getRandomNTimes(totalHealed, 3));

        Location[] locations = new Location[]{
                player.getLocation(),
                player.getEyeLocation()
        };
        EnvironmentUtil.generateRadialEffect(locations, Effect.SMOKE);

        // Offset by 1 so that the bomb is not messed up by blocks
        if (targetLoc.getBlock().getType() != Material.AIR) {
            targetLoc.add(0, 1, 0);
        }

        final Location finalTargetLoc = targetLoc;
        CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
            ExplosionStateFactory.createPvPExplosion(
                    player,
                    finalTargetLoc,
                    event.getExplosionPower(),
                    false,
                    true
            );
        }, event.getDelay());

        player.teleport(event.getTeleportLoc(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    public void grapple(final Player player, NinjaState state, Block block, BlockFace clickedFace, double maxClimb) {

        NinjaGrappleEvent event = new NinjaGrappleEvent(player, maxClimb);
        CommandBook.server().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        Location k = player.getLocation();

        switch (k.getBlock().getTypeId()) {
            case BlockID.AIR:
                k.setX(k.getBlockX() + .5);
                k.setZ(k.getBlockZ() + .5);
                player.teleport(k, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                break;
        }


        CommandBook.server().getPluginManager().callEvent(new ThrowPlayerEvent(player));

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

        state.grapple(Math.max(i, 3) * 200);
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

    @EventHandler
    public void onClick(PlayerInteractEvent event) {

        final Player player = event.getPlayer();
        ItemStack stack = player.getItemInHand();

        Optional<NinjaState> optState = getState(player);
        if (optState.isEmpty()) {
            return;
        }

        NinjaState state = optState.get();

        Block clicked = event.getClickedBlock();
        BlockFace face = event.getBlockFace();

        boolean usingBow = stack.getType() == Material.BOW;

        switch (event.getAction()) {
            case LEFT_CLICK_AIR:
                if (!state.canArrowBomb()) break;
                if (usingBow) {
                    arrowBomb(player, state);
                }
                break;
            case LEFT_CLICK_BLOCK:
                if (!state.canSmokeBomb()) break;
                if (usingBow) {
                    smokeBomb(player, state);
                }
                break;
            case RIGHT_CLICK_BLOCK: {
                if (!state.canGrapple()) break;

                boolean isClimbableItem = stack.getType() == Material.AIR || ItemUtil.isSword(stack.getType());
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
                        grapple(player, state, clicked, face, player.isSneaking() ? 0 : 12);
                    }
                }
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPitfallEvent(PitfallTriggerEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player) entity;

            if (getState(player).isPresent() && player.isSneaking()) {
                event.setCancelled(true);
            }
        }
    }
}
