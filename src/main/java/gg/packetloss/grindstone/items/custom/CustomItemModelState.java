/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import java.util.HashMap;
import java.util.Map;

class CustomItemModelState {
    // Count down from the maxmimum value since matches occur at anything higher,
    // this prevents new items from needing special handling.
    //
    // The "maximum value" 16777216 is the maximum value that can be accurate represented since Mojang uses
    // floats to represent custom_model_data.
    public int counter = 16777216;

    public Map<String, Integer> itemNameToModelId = new HashMap<>();
    public transient Map<CustomItems, Integer> itemToModelId = new HashMap<>();

    /**
     * @return true - if the model info was updated
     */
    public boolean update() {
        int initialCounter = counter;
        for (CustomItems value : CustomItems.values()) {
            int id = itemNameToModelId.computeIfAbsent(value.getNamespaceName(), (ignored) -> counter--);
            itemToModelId.put(value, id);
        }
        return initialCounter != counter;
    }
}
