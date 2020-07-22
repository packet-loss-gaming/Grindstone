/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.manager;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class NewSinkResult implements NewChestResult {
    private final int numChestsDeleted;
    private final ImmutableSet<String> itemNames;

    public NewSinkResult(Integer numChestsDeleted, Set<String> itemNames) {
        this.numChestsDeleted = numChestsDeleted;
        this.itemNames = ImmutableSet.copyOf(itemNames);
    }

    @Override
    public boolean isNew() {
        return numChestsDeleted == 0;
    }

    public ImmutableSet<String> getItemNames() {
        return itemNames;
    }
}
