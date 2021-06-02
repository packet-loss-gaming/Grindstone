/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.db;

import java.util.ArrayList;
import java.util.List;

public class PixieNetworkDefinition {
    private final int networkID;
    private final List<PixieChestDefinition> definitions = new ArrayList<>();

    public PixieNetworkDefinition(int networkID) {
        this.networkID = networkID;
    }

    public int getNetworkID() {
        return networkID;
    }

    public List<PixieChestDefinition> getChests() {
        return definitions;
    }
}
