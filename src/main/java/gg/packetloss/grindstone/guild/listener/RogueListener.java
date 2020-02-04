package gg.packetloss.grindstone.guild.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLib;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.sk89q.commandbook.CommandBook;
import fr.neatmonster.nocheatplus.checks.net.protocollib.ProtocolLibComponent;
import gg.packetloss.Pitfall.bukkit.event.PitfallTriggerEvent;
import gg.packetloss.grindstone.city.engine.combat.PvPComponent;
import gg.packetloss.grindstone.click.ClickType;
import gg.packetloss.grindstone.events.DoubleClickEvent;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.events.guild.*;
import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.powers.RoguePower;
import gg.packetloss.grindstone.guild.state.InternalGuildState;
import gg.packetloss.grindstone.guild.state.RogueState;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.guild.rogue.Nightmare;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

public class RogueListener implements Listener {
    private static final float DEFAULT_SPEED = .2F;

    private Function<Player, InternalGuildState> internalStateLookup;

    private PacketContainer packetContainer = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
    private WrappedDataWatcher watcher = new WrappedDataWatcher();
    private WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Byte.class);

    private static final byte ENABLE_RIPTIDE = 0x04;
    private static final byte DISABLE_RIPTIDE = 0x00;

    public RogueListener(Function<Player, InternalGuildState> internalStateLookup) {
        this.internalStateLookup = internalStateLookup;
    }

    private void sendRiptidePacket(Player player, byte packetVal) {
        packetContainer.getIntegers().write(0, player.getEntityId());
        watcher.setEntity(player);
        watcher.setObject(7, serializer, packetVal);
        packetContainer.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
        ProtocolLibrary.getProtocolManager().broadcastServerPacket(packetContainer, player, true);
//            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packetContainer);
    }

    private Optional<RogueState> getStateAllowDisabled(Player player) {
        InternalGuildState internalState = internalStateLookup.apply(player);
        if (internalState instanceof RogueState) {
            return Optional.of((RogueState) internalState);
        }
        return Optional.empty();

    }

    private Optional<RogueState> getState(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return Optional.empty();
        }

        return getStateAllowDisabled(player).filter(InternalGuildState::isEnabled);
    }

    private void applyPlayerModifications(Player player, RogueState state) {
        float multiplier = state.hasPower(RoguePower.SUPER_SPEED) ? 2.5f : 2f;
        player.setWalkSpeed(DEFAULT_SPEED * multiplier);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPowersEnable(GuildPowersEnableEvent event) {
        if (event.getGuild() != GuildType.ROGUE) {
            return;
        }

        Player player = event.getPlayer();
        applyPlayerModifications(player, getStateAllowDisabled(player).orElseThrow());

        ChatUtil.sendNotice(player, "You gain the power of a rogue warrior!");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelUp(GuildLevelUpEvent event) {
        if (event.getGuild() != GuildType.ROGUE) {
            return;
        }

        Player player = event.getPlayer();
        getState(player).ifPresent((state) -> {
            applyPlayerModifications(player, state);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPowersDisable(GuildPowersDisableEvent event) {
        if (event.getGuild() != GuildType.ROGUE) {
            return;
        }

        Player player = event.getPlayer();
        player.setWalkSpeed(DEFAULT_SPEED);

        ChatUtil.sendNotice(player, "You return to your weak existence.");
    }

    public void blip(Player player, RogueState state, double modifier, boolean auto) {
        RogueBlipEvent event = new RogueBlipEvent(player, modifier, auto);
        CommandBook.server().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        modifier = event.getModifier();

        state.blip();

        CommandBook.server().getPluginManager().callEvent(new ThrowPlayerEvent(player));

        Vector vel = player.getLocation().getDirection();
        vel.multiply(3 * modifier * Math.max(.1, player.getFoodLevel() / 20.0));

        double yMax;
        double yMin;

        if (auto) {
            yMax = .8;
            yMin = .175;
        } else {
            yMax = 1.4;
            yMin = -2;
        }

        vel.setY(Math.min(yMax, Math.max(yMin, vel.getY())));

        sendRiptidePacket(player, ENABLE_RIPTIDE);
        player.setVelocity(vel);

        TaskBuilder.Countdown task = TaskBuilder.countdown();

        task.setInterval(4);
        task.setDelay(20);
        task.setNumberOfRuns(1);
        task.setAction((times) -> {
            Material type = player.getLocation().subtract(0, 1, 0).getBlock().getType();
            Optional<RogueState> rogueState = getState(player);
            if (rogueState.isPresent()) {
                if (rogueState.get().canBlip() || type.isSolid() || EnvironmentUtil.isLiquid(type)) {
                    return true;
                }
            }
            return false;
        });

        task.setFinishAction(() -> sendRiptidePacket(player, DISABLE_RIPTIDE));

        task.build();



    }

    public void grenade(Player player, RogueState state) {
        RogueGrenadeEvent event = new RogueGrenadeEvent(player, ChanceUtil.getRandom(5) + 4);
        CommandBook.server().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        state.grenade();

        boolean rocketJump = GeneralPlayerUtil.isLookingDown(player) && GeneralPlayerUtil.isStandingOnSolidGround(player);

        boolean hasSniperSnowballs = state.hasPower(RoguePower.SNIPER_SNOWBALLS);

        for (int i = event.getGrenadeCount(); i > 0; --i) {
            Snowball snowball = player.launchProjectile(Snowball.class);

            double xAdjustment = ChanceUtil.getRandom(2.0);
            double zAdjustment = ChanceUtil.getRandom(2.0);

            if (hasSniperSnowballs) {
                xAdjustment = (xAdjustment / 3) + 1;
                zAdjustment = (zAdjustment / 3) + 1;
            }

            Vector vector = new Vector(xAdjustment, 1, zAdjustment);
            snowball.setVelocity(snowball.getVelocity().multiply(vector));
            snowball.setMetadata("rogue-snowball", new FixedMetadataValue(CommandBook.inst(), true));
            if (rocketJump) {
                snowball.setMetadata("rocket-jump", new FixedMetadataValue(CommandBook.inst(), true));
            }
        }

        if (rocketJump) {
            Vector newVelocity = player.getVelocity().add(new Vector(0, 6, 0));
            player.setVelocity(newVelocity);

            EntityUtil.forceDamage(player, 4);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent event) {
        Entity e = event.getEntity();
        if (e instanceof Player && event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
            Player player = (Player) e;
            Optional<RogueState> optState = getState(player);
            if (optState.isEmpty()) {
                return;
            }

            RogueState state = optState.get();
            if (!state.hasPower(RoguePower.FALL_DAMAGE_REDIRECTION)) {
                return;
            }

            List<Entity> entities = player.getNearbyEntities(2, 2, 2);

            if (entities.size() < 1) return;

            entities.sort(new EntityDistanceComparator(player.getLocation()));

            CommandBook.server().getPluginManager().callEvent(new RapidHitEvent(player));

            for (Entity entity : entities) {
                if (entity.equals(player)) continue;
                if (entity instanceof LivingEntity) {
                    if (entity instanceof Player && !PvPComponent.allowsPvP(player, (Player) entity)) continue;
                    if (entity.isDead()) continue;

                    if (entity instanceof Tameable && ((Tameable) entity).isTamed()) {
                        continue;
                    }

                    ((LivingEntity) entity).damage(event.getDamage() * .5, player);
                    event.setCancelled(true);
                    break;
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

        if (result.hasProjectile()) {
            Projectile projectile = result.getProjectile();
            if (projectile.hasMetadata("nightmare")) {
                event.setCancelled(true);
                return;
            }
        } else if (attacker instanceof Player) {
            Optional<RogueState> optState = getState((Player) attacker);
            if (optState.isEmpty()) {
                return;
            }

            if (event.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
                RogueState state = optState.get();
                if (state.hasPower(RoguePower.DAMAGE_BUFF)) {
                    event.setDamage(Math.max(event.getDamage(), Math.min((event.getDamage() + 10) * 1.2, 20)));
                }
            }
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
                    CommandBook.server().getPluginManager().callEvent(new RapidHitEvent((Player) shooter));
                }

                for (Entity entity : p.getNearbyEntities(3, 3, 3)) {
                    if (!entity.isValid() || entity.equals(shooter) || !(entity instanceof LivingEntity)) continue;

                    if (!ChanceUtil.getChance(10)) continue;

                    if (entity instanceof Player) {
                        if (((Player) entity).getGameMode().equals(GameMode.CREATIVE)) continue;
                        if (shooter instanceof Player) {
                            if (!PvPComponent.allowsPvP((Player) shooter, (Player) entity)) continue;
                        }
                    }

                    if (((LivingEntity) entity).getHealth() < 2) continue;

                    EntityUtil.heal(shooter, 1);
                    EntityUtil.forceDamage(entity, 1);
                }
            } else {
                if (shooter instanceof Player) {
                    double distanceSq = p.getLocation().distanceSquared(shooter.getLocation());
                    if (p.hasMetadata("rocket-jump") && distanceSq < Math.pow(3, 2)) {
                        p.getWorld().createExplosion(p.getLocation(), 0, false, false);
                    } else {
                        ExplosionStateFactory.createPvPExplosion(
                                (Player) shooter,
                                p.getLocation(),
                                1.75F,
                                false,
                                true
                        );
                    }
                } else {
                    ExplosionStateFactory.createExplosion(
                            p.getLocation(),
                            1.75F,
                            false,
                            true
                    );
                }
            }
        }
    }

    @EventHandler
    public void onDoubleClick(DoubleClickEvent event) {
        Player player = event.getPlayer();

        ItemStack stack = player.getItemInHand();
        if (!ItemUtil.isSword(stack)) {
            return;
        }

        Optional<RogueState> optState = getState(player);
        if (optState.isEmpty()) {
            return;
        }

        RogueState state = optState.get();
        if (event.getClickType() == ClickType.LEFT) {
            if (event.getAssociatedBlock() != null) {
                return;
            }

            if (player.isSneaking() && !state.getSettings().shouldBlipWhileSneaking()) {
                return;
            }

            CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
                if (state.canBlip()) {
                    blip(player, state, 2, false);
                }
            }, 1);
        } else if (event.getClickType() == ClickType.RIGHT) {
            if (state.canGrenade()) {
                grenade(player, state);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBowFire(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            final Player player = (Player) event.getEntity();

            Optional<RogueState> optState = getState(player);
            if (optState.isEmpty()) {
                return;
            }

            RogueState state = optState.get();

            Entity p = event.getProjectile();
            p.setVelocity(p.getVelocity().multiply(.9));

            state.stallBlip();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPitfallTrigger(PitfallTriggerEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Player) {
            Player player = (Player) entity;
            getState(player).ifPresent((state) -> {
                if (state.hasPower(RoguePower.PITFALL_LEAP)) {
                    blip(player, state, 1, true);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpecialAttack(SpecialAttackEvent event) {
        Player player = event.getPlayer();

        Optional<RogueState> optState = getState(player);
        if (optState.isEmpty()) {
            return;
        }

        RogueState state = optState.get();

        SpecialAttack attack = event.getSpec();

        if (event.getContext().equals(SpecType.MELEE)) {
            if (state.hasPower(RoguePower.NIGHTMARE_SPECIAL) && ChanceUtil.getChance(14)) {
                Nightmare newSpec = new Nightmare(attack.getOwner(), attack.getUsedItem(), attack.getTarget());
                if (newSpec.isValid()) {
                    event.setSpec(newSpec);
                }
            } else if (state.hasPower(RoguePower.SPEEDY_SPECIALS)) {
                float remainingCooldownPercentage = ChanceUtil.getChance(10) ? .1F : .66F;
                event.setContextCooldown((long) (event.getContextCoolDown() * remainingCooldownPercentage));
            }
        }
    }
}
