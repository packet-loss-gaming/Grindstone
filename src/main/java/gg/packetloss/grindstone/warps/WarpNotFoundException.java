/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.warps;

public class WarpNotFoundException extends IllegalArgumentException {
    public WarpNotFoundException() {
        super("No warp could be found by that name!");
    }
}
