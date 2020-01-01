/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.combat;

import com.sk89q.commandbook.component.session.PersistentSession;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PvMSession extends PersistentSession {
    public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

    private UUID lastAttacked = null;

    protected PvMSession() {
        super(MAX_AGE);
    }

    public boolean checkLast(UUID lastAttacked) {
        if (this.lastAttacked == lastAttacked) {
            return true;
        }
        this.lastAttacked = lastAttacked;
        return false;
    }
}
