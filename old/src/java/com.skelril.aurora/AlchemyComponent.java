/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Alchemy", desc = "Deconstruct Materials!")
public class AlchemyComponent extends BukkitComponent implements Listener {

    @Override
    public void enable() {

        //registerCommands(Commands.class);
    }

    public class Commands {

        @Command(aliases = {"alchemy", "alch"}, desc = "Revert your gold item into gold bars",
                flags = "", min = 0, max = 0)
        @CommandPermissions({"aurora.alch"})
        public void alch(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {

                Player player = (Player) sender;
                ItemStack item = player.getInventory().getItemInHand();
                if (item.getTypeId() == BlockID.AIR) {
                    player.setHealth(1);
                    throw new CommandException("You shouldn't try to break your hand down!");
                }

                if (item.getTypeId() == BlockID.TNT) {
                    double x, y, z;
                    x = player.getLocation().getX();
                    y = player.getLocation().getY();
                    z = player.getLocation().getZ();
                    player.getWorld().createExplosion(x, y, z, 2, false, false);
                    throw new CommandException("The TNT explodes!");
                }
                Set<ItemStack> itemStacks = getBaseItemStack(item);


                if (itemStacks == null || item.getItemMeta().hasDisplayName()) {
                    throw new CommandException("You cannot break this item down.");
                }
                if (item.getDurability() != 0 && item.getTypeId() != ItemID.GOLD_APPLE) {
                    throw new CommandException("You cannot break a damaged item down!");
                }

                player.getInventory().setItemInHand(new ItemStack(BlockID.AIR));
                for (ItemStack itemStack : itemStacks) {
                    if (player.getInventory().firstEmpty() == -1) {
                        player.getWorld().dropItem(player.getLocation(), itemStack);
                        continue;
                    }
                    player.getInventory().addItem(itemStack);
                }
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }
    }

