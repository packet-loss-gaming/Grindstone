/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.db;

import java.util.Set;

public class PixieChestDetail {
    private final int networkID;
    private final Set<String> itemNames;

    public PixieChestDetail(int networkID, Set<String> itemNames) {
        this.networkID = networkID;
        this.itemNames = itemNames;
    }

    public int getNetworkID() {
        return networkID;
    }

    public ChestKind getChestKind() {
        return itemNames == null ? ChestKind.SOURCE : ChestKind.SINK;
    }

    public Set<String> getSinkItems() {
        return itemNames;
    }
}
