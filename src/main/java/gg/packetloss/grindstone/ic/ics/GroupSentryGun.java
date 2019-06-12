/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.ic.ics;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.mechanics.ic.*;
import com.sk89q.craftbook.util.LocationUtil;
import com.sk89q.craftbook.util.SearchArea;
import com.sk89q.util.yaml.YAMLProcessor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public class GroupSentryGun extends AbstractSelfTriggeredIC {

    private static final CommandBook inst = CommandBook.inst();
    private static final Logger log = inst.getLogger();
    private static final Server server = CommandBook.server();

    private SearchArea area;
    private String group;
    private float speed;

    public GroupSentryGun(Server server, ChangedSign block, ICFactory factory) {

        super(server, block, factory);
    }

    @Override
    public void load() {

        speed = 0.8f;
        String radius = "";

        String[] parts = getSign().getLine(2).split(":");
        for (int i = 0; i < parts.length; i++) {
            switch (i) {
                case 0:
                    try {
                        speed = Float.parseFloat(parts[i]);
                    } catch (Exception ignored) {
                    }
                    break;
                case 1:
                    radius = parts[i];
                    break;
            }
        }

        area = SearchArea.createArea(getBackBlock(), radius.isEmpty() ? "10" : radius);
        group = getSign().getLine(3);
    }

    @Override
    public String getTitle() {

        return "Group Sentry";
    }

    @Override
    public String getSignTitle() {

        return "GROUP SENTRY";
    }

    @Override
    public void trigger(ChipState chip) {

        shoot();
    }

    @Override
    public void think(ChipState chip) {

        if (((Factory) getFactory()).inverted == chip.getInput(0)) {
            trigger(chip);
        }
    }

    public void shoot() {

        for (Entity ent : area.getEntitiesInArea()) {
            if (!(ent instanceof LivingEntity)) continue;

            if (ent instanceof Player && !group.isEmpty()) {
                if (inst.getPermissionsResolver().inGroup((OfflinePlayer) ent, group)) continue;
            }

            Location k = LocationUtil.getCenterOfBlock(LocationUtil.getNextFreeSpace(getBackBlock(), BlockFace.UP));
            Arrow ar = area.getWorld().spawnArrow(k, ent.getLocation().add(0, ((LivingEntity) ent).getEyeHeight(), 0).subtract(k.clone().add(0.5, 0.5, 0.5)).toVector().normalize(), speed, 0);
            if (!((LivingEntity) ent).hasLineOfSight(ar)) {
                ar.remove();
                continue;
            }
            break;
        }
    }

    public static class Factory extends AbstractICFactory implements RestrictedIC, ConfigurableIC {

        public boolean inverted = false;

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new GroupSentryGun(getServer(), sign, this);
        }

        @Override
        public String getShortDescription() {

            return "Shoots nearby mobs with arrows.";
        }

        @Override
        public String[] getLineHelp() {

            return new String[]{"Speed[:radius]", "GroupName"};
        }

        @Override
        public void addConfiguration(YAMLProcessor config, String path) {

            inverted = config.getBoolean(path + "inverted", false);
        }
    }
}