package com.skelril.aurora.city.engine.arena.factory;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.city.engine.arena.ArenaType;
import com.skelril.aurora.city.engine.arena.GenericArena;
import com.skelril.aurora.util.ChanceUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Logger;

public class FactoryFloor extends AbstractFactoryArea implements GenericArena {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;

    private List<FactoryMech> mechs;
    private Random random = new Random();
    private Queue<ItemStack> que = new LinkedList<>();

    public FactoryFloor(World world, ProtectedRegion[] regions, List<FactoryMech> mechs,
                        AdminComponent adminComponent) {
        super(world, regions[0], regions[1]);
        this.adminComponent = adminComponent;

        this.mechs = Lists.newArrayList(mechs);
    }

    public void produce(ItemStack... products) {

        Vector vec = new CuboidRegion(chamber.getMinimumPoint(), chamber.getMaximumPoint()).getCenter();
        Location loc = BukkitUtil.toLocation(getWorld(), vec);
        for (ItemStack product : products) getWorld().dropItem(loc, product);
    }

    @Override
    public void run() {

        for (FactoryMech mech : Collections.synchronizedList(mechs)) que.addAll(mech.process());

        if (que.isEmpty()) return;
        List<ItemStack> shortList = new ArrayList<>();
        for (int i = 0; i < ChanceUtil.getRandom(9); i++) {
            if (que.isEmpty()) break;
            shortList.add(que.poll());
        }
        produce(shortList.toArray(new ItemStack[shortList.size()]));
    }

    @Override
    public void disable() {

        mechs.clear();
    }

    @Override
    public String getId() {

        return getRegion().getId();
    }

    @Override
    public void equalize() {

        for (Player player : getContainedPlayers()) {

            adminComponent.deadmin(player);
        }
    }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.MONITORED;
    }
}
