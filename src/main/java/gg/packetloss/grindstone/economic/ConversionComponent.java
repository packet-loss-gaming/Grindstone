/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.economic.wallet.WalletComponent;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.item.inventory.InventoryAdapter;
import gg.packetloss.grindstone.util.item.inventory.PlayerStoragePriorityInventoryAdapter;
import gg.packetloss.grindstone.util.task.promise.TaskResult;
import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@ComponentInformation(friendlyName = "Conversion", desc = "Convert your cash.")
@Depend(plugins = {"Vault"}, components = {AdminComponent.class, WalletComponent.class})
public class ConversionComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private AdminComponent admin;
    @InjectComponent
    private WalletComponent wallet;

    private List<Player> recentList = new ArrayList<>();

    private static ItemPointTranslator goldConverter = new ItemPointTranslator();

    static {
        goldConverter.addMapping(new ItemStack(Material.GOLD_BLOCK), 81);
        goldConverter.addMapping(new ItemStack(Material.GOLD_INGOT), 9);
        goldConverter.addMapping(new ItemStack(Material.GOLD_NUGGET), 1);
    }

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
    }

    private BigDecimal getAmountToDeposit(Player player, Sign sign) {
        InventoryAdapter adapter = new PlayerStoragePriorityInventoryAdapter(player);

        // Count the amount of gold in the inventory.
        int goldCount = goldConverter.calculateValue(adapter, true);

        // Figure out how much we are trying to deposit, up to the amount of gold in the inventory.
        // Then create the new inventory value.
        int tranCount = Math.min(goldCount, Integer.parseInt(sign.getLine(2)));
        int newInvValue = goldCount - tranCount;

        // Set the new inventory value.
        int remainingValue = goldConverter.assignValue(adapter, newInvValue);
        Validate.isTrue(remainingValue == 0, "Failed to assign gold value while depositing!");

        // Remove the gold from the inventory.
        adapter.applyChanges();

        return new BigDecimal(tranCount);
    }

    private BigDecimal getBestGuessWithdrawAmount(Player player, Sign sign) {
        InventoryAdapter adapter = new PlayerStoragePriorityInventoryAdapter(player);

        // Establish an existing wallet balance/count of gold in the inventory.
        int goldCount = goldConverter.calculateValue(adapter, true);

        // Establish the amount of gold we're trying to place in the inventory,
        // and what the new inventory balance should be with that withdraw.
        int tranCount = Integer.parseInt(sign.getLine(2));
        int newInvValue = goldCount + tranCount;

        // Set the new inventory value, minus any amount that couldn't be placed into the inventory.
        tranCount -= goldConverter.assignValue(adapter, newInvValue);

        return new BigDecimal(tranCount);
    }

    private void addWithdrawnGold(Player player, BigDecimal amount) {
        InventoryAdapter adapter = new PlayerStoragePriorityInventoryAdapter(player);

        // Count the amount of gold in the inventory.
        int goldCount = goldConverter.calculateValue(adapter, true);
        goldCount += amount.intValue();

        // Figure out how much excess gold we have (if any, hopefully none)
        int remainder = goldConverter.assignValue(adapter, goldCount);

        // Give the withdrawn gold
        adapter.applyChanges();

        // Drop any excess as a protected stacks
        goldConverter.streamValue(remainder, (stack) -> {
            EntityUtil.spawnProtectedItem(stack, player);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        final Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null || !EnvironmentUtil.isSign(block)
                || recentList.contains(player) || admin.isAdmin(player)) return;

        Sign sign = (Sign) block.getState();

        if (sign.getLine(1).equals("[Bank]") || sign.getLine(1).equals("[Conversion]")) {
            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                try {
                    BigDecimal amount = getAmountToDeposit(player, sign);
                    if (!(amount.compareTo(BigDecimal.ZERO) > 0)) {
                        ChatUtil.sendError(player, "Nothing to deposit.");
                        return;
                    }

                    recentList.add(player);

                    // Perform the deposit.
                    wallet.addToBalance(player, amount).thenAccept(
                        (newBalance) -> {
                            ChatUtil.sendNotice(player, "You deposited: ", wallet.format(amount), ".");
                        },
                        (ignored) -> {
                            ErrorUtil.reportUnexpectedError(player, Map.of(
                                "amount", amount
                            ));
                        }
                    ).thenFinally(() -> recentList.remove(player));
                } catch (NumberFormatException ignored) {
                }
            } else if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                if (player.isSneaking()) return;
                try {
                    BigDecimal amount = getBestGuessWithdrawAmount(player, sign);
                    if (!(amount.compareTo(BigDecimal.ZERO) > 0)) {
                        ChatUtil.sendError(player, "Out of inventory space.");
                        return;
                    }

                    recentList.add(player);

                    // Perform the withdraw.
                    wallet.removeFromBalance(player, amount).thenApplyFailableAsynchronously(
                        TaskResult::fromCondition,
                        (ignored) -> {
                            ErrorUtil.reportUnexpectedError(player, Map.of(
                                "amount", amount
                            ));
                        }
                    ).thenAccept(
                        (ignored) -> {
                            ChatUtil.sendNotice(player, "You withdrew: ", wallet.format(amount), ".");

                            addWithdrawnGold(player, amount);
                        },
                        (ignored) -> {
                            ChatUtil.sendError(player, "You do not have sufficient funds.");
                        }
                    ).thenFinally(() -> recentList.remove(player));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBlockPlace(SignChangeEvent event) {

        Block block = event.getBlock();
        Player player = event.getPlayer();

        String header = event.getLine(1);
        boolean validHeader = false;

        if (header.equalsIgnoreCase("[Bank]")) {
            if (player.hasPermission("aurora.conversion.sign")) {
                event.setLine(1, "[Bank]");
                validHeader = true;
            }
        } else if (header.equalsIgnoreCase("[Conversion]")) {
            if (player.hasPermission("aurora.conversion.sign")) {
                event.setLine(1, "[Conversion]");
                validHeader = true;
            }
        } else {
            return;
        }

        if (!validHeader) {
            event.setCancelled(true);
            block.breakNaturally();
        }

        try {
            Integer.parseInt(event.getLine(2).trim());
        } catch (NumberFormatException e) {
            ChatUtil.sendError(player, "The third line must be the amount of ", wallet.currencyNamePlural(), " to be transferred.");
            event.setCancelled(true);
            block.breakNaturally();
        }
    }
}
