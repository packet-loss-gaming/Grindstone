/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.events.entity.HallowCreeperEvent;
import gg.packetloss.grindstone.events.environment.CreepSpeakEvent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static gg.packetloss.grindstone.apocalypse.ApocalypseHelper.checkEntity;

@ComponentInformation(friendlyName = "Creep Speak", desc = "Make mobs talk.")
@Depend(components = {SessionComponent.class})
public class CreepSpeakComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private SessionComponent sessions;

    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        CommandBook.registerEvents(this);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("hallow-creeper-chance")
        public int hallowCreeperChance = 157;
        @Setting("chance-of-message")
        public int chance = 7;
    }

    private boolean nearbyAreaHasTooManyCreepers(Location location) {
        return location.getNearbyEntitiesByType(Creeper.class, 50).stream().count() > 10;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        Entity entity = event.getEntity();
        Entity targetEntity = event.getTarget();

        // Important Check
        if (!(targetEntity instanceof Player player)) return;

        long time = player.getWorld().getTime();

        if (!ChanceUtil.getChance(config.chance)) return;

        ChatColor color = ChatColor.RED;
        String message = "";
        if (entity instanceof Creeper) {
            try {
                LocalDate now = LocalDate.now();
                boolean isFriday = now.getDayOfWeek().equals(DayOfWeek.FRIDAY);
                boolean isThirteenth = now.getDayOfMonth() == 13;
                int creeperChance = isFriday && isThirteenth ? 1 : config.hallowCreeperChance;
                // Hallow Feature
                if (creeperChance != -1 && ChanceUtil.getChance(creeperChance)
                        && !nearbyAreaHasTooManyCreepers(player.getLocation())) {
                    HallowCreeperEvent hallowEvent = new HallowCreeperEvent(player, (Creeper) entity);
                    CommandBook.callEvent(hallowEvent);
                    if (hallowEvent.isCancelled()) return;
                    Location loc = entity.getLocation();

                    for (int i = ((ChanceUtil.getRandom(12) * ChanceUtil.getRandom(12)) + 6); i > 0; --i) {
                        entity.getWorld().spawn(loc, Creeper.class);
                    }

                    color = ChatColor.DARK_RED;
                    message = "Haaaallooowwwww ssssent ussss.";
                } else {
                    color = ChatColor.DARK_GREEN;
                    message = "That'sssss a very niccce everything you have there.";
                }
            } catch (Exception e) {
                CommandBook.logger().warning("The creeper feature of the: " + this.getInformation().friendlyName()
                        + " component could not be executed.");
            }
        } else if (entity instanceof IronGolem) {
            try {
                color = ChatColor.RED;
                if (event.getReason().equals(TargetReason.TARGET_ATTACKED_ENTITY)) {
                    message = "Vengance!";
                } else if (event.getReason().equals(TargetReason.TARGET_DIED)) {
                    message = "You ssssshall follow in your allies fate.";
                } else {
                    message = "RAUGHHHHHHHHHHH!!!!!";
                }
            } catch (Exception e) {
                CommandBook.logger().warning("The IronGolem feature of the: "
                        + this.getInformation().friendlyName()
                        + " component could not be executed.");
            }
        } else if (entity instanceof Zombie && !(entity instanceof PigZombie)) {
            try {
                if (checkEntity(entity)) return;
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
                CommandBook.logger().warning("The Zombie feature of the: " + this.getInformation().friendlyName()
                        + " component could not be executed.");
            }
        }

        if (!message.isEmpty()) {
            CreepSpeakEvent creepyEvent = new CreepSpeakEvent(player, entity, color + message);
            CommandBook.callEvent(creepyEvent);
            if (!creepyEvent.isCancelled()) {
                ChatUtil.sendNotice(creepyEvent.getPlayer(), creepyEvent.getMessage());
            }

        }
    }
}
