/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class DBUtil {
    public static String preparePlaceHolders(int length) {
        return String.join(",", Collections.nCopies(length, "?"));
    }

    public static void setStringValues(PreparedStatement preparedStatement, List<String> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            preparedStatement.setString(i + 1, values.get(i));
        }
    }

    public static void setIntValues(PreparedStatement preparedStatement, List<Integer> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            preparedStatement.setInt(i + 1, values.get(i));
        }
    }
}
