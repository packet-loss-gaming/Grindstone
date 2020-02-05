package gg.packetloss.grindstone.util.task;

import org.bukkit.scheduler.BukkitTask;

public class CountdownHandle {
    private final BukkitTask underlyingTask;
    private int runsRemaining;

    public CountdownHandle(BukkitTask underlyingTask, int runsRemaining) {
        this.underlyingTask = underlyingTask;
        this.runsRemaining = runsRemaining;
    }

    public void setRunsRemaining(int runsRemaining) {
        this.runsRemaining = runsRemaining;
    }

    public int getRunsRemaining() {
        return runsRemaining;
    }

    public void cancel() {
        underlyingTask.cancel();
    }
}
