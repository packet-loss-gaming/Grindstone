/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.admin;

import java.util.HashSet;
import java.util.Set;


public enum AdminState {
    MEMBER,
    MODERATOR(MEMBER),
    ADMIN(MODERATOR),
    SYSOP(ADMIN);

    private final AdminState child;
    private Set<AdminState> states = new HashSet<>();

    AdminState() {
        child = null;
    }

    AdminState(AdminState child) {
        this.child = child;
        addState(child);
    }

    private void addState(AdminState state) {
        if (state == null) return;
        states.add(state);
        addState(state.child);
    }

    public boolean isAbove(AdminState state) {
        return this.equals(state) || states.contains(state);
    }
}
