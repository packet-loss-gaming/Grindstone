/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.data;

import java.sql.Connection;
import java.sql.SQLException;

public interface MySQLPreparedStatement {
    void setConnection(Connection connection);
    void executeStatements() throws SQLException;
}
