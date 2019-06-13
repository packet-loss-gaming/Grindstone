/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.restoration;

public abstract class BlockRecordIndex {

    public abstract void revertByTime(long time);

    public abstract void revertAll();

    public abstract int size();

    public abstract void dropAll();
}
