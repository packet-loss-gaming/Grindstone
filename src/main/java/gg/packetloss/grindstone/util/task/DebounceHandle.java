package gg.packetloss.grindstone.util.task;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class DebounceHandle<T> {
    private final long waitTime;
    private final T initialValue;
    private final BiFunction<T, T, T> updateFunction;
    private final Consumer<T> bounceAction;

    private State state;

    public DebounceHandle(long waitTime, T initialValue, BiFunction<T, T, T> updateFunction, Consumer<T> bounceAction) {
        this.waitTime = waitTime;
        this.initialValue = initialValue;
        this.updateFunction = updateFunction;
        this.bounceAction = bounceAction;

        this.state = new State();

        reset();
    }

    public DebounceHandle(long waitTime, T initialValue, BiFunction<T, T, T> updateFunction,
                          Consumer<T> bounceAction, State state) {
        this.waitTime = waitTime;
        this.initialValue = initialValue;
        this.updateFunction = updateFunction;
        this.bounceAction = bounceAction;
        this.state = state;
    }

    public State getState() {
        return state;
    }

    private void reset() {
        state.setCurrentValue(initialValue);
        state.setCurrentTask(null);
    }

    public void accept(T newValue) {
        state.setCurrentValue(updateFunction.apply(state.getCurrentValue(), newValue));

        BukkitTask currentTask = state.getCurrentTask();
        if (currentTask != null) {
            currentTask.cancel();
        }

        state.setCurrentTask(CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
            bounceAction.accept(state.getCurrentValue());
            reset();
        }, waitTime));
    }

    public class State {
        private T currentValue;
        private BukkitTask currentTask;

        private State() {
        }

        private T getCurrentValue() {
            return currentValue;
        }

        private void setCurrentValue(T newValue) {
            this.currentValue = newValue;
        }

        private BukkitTask getCurrentTask() {
            return this.currentTask;
        }

        private void setCurrentTask(BukkitTask task) {
            this.currentTask = task;
        }
    }
}
