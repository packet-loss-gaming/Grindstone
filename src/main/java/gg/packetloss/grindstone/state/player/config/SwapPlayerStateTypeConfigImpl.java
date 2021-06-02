/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.player.config;

public class SwapPlayerStateTypeConfigImpl implements PlayerStateTypeConfigImpl {
    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public boolean allowUseWithTemporaryState() {
        return false;
    }

    @Override
    public boolean shouldSwapOnDuplicate() {
        return true;
    }
}
