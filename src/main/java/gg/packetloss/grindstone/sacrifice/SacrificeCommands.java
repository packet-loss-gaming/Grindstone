/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.sacrifice;

import gg.packetloss.grindstone.highscore.scoretype.ScoreTypes;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;

import java.math.BigDecimal;

@CommandContainer
public class SacrificeCommands {
    private final SacrificeComponent component;

    public SacrificeCommands(SacrificeComponent component) {
        this.component = component;
    }

    @Command(name = "value", desc = "Value an item")
    public void valueCmd(Player player) {
        ItemStack questioned = player.getInventory().getItemInHand();

        // Check value & validity
        double value = component.getValue(questioned);
        if (value == 0) {
            ChatUtil.sendError(player, "You can't sacrifice that!");
            return;
        }

        // Mask the value so it doesn't just show the market price and print it
        BigDecimal shownValue = BigDecimal.valueOf(value).multiply(ScoreTypes.SACRIFICED_VALUE.getScalingConstant());
        BigDecimal minShownValue = shownValue.multiply(new BigDecimal(component.getConfig().valueMinMultiplier));
        BigDecimal maxShownValue = shownValue.multiply(new BigDecimal(component.getConfig().valueMaxMultiplier));
        ChatUtil.sendNotice(player, "This has a sacrificial value of: " +
                ChatColor.WHITE + ChatUtil.WHOLE_NUMBER_FORMATTER.format(minShownValue.toBigInteger()) +
                ChatColor.YELLOW + " - " +
                ChatColor.WHITE + ChatUtil.WHOLE_NUMBER_FORMATTER.format(maxShownValue.toBigInteger()));
    }
}