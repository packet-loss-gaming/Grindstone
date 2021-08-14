/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.task.promise;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.util.PluginTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FailableTaskFuture<T, K> {
    protected CompletableFuture<T> underlyingSuccess;
    protected CompletableFuture<K> underlyingFailure;
    protected CompletableFuture<Void> underlyingParentFailure = new CompletableFuture<>();

    public FailableTaskFuture() {
        this.underlyingSuccess = new CompletableFuture<>();
        this.underlyingFailure = new CompletableFuture<>();
    }

    public static <S, F> FailableTaskFuture<S, F> asyncTask(Supplier<TaskResult<S, F>> supplier) {
        FailableTaskFuture<S, F> future = new FailableTaskFuture<>();
        PluginTaskExecutor.submitAsync(() -> {
            future.complete(supplier.get());
        });
        return future;
    }

    public boolean complete(TaskResult<T, K> value) {
        if (value.failed) {
            return underlyingFailure.complete(value.failureValue);
        } else {
            return underlyingSuccess.complete(value.successValue);
        }
    }

    private <U> void runFailureHandle(TaskFuture<U> taskFuture, K value, Consumer<K> failureHandler) {
        failureHandler.accept(value);
        taskFuture.underlyingParentFailure.complete(null);
    }

    private <U, L> void runFailureHandle(FailableTaskFuture<U, L> taskFuture, K value, Consumer<K> failureHandler) {
        failureHandler.accept(value);
        taskFuture.underlyingParentFailure.complete(null);
    }

    private <U> void handleFailure(TaskFuture<U> taskFuture, Consumer<K> failureHandler) {
        underlyingFailure.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                runFailureHandle(taskFuture, value, failureHandler);
            });
        });
        underlyingParentFailure.thenAccept((value) -> taskFuture.underlyingParentFailure.complete(value));
    }

    private <U, L> void handleFailure(FailableTaskFuture<U, L> taskFuture, Consumer<K> failureHandler) {
        underlyingFailure.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                runFailureHandle(taskFuture, value, failureHandler);
            });
        });
        underlyingParentFailure.thenAccept((value) -> taskFuture.underlyingParentFailure.complete(value));
    }

    private <U> void handleFailureAsynchronously(TaskFuture<U> taskFuture, Consumer<K> failureHandler) {
        underlyingFailure.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(() -> {
                runFailureHandle(taskFuture, value, failureHandler);
            });
        });
        underlyingParentFailure.thenAccept((value) -> taskFuture.underlyingParentFailure.complete(value));
    }

    private <U, L> void handleFailureAsynchronously(FailableTaskFuture<U, L> taskFuture, Consumer<K> failureHandler) {
        underlyingFailure.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(() -> {
                runFailureHandle(taskFuture, value, failureHandler);
            });
        });
        underlyingParentFailure.thenAccept((value) -> taskFuture.underlyingParentFailure.complete(value));
    }

    public <U> TaskFuture<U> thenApply(Function<? super T, ? extends U> fn, Consumer<K> failureHandler) {
        TaskFuture<U> taskFuture = new TaskFuture<>();
        underlyingSuccess.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                taskFuture.complete(fn.apply(value));
            });
        });
        handleFailure(taskFuture, failureHandler);
        return taskFuture;
    }

    public <U> TaskFuture<U> thenApplyAsynchronously(Function<? super T, ? extends U> fn, Consumer<K> failureHandler) {
        TaskFuture<U> taskFuture = new TaskFuture<>();
        underlyingSuccess.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(() -> {
                taskFuture.complete(fn.apply(value));
            });
        });
        handleFailureAsynchronously(taskFuture, failureHandler);
        return taskFuture;
    }

    public <U, L> FailableTaskFuture<U, L> thenApplyFailable(Function<? super T, TaskResult<U, L>> fn, Consumer<K> failureHandler) {
        FailableTaskFuture<U, L> taskFuture = new FailableTaskFuture<>();
        underlyingSuccess.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                taskFuture.complete(fn.apply(value));
            });
        });
        handleFailure(taskFuture, failureHandler);
        return taskFuture;
    }

    public <U, L> FailableTaskFuture<U, L> thenApplyFailableAsynchronously(Function<? super T, TaskResult<U, L>> fn, Consumer<K> failureHandler) {
        FailableTaskFuture<U, L> taskFuture = new FailableTaskFuture<>();
        underlyingSuccess.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(() -> {
                taskFuture.complete(fn.apply(value));
            });
        });
        handleFailureAsynchronously(taskFuture, failureHandler);
        return taskFuture;
    }

    public TaskFuture<Void> thenAccept(Consumer<? super T> action, Consumer<K> failureHandler) {
        TaskFuture<Void> taskFuture = new TaskFuture<>();
        underlyingSuccess.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                action.accept(value);
                taskFuture.complete(null);
            });
        });
        handleFailure(taskFuture, failureHandler);
        return taskFuture;
    }

    public TaskFuture<Void> thenAcceptAsynchronously(Consumer<? super T> action, Consumer<K> failureHandler) {
        TaskFuture<Void> taskFuture = new TaskFuture<>();
        underlyingSuccess.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(() -> {
                action.accept(value);
                taskFuture.complete(null);
            });
        });
        handleFailureAsynchronously(taskFuture, failureHandler);
        return taskFuture;
    }

    public <U> TaskFuture<U> thenCompose(Function<? super T, TaskFuture<U>> fn, Consumer<K> failureHandler) {
        TaskFuture<U> taskFuture = new TaskFuture<>();
        underlyingSuccess.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                fn.apply(value).underlying.thenAccept(taskFuture::complete);
            });
        });
        handleFailure(taskFuture, failureHandler);
        return taskFuture;
    }

    public <U> TaskFuture<U> thenComposeAsynchronously(Function<? super T, TaskFuture<U>> fn, Consumer<K> failureHandler) {
        TaskFuture<U> taskFuture = new TaskFuture<>();
        underlyingSuccess.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(() -> {
                fn.apply(value).underlying.thenAccept(taskFuture::complete);
            });
        });
        handleFailureAsynchronously(taskFuture, failureHandler);
        return taskFuture;
    }

    public <U, L> FailableTaskFuture<U, L> thenComposeFailable(Function<? super T, FailableTaskFuture<U, L>> fn, Consumer<K> failureHandler) {
        FailableTaskFuture<U, L> taskFuture = new FailableTaskFuture<>();
        underlyingSuccess.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                FailableTaskFuture<U, L> composedFuture = fn.apply(value);

                composedFuture.underlyingSuccess.thenAccept(
                    (innerValue) -> taskFuture.underlyingSuccess.complete(innerValue)
                );
                composedFuture.underlyingFailure.thenAccept(
                    (innerValue) -> taskFuture.underlyingFailure.complete(innerValue)
                );
            });
        });
        handleFailure(taskFuture, failureHandler);
        return taskFuture;
    }

    public <U, L> FailableTaskFuture<U, L> thenComposeFailableAsynchronously(Function<? super T, FailableTaskFuture<U, L>> fn, Consumer<K> failureHandler) {
        FailableTaskFuture<U, L> taskFuture = new FailableTaskFuture<>();
        underlyingSuccess.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(() -> {
                FailableTaskFuture<U, L> composedFuture = fn.apply(value);

                composedFuture.underlyingSuccess.thenAccept(
                    (innerValue) -> taskFuture.underlyingSuccess.complete(innerValue)
                );
                composedFuture.underlyingFailure.thenAccept(
                    (innerValue) -> taskFuture.underlyingFailure.complete(innerValue)
                );
            });
        });
        handleFailureAsynchronously(taskFuture, failureHandler);
        return taskFuture;
    }

    public void thenFinally(Runnable finallyHandler) {
        this.underlyingSuccess.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), finallyHandler);
        });
        this.underlyingFailure.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), finallyHandler);
        });
        this.underlyingParentFailure.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), finallyHandler);
        });
    }

    public void thenFinallyAsynchronously(Runnable finallyHandler) {
        this.underlyingSuccess.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(finallyHandler);
        });
        this.underlyingFailure.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(finallyHandler);
        });
        this.underlyingParentFailure.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(finallyHandler);
        });
    }
}
