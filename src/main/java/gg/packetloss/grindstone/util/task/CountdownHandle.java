/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.task;

import org.bukkit.scheduler.BukkitTask;

public class CountdownHandle {
    private final BukkitTask underlyingTask;
    private int runsRemaining;
    private boolean isDirty = false;
    private boolean canceled = false;

    public CountdownHandle(BukkitTask underlyingTask, int runsRemaining) {
        this.underlyingTask = underlyingTask;
        this.runsRemaining = runsRemaining;
    }

    protected boolean isDirty() {
        return isDirty;
    }

    protected int acceptRuns() {
        isDirty = false;
        return runsRemaining;
    }

    public void setRunsRemaining(int runsRemaining) {
        this.runsRemaining = runsRemaining;
        this.isDirty = true;
    }

    public int getRunsRemaining() {
        return runsRemaining;
    }

    public void cancel() {
        if (canceled) {
            return;
        }

        underlyingTask.cancel();
        canceled = true;
    }
}
