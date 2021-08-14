/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import com.sk89q.commandbook.CommandBook;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PluginTaskExecutor {
    private static ExecutorService INSTANCE = Executors.newCachedThreadPool();

    public static void submitAsync(Runnable task) {
        INSTANCE.submit(task);
    }

    public static void shutdown() throws InterruptedException {
        INSTANCE.shutdown();

        if (INSTANCE.awaitTermination(5, TimeUnit.SECONDS)) {
            return;
        }

        CommandBook.logger().info("Waiting for background jobs...");
        if (INSTANCE.awaitTermination(30, TimeUnit.SECONDS)) {
            return;
        }

        int numTasksCancelled = INSTANCE.shutdownNow().size();
        CommandBook.logger().severe(numTasksCancelled + " background jobs timed out.");
    }
}
