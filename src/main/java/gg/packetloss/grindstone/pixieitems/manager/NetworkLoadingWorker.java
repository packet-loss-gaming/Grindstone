/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.manager;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class NetworkLoadingWorker implements Runnable {
    private final Thread thread;
    private final ReentrantReadWriteLock lock;

    public NetworkLoadingWorker(ReentrantReadWriteLock lock) {
        this.thread = new Thread(this);
        this.lock = lock;
    }

    public void start() {
        thread.start();
    }

    private final LinkedBlockingDeque<Runnable> tasks = new LinkedBlockingDeque<>();

    public void addTasks(Consumer<Consumer<Runnable>> op) {
        op.accept(tasks::add);

        synchronized (tasks) {
            tasks.notify();
        }
    }

    private void waitForWork() {
        try{
            synchronized (tasks) {
                while (tasks.isEmpty()) {
                    tasks.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void processQueue() {
        lock.writeLock().lock();
        try {
            while (true) {
                // Work through the existing task, unless no more exist.
                Runnable task = tasks.poll();
                if (task == null) {
                    break;
                }

                try {
                    task.run();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            waitForWork();
            processQueue();
        }
    }
}
