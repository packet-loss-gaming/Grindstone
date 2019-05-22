/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.generic;

import gg.packetloss.grindstone.util.ItemCondenser;

public class AbstractCondenserImpl extends AbstractItemFeatureImpl {
  protected ItemCondenser condenser;

  public AbstractCondenserImpl(ItemCondenser condenser) {
    this.condenser = condenser;
  }
}
