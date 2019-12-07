package gg.packetloss.grindstone.city.engine.pixieitems.broker;

import gg.packetloss.grindstone.city.engine.pixieitems.BrokerTransaction;
import gg.packetloss.grindstone.city.engine.pixieitems.TransactionBroker;
import gg.packetloss.grindstone.util.ChatUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EconomyBroker implements TransactionBroker {
    private static final double TRANSACTION_BASE_COST = 3;
    private static final double STACK_BASE_COST = .5;
    private static final double PER_ITEM_COST = .1;

    private final Economy economy;
    private final OfflinePlayer player;

    private double playerBalance;
    private double cost = TRANSACTION_BASE_COST;

    public EconomyBroker(Economy economy, OfflinePlayer player) {
        this.economy = economy;
        this.player = player;

        this.playerBalance = economy.getBalance(player);
    }

    @Override
    public BrokerTransaction authorizeMovement(ItemStack stack) {
        return new EconomyBrokerTransaction(this, stack);
    }

    @Override
    public void applyCharges() {
        if (cost == TRANSACTION_BASE_COST) {
            return;
        }

        economy.withdrawPlayer(player, cost);

        if (player.isOnline()) {
            Player onlinePlayer = player.getPlayer();
            ChatUtil.sendNotice(
                    onlinePlayer,
                    "The pixies charge you " + economy.format(cost) + " for their services."
            );
        }
    }

    private boolean hasFunds(double additionalCost) {
        return playerBalance >= additionalCost + cost;
    }

    private boolean tryAuthorize(double additionalCost) {
        if (hasFunds(additionalCost)) {
            cost += additionalCost;
            return true;
        }

        return false;
    }

    private void reduceCost(double amount) {
        cost -= amount;
    }

    private static class EconomyBrokerTransaction implements BrokerTransaction {
        private EconomyBroker broker;
        private int originalAmount;
        private boolean isAuthorized;

        public EconomyBrokerTransaction(EconomyBroker broker, ItemStack stack) {
            this.broker = broker;
            this.originalAmount = stack.getAmount();
            this.isAuthorized = broker.tryAuthorize(STACK_BASE_COST + (PER_ITEM_COST * stack.getAmount()));
        }

        @Override
        public boolean isAuthorized() {
            return isAuthorized;
        }

        @Override
        public void complete(ItemStack unused) {
            if (unused == null) {
                return;
            }

            if (unused.getAmount() == originalAmount) {
                broker.reduceCost(STACK_BASE_COST);
            }

            broker.reduceCost(unused.getAmount());
        }
    }
}
