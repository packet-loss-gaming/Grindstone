/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations.combotools;

import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.Material;

public class ComboAxe extends AbstractItemFeatureImpl {
    protected static boolean accepts(Material type) {
        if (EnvironmentUtil.isLog(type)) {
            return true;
        }

        // FIXME: Add other wood blocks

        return false;
    }
}
