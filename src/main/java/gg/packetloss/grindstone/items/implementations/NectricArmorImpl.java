/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.implementations.support.Necrosis;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class NectricArmorImpl extends AbstractItemFeatureImpl {
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void necrosis(EntityDamageByEntityEvent event) {
    new Necrosis(prayers).handleEvent(event);
  }
}
