/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.ItemID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.ItemPointTranslator;
import gg.packetloss.grindstone.util.item.inventory.InventoryAdapter;
import gg.packetloss.grindstone.util.item.inventory.PlayerStoragePriorityInventoryAdapter;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang3.Validate;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


@ComponentInformation(friendlyName = "Conversion", desc = "Convert your cash.")
@Depend(plugins = {"Vault"}, components = {AdminComponent.class, ImpersonalComponent.class})
public class ConversionComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    @InjectComponent
    AdminComponent adminComponent;
    @InjectComponent
    ImpersonalComponent impersonalComponent;

    private static Economy economy = null;
    private List<Player> recentList = new ArrayList<>();

    private static ItemPointTranslator goldConverter = new ItemPointTranslator();

    static {
        goldConverter.addMapping(new ItemStack(Material.GOLD_BLOCK), 81);
        goldConverter.addMapping(new ItemStack(Material.GOLD_INGOT), 9);
        goldConverter.addMapping(new ItemStack(Material.GOLD_NUGGET), 1);
    }

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        setupEconomy();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        final Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null || !EnvironmentUtil.isSign(block)
                || recentList.contains(player) || adminComponent.isAdmin(player)) return;

        Sign sign = (Sign) block.getState();

        if (sign.getLine(1).equals("[Bank]") || sign.getLine(1).equals("[Conversion]")) {
            if (!impersonalComponent.check(block, true)) return;
            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                try {
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

                    // Update inventory state.
                    adapter.applyChanges();

                    // Perform the deposit.
                    economy.depositPlayer(player, tranCount);
                    ChatUtil.sendNotice(player, "You deposited: "
                            + ChatUtil.makeCountString(economy.format(tranCount), "."));
                } catch (NumberFormatException ignored) {
                }
            } else if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                if (player.isSneaking()) return;
                try {
                    InventoryAdapter adapter = new PlayerStoragePriorityInventoryAdapter(player);

                    // Establish an existing wallet balance/count of gold in the inventory.
                    int goldCount = goldConverter.calculateValue(adapter, true);

                    // Establish the amount of gold we're trying to place in the inventory,
                    // and what the new inventory balance should be with that withdraw.
                    int tranCount = Integer.parseInt(sign.getLine(2));
                    int newInvValue = goldCount + tranCount;

                    // Set the new inventory value, minus any amount that couldn't be placed into the inventory.
                    tranCount -= goldConverter.assignValue(adapter, newInvValue);

                    // Check for sufficient balance factoring in inventory space results.
                    int bankGold = (int) economy.getBalance(player);
                    if (bankGold < tranCount) {
                        ChatUtil.sendError(player, "You do not have sufficient funds.");
                        return;
                    }

                    // Update inventory state.
                    adapter.applyChanges();

                    // Perform the withdraw.
                    economy.withdrawPlayer(player, tranCount);
                    ChatUtil.sendNotice(player, "You withdrew: "
                            + ChatUtil.makeCountString(economy.format(tranCount), "."));
                } catch (NumberFormatException ignored) {
                }
            }
            recentList.add(player);
            server.getScheduler().scheduleSyncDelayedTask(inst, () -> recentList.remove(player), 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBlockPlace(SignChangeEvent event) {

        Block block = event.getBlock();
        Player player = event.getPlayer();

        String header = event.getLine(1);
        boolean validHeader = false;

        if (header.equalsIgnoreCase("[Bank]")) {
            if (inst.hasPermission(player, "aurora.conversion.sign")) {
                event.setLine(1, "[Bank]");
                validHeader = true;
            }
        } else if (header.equalsIgnoreCase("[Conversion]")) {
            if (inst.hasPermission(player, "aurora.conversion.sign")) {
                event.setLine(1, "[Conversion]");
                validHeader = true;
            }
        } else {
            return;
        }

        if (!validHeader) {
            event.setCancelled(true);
            block.breakNaturally(new ItemStack(ItemID.SIGN));
        }

        try {
            Integer.parseInt(event.getLine(2).trim());
        } catch (NumberFormatException e) {
            ChatUtil.sendError(player, "The third line must be the amount of "
                    + economy.currencyNamePlural() + " to be transferred.");
            event.setCancelled(true);
            block.breakNaturally(new ItemStack(ItemID.SIGN));
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
