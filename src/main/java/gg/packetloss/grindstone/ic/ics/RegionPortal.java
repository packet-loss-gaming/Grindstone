/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.ic.ics;

import com.destroystokyo.paper.ParticleBuilder;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.mechanics.ic.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.VectorUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import org.bukkit.*;
import org.bukkit.entity.Entity;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RegionPortal extends AbstractSelfTriggeredIC {

    private static final CommandBook inst = CommandBook.inst();
    private static final Logger log = inst.getLogger();
    private static final Server server = CommandBook.server();

    private ProtectedRegion topRegion = null;
    private ProtectedRegion bottomRegion = null;

    private Queue<UUID> coolDown = new ArrayDeque<>();

    public RegionPortal(Server server, ChangedSign block, ICFactory factory) {
        super(server, block, factory);
    }

    @Override
    public void load() {
        RegionManager regionManager = WorldGuardBridge.getManagerFor(getLocation().getWorld());

        topRegion = regionManager.getRegion(getSign().getLine(2));
        bottomRegion = regionManager.getRegion(getSign().getLine(3));
    }

    @Override
    public String getTitle() {
        return "Region Portal";
    }

    @Override
    public String getSignTitle() {
        return "REGION PORTAL";
    }

    @Override
    public void trigger(ChipState chip) {
        swap();
    }

    @Override
    public void think(ChipState chip) {
        if (!chip.getInput(0)) {
            trigger(chip);
        }
    }

    private List<Entity> entitiesIn(ProtectedRegion region) {
        return getLocation().getWorld().getEntities().stream()
                .filter(e -> e.isValid() && LocationUtil.isInRegion(region, e) && !coolDown.contains(e.getUniqueId()))
                .collect(Collectors.toList());
    }

    private org.bukkit.util.Vector createDirectionalVector(Location loc, ProtectedRegion toRegion) {
        BlockVector3 midpointOffset = toRegion.getMaximumPoint().subtract(toRegion.getMinimumPoint()).divide(2);
        BlockVector3 midpoint = toRegion.getMinimumPoint().add(midpointOffset);
        org.bukkit.util.Vector bukkitMidpoint = new org.bukkit.util.Vector(
                midpoint.getX() + .5, midpoint.getY(), midpoint.getZ() + .5
        );

        org.bukkit.util.Vector diff = VectorUtil.createDirectionalVector(bukkitMidpoint, loc.toVector());
        diff.setY(0);

        return diff;
    }

    private Location translate(ProtectedRegion fromRegion, ProtectedRegion toRegion, Location loc) {
        World world = loc.getWorld();

        Location fromAnchor = BukkitAdapter.adapt(world, fromRegion.getMaximumPoint());
        Location toAnchor = BukkitAdapter.adapt(world, toRegion.getMaximumPoint());

        return toAnchor.subtract(
                fromAnchor.getX() - loc.getX(),
                0,
                fromAnchor.getZ() - loc.getZ()
        );
    }

    private void teleport(ProtectedRegion fromRegion, ProtectedRegion toRegion, Entity entity) {
        Location tpLoc = translate(fromRegion, toRegion, entity.getLocation());
        tpLoc.setDirection(createDirectionalVector(tpLoc, toRegion));

        entity.teleport(LocationUtil.findFreePosition(tpLoc, false));
    }

    private void playEffect(Location location) {
        new ParticleBuilder(Particle.SPELL_MOB).count(40).location(location).allPlayers().spawn();
    }

    private void send(ProtectedRegion fromRegion, ProtectedRegion toRegion, Entity e) {
        playEffect(e.getLocation());
        teleport(fromRegion, toRegion, e);
        playEffect(e.getLocation());

        coolDown.add(e.getUniqueId());
    }

    private void send(ProtectedRegion fromRegion, ProtectedRegion toRegion, List<Entity> entities) {
        for (Entity e : entities) {
            send(fromRegion, toRegion, e);
        }
    }

    public void swap() {
        if (topRegion == null || bottomRegion == null) {
            return;
        }

        List<Entity> topEntities = entitiesIn(topRegion);
        List<Entity> bottomEntities = entitiesIn(bottomRegion);

        int curSize = coolDown.size();
        send(topRegion, bottomRegion, topEntities);
        send(bottomRegion, topRegion, bottomEntities);
        int finalSize = coolDown.size();

        if (curSize == finalSize) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
            for (int i = finalSize - curSize; i > 0; --i) {
                coolDown.poll();
            }
        }, 30);
    }

    public static class Factory extends AbstractICFactory implements RestrictedIC {

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new RegionPortal(getServer(), sign, this);
        }

        @Override
        public String getShortDescription() {

            return "Teleports between two locations.";
        }

        @Override
        public String[] getLineHelp() {

            return new String[]{"Top Region", "Bottom Region"};
        }
    }
}