    private Set<ItemStack> getBaseItemStack(ItemStack item) {

        Set<ItemStack> itemStack = new HashSet<>();
        switch (item.getTypeId()) {
            case ItemID.GOLD_AXE:
                itemStack.add(new ItemStack(ItemID.GOLD_BAR, 3));
                return itemStack;
            case ItemID.GOLD_BOOTS:
                itemStack.add(new ItemStack(ItemID.GOLD_BAR, 4));
                return itemStack;
            case ItemID.GOLD_PANTS:
                itemStack.add(new ItemStack(ItemID.GOLD_BAR, 7));
                return itemStack;
            case ItemID.GOLD_CHEST:
                itemStack.add(new ItemStack(ItemID.GOLD_BAR, 8));
                return itemStack;
            case ItemID.GOLD_HELMET:
                itemStack.add(new ItemStack(ItemID.GOLD_BAR, 5));
                return itemStack;
            case ItemID.GOLD_HOE:
                itemStack.add(new ItemStack(ItemID.GOLD_BAR, 2));
                return itemStack;
            case ItemID.GOLD_PICKAXE:
                itemStack.add(new ItemStack(ItemID.GOLD_BAR, 3));
                return itemStack;
            case ItemID.GOLD_SWORD:
                itemStack.add(new ItemStack(ItemID.GOLD_BAR, 2));
                return itemStack;
            case ItemID.GOLD_SHOVEL:
                itemStack.add(new ItemStack(ItemID.GOLD_BAR, 1));
                return itemStack;
            case ItemID.GOLD_APPLE:
                if (item.getData().getData() == 1) {
                    itemStack.add(new ItemStack(BlockID.GOLD_BLOCK, item.getAmount() * 8));
                } else {
                    itemStack.add(new ItemStack(ItemID.GOLD_BAR, item.getAmount() * 8));
                }
                itemStack.add(new ItemStack(ItemID.RED_APPLE, item.getAmount()));
                return itemStack;
            case ItemID.WOOD_AXE:
                itemStack.add(new ItemStack(BlockID.WOOD, 3));
                return itemStack;
            case ItemID.WOOD_HOE:
                itemStack.add(new ItemStack(BlockID.WOOD, 2));
                return itemStack;
            case ItemID.WOOD_PICKAXE:
                itemStack.add(new ItemStack(BlockID.WOOD, 3));
                return itemStack;
            case ItemID.WOOD_SHOVEL:
                itemStack.add(new ItemStack(BlockID.WOOD, 1));
                return itemStack;
            case ItemID.WOOD_SWORD:
                itemStack.add(new ItemStack(BlockID.WOOD, 2));
                return itemStack;
            case ItemID.LEATHER_BOOTS:
                itemStack.add(new ItemStack(ItemID.LEATHER, 4));
                return itemStack;
            case ItemID.LEATHER_CHEST:
                itemStack.add(new ItemStack(ItemID.LEATHER, 8));
                return itemStack;
            case ItemID.LEATHER_HELMET:
                itemStack.add(new ItemStack(ItemID.LEATHER, 5));
                return itemStack;
            case ItemID.LEATHER_PANTS:
                itemStack.add(new ItemStack(ItemID.LEATHER, 7));
                return itemStack;
            case ItemID.STONE_AXE:
                itemStack.add(new ItemStack(BlockID.COBBLESTONE, 3));
                return itemStack;
            case ItemID.STONE_HOE:
                itemStack.add(new ItemStack(BlockID.COBBLESTONE, 2));
                return itemStack;
            case ItemID.STONE_PICKAXE:
                itemStack.add(new ItemStack(BlockID.COBBLESTONE, 3));
                return itemStack;
            case ItemID.STONE_SHOVEL:
                itemStack.add(new ItemStack(BlockID.COBBLESTONE, 1));
                return itemStack;
            case ItemID.STONE_SWORD:
                itemStack.add(new ItemStack(BlockID.COBBLESTONE, 2));
                return itemStack;
            case ItemID.IRON_AXE:
                itemStack.add(new ItemStack(ItemID.IRON_BAR, 3));
                return itemStack;
            case ItemID.IRON_BOOTS:
                itemStack.add(new ItemStack(ItemID.IRON_BAR, 4));
                return itemStack;
            case ItemID.IRON_CHEST:
                itemStack.add(new ItemStack(ItemID.IRON_BAR, 8));
                return itemStack;
            case ItemID.IRON_HELMET:
                itemStack.add(new ItemStack(ItemID.IRON_BAR, 5));
                return itemStack;
            case ItemID.IRON_HOE:
                itemStack.add(new ItemStack(ItemID.IRON_BAR, 2));
                return itemStack;
            case ItemID.IRON_PANTS:
                itemStack.add(new ItemStack(ItemID.IRON_BAR, 7));
                return itemStack;
            case ItemID.IRON_PICK:
                itemStack.add(new ItemStack(ItemID.IRON_BAR, 3));
                return itemStack;
            case ItemID.IRON_SHOVEL:
                itemStack.add(new ItemStack(ItemID.IRON_BAR, 1));
                return itemStack;
            case ItemID.IRON_SWORD:
                itemStack.add(new ItemStack(ItemID.IRON_BAR, 2));
                return itemStack;
            case ItemID.DIAMOND_AXE:
                itemStack.add(new ItemStack(ItemID.DIAMOND, 3));
                return itemStack;
            case ItemID.DIAMOND_BOOTS:
                itemStack.add(new ItemStack(ItemID.DIAMOND, 4));
                return itemStack;
            case ItemID.DIAMOND_CHEST:
                itemStack.add(new ItemStack(ItemID.DIAMOND, 8));
                return itemStack;
            case ItemID.DIAMOND_HELMET:
                itemStack.add(new ItemStack(ItemID.DIAMOND, 5));
                return itemStack;
            case ItemID.DIAMOND_HOE:
                itemStack.add(new ItemStack(ItemID.DIAMOND, 2));
                return itemStack;
            case ItemID.DIAMOND_PANTS:
                itemStack.add(new ItemStack(ItemID.DIAMOND, 7));
                return itemStack;
            case ItemID.DIAMOND_PICKAXE:
                itemStack.add(new ItemStack(ItemID.DIAMOND, 3));
                return itemStack;
            case ItemID.DIAMOND_SHOVEL:
                itemStack.add(new ItemStack(ItemID.DIAMOND, 1));
                return itemStack;
            case ItemID.DIAMOND_SWORD:
                itemStack.add(new ItemStack(ItemID.DIAMOND, 2));
                return itemStack;
            default:
                return null;
        }
    }
}
