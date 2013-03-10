package com.skelril.aurora;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Custom Items Component", desc = "Custom Items")
@Depend(plugins = "SpoutPlugin")
public class CustomItemsComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        //registerCommands(Commands.class);
        //SpoutManager.getFileManager().addToPreLoginCache(inst,
        // "http://dl.dropbox.com/u/28513466/CustomItems/Food/RootBeer.png");
        //SpoutManager.getFileManager().addToPreLoginCache(inst,
        // "http://dl.dropbox.com/u/28513466/CustomItems/Food/BeerGlass.png");
    }

    public class Commands {

        @Command(aliases = {"rb"},
                usage = "", desc = "Get Root Beer",
                min = 0, max = 0)
        public void rbCmd(CommandContext args, CommandSender sender) {

            if (sender instanceof Player) {

                Player player = (Player) sender;

                //RootBeer rootBeer = new RootBeer(inst, "Root Beer",
                //        "http://dl.dropbox.com/u/28513466/CustomItems/Food/RootBeer.png", 20);
                //ItemStack item = new SpoutItemStack(rootBeer, 1);
                //player.getInventory().addItem(item);
            }
        }
    }
}
