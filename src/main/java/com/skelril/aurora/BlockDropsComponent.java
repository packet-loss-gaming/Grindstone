/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.CollectionUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Block Drops", desc = "More Block Drops")
public class BlockDropsComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

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

        @Setting("common-chance")
        public int commonChance = 10;
        @Setting("rare-chance")
        public int rareChance = 1000;

        @Setting("gravel-drops")
        public List<Integer> gravelDrops = new ArrayList<>(Arrays.asList(
                ItemID.SULPHUR, ItemID.BUCKET, ItemID.ARROW,
                ItemID.BOWL, ItemID.BONE, ItemID.BRICK_BAR,
                ItemID.STICK
        ));

        @Setting("rare-gravel-drops")
        public List<Integer> rareGravelDrops = new ArrayList<>(Arrays.asList(
                ItemID.DIAMOND
        ));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        Block block = event.getBlock();

        if (ChanceUtil.getChance(config.commonChance)) {
            switch (block.getTypeId()) {
                case BlockID.GRAVEL:
                    int item = CollectionUtil.getElement(config.gravelDrops);
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(item, ChanceUtil.getRandom(6)));
                    break;
            }
        }

        if (ChanceUtil.getChance(config.rareChance)) {
            switch (block.getTypeId()) {
                case BlockID.GRAVEL:
                    int item = CollectionUtil.getElement(config.rareGravelDrops);
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(item, ChanceUtil.getRandom(6)));
                    break;
            }
        }
    }
}
