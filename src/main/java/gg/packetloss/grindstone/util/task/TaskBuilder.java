/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.task;

import com.sk89q.commandbook.CommandBook;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.BiFunction;
import java.util.function.Consumer;
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
                int runsRemaining = handle[0].acceptRuns();
                if (runsRemaining == 0) {
                    finishAction.run();
                    handle[0].cancel();
                    return;
                }

                // Dirty marker prevents modifications during the action from being overridden.
                if (testedAction.apply(runsRemaining) && !handle[0].isDirty()) {
                    handle[0].setRunsRemaining(runsRemaining - 1);
                }
            }, delay, interval);

            return handle[0] = new CountdownHandle(underlyingTask, numRuns);
        }
    }

    public static <T> Debounce<T> debounce() {
        return new Debounce<T>();
    }

    public static class Debounce<T> {
        private long waitTime;
        private T initialValue;
        private BiFunction<T, T, T> updateFunction;
        private Consumer<T> bounceAction;
        private DebounceHandle<T>.State state;

        private Debounce() {
            reset();
        }

        public void reset() {
            waitTime = 20;
            initialValue = null;
            updateFunction = null;
            bounceAction = null;
            state = null;
        }

        public void setWaitTime(long waitTime) {
            this.waitTime = waitTime;
        }

        public void setInitialValue(T initialValue) {
            this.initialValue = initialValue;
        }

        public void setUpdateFunction(BiFunction<T, T, T> updateFunction) {
            this.updateFunction = updateFunction;
        }

        public void setBounceAction(Consumer<T> bounceAction) {
            this.bounceAction = bounceAction;
        }

        public void setExistingState(DebounceHandle<T>.State state) {
            this.state = state;
        }

        public DebounceHandle<T> build() {
            Validate.isTrue(waitTime > 0);
            Validate.isTrue(updateFunction != null);
            Validate.isTrue(bounceAction != null);

            if (state != null) {
                return new DebounceHandle<>(waitTime, initialValue, updateFunction, bounceAction, state);
            } else {
                return new DebounceHandle<>(waitTime, initialValue, updateFunction, bounceAction);
            }
        }
    }
}
