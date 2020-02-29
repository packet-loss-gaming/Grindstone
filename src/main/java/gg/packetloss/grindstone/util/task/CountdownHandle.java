package gg.packetloss.grindstone.util.task;

import org.bukkit.scheduler.BukkitTask;

public class CountdownHandle {
    private final BukkitTask underlyingTask;
    private int runsRemaining;
    private boolean isDirty = false;

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
        underlyingTask.cancel();
    }
}
