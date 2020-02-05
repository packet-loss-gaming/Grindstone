package gg.packetloss.grindstone.util.task;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Function;

public class TaskBuilder {
    private TaskBuilder() { }

    public static Countdown countdown() {
        return new Countdown();
    }

    public static class Countdown {
        private long delay;
        private long interval;
        private int numRuns;
        private Function<Integer, Boolean> testedAction;
        private Runnable finishAction;

        private Countdown() {
            reset();
        }

        public void reset() {
            delay = 0;
            interval = 1;
            numRuns = 1;
            testedAction = (runsRemaining) -> true;
            finishAction = () -> { };
        }

        public void setDelay(long delay) {
            this.delay = delay;
        }

        public void setInterval(long interval) {
            this.interval = interval;
        }

        public void setNumberOfRuns(int numRuns) {
            this.numRuns = numRuns;
        }

        public void setAction(Function<Integer, Boolean> action) {
            this.testedAction = action;
        }

        public void setFinishAction(Runnable finishAction) {
            this.finishAction = finishAction;
        }

        public CountdownHandle build() {
            CountdownHandle[] handle = { null };

            BukkitTask underlyingTask = Bukkit.getScheduler().runTaskTimer(CommandBook.inst(), () -> {
                int runsRemaining = handle[0].getRunsRemaining();

                if (testedAction.apply(runsRemaining)) {
                    handle[0].setRunsRemaining(--runsRemaining);
                }

                if (runsRemaining == 0) {
                    finishAction.run();
                    handle[0].cancel();
                }
            }, delay, interval);

            return handle[0] = new CountdownHandle(underlyingTask, numRuns);
        }
    }
}
