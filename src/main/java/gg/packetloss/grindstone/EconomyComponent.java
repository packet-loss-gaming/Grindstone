package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang3.Validate;
import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Economy", desc = "Economy integration")
public class EconomyComponent extends BukkitComponent {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private Economy economy = null;

    @Override
    public void enable() {
        setupEconomy();
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public Economy getHandle() {
        Validate.notNull(economy);
        return economy;
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
