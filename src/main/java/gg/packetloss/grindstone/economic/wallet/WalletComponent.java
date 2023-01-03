/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.wallet;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.data.DataBaseComponent;
import gg.packetloss.grindstone.economic.wallet.database.DatabaseWalletProvider;
import gg.packetloss.grindstone.economic.wallet.database.mysql.MySQLWalletDatabase;
import gg.packetloss.grindstone.events.PlayerWalletUpdate;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.highscore.scoretype.ScoreTypes;
import gg.packetloss.grindstone.util.task.promise.FailableTaskFuture;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;

import java.math.BigDecimal;

import static gg.packetloss.grindstone.util.ChatUtil.TWO_DECIMAL_FORMATTER;

@ComponentInformation(friendlyName = "Wallet", desc = "Asynchronous currency system")
@Depend(plugins = {"Vault"}, components = {DataBaseComponent.class, HighScoresComponent.class})
public class WalletComponent extends BukkitComponent implements WalletProvider, Listener {
    @InjectComponent
    private HighScoresComponent highScores;

    private WalletProvider provider;

    @Override
    public void enable() {
        provider = new DatabaseWalletProvider(new MySQLWalletDatabase());

        // Register a vault compatibility layer
        CommandBook.server().getServicesManager().register(
            Economy.class, new VaultCompatibilityLayer(this), CommandBook.inst(), ServicePriority.Normal
        );

        // Register commands
        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            registrar.registerAsSubCommand("wallet", "Wallet commands", (walletRegistrar) -> {
                walletRegistrar.register(WalletCommandsRegistration.builder(), new WalletCommands(this));

                walletRegistrar.registerAsSubCommand("admin", "Wallet Admin Commmands", (walletAdminRegistrar) -> {
                    walletAdminRegistrar.register(WalletAdminCommandsRegistration.builder(), new WalletAdminCommands(this));
                });
            });
        });

        CommandBook.registerEvents(this);
    }

    @Deprecated
    public FailableTaskFuture<Boolean, Void> hasAtLeast(OfflinePlayer player, double amount) {
        return hasAtLeast(player, new BigDecimal(amount));
    }

    @Deprecated
    public FailableTaskFuture<BigDecimal, Void> addToBalance(OfflinePlayer player, double amount) {
        return provider.addToBalance(player, new BigDecimal(amount));
    }

    @Deprecated
    public FailableTaskFuture<Boolean, Void> removeFromBalance(OfflinePlayer player, double amount) {
        return provider.removeFromBalance(player, new BigDecimal(amount));
    }

    @Deprecated
    public FailableTaskFuture<Void, Void> setBalance(OfflinePlayer player, double amount) {
        return setBalance(player, new BigDecimal(amount));
    }

    @Override
    public FailableTaskFuture<Boolean, Void> hasAtLeast(OfflinePlayer player, BigDecimal amount) {
        return provider.hasAtLeast(player, amount);
    }

    @Override
    public FailableTaskFuture<BigDecimal, Void> getBalance(OfflinePlayer player) {
        return provider.getBalance(player);
    }

    @Override
    public FailableTaskFuture<BigDecimal, Void> addToBalance(OfflinePlayer player, BigDecimal amount) {
        return provider.addToBalance(player, amount);
    }

    @Override
    public FailableTaskFuture<Boolean, Void> removeFromBalance(OfflinePlayer player, BigDecimal amount) {
        return provider.removeFromBalance(player, amount);
    }

    @Override
    public FailableTaskFuture<Void, Void> setBalance(OfflinePlayer player, BigDecimal amount) {
        return provider.setBalance(player, amount);
    }

    public String currencyName() {
        return "Skrin";
    }

    public String currencyNamePlural() {
        return currencyName();
    }

    @Deprecated
    public Text format(double amount) {
        return format(new BigDecimal(amount));
    }

    public Text format(BigDecimal amount) {
        return Text.of(
            Text.of(ChatColor.WHITE, TWO_DECIMAL_FORMATTER.format(amount)),
            " ",
            amount.equals(BigDecimal.ONE) ? currencyName() : currencyNamePlural()
        );
    }

    @EventHandler
    public void onWalletUpdate(PlayerWalletUpdate event) {
        highScores.update(event.getPlayer(), ScoreTypes.SKRIN, event.getNewBalance().toBigInteger());
    }
}
