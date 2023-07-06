/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.session.PersistentSession;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.Association;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.association.Associables;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.events.egg.EggDropEvent;
import gg.packetloss.grindstone.events.egg.EggHatchEvent;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static gg.packetloss.grindstone.util.bridge.WorldEditBridge.toBlockVec3;

@ComponentInformation(friendlyName = "Eggs", desc = "Mob Eggs")
@Depend(components = {SessionComponent.class})
public class EggComponent extends BukkitComponent implements Listener, Runnable {
    @InjectComponent
    private SessionComponent sessions;

    private WorldGuardPlugin worldGuard;
    private LocalConfiguration config;

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        CommandBook.registerEvents(this);

        setUpWorldGuard();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(CommandBook.inst(), this, 20 * 2, 120);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("enable-easter-eggs")
        public boolean enableEasterEggs = true;
        @Setting("enable-halloween-eggs")
        public boolean enableHalloweenEggs = true;
        @Setting("strict-mode")
        public boolean strictMode = true;
    }

    @Override
    public void run() {

        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (item.getTicksLived() < 300) {
                    continue;
                }

                ItemStack itemStack = item.getItemStack();
                SpawnEgg eggInfo = SpawnEgg.fromMaterial(itemStack.getType());
                if (eggInfo == null) continue;

                // Attempt to hatch the egg
                hatchEgg(item, eggInfo);
            }
        }
    }

    private void setUpWorldGuard() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (!(plugin instanceof WorldGuardPlugin)) {
            worldGuard = null;
        }

        //noinspection ConstantConditions
        worldGuard = (WorldGuardPlugin) plugin;
    }

    public void eggOn(Player player) {

        for (EggType eggType : EggType.values()) {
            if (eggType == EggType.INVALID) continue;
            sessions.getSession(EggState.class, player).setEggDrop(eggType, true);
        }
    }

    public void eggOn(Player player, EggType eggType) {

        if (eggType.equals(EggType.INVALID)) eggOn(player);
        else sessions.getSession(EggState.class, player).setEggDrop(eggType, true);
    }

    public boolean allowedEggs(Player player, EggType eggType) {

        return sessions.getSession(EggState.class, player).getEggDropAllowed(eggType);
    }

    public void eggOff(Player player) {

        for (EggType eggType : EggType.values()) {
            if (eggType == EggType.INVALID) continue;
            sessions.getSession(EggState.class, player).setEggDrop(eggType, false);
        }
    }

    public void eggOff(Player player, EggType eggType) {

        if (eggType.equals(EggType.INVALID)) eggOff(player);
        else sessions.getSession(EggState.class, player).setEggDrop(eggType, false);
    }

    private void attemptEggDrop(SpawnEgg egg, Location location) {
        EggDropEvent eggDropEvent = new EggDropEvent(egg, location);
        CommandBook.callEvent(eggDropEvent);
        if (!eggDropEvent.isCancelled()) {
            location.getWorld().dropItemNaturally(eggDropEvent.getLocation(), eggDropEvent.getEggType().toItemStack());
        }
    }

    private static final Map<EggType, List<SpawnEgg>> EGGS = Map.of(
        EggType.EASTER, List.of(
            SpawnEgg.BAT,
            SpawnEgg.CHICKEN,
            SpawnEgg.COW,
            SpawnEgg.DOLPHIN,
            SpawnEgg.FOX,
            SpawnEgg.FROG,
            SpawnEgg.MUSHROOM_COW,
            SpawnEgg.OCELOT,
            SpawnEgg.PIG,
            SpawnEgg.SHEEP,
            SpawnEgg.TURTLE,
            SpawnEgg.WOLF
        ),
        EggType.HALLOWEEN, List.of(
            SpawnEgg.ENDERMAN,
            SpawnEgg.SPIDER,
            SpawnEgg.CAVE_SPIDER,
            SpawnEgg.SLIME,
            SpawnEgg.MAGMA_CUBE,
            SpawnEgg.WITCH,
            SpawnEgg.SKELETON,
            SpawnEgg.ZOMBIE
        )
    );

    private void dropEgg(EggType eggType, Location location) {
        attemptEggDrop(CollectionUtil.getElement(EGGS.get(eggType)), location);
    }

    private void dropEasterEggs(Location location) {
        for (short c = 0; c < ChanceUtil.getRangedRandom(7, 17); ++c) {
            dropEgg(EggType.EASTER, location);
        }
    }

    private void dropHalloweenEggs(Location location) {
        for (short c = 0; c < ChanceUtil.getRangedRandom(7, 13); ++c) {
            dropEgg(EggType.HALLOWEEN, location);
        }
    }

    public void dropEggs(EggType eggType, Location location) {
        switch (eggType) {
            case EASTER ->    dropEasterEggs(location);
            case HALLOWEEN -> dropHalloweenEggs(location);
        }
    }

    public boolean isEasterActive() {
        return LocalDate.now().getMonth().equals(Month.APRIL) && config.enableEasterEggs;
    }

    public boolean isEasterActive(Player player) {
        return isEasterActive() && allowedEggs(player, EggType.EASTER);
    }

    public boolean isHalloweenActive() {
        return LocalDate.now().getMonth().equals(Month.OCTOBER) && config.enableHalloweenEggs;
    }

    public boolean isHalloweenActive(Player player) {
        return isHalloweenActive() && allowedEggs(player, EggType.HALLOWEEN);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();

        Block block = event.getBlock();
        Material blockType = block.getType();

        if (EnvironmentUtil.isShrubBlock(blockType)) {
            if (isEasterActive(player) && !ChanceUtil.getChance(5, 6)) {
                dropEggs(EggType.EASTER, block.getLocation());
            }

            if (isHalloweenActive(player) && !ChanceUtil.getChance(7, 8)) {
                dropEggs(EggType.HALLOWEEN, block.getLocation());
            }
        }
    }

    private boolean hatchEgg(Item egg, SpawnEgg eggInfo) {
        EggHatchEvent event = new EggHatchEvent(egg, eggInfo.getEntityType(), egg.getLocation());
        CommandBook.callEvent(event);

        if (!event.isCancelled()) {

            if (config.strictMode && worldGuard != null) {

                RegionManager mgr = WorldGuardBridge.getManagerFor(egg.getWorld());
                ApplicableRegionSet set = mgr.getApplicableRegions(toBlockVec3(event.getLocation()));
                RegionAssociable associate = Associables.constant(Association.NON_MEMBER);

                if (set.queryState(associate, Flags.MOB_SPAWNING) == StateFlag.State.ALLOW) {
                    event.setCancelled(true);
                    return false;
                }

                Set<EntityType> entityTypes = set.queryValue(associate, Flags.DENY_SPAWN);
                if (entityTypes != null && entityTypes.contains(BukkitAdapter.adapt(event.getEggType()))) {
                    event.setCancelled(true);
                    return false;
                }
            }

            int hatchCount = 0;
            int c;
            for (c = 0; c < egg.getItemStack().getAmount(); c++) {
                if (ChanceUtil.getChance(200)) {
                    hatchCount++;
                    event.getLocation().getWorld().spawn(event.getLocation(), event.getEggType().getEntityClass());
                }
            }

            if (c - hatchCount > 0) {
                egg.getItemStack().setAmount(c - hatchCount);
            } else {
                egg.remove();
            }
            return true;
        }
        return false;
    }

    public enum EggType {
        EASTER,
        HALLOWEEN,
        INVALID
    }

    private static class EggState extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        @Setting("easter-egg")
        private boolean easterEgg = true;
        @Setting("halloween-egg")
        private boolean halloweenEgg = true;

        protected EggState() {

            super(MAX_AGE);
        }

        public boolean getEggDropAllowed(EggType eggType) {

            switch (eggType) {
                case EASTER:
                    return easterEgg;
                case HALLOWEEN:
                    return halloweenEgg;
                default:
                    return false;
            }
        }

        public void setEggDrop(EggType eggType, boolean eggDrop) {

            StringBuilder sb = new StringBuilder();

            switch (eggType) {
                case EASTER:
                    this.easterEgg = eggDrop;
                    sb.append("Easter ");
                    break;
                case HALLOWEEN:
                    this.halloweenEgg = eggDrop;
                    sb.append("Halloween ");
                    break;
            }
            sb.append("eggs are now ");
            if (eggDrop) sb.append("enabled.");
            else sb.append("disabled.");
            if (getPlayer().isOnline()) ChatUtil.sendNotice(getPlayer(), sb.toString());
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}
