/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.listener;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.Pitfall.bukkit.event.PitfallTriggerEvent;
import gg.packetloss.grindstone.click.ClickType;
import gg.packetloss.grindstone.events.DoubleClickEvent;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackSelectEvent;
import gg.packetloss.grindstone.events.guild.*;
import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.powers.RoguePower;
import gg.packetloss.grindstone.guild.state.InternalGuildState;
import gg.packetloss.grindstone.guild.state.RogueState;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.SpecialAttackFactory;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.guild.rogue.Nightmare;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EntityDistanceComparator;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import gg.packetloss.grindstone.world.type.city.combat.PvPComponent;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class RogueListener implements Listener {
    private static final float DEFAULT_SPEED = .2F;

    private Function<Player, InternalGuildState> internalStateLookup;

    public RogueListener(Function<Player, InternalGuildState> internalStateLookup) {
        this.internalStateLookup = internalStateLookup;
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

    private void impactBlip(Player player, RogueState state) {
        Vector oldVelocity = player.getVelocity();
        Vector newVelocity = oldVelocity.setY(Math.min(oldVelocity.getY(), -6));
        player.setVelocity(newVelocity);

        state.setImpactEnabled(true);

        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setAction((times) -> {
            return GeneralPlayerUtil.isStandingOnSolidGround(player);
        });
        taskBuilder.setFinishAction(() -> {
            state.setImpactEnabled(false);
        });

        taskBuilder.build();
    }

    private void constrainedBlip(Player player, double modifier, double yMin, double yMax) {
        Vector vel = player.getLocation().getDirection();
        vel.multiply(3 * modifier * Math.max(.1, player.getFoodLevel() / 20.0));

        vel.setY(Math.min(yMax, Math.max(yMin, vel.getY())));

        player.setVelocity(vel);
    }

    private void verticalBlip(Player player, double modifier) {
        double yMax = 2;
        double yMin = 0;

        constrainedBlip(player, modifier, yMin, yMax);
    }

    private void autoBlip(Player player, double modifier) {
        double yMax = .8;
        double yMin = .175;

        constrainedBlip(player, modifier, yMin, yMax);
    }

    private void normalBlip(Player player, double modifier) {
        double yMax = 1.4;
        double yMin = -2;

        constrainedBlip(player, modifier, yMin, yMax);
    }

    public void blip(Player player, RogueState state, double modifier, boolean auto) {
        RogueBlipEvent event = new RogueBlipEvent(player, modifier, auto);
        CommandBook.server().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        modifier = event.getModifier();

        state.blip();

        CommandBook.server().getPluginManager().callEvent(new ThrowPlayerEvent(player));

        if (auto) {
            autoBlip(player, modifier);
            return;
        }

        boolean isOnSolidGround = GeneralPlayerUtil.isStandingOnSolidGround(player);
        if (GeneralPlayerUtil.isLookingDown(player) && !isOnSolidGround) {
            impactBlip(player, state);
            return;
        }

        if (GeneralPlayerUtil.isLookingUp(player) && isOnSolidGround) {
            verticalBlip(player, modifier);
            return;
        }

        normalBlip(player, modifier);
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
            snowball.setMetadata("guild-exp-modifier", new FixedMetadataValue(CommandBook.inst(), 0D));
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

            if (state.isUsingImpact()) {
                event.setDamage(event.getDamage() * .4);

                if (state.hasPower(RoguePower.LIKE_A_METEOR) && player.isSneaking()) {
                    final boolean breakBlocks = state.getSettings().shouldAllowEnvironmentalDamage();

                    final double circleDist = 2 * Math.PI;
                    final double explosionPointDist = circleDist / 8;

                    final double radius = 5.5;

                    for (double i = 0; i < circleDist; i += explosionPointDist) {
                        double x = radius * Math.cos(i);
                        double z = radius * Math.sin(i);

                        ExplosionStateFactory.createPvPExplosion(
                            player,
                            player.getLocation().add(x, 0, z),
                            3,
                            /*setFire=*/breakBlocks,
                            /*breakBlocks=*/breakBlocks
                        );
                    }
                }
            }

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageEventMonitor(EntityDamageEvent event) {
        Entity e = event.getEntity();
        if (e instanceof Player) {
            Player player = (Player) e;
            Optional<RogueState> optState = getState(player);
            if (optState.isEmpty()) {
                return;
            }

            RogueState state = optState.get();
            state.clearHits();
        }
    }

    private static EDBEExtractor<LivingEntity, LivingEntity, Projectile> extractor = new EDBEExtractor<>(
            LivingEntity.class,
            LivingEntity.class,
            Projectile.class
    );

    private boolean isBackstab(Player attacker, LivingEntity defender) {
        float attackerYaw = (defender.getEyeLocation().getYaw() + 180) % 360;
        float defenderYaw = (attacker.getEyeLocation().getYaw() + 180) % 360;

        float yawDiff = Math.abs(attackerYaw - defenderYaw);
        return yawDiff <= 90 || yawDiff >= (360 - 90);
    }

    private int getBoostDamage(int hits, boolean backstabbed) {
        int boostDamage = 5 + (hits - 1);
        if (backstabbed) {
            boostDamage *= 3;
        }

        return Math.min(100, boostDamage);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {

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

            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                if (SpecialAttackFactory.getCurrentSpecialAttack().isPresent()) {
                    return;
                }

                RogueState state = optState.get();
                if (state.hasPower(RoguePower.BERSERKER)) {
                    int hits = state.getUninterruptedHits();
                    boolean backstabbed = state.hasPower(RoguePower.BACKSTAB) &&
                                          isBackstab((Player) attacker, result.getDefender());
                    if (hits > 0 || backstabbed) {
                        int boostDamage = getBoostDamage(hits, backstabbed);
                        event.setDamage(event.getDamage() + boostDamage);

                        CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                            if (event.isCancelled()) {
                                return;
                            }

                            if (backstabbed) {
                                ChatUtil.sendNotice(attacker, "Backstabbed!");
                            }

                            if (state.getSettings().shouldShowBerserkerBuffs()) {
                                ((Player) attacker).sendActionBar(ChatColor.RED + "Berserker Boost: +" + boostDamage);
                            }
                        });
                    }
                }
            }
        }
    }

    private static List<EntityDamageEvent.DamageCause> MELEE_HIT_CAUSES = List.of(
            EntityDamageEvent.DamageCause.ENTITY_ATTACK, EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageEntityMonitor(EntityDamageByEntityEvent event) {
        CombatantPair<LivingEntity, LivingEntity, Projectile> result = extractor.extractFrom(event);

        if (result == null) return;

        final LivingEntity attacker = result.getAttacker();
        if (attacker instanceof Player) {
            Optional<RogueState> optState = getState((Player) attacker);
            if (optState.isEmpty()) {
                return;
            }

            if (MELEE_HIT_CAUSES.contains(event.getCause())) {
                if (SpecialAttackFactory.getCurrentSpecialAttack().isPresent()) {
                    return;
                }

                RogueState state = optState.get();
                if (state.hasPower(RoguePower.BERSERKER)) {
                    LivingEntity defender = result.getDefender();
                    if (EntityUtil.isHostileMobOrPlayer(defender)) {
                        state.hitEntity();
                    } else {
                        state.clearHits();
                    }
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

                    if (EntityUtil.getHealth(shooter, (LivingEntity) entity) < 2) continue;

                    EntityUtil.heal(shooter, 1);
                    EntityUtil.forceDamage(shooter, (LivingEntity) entity, 1);
                }
            } else {
                if (shooter instanceof Player shootingPlayer) {
                    Optional<RogueState> optState = getState(shootingPlayer);
                    if (optState.isEmpty()) {
                        return;
                    }

                    double distanceSq = p.getLocation().distanceSquared(shooter.getLocation());
                    if (p.hasMetadata("rocket-jump") && distanceSq < Math.pow(3, 2)) {
                        ExplosionStateFactory.createFakeExplosion(p.getLocation());
                    } else {
                        RogueState state = optState.get();

                        ExplosionStateFactory.createPvPExplosion(
                            shootingPlayer,
                            p.getLocation(),
                            1.75F,
                            false,
                            state.getSettings().shouldAllowEnvironmentalDamage()
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

            boolean isOnSolidGround = GeneralPlayerUtil.isStandingOnSolidGround(player);
            if (isOnSolidGround && player.isSneaking() && !state.getSettings().shouldBlipWhileSneaking()) {
                return;
            }

            CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
                if (state.canBlip()) {
                    blip(player, state, 2, false);
                }
            }, 1);
        } else if (event.getClickType() == ClickType.RIGHT) {
            if (event.isInteractive()) {
                return;
            }

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
    public void onSpecialAttack(SpecialAttackSelectEvent event) {
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
            }
        }
    }
}
