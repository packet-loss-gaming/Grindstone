/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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
