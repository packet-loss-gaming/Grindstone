package us.arrowcraft.aurora;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.SessionComponent;
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
import us.arrowcraft.aurora.util.ChanceUtil;
import us.arrowcraft.aurora.util.ChatUtil;
import us.arrowcraft.aurora.util.EnvironmentUtil;

import java.util.HashSet;
import java.util.logging.Logger;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Creep Speak", desc = "Make mobs talk.")
@Depend(components = {SessionComponent.class, NinjaComponent.class, RogueComponent.class})
public class CreepSpeakComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

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

            if (entity instanceof Creeper) {
                try {
                    if (event.getReason().equals(TargetReason.FORGOT_TARGET)
                            || event.getReason().equals(TargetReason.TARGET_ATTACKED_ENTITY)
                            || event.getReason().equals(TargetReason.TARGET_ATTACKED_OWNER)) return;

                    // Alonzo Feature
                    if (ChanceUtil.getChance(220) && !inst.hasPermission(player, "aurora.alonzo.immune")
                            && config.enableAlonzo && !alonzoCreepersActive.contains(player)) {
                        Location loc = entity.getLocation();

                        for (int i = 0; i < ((ChanceUtil.getRandom(10) * ChanceUtil.getRandom(6)) + 4); i++) {
                            entity.getWorld().spawn(loc, Creeper.class);
                        }

                        alonzoCreepersActive.add(player);
                        server.getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {

                            @Override
                            public void run() {

                                alonzoCreepersActive.remove(player);
                            }
                        }, 20 * 30);

                        ChatUtil.sendNotice(player, ChatColor.DARK_RED, "Alonzzzo ssssent ussss.");
                    } else if (!ChanceUtil.getChance(5)) {
                        ChatUtil.sendNotice(player, ChatColor.DARK_GREEN, "That'sssss a very niccce"
                                + " everything you have there.");
                    }
                } catch (Exception e) {
                    log.warning("The creeper feature of the: "
                            + this.getInformation().friendlyName()
                            + " component could not be executed.");
                }
            } else if (entity instanceof IronGolem) {
                try {
                    if (event.getReason().equals(TargetReason.FORGOT_TARGET)
                            || event.getReason().equals(TargetReason.TARGET_ATTACKED_OWNER)) return;

                    if (event.getReason().equals(TargetReason.TARGET_ATTACKED_ENTITY)) {
                        ChatUtil.sendWarning(player, "Vengance!");
                    } else if (event.getReason().equals(TargetReason.TARGET_DIED)) {
                        ChatUtil.sendWarning(player, "You ssssshall follow in your allies fate.");
                    } else {
                        ChatUtil.sendWarning(player, "RAUGHHHHHHHHHHH!!!!!");
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

                    if (EnvironmentUtil.isServerTimeOdd(time)) {
                        ChatUtil.sendWarning(player, "Brainz!!!");
                    } else {
                        ChatUtil.sendWarning(player, "Ugh...");
                    }
                } catch (Exception e) {
                    log.warning("The Zombie feature of the: "
                            + this.getInformation().friendlyName()
                            + " component could not be executed.");
                }
            }
        } catch (Exception e) {
            log.warning("The rouge & ninja settings settings might have been ignored by the: "
                    + this.getInformation().friendlyName() + " component.");
        }
    }
}
