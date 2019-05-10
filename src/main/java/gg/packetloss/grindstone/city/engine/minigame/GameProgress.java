/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.minigame;

public enum GameProgress {
    DONE(0),
    INITIALIZED(1),
    ACTIVE(2),
    ENDING(3);

    public int level;

    GameProgress(int level) {

        this.level = level;
    }
}