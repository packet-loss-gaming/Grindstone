/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Updater", desc = "Update old chest")
public class UpdateComponent extends BukkitComponent implements Listener {

    private CommandBook inst = CommandBook.inst();
    private Logger log = inst.getLogger();
    private Server server = CommandBook.server();

    private List<Player> playerList = new ArrayList<>();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);
    }

    public class Commands {

        @Command(aliases = {"update"}, desc = "Update a chest",
                min = 0, max = 0)
        public void update(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");

            ChatUtil.sendNotice(sender, "Click the chest you would like to update.");
            playerList.add((Player) sender);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (playerList.contains(player) && event.getClickedBlock().getState() instanceof Chest) {

            Chest c = (Chest) block.getState();
            ItemStack[] contents = c.getInventory().getContents();

            for (ItemStack aStack : contents) {

                if (aStack == null) continue;
                if (aStack.getItemMeta().hasDisplayName()) continue;
                ItemMeta itemMeta;

                switch (aStack.getTypeId()) {
                    case ItemID.DIAMOND_SWORD:
                        if (aStack.getEnchantmentLevel(Enchantment.DAMAGE_ALL) < 5) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.DAMAGE_ARTHROPODS) < 5) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.DAMAGE_UNDEAD) < 5) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.FIRE_ASPECT) < 2) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.KNOCKBACK) < 2) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS) < 3) continue;
                        itemMeta = aStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.RED + "God Sword");
                        break;
                    case ItemID.BOW:
                        if (aStack.getEnchantmentLevel(Enchantment.ARROW_DAMAGE) < 5) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.ARROW_FIRE) < 1) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.ARROW_INFINITE) < 1) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK) < 2) continue;
                        itemMeta = aStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.RED + "God Bow");
                        break;
                    case ItemID.DIAMOND_PICKAXE:
                        if (aStack.getEnchantmentLevel(Enchantment.DIG_SPEED) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.DIG_SPEED) > 4) {
                            if (aStack.getEnchantmentLevel(Enchantment.DURABILITY) < 3) continue;
                            if (aStack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS) < 3) continue;
                            itemMeta = aStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.GREEN + "Legendary God Pickaxe");
                        } else {
                            if (aStack.getEnchantmentLevel(Enchantment.SILK_TOUCH) < 1) continue;
                            itemMeta = aStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.GREEN + "God Pickaxe");
                        }
                        break;
                    case ItemID.CHAINMAIL_HELMET:
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_FIRE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_EXPLOSIONS) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.OXYGEN) < 3) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.WATER_WORKER) < 1) continue;
                        itemMeta = aStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.GOLD + "Ancient Helmet");
                        break;
                    case ItemID.CHAINMAIL_CHEST:
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_FIRE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_EXPLOSIONS) < 4) continue;
                        itemMeta = aStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.GOLD + "Ancient Chestplate");
                        break;
                    case ItemID.CHAINMAIL_PANTS:
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_FIRE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_EXPLOSIONS) < 4) continue;
                        itemMeta = aStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.GOLD + "Ancient Leggings");
                        break;
                    case ItemID.CHAINMAIL_BOOTS:
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_FIRE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_EXPLOSIONS) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_FALL) < 4) continue;
                        itemMeta = aStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.GOLD + "Ancient Boots");
                        break;
                    case ItemID.DIAMOND_HELMET:
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_FIRE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_EXPLOSIONS) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.OXYGEN) < 3) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.WATER_WORKER) < 1) continue;
                        itemMeta = aStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.BLUE + "God Helmet");
                        break;
                    case ItemID.DIAMOND_CHEST:
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_FIRE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_EXPLOSIONS) < 4) continue;
                        itemMeta = aStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.BLUE + "God Chestplate");
                        break;
                    case ItemID.DIAMOND_PANTS:
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_FIRE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_EXPLOSIONS) < 4) continue;
                        itemMeta = aStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.BLUE + "God Leggings");
                        break;
                    case ItemID.DIAMOND_BOOTS:
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_FIRE) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_EXPLOSIONS) < 4) continue;
                        if (aStack.getEnchantmentLevel(Enchantment.PROTECTION_FALL) < 4) continue;
                        itemMeta = aStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.BLUE + "God Boots");
                        break;
                    default:
                        continue;
                }

                if (itemMeta instanceof Repairable) ((Repairable) itemMeta).setRepairCost(400);
                aStack.setItemMeta(itemMeta);
            }
            c.getInventory().setContents(contents);
            c.update(true);
            playerList.remove(player);
            event.setUseInteractedBlock(Event.Result.DENY);
            ChatUtil.sendNotice(player, "Updated!");
        }
    }
}
