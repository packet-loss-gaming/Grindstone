package com.skelril.aurora.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Map", desc = "Maps.")
@Depend(plugins = {"Vault"})
public class MapComponent extends BukkitComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private static Economy economy = null;
    private LocalConfiguration config;

    @Override
    public void enable() {

        setupEconomy();
        registerCommands(Commands.class);
        config = configure(new LocalConfiguration());
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("map-cost")
        public int mapCost = 100;
    }

    public class Commands {

        @Command(aliases = {"map"},
                usage = "", desc = "Obtain a map.",
                min = 0, max = 0)
        public void mapCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) {
                throw new CommandException("You must be a player to use this command.");
            }

            if (economy.getBalance(sender.getName()) - config.mapCost <= 0) {
                throw new CommandException("You do not have enough money.");
            }

            economy.withdrawPlayer(sender.getName(), config.mapCost);
            ((Player) sender).getInventory().addItem(new ItemStack(ItemID.MAP));
            ChatUtil.sendNotice(sender, "Here is your map!");
        }
    }

    private boolean setupEconomy() {

        RegisteredServiceProvider<Economy> economyProvider = server.getServicesManager().getRegistration(net.milkbowl
                .vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
}
