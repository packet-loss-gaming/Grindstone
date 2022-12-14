/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.task.promise;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.util.PluginTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class TaskFuture<T> {
    protected CompletableFuture<T> underlying;
    protected CompletableFuture<Void> underlyingParentFailure = new CompletableFuture<>();

    public TaskFuture() {
        this.underlying = new CompletableFuture<>();
    }

    private TaskFuture(CompletableFuture<T> underlying) {
        this.underlying = underlying;
    }

    public static <U> TaskFuture<U> completed(U value) {
        return new TaskFuture<>(CompletableFuture.completedFuture(value));
    }

    public static <U> TaskFuture<U> asyncTask(Supplier<U> supplier) {
        TaskFuture<U> taskFuture = new TaskFuture<>();
        PluginTaskExecutor.submitAsync(() -> {
            taskFuture.complete(supplier.get());
        });
        return taskFuture;
    }

    public boolean complete(T value) {
        return underlying.complete(value);
    }

    public T get() throws ExecutionException, InterruptedException {
        return underlying.get();
    }

    private <U> void handleParentFailure(TaskFuture<U> taskFuture) {
        underlyingParentFailure.thenAccept((value) -> taskFuture.underlyingParentFailure.complete(value));
    }

    private <U, L> void handleParentFailure(FailableTaskFuture<U, L> taskFuture) {
        underlyingParentFailure.thenAccept((value) -> taskFuture.underlyingParentFailure.complete(value));
    }

    public <U> TaskFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        TaskFuture<U> taskFuture = new TaskFuture<>();
        underlying.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                taskFuture.complete(fn.apply(value));
            });
        });
        handleParentFailure(taskFuture);
        return taskFuture;
    }

    public <U, L> FailableTaskFuture<U, L> thenApplyFailable(Function<? super T, TaskResult<U, L>> fn) {
        FailableTaskFuture<U, L> taskFuture = new FailableTaskFuture<>();
        underlying.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                taskFuture.complete(fn.apply(value));
            });
        });
        handleParentFailure(taskFuture);
        return taskFuture;
    }

    public <U> TaskFuture<U> thenApplyAsynchronously(Function<? super T, ? extends U> fn) {
        TaskFuture<U> taskFuture = new TaskFuture<>();
        underlying.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(() -> {
                taskFuture.complete(fn.apply(value));
            });
        });
        handleParentFailure(taskFuture);
        return taskFuture;
    }

    public <U, L> FailableTaskFuture<U, L> thenApplyFailableAsynchronously(Function<? super T, TaskResult<U, L>> fn) {
        FailableTaskFuture<U, L> taskFuture = new FailableTaskFuture<>();
        underlying.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(() -> {
                taskFuture.complete(fn.apply(value));
            });
        });
        handleParentFailure(taskFuture);
        return taskFuture;
    }

    public TaskFuture<Void> thenAccept(Consumer<? super T> action) {
        TaskFuture<Void> taskFuture = new TaskFuture<>();
        underlying.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                action.accept(value);
                taskFuture.complete(null);
            });
        });
        handleParentFailure(taskFuture);
        return taskFuture;
    }

    public TaskFuture<Void> thenAcceptAsynchronously(Consumer<? super T> action) {
        TaskFuture<Void> taskFuture = new TaskFuture<>();
        underlying.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(() -> {
                action.accept(value);
                taskFuture.complete(null);
            });
        });
        handleParentFailure(taskFuture);
        return taskFuture;
    }

    public <U> TaskFuture<U> thenCompose(Function<? super T, TaskFuture<U>> fn) {
        TaskFuture<U> taskFuture = new TaskFuture<>();
        underlying.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                fn.apply(value).underlying.thenAccept(taskFuture::complete);
            });
        });
        handleParentFailure(taskFuture);
        return taskFuture;
    }

    public <U> TaskFuture<U> thenComposeNative(Function<? super T, CompletableFuture<U>> fn) {
        TaskFuture<U> taskFuture = new TaskFuture<>();
        underlying.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
                fn.apply(value).thenAccept(taskFuture::complete);
            });
        });
        handleParentFailure(taskFuture);
        return taskFuture;
    }

    public <U> TaskFuture<U> thenComposeAsynchronously(Function<? super T, TaskFuture<U>> fn) {
        TaskFuture<U> taskFuture = new TaskFuture<>();
        underlying.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(() -> {
                fn.apply(value).underlying.thenAccept(taskFuture::complete);
            });
        });
        handleParentFailure(taskFuture);
        return taskFuture;
    }

    public <U> TaskFuture<U> thenComposeNativeAsynchronously(Function<? super T, CompletableFuture<U>> fn) {
        TaskFuture<U> taskFuture = new TaskFuture<>();
        underlying.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(() -> {
                fn.apply(value).thenAccept(taskFuture::complete);
            });
        });
        handleParentFailure(taskFuture);
        return taskFuture;
    }

    public <U, L> FailableTaskFuture<U, L> thenComposeFailable(Function<? super T, FailableTaskFuture<U, L>> fn) {
        FailableTaskFuture<U, L> taskFuture = new FailableTaskFuture<>();
        underlying.thenAccept((value) -> {
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
        handleParentFailure(taskFuture);
        return taskFuture;
    }

    public <U, L> FailableTaskFuture<U, L> thenComposeFailableAsynchronously(Function<? super T, FailableTaskFuture<U, L>> fn) {
        FailableTaskFuture<U, L> taskFuture = new FailableTaskFuture<>();
        underlying.thenAccept((value) -> {
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
        handleParentFailure(taskFuture);
        return taskFuture;
    }

    public void thenFinally(Runnable finallyHandler) {
        this.underlying.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), finallyHandler);
        });
        this.underlyingParentFailure.thenAccept((value) -> {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), finallyHandler);
        });
    }

    public void thenFinallyAsynchronously(Runnable finallyHandler) {
        this.underlying.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(finallyHandler);
        });
        this.underlyingParentFailure.thenAccept((value) -> {
            PluginTaskExecutor.submitAsync(finallyHandler);
        });
    }
}
