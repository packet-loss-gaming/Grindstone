/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.db;

import java.util.List;
import java.util.Map;

public class PixieChestDetail {
    private final int networkID;
    private final Map<String, List<Integer>> itemMapping;

    public PixieChestDetail(int networkID, Map<String, List<Integer>> itemMapping) {
        this.networkID = networkID;
        this.itemMapping = itemMapping;
    }

    public int getNetworkID() {
        return networkID;
    }

    public ChestKind getChestKind() {
        return itemMapping == null ? ChestKind.SOURCE : ChestKind.SINK;
    }

    public Map<String, List<Integer>> getItemMapping() {
        return itemMapping;
    }
}
