/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.playerhistory;

import java.util.concurrent.CompletableFuture;

class PlayerHistory {
    private final long initTime = System.currentTimeMillis();

    private CompletableFuture<Long> playTimeFuture = new CompletableFuture<>();
    private long playTime;

    public void loadExistingPlayer(long playTime) {
        setPlayTime(playTime);
    }

    public void loadNewPlayer() {
        setPlayTime(0);
    }

    public long getSecondsPlayedSinceLogin() {
        return (System.currentTimeMillis() - initTime) / 1000;
    }

    private synchronized long getPlayTimeInternal() {
        return playTime + getSecondsPlayedSinceLogin();
    }

    private synchronized void setPlayTime(long playTime) {
        this.playTime = playTime;

        playTimeFuture.complete(getPlayTimeInternal());
        playTimeFuture = null;
    }

    public synchronized CompletableFuture<Long> getPlayTime() {
        if (playTimeFuture != null) {
            return playTimeFuture;
        }

        return CompletableFuture.completedFuture(getPlayTimeInternal());
    }
}
