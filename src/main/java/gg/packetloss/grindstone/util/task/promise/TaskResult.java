/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.task.promise;

public class TaskResult<T, K> {
    protected T successValue;
    protected K failureValue;
    protected boolean failed;

    protected TaskResult() { }

    public FailableTaskFuture<T, K> asTaskFuture() {
        FailableTaskFuture<T, K> future = new FailableTaskFuture<>();
        future.complete(this);
        return future;
    }

    public static TaskResult<Void, Void> fromCondition(boolean condition) {
        TaskResult<Void, Void> result = new TaskResult<>();
        result.failed = condition;
        return result;
    }

    public static <S> TaskResult<S, Void> success() {
        TaskResult<S, Void> result = new TaskResult<>();
        result.failed = false;
        return result;
    }

    public static <S, F> TaskResult<S, F> of(S value) {
        TaskResult<S, F> result = new TaskResult<>();
        result.successValue = value;
        result.failed = false;
        return result;
    }

    public static <S> TaskResult<S, Void> failed() {
        TaskResult<S, Void> result = new TaskResult<>();
        result.failed = true;
        return result;
    }

    public static <S, F> TaskResult<S, F> failed(F value) {
        TaskResult<S, F> result = new TaskResult<>();
        result.failureValue = value;
        result.failed = true;
        return result;
    }
}
