package gg.packetloss.grindstone.guild.listener;

import com.sk89q.commandbook.CommandBook;
import com.skelril.Pitfall.bukkit.event.PitfallTriggerEvent;
import gg.packetloss.grindstone.city.engine.combat.PvPComponent;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.events.guild.GuildPowersDisableEvent;
import gg.packetloss.grindstone.events.guild.GuildPowersEnableEvent;
import gg.packetloss.grindstone.events.guild.RogueBlipEvent;
import gg.packetloss.grindstone.events.guild.RogueGrenadeEvent;
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
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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

    private Optional<RogueState> getState(Player player) {
        InternalGuildState internalState = internalStateLookup.apply(player);
        if (internalState instanceof RogueState && internalState.isEnabled()) {
            return Optional.of((RogueState) internalState);
        }
        return Optional.empty();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPowersEnable(GuildPowersEnableEvent event) {
        if (event.getGuild() != GuildType.ROGUE) {
            return;
        }

        Player player = event.getPlayer();

        float multiplier = getState(player).orElseThrow().hasPower(RoguePower.SUPER_SPEED) ? 2.5f : 2f;
        player.setWalkSpeed(DEFAULT_SPEED * multiplier);

        ChatUtil.sendNotice(player, "You gain the power of a rogue warrior!");
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

        player.setVelocity(vel);
    }

    public void grenade(Player player, RogueState state) {
        RogueGrenadeEvent event = new RogueGrenadeEvent(player, ChanceUtil.getRandom(5) + 4);
        CommandBook.server().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        state.grenade();

        for (int i = event.getGrenadeCount(); i > 0; --i) {
            Snowball snowball = player.launchProjectile(Snowball.class);
            Vector vector = new Vector(ChanceUtil.getRandom(2.0), 1, ChanceUtil.getRandom(2.0));
            snowball.setVelocity(snowball.getVelocity().multiply(vector));
            snowball.setMetadata("rogue-snowball", new FixedMetadataValue(CommandBook.inst(), true));
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

            List<Entity> entities = player.getNearbyEntities(2, 2, 2);

            if (entities.size() < 1) return;

            entities.sort(new EntityDistanceComparator(player.getLocation()));

            CommandBook.server().getPluginManager().callEvent(new RapidHitEvent(player));

            for (Entity entity : entities) {
                if (entity.equals(player)) continue;
                if (entity instanceof LivingEntity) {
                    if (entity instanceof Player && !PvPComponent.allowsPvP(player, (Player) entity)) continue;
                    if (entity.isDead()) continue;

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
                state.recordAttack();

                event.setDamage(Math.max(event.getDamage(), Math.min((event.getDamage() + 10) * 1.2, 20)));
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
                    entity.playEffect(EntityEffect.HURT);
                }
            } else {
                if (shooter instanceof Player) {
                    ExplosionStateFactory.createPvPExplosion(
                            (Player) shooter,
                            p.getLocation(),
                            1.75F,
                            false,
                            true
                    );
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
    public void onRightClick(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        ItemStack stack = player.getItemInHand();

        Optional<RogueState> optState = getState(player);
        if (optState.isEmpty()) {
            return;
        }

        RogueState state = optState.get();
        if (stack != null && ItemUtil.isSword(stack)) {
            switch (event.getAction()) {
                case LEFT_CLICK_AIR:
                    if (state.isDoubleLeftClick()) {
                        CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
                            if (state.canBlip()) {
                                blip(player, state, 2, false);
                            }
                        }, 1);
                    }
                    break;
                case RIGHT_CLICK_AIR:
                    if (state.isDoubleRightClick() && state.canGrenade()) {
                        grenade(player, state);
                    }
                    break;
                case RIGHT_CLICK_BLOCK:
                    if (state.isDoubleRightClick() && state.canGrenade()) {
                        Block clicked = event.getClickedBlock();
                        BlockFace face = event.getBlockFace();

                        // Never throw grenades if the clicked block was interactive, or could've been a misclick
                        // of a nearby interactive block.
                        if (EnvironmentUtil.isMaybeInteractiveBlock(clicked, face)) {
                            break;
                        }

                        grenade(player, state);
                    }
                    break;
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
                blip(player, state, 1, true);
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpecialAttack(SpecialAttackEvent event) {

        Player player = event.getPlayer();

        if (getState(player).isEmpty()) {
            return;
        }

        SpecialAttack attack = event.getSpec();

        if (event.getContext().equals(SpecType.MELEE)) {
            if (ChanceUtil.getChance(14)) {
                event.setSpec(new Nightmare(attack.getOwner(), attack.getTarget()));
            } else {
                float remainingCooldownPercentage = ChanceUtil.getChance(10) ? .1F : .66F;
                event.setContextCooldown((long) (event.getContextCoolDown() * remainingCooldownPercentage));
            }
        }
    }
}
