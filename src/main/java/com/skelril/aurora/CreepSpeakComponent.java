package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.SessionComponent;
import com.skelril.aurora.city.engine.ApocalypseComponent;
import com.skelril.aurora.events.environment.CreepSpeakEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;

import java.util.HashSet;
import java.util.logging.Logger;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Creep Speak", desc = "Make mobs talk.")
@Depend(components = {ApocalypseComponent.class, SessionComponent.class, NinjaComponent.class, RogueComponent.class})
public class CreepSpeakComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private ApocalypseComponent apocalypse;
    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private NinjaComponent ninjaComponent;
    @InjectComponent
    private RogueComponent rogueComponent;

    private HashSet<Player> alonzoCreepersActive = new HashSet<>();
    private LocalConfiguration config;

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("enable-alonzo")
        public boolean enableAlonzo = true;
        @Setting("alonzo-creeper-chance")
        public int alonzoCreeperChance = 157;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {

        Entity entity = event.getEntity();
        Entity targetEntity = event.getTarget();

        // Important Check
        if (!(targetEntity instanceof Player)) return;

        final Player player = (Player) targetEntity;
        long time = player.getWorld().getTime();

        try {
            // Ninja Check
            if ((ninjaComponent.isNinja(player) && player.isSneaking()) || !rogueComponent.isVisible(player)
                    || (rogueComponent.isRogue(player) && inst.hasPermission(player, "aurora.rouge.guild"))) return;

            ChatColor color = ChatColor.RED;
            String message = "";
            if (entity instanceof Creeper) {
                try {
                    if (event.getReason().equals(TargetReason.FORGOT_TARGET)
                            || event.getReason().equals(TargetReason.TARGET_ATTACKED_ENTITY)
                            || event.getReason().equals(TargetReason.TARGET_ATTACKED_OWNER)) return;

                    // Alonzo Feature
                    if (config.enableAlonzo && ChanceUtil.getChance(config.alonzoCreeperChance)
                            && !inst.hasPermission(player, "aurora.alonzo.immune")
                            && !alonzoCreepersActive.contains(player)) {
                        Location loc = entity.getLocation();

                        for (int i = 0; i < ((ChanceUtil.getRandom(12) * ChanceUtil.getRandom(12)) + 6); i++) {
                            entity.getWorld().spawn(loc, Creeper.class);
                        }

                        alonzoCreepersActive.add(player);
                        server.getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {

                            @Override
                            public void run() {

                                alonzoCreepersActive.remove(player);
                            }
                        }, 20 * 30);

                        color = ChatColor.DARK_RED;
                        message = "Alonzzzo ssssent ussss.";
                    } else if (!ChanceUtil.getChance(5)) {
                        color = ChatColor.DARK_GREEN;
                        message = "That'sssss a very niccce everything you have there.";
                    }
                } catch (Exception e) {
                    log.warning("The creeper feature of the: " + this.getInformation().friendlyName()
                            + " component could not be executed.");
                }
            } else if (entity instanceof IronGolem) {
                try {
                    if (event.getReason().equals(TargetReason.FORGOT_TARGET)
                            || event.getReason().equals(TargetReason.TARGET_ATTACKED_OWNER)) return;

                    color = ChatColor.RED;
                    if (event.getReason().equals(TargetReason.TARGET_ATTACKED_ENTITY)) {
                        message = "Vengance!";
                    } else if (event.getReason().equals(TargetReason.TARGET_DIED)) {
                        message = "You ssssshall follow in your allies fate.";
                    } else {
                        message = "RAUGHHHHHHHHHHH!!!!!";
                    }
                } catch (Exception e) {
                    log.warning("The IronGolem feature of the: "
                            + this.getInformation().friendlyName()
                            + " component could not be executed.");
                }
            } else if (entity instanceof Zombie && !(entity instanceof PigZombie)) {
                try {
                    if (!(event.getReason().equals(TargetReason.TARGET_DIED)
                            || event.getReason().equals(TargetReason.CLOSEST_PLAYER)
                            || event.getReason().equals(TargetReason.RANDOM_TARGET))) return;

                    if (apocalypse.checkEntity((LivingEntity) entity)) return;
                    color = ChatColor.RED;
                    if (EnvironmentUtil.isServerTimeOdd(time)) {
                        message = "Brainz!!!";
                    } else if (ChanceUtil.getChance(300)) {
                        color = ChatColor.DARK_GREEN;
                        message = "Graaaiiiinnnnsssss...";
                    } else {
                        message = "Ugh...";
                    }
                } catch (Exception e) {
                    log.warning("The Zombie feature of the: " + this.getInformation().friendlyName()
                            + " component could not be executed.");
                }
            }

            if (!message.isEmpty()) {
                CreepSpeakEvent creepyEvent = new CreepSpeakEvent(player, entity, color + message);
                server.getPluginManager().callEvent(creepyEvent);
                if (!creepyEvent.isCancelled()) {
                    ChatUtil.sendNotice(creepyEvent.getPlayer(), creepyEvent.getMessage());
                }

            }
        } catch (Exception e) {
            log.warning("The rouge & ninja settings settings might have been ignored by the: "
                    + this.getInformation().friendlyName() + " component.");
        }
    }
}
