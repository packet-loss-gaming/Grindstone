/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.mysql;

import gg.packetloss.grindstone.data.MySQLPreparedStatement;

public abstract class ItemStatement implements MySQLPreparedStatement {
    protected final String name;

    public ItemStatement(String name) {
        this.name = name;
    }
}
