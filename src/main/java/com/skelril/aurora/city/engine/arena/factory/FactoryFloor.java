/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena.factory;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.util.yaml.YAMLNode;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.city.engine.arena.ArenaType;
import com.skelril.aurora.city.engine.arena.GenericArena;
import com.skelril.aurora.city.engine.arena.PersistentArena;
import com.skelril.aurora.modifiers.ModifierType;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.DamageUtil;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Logger;

import static com.skelril.aurora.modifiers.ModifierComponent.getModifierCenter;

public class FactoryFloor extends AbstractFactoryArea implements GenericArena, Listener, PersistentArena {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;

    private YAMLProcessor processor;
    private List<FactoryMech> mechs;
    private LinkedList<ItemStack> que = new LinkedList<>();

    public FactoryFloor(World world, ProtectedRegion[] regions, List<FactoryMech> mechs, YAMLProcessor processor,
                        AdminComponent adminComponent) {
        super(world, regions[0], regions[1], Arrays.copyOfRange(regions, 2, 4));
        this.processor = processor;
        this.adminComponent = adminComponent;
        this.mechs = Lists.newArrayList(mechs);

        reloadData();

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
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

    public void produce(ItemStack product) {
        ProtectedRegion region = getChamber(getProductType(product));
        Vector vec = new CuboidRegion(region.getMinimumPoint(), region.getMaximumPoint()).getCenter();
        getWorld().dropItem(BukkitUtil.toLocation(getWorld(), vec), product);
    }

    @Override
    public void run() {

        equalize();

        int queueSize = que.size();
        for (FactoryMech mech : Collections.synchronizedList(mechs)) que.addAll(mech.process());

        if (queueSize != que.size()) {
            writePrime();
            queueSize = que.size();
        }

        if (que.isEmpty()) return;
        boolean hexa = getModifierCenter().isActive(ModifierType.HEXA_FACTORY_SPEED);
        int max = getContained(Player.class).size() * (hexa ? 54 : 9);
        for (int i = ChanceUtil.getRangedRandom(max / 3, max); i > 0; --i) {
            if (que.isEmpty()) break;
            produce(que.poll());
        }
        if (queueSize != que.size()) writePrime();
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

    @Override
    public String getId() {
        return getRegion().getId();
    }

    @Override
    public void equalize() {
        getContained(Player.class).stream().filter(adminComponent::isAdmin).forEach(adminComponent::deadmin);
    }

    @Override
    public ArenaType getArenaType() {
        return ArenaType.MONITORED;
    }

    public void writePrime() {
        processor.removeProperty("products");
        for (int i = 0; i < que.size(); ++i) {
            YAMLNode node = processor.addNode("products." + i);
            ItemStack stack = que.get(i);
            node.setProperty("type", stack.getTypeId());
            node.setProperty("data", stack.getDurability());
            node.setProperty("amt", stack.getAmount());
        }
        processor.save();
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
            Map<String, YAMLNode> nodes = processor.getNodes("products");
            if (nodes != null) {
                for (Map.Entry<String, YAMLNode> entry : nodes.entrySet()) {
                    YAMLNode node = entry.getValue();
                    int type = node.getInt("type");
                    short data = node.getInt("data").shortValue();
                    int amt = node.getInt("amt");
                    que.add(Integer.parseInt(entry.getKey()), new ItemStack(type, amt, data));
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
