/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.admin;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.item.custom.CustomItemCenter;
import com.skelril.aurora.util.item.custom.CustomItems;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Lost Custom Items", desc = "Lost item commands.")
public class LostItemsComponent extends BukkitComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        registerCommands(Commands.class);
    }

    public class Commands {
        @Command(aliases = {"/give"}, desc = "Custom Item give command")
        @NestedCommand({NestedLostItemCommands.class})
        public void lostItemCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class NestedLostItemCommands {

        @Command(aliases = {"god"}, desc = "Lost God items")
        @NestedCommand({LostGodItem.class})
        public void lostGodCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"ancient"}, desc = "Lost Ancient items")
        @NestedCommand({LostAncientItem.class})
        public void lostAncientCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"necros"}, desc = "Lost Necros items")
        @NestedCommand({LostNecrosItem.class})
        public void lostNecrosCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"master"}, desc = "Lost Master items")
        @NestedCommand({LostMasterItem.class})
        public void lostMasterCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"fear"}, desc = "Lost Fear items")
        @NestedCommand({LostFearItem.class})
        public void lostFearCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"unleashed", "unl"}, desc = "Lost Unleashed items")
        @NestedCommand({LostUnleashedItem.class})
        public void lostUnleashedCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"donation"}, desc = "Lost Donation items")
        @NestedCommand({LostDonationItem.class})
        public void lostDonationCommands(CommandContext args, CommandSender sender) throws CommandException {

        }


        @Command(aliases = {"admin"}, desc = "Lost Admin items")
        @NestedCommand({LostAdminItem.class})
        public void lostAdminCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"shadow"}, desc = "Lost Shadow items")
        @NestedCommand({LostShadowItem.class})
        public void lostShadowCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"red"}, desc = "Lost Red items")
        @NestedCommand({LostRedItem.class})
        public void lostRedCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"misc"}, desc = "Lost Grave Yard items")
        @NestedCommand({LostMiscItem.class})
        public void lostMiscCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class LostGodItem {

        @Command(aliases = {"armor"},
                usage = "<player>", desc = "Return a player's God Armor",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.god.armor"})
        public void lostArmorCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(
                    CustomItemCenter.build(CustomItems.GOD_HELMET),
                    CustomItemCenter.build(CustomItems.GOD_CHESTPLATE),
                    CustomItemCenter.build(CustomItems.GOD_LEGGINGS),
                    CustomItemCenter.build(CustomItems.GOD_BOOTS)
            );

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given new god armour.");
        }

        @Command(aliases = {"bow"},
                usage = "<player>", desc = "Return a player's God Bow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.god.bow"})
        public void lostBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.GOD_BOW));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new god bow.");
        }

        @Command(aliases = {"sword"},
                usage = "<player>", desc = "Return a player's God Sword",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.god.sword"})
        public void lostSwordCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.GOD_SWORD));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new god sword.");
        }

        @Command(aliases = {"pickaxe", "pick"},
                usage = "<player>", desc = "Return a player's God Pickaxe",
                flags = "l", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.god.pickaxe"})
        public void lostPickaxeCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(args.hasFlag('l')
                            ? CustomItems.LEGENDARY_GOD_PICKAXE : CustomItems.GOD_PICKAXE
            ));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new god pickaxe.");
        }

        @Command(aliases = {"axe"},
                usage = "<player>", desc = "Return a player's God Axe",
                flags = "l", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.god.axe"})
        public void lostAxeCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(args.hasFlag('l')
                            ? CustomItems.LEGENDARY_GOD_AXE : CustomItems.GOD_AXE
            ));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new god axe.");
        }
    }

    public class LostAncientItem {

        @Command(aliases = {"crown"},
                usage = "<player>", desc = "Return a player's Ancient Crown",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.ancient.crown"})
        public void lostSwordCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.ANCIENT_CROWN));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new ancient crown.");
        }

        @Command(aliases = {"armor"},
                usage = "<player>", desc = "Return a player's Ancient Armour",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.ancient.armor"})
        public void lostArmorCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(
                    CustomItemCenter.build(CustomItems.ANCIENT_HELMET),
                    CustomItemCenter.build(CustomItems.ANCIENT_CHESTPLATE),
                    CustomItemCenter.build(CustomItems.ANCIENT_LEGGINGS),
                    CustomItemCenter.build(CustomItems.ANCIENT_BOOTS)
            );

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given new ancient armour.");
        }
    }

    public class LostNecrosItem {

        @Command(aliases = {"armor"},
                usage = "<player>", desc = "Return a player's Necros Armor",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.necros.armor"})
        public void lostArmorCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(
                    CustomItemCenter.build(CustomItems.NECROS_HELMET),
                    CustomItemCenter.build(CustomItems.NECROS_CHESTPLATE),
                    CustomItemCenter.build(CustomItems.NECROS_LEGGINGS),
                    CustomItemCenter.build(CustomItems.NECROS_BOOTS)
            );

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given new necros armour.");
        }
    }

    public class LostMasterItem {

        @Command(aliases = {"sword"},
                usage = "<player>", desc = "Return a player's Master Sword",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.master.sword"})
        public void lostSwordCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.MASTER_SWORD));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new master sword.");
        }

        @Command(aliases = {"bow"},
                usage = "<player>", desc = "Return a player's Master Bow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.master.bow"})
        public void lostBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.MASTER_BOW));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new master sword.");
        }
    }

    public class LostFearItem {

        @Command(aliases = {"sword"},
                usage = "<player>", desc = "Return a player's Fear Sword",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.fear.sword"})
        public void lostSwordCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.FEAR_SWORD));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new fear sword.");
        }

        @Command(aliases = {"bow"},
                usage = "<player>", desc = "Return a player's Fear Bow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.fear.bow"})
        public void lostBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.FEAR_BOW));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new fear bow.");
        }
    }

    public class LostUnleashedItem {

        @Command(aliases = {"sword"},
                usage = "<player>", desc = "Return a player's Unleashed Sword",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.unleashed.sword"})
        public void lostSwordCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.UNLEASHED_SWORD));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new unleashed sword.");
        }

        @Command(aliases = {"bow"},
                usage = "<player>", desc = "Return a player's Unleashed Bow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.unleashed.bow"})
        public void lostBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.UNLEASHED_BOW));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new unleashed bow.");
        }
    }

    public class LostShadowItem {

        @Command(aliases = {"sword"},
                usage = "<player>", desc = "Return a player's Shadow Sword",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.shadow.sword"})
        public void lostSwordCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.SHADOW_SWORD));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new shadow sword.");
        }

        @Command(aliases = {"bow"},
                usage = "<player>", desc = "Return a player's Shadow Bow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.shadow.bow"})
        public void lostBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.SHADOW_BOW));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new shadow bow.");
        }
    }

    public class LostRedItem {

        @Command(aliases = {"feather"},
                usage = "<player>", desc = "Return a player's Red Feather",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.red.feather"})
        public void lostSwordCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.RED_FEATHER));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new red feather.");
        }
    }

    public class LostMiscItem {

        @Command(aliases = {"potionofrestitution"},
                usage = "<player>", desc = "Give a player a Potion of Restitution",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.misc.potionofrestitution"})
        public void lostPotionOfRestitutionCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));
            player.getInventory().addItem(CustomItemCenter.build(CustomItems.POTION_OF_RESTITUTION));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given a Potion of Restitution.");
        }

        @Command(aliases = {"gemofdarkness"},
                usage = "<player> [amount]", desc = "Give a player some Gems of Darkness",
                flags = "", min = 1, max = 2)
        @CommandPermissions({"aurora.lost.misc.gemofdarkness"})
        public void lostGemOfDarknessCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            int amount = 1;
            if (args.argsLength() > 1) {
                amount = args.getInteger(1);
            }

            ItemStack stack = CustomItemCenter.build(CustomItems.GEM_OF_DARKNESS, Math.max(0, Math.min(64, amount)));
            player.getInventory().addItem(stack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given " + stack.getAmount() + " Gems of Darkness.");
        }

        @Command(aliases = {"pixiedust"},
                usage = "<player> [amount]", desc = "Give a player some Pixie Dust",
                flags = "", min = 1, max = 2)
        @CommandPermissions({"aurora.lost.misc.phantomgold"})
        public void lostPixieDustCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            int amount = 1;
            if (args.argsLength() > 1) {
                amount = args.getInteger(1);
            }

            ItemStack stack = CustomItemCenter.build(CustomItems.PIXIE_DUST, Math.max(0, Math.min(64, amount)));
            player.getInventory().addItem(stack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given " + stack.getAmount() + " Pixie Dust.");
        }

        @Command(aliases = {"phantomgold"},
                usage = "<player> [amount]", desc = "Give a player some Phantom Gold",
                flags = "", min = 1, max = 2)
        @CommandPermissions({"aurora.lost.misc.phantomgold"})
        public void lostPhantomGoldCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            int amount = 1;
            if (args.argsLength() > 1) {
                amount = args.getInteger(1);
            }

            ItemStack stack = CustomItemCenter.build(CustomItems.PHANTOM_GOLD, Math.max(0, Math.min(64, amount)));
            player.getInventory().addItem(stack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given " + stack.getAmount() + " Phantom Gold.");
        }

        @Command(aliases = {"phantomclock"},
                usage = "<player> [amount]", desc = "Give a player some Phantom Clocks",
                flags = "", min = 1, max = 2)
        @CommandPermissions({"aurora.lost.misc.phantomclock"})
        public void lostPhantomClockCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            int amount = 1;
            if (args.argsLength() > 1) {
                amount = args.getInteger(1);
            }

            ItemStack stack = CustomItemCenter.build(CustomItems.PHANTOM_CLOCK, Math.max(0, Math.min(64, amount)));
            player.getInventory().addItem(stack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given " + stack.getAmount() + " Phantom Clocks.");
        }

        @Command(aliases = {"phantomhymn"},
                usage = "<player>", desc = "Give a player a Phantom Hymn",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.misc.phantomhymn"})
        public void lostPhantomHymnCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.PHANTOM_HYMN));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given a Phantom Hymn.");
        }

        @Command(aliases = {"gemoflife"},
                usage = "<player> [amount]", desc = "Give a player some Gems of Life",
                flags = "", min = 1, max = 2)
        @CommandPermissions({"aurora.lost.misc.gemoflife"})
        public void lostGemOfLifeCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            int amount = 1;
            if (args.argsLength() > 1) {
                amount = args.getInteger(1);
            }

            ItemStack stack = CustomItemCenter.build(CustomItems.GEM_OF_LIFE, Math.max(0, Math.min(64, amount)));
            player.getInventory().addItem(stack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given " + stack.getAmount() + " Gems of Life.");
        }

        @Command(aliases = {"imbuedcrystal"},
                usage = "<player> [amount]", desc = "Give a player some Imbued Crystals",
                flags = "", min = 1, max = 2)
        @CommandPermissions({"aurora.lost.misc.imbuedcrystal"})
        public void lostImbuedCrystalCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            int amount = 1;
            if (args.argsLength() > 1) {
                amount = args.getInteger(1);
            }

            ItemStack stack = CustomItemCenter.build(CustomItems.IMBUED_CRYSTAL, Math.max(0, Math.min(64, amount)));
            player.getInventory().addItem(stack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given " + stack.getAmount() + " Imbued Crystals.");
        }

        @Command(aliases = {"batbow"},
                usage = "<player>", desc = "Return a player's Bat Bow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.misc.bow.bat"})
        public void lostBatBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.BAT_BOW));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been a new Bat Bow.");
        }

        @Command(aliases = {"chickenbow"},
                usage = "<player>", desc = "Return a player's Chicken Bow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.misc.bow.chicken"})
        public void lostChickenBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.CHICKEN_BOW));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been a new Chicken Bow.");
        }

        @Command(aliases = {"chickenhymn"},
                usage = "<player>", desc = "Give a player a Chicken Hymn",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.misc.chickenhymn"})
        public void lostChickenHymnCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.CHICKEN_HYMN));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given a Chicken Hymn.");
        }

        @Command(aliases = {"magicbucket"},
                usage = "<player>", desc = "Return a player's Magic Bucket",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.misc.magicbucket"})
        public void lostMagicBucketCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(CustomItemCenter.build(CustomItems.MAGIC_BUCKET));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been a new Magic Bucket.");
        }
    }

    public class LostDonationItem {

        @Command(aliases = {"butterboots"},
                usage = "<player>", desc = "Return a player's butter boots",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.donation.butterboots"})
        public void lostButterBootsCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            ItemStack bootStack = new ItemStack(ItemID.GOLD_BOOTS);
            ItemMeta butterMeta = bootStack.getItemMeta();
            butterMeta.setDisplayName(ChatColor.GOLD + "Butter Boots");
            bootStack.setItemMeta(butterMeta);
            player.getInventory().addItem(bootStack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given new butter boots.");
        }
    }

    public class LostAdminItem {

        @Command(aliases = {"pwngbow"},
                usage = "<player>", desc = "Return a player's pwngbow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.admin.pwngbow"})
        public void lostPwngBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            ItemStack pwngBowStack = new ItemStack(Material.BOW);
            ItemMeta pwngBow = pwngBowStack.getItemMeta();
            pwngBow.addEnchant(Enchantment.ARROW_DAMAGE, 10000, true);
            pwngBow.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
            pwngBow.setDisplayName(ChatColor.DARK_PURPLE + "Pwng Bow");
            pwngBowStack.setItemMeta(pwngBow);
            player.getInventory().addItem(pwngBowStack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new pwng bow.");
        }
    }
}
