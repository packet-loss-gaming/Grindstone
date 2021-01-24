/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class ErrorUtil {
    public static void reportUnexpectedError(CommandSender sender, Map<String, Object> extraData) {
        ChatUtil.sendError(sender, "An unspecified system error occurred. Please report this.");

        String senderId = sender.getName();
        if (sender instanceof Player) {
            senderId += " (" + ((Player) sender).getUniqueId() + ")";
        }

        (new Exception("Unspecified error for " + senderId)).printStackTrace();

        if (!extraData.entrySet().isEmpty()) {
            CommandBook.logger().severe("Additional information:");
            for (Map.Entry<String, Object> entry : extraData.entrySet()) {
                CommandBook.logger().severe(" - " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    public static void reportUnexpectedError(CommandSender sender) {
        reportUnexpectedError(sender, Map.of());
    }
}
