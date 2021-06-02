/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.state;

import org.bukkit.entity.Arrow;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

class NinjaArrow {
    private final WeakReference<Arrow> arrowRef;
    private final long creationTimeStamp;

    protected NinjaArrow(Arrow arrow) {
        this.arrowRef = new WeakReference<>(arrow);
        this.creationTimeStamp = System.currentTimeMillis();
    }

    protected long getCreationTimeStamp() {
        return creationTimeStamp;
    }

    protected Optional<Arrow> getIfStillRelevant(long lastArrowTime) {
        if (lastArrowTime - creationTimeStamp >= TimeUnit.SECONDS.toMillis(5)) {
            return Optional.empty();
        }

        Arrow arrow = arrowRef.get();
        if (arrow == null) {
            return Optional.empty();
        }

        if (!arrow.isValid()) {
            return Optional.empty();
        }

        return Optional.of(arrow);
    }
}
