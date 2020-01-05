/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.TemplateComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import gg.packetloss.grindstone.util.CollectionUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@TemplateComponent
public abstract class AreaComponent<Config extends ConfigurationBase> extends BukkitComponent implements Runnable {
    protected final CommandBook inst = CommandBook.inst();
    protected final Logger log = inst.getLogger();
    protected final Server server = CommandBook.server();

    protected int tick;
    protected AreaListener listener;

    protected World world;
    protected ProtectedRegion region;
    protected Config config;
    protected boolean empty = true;

    public abstract void setUp();

    @Override
    public void enable() {
        setUp();
        if (listener != null) {
            //noinspection AccessStaticViaInstance
            inst.registerEvents(listener);
        }
        if (config != null) {
            config = configure(config);
            saveConfig();
        }
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 0, tick);
    }

    @Override
    public void reload() {
        super.reload();
        if (config != null) {
            configure(config);
        }
    }

    @Override
    public abstract void run();

    public Config getConfig() {
        return config;
    }

    public Collection<Player> getContainedParticipants() {
        return getContained(Player.class).stream()
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL)
                .collect(Collectors.toList());
    }

    public Optional<Player> getRandomParticipant() {
        Collection<Player> players = getContainedParticipants();
        if (players.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(CollectionUtil.getElement(players));
    }

    public Collection<Player> getAudiblePlayers() {
        return getContained(Player.class);
    }

    public <T extends Entity> Collection<T> getContained(Class<T> clazz) {
        return getContained(0, clazz);
    }

    public <T extends Entity> Collection<T> getContained(int parentsUp, Class<T> clazz) {
        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();
        return getContained(r, clazz);
    }

    public <T extends Entity> Collection<T> getContained(ProtectedRegion region, Class<T> clazz) {
        return world.getEntitiesByClass(clazz).stream()
                .filter(e -> e.isValid() && LocationUtil.isInRegion(region, e))
                .collect(Collectors.toList());
    }

    public Collection<Entity> getContained() {
        return getContained(Entity.class);
    }

    public Collection<Entity> getContained(int parentsUp) {
        return getContained(parentsUp, Entity.class);
    }

    public Collection<Entity> getContained(Class<?>... classes) {
        return getContained(0, classes);
    }

    public Collection<Entity> getContained(int parentsUp, Class<?>... classes) {
        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();
        return getContained(r, classes);
    }

    public Collection<Entity> getContained(ProtectedRegion region, Class<?>... classes) {
        return world.getEntitiesByClasses(classes).stream()
                .filter(e -> e.isValid() && LocationUtil.isInRegion(region, e))
                .collect(Collectors.toList());
    }

    public boolean isEmpty(int parentsUp) {
        for (Player player : server.getOnlinePlayers()) {
            if (contains(player, parentsUp)) {
                empty = false;
                return false;
            }
        }
        empty = true;
        return true;
    }

    public boolean isEmpty() {
        return isEmpty(0);
    }

    public boolean cachedEmpty() {
        return empty;
    }

    public boolean contains(Entity entity) {
        return contains(entity.getLocation());
    }

    public boolean contains(Entity entity, int parentsUp) {
        return contains(entity.getLocation(), parentsUp);
    }

    public boolean contains(ProtectedRegion region, Entity entity) {
        return contains(region, entity.getLocation());
    }

    public boolean contains(Block block) {
        return contains(block.getLocation());
    }

    public boolean contains(Block block, int parentsUp) {
        return contains(block.getLocation(), parentsUp);
    }

    public boolean contains(ProtectedRegion region, Block block) {
        return contains(region, block.getLocation());
    }

    public boolean contains(Location location) {
        return contains(region, location);
    }

    public boolean contains(Location location, int parentsUp) {
        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();
        return contains(r, location);
    }

    public boolean contains(ProtectedRegion region, Location location) {
        return LocationUtil.isInRegion(world, region, location);
    }

    public ProtectedRegion getRegion() {
        return region;
    }

    public World getWorld() {
        return world;
    }

    public File getWorkingDir() {
        return new File(inst.getDataFolder() + "/area/" + region.getId() + "/");
    }
}
