/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.arena.factory;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.util.yaml.YAMLNode;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.city.engine.arena.ArenaType;
import gg.packetloss.grindstone.city.engine.arena.GenericArena;
import gg.packetloss.grindstone.city.engine.arena.PersistentArena;
import gg.packetloss.grindstone.events.entity.EntitySpawnBlockedEvent;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.DamageUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.RegionUtil;
import gg.packetloss.grindstone.util.item.itemstack.StackSerializer;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class FactoryFloor extends AbstractFactoryArea implements GenericArena, Listener, PersistentArena {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    protected static FactoryFloor factInst;

    private YAMLProcessor processor;
    private List<FactoryMech> mechs;
    private ArrayDeque<ItemStack> que = new ArrayDeque<>();
    private long nextMobSpawn = 0L;
    private boolean queueDirty = false;

    public FactoryFloor(World world, ProtectedRegion[] regions, List<FactoryMech> mechs, YAMLProcessor processor) {
        super(world, regions[0], regions[1], Arrays.copyOfRange(regions, 2, 4), Arrays.copyOfRange(regions, 4, 6));
        this.processor = processor;
        this.mechs = Lists.newArrayList(mechs);

        reloadData();

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        factInst = this;
    }

    public void madMilk() {
        nextMobSpawn = Math.max(nextMobSpawn, System.currentTimeMillis()) + TimeUnit.MINUTES.toMillis(40);
        writePrime();
    }

    public void throwPotions() {
        Random random = new Random();
        PotionType type = PotionType.INSTANT_HEAL;
        getContained(Zombie.class, Skeleton.class).forEach(e -> {
            for (int i = ChanceUtil.getRandom(3); i > 0; --i) {
                Location tg = e.getLocation().add(0, ChanceUtil.getRangedRandom(2, 6), 0);
                if (tg.getBlock().getType().isSolid()) continue;
                ThrownPotion potion = e.getWorld().spawn(
                        tg,
                        ThrownPotion.class
                );
                Potion brewedPotion = new Potion(type);
                brewedPotion.setLevel(type.getMaxLevel());
                brewedPotion.setSplash(true);
                potion.setItem(brewedPotion.toItemStack(1));
                potion.setVelocity(new org.bukkit.util.Vector(
                        random.nextDouble() * 2.0 - 1,
                        0,
                        random.nextDouble() * 2.0 - 1));
            }
        });
    }

    public ChamberType getProductType(ItemStack product) {
        switch (product.getType()) {
            case POTION:
                return ChamberType.POTION;
            case IRON_INGOT:
            case GOLD_INGOT:
                return ChamberType.SMELTING;
        }
        return ChamberType.POTION;
    }

    private Location getSpawnPoint(ProtectedRegion protectedRegion) {
        Region region = RegionUtil.convert(protectedRegion).orElseThrow();
        Location spawnLoc = RegionUtil.getCenter(getWorld(), protectedRegion);

        // Divide by 2 since we're center, then subtract 1/2 a block divided by 2 (1/4 block)
        // so that the items don't get stuck in the sides of the device.
        double xWidth = (RegionUtil.getXWidth(region).orElseThrow() / 2.0) - .25;
        double zWidth = (RegionUtil.getZWidth(region).orElseThrow() / 2.0) - .25;

        if (xWidth > zWidth) {
            spawnLoc.add(ChanceUtil.getRangedRandom(-xWidth, xWidth), 0, 0);
        } else {
            spawnLoc.add(0, 0, ChanceUtil.getRangedRandom(-zWidth, zWidth));
        }

        return spawnLoc;
    }

    public void produce(ItemStack product) {
        ProtectedRegion protectedRegion = getChamber(getProductType(product));
        Item item = getWorld().dropItem(getSpawnPoint(protectedRegion), product);
        item.setVelocity(new Vector());
    }

    @Override
    public void run() {
        if (queueDirty) {
            writePrime();
        }

        if (isEmpty()) return;

        equalize();

        if (System.currentTimeMillis() < nextMobSpawn) {
            throwPotions();
        }

        int queueSize = que.size();
        for (FactoryMech mech : Collections.synchronizedList(mechs)) que.addAll(mech.process());
        if (queueSize != que.size()) {
            queueDirty = true;
        }

        if (que.isEmpty()) return;
        boolean hexa = ModifierComponent.getModifierCenter().isActive(ModifierType.HEXA_FACTORY_SPEED);
        int max = getContained(Player.class).size() * (hexa ? 54 : 9);
        for (int i = Math.min(que.size(), ChanceUtil.getRangedRandom(max / 3, max)); i > 0; --i) {
            CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
                produce(que.poll());
                queueDirty = true;
            }, i * 10 + ChanceUtil.getRandom(10));
        }
    }

    @Override
    public void disable() {
        mechs.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {

        final Entity entity = event.getEntity();

        if (!(entity instanceof Player) || !contains(entity)) return;

        if (((Player) entity).isFlying() && event.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
            DamageUtil.multiplyFinalDamage(event, 2);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawnBlocked(EntitySpawnBlockedEvent event) {
        Entity entity = event.getEntity();

        if (!contains(entity)) {
            return;
        }

        if (!(entity instanceof Zombie || entity instanceof Skeleton || entity instanceof Spider)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemMege(ItemMergeEvent event) {
        for (ProtectedRegion region : smeltingTracks) {
            if (LocationUtil.isInRegion(region, event.getEntity()) || LocationUtil.isInRegion(region, event.getTarget())) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public String getId() {
        return getRegion().getId();
    }

    @Override
    public void equalize() { }

    @Override
    public ArenaType getArenaType() {
        return ArenaType.MONITORED;
    }

    public void writePrime() {
        processor.setProperty("mob-delay-timer", nextMobSpawn);
        processor.removeProperty("products");
        int counter = 0;
        for (ItemStack stack : que) {
            YAMLNode node = processor.addNode("products." + counter++);
            node.setProperty("stack-data", StackSerializer.getMap(stack));
        }
        processor.save();

        queueDirty = false;
    }

    public void writeAreas() {
        for (FactoryMech mech : mechs) {
            mech.save();
        }
    }

    @Override
    public void writeData(boolean doAsync) {
        Runnable r = () -> {
            writePrime();
            writeAreas();
        };

        if (!doAsync) {
            r.run();
            return;
        }
        server.getScheduler().runTaskAsynchronously(inst, r);
    }

    @Override
    public void reloadData() {
        try {
            processor.load();
            Object timerO = processor.getProperty("mob-delay-timer");
            if (timerO instanceof Long) {
                nextMobSpawn = (long) timerO;
            }
            Map<String, YAMLNode> nodes = processor.getNodes("products");
            if (nodes != null) {
                for (Map.Entry<String, YAMLNode> entry : nodes.entrySet()) {
                    try {
                        YAMLNode node = entry.getValue();
                        //noinspection unchecked
                        que.add(StackSerializer.fromMap((Map<String, Object>) node.getProperty("stack-data")));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            for (FactoryMech mech : mechs) {
                mech.load();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
