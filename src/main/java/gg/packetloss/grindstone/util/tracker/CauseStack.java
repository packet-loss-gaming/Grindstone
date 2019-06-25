package gg.packetloss.grindstone.util.tracker;

import java.util.Optional;
import java.util.function.Supplier;

public class CauseStack<T> {
    private T curCause;

    public <K> K executeOnStackWithCause(T cause, Supplier<K> action) {
        T prevCause = curCause;

        curCause = cause;
        K result = action.get();
        curCause = prevCause;

        return result;
    }

    public void executeOnStackWithCause(T cause, Runnable action) {
        executeOnStackWithCause(cause, () -> {
            action.run();
            return null;
        });
    }

    public Optional<T> getCurCause() {
        return Optional.ofNullable(curCause);
    }
}
