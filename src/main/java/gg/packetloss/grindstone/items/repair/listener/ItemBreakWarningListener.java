/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.repair.listener;

import com.destroystokyo.paper.Title;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.items.repair.profile.RepairProfile;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.List;

public class ItemBreakWarningListener implements Listener {
    private static final int REPAIR_WARNING_CHANCE = 20;

    private List<RepairProfile> repairProfiles;

    public ItemBreakWarningListener(List<RepairProfile> repairProfiles) {
        this.repairProfiles = repairProfiles;
    }

    private boolean isNearlyBroken(ItemStack itemStack) {
        if (itemStack.getItemMeta() instanceof Damageable) {
            Damageable damageable = (Damageable) itemStack.getItemMeta();
            int damage = damageable.getDamage();
            int maxDurability = itemStack.getType().getMaxDurability();

            float damagePercentage = (float) ((double) damage / maxDurability);
            if (damagePercentage > .9 || maxDurability - damage < REPAIR_WARNING_CHANCE * 3) {
                return true;
            }
        }

        return false;
    }


    private void maybeWarnPlayer(Player player, ItemStack itemStack) {
        if (!ChanceUtil.getChance(REPAIR_WARNING_CHANCE)) {
            return;
        }

        if (!isNearlyBroken(itemStack)) {
            return;
        }

        for (RepairProfile repairProfile : repairProfiles) {
            if (repairProfile.matches(itemStack)) {
                Title warning = Title.builder()
                        .title(Text.of(ChatColor.DARK_RED, "WARNING").build())
                        .subtitle(Text.of(ChatColor.DARK_RED, "REPAIRABLE ITEM NEARLY BROKEN").build())
                        .stay(20 * 3)
                        .build();
                player.sendTitle(warning);

                player.sendMessage(Text.of(
                        ChatColor.YELLOW,
                        "To repair your ",
                        Text.of(TextAction.Hover.showItem(itemStack), ItemNameCalculator.getDisplayName(itemStack)),
                        ":"
                ).build());
                player.sendMessage(repairProfile.getWarningMessage());
                break;
            }
        }

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        maybeWarnPlayer(player, itemInHand);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBowFire(EntityShootBowEvent event) {
        Entity entity = event.getProjectile();
        if (entity instanceof Player) {
            maybeWarnPlayer((Player) entity, event.getBow());
        }
    }
}
