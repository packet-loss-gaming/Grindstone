/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item;

import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerDropMapping implements Runnable {
    private HashMap<UUID, List<Item>> playerDrops = new HashMap<>();

    @Override
    public void run() {
        Set<UUID> onlinePlayerIds = GeneralPlayerUtil.getOnlinePlayerUUIDs();

        Iterator<Map.Entry<UUID, List<Item>>> entrySetIterator = playerDrops.entrySet().iterator();
        while (entrySetIterator.hasNext()) {
            Map.Entry<UUID, List<Item>> entry = entrySetIterator.next();
            UUID playerUUID = entry.getKey();

            if (!onlinePlayerIds.contains(playerUUID)) {
                entrySetIterator.remove();
                continue;
            }

            List<Item> items = entry.getValue();
            items.removeIf(i -> !i.isValid());
        }
    }

    public boolean trackItem(UUID playerID, Item item) {
        playerDrops.putIfAbsent(playerID, new ArrayList<>());
        List<Item> drops = playerDrops.get(playerID);
        drops.add(item);

        return true;
    }

    public boolean trackItem(Player player, Item item) {
        return trackItem(player.getUniqueId(), item);
    }

    public List<Item> getDropsList(UUID playerID) {
        return playerDrops.getOrDefault(playerID, new ArrayList<>());
    }
}
