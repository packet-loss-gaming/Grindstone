package gg.packetloss.grindstone.economic.store;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionLine;
import gg.packetloss.grindstone.events.economy.MarketPurchaseEvent;
import gg.packetloss.grindstone.events.economy.MarketSellEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MarketTransactionLogger implements Listener {
    private MarketTransactionDatabase transactionDatabase;

    public MarketTransactionLogger(MarketTransactionDatabase transactionDatabase) {
        this.transactionDatabase = transactionDatabase;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMarketPurchase(MarketPurchaseEvent event) {
        Player player = event.getPlayer();
        CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
            for (MarketTransactionLine transactionLine : event.getTransactionLines()) {
                transactionDatabase.logPurchaseTransaction(player, transactionLine);
            }
            transactionDatabase.save();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMarketSale(MarketSellEvent event) {
        Player player = event.getPlayer();
        CommandBook.server().getScheduler().runTaskAsynchronously(CommandBook.inst(), () -> {
            for (MarketTransactionLine transactionLine : event.getTransactionLines()) {
                transactionDatabase.logSaleTransaction(player, transactionLine);
            }
            transactionDatabase.save();
        });
    }
}
