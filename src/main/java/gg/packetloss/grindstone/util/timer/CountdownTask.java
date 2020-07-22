/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.timer;

@Deprecated
public abstract class CountdownTask implements IntegratedRunnable {
    public abstract boolean matchesFilter(int times);
    public abstract void performStep(int times);
    public abstract void performFinal();
    public void performEvery(int times) {
    }

    @Override
    public boolean run(int times) {
        performEvery(times);

        if (matchesFilter(times)) {
            performStep(times);
        }
        return true;
    }

    @Override
    public void end() {
        performEvery(-1);
        performFinal();
    }
}
