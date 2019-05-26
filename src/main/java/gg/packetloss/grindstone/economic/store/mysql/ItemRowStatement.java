/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ItemRowStatement extends ItemStatement {

    private Connection con;
    private final double price;
    private final boolean buyable;
    private final boolean sellable;

    public ItemRowStatement(String name, double price, boolean buyable, boolean sellable) {
        super(name);
        this.price = price;
        this.buyable = buyable;
        this.sellable = sellable;
    }

    @Override
    public void setConnection(Connection con) {
        this.con = con;
    }

    @Override
    public void executeStatements() throws SQLException {
        String sql = "INSERT INTO `market-items` (name, price, `current-price`, buyable, sellable) VALUES (?, ?, ?, ?, ?)"
                + "ON DUPLICATE KEY UPDATE price=values(price), `current-price`=values(`current-price`), buyable=values(buyable), sellable=values(sellable)";

        try (PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setDouble(2, price);
            statement.setDouble(3, price);
            statement.setBoolean(4, buyable);
            statement.setBoolean(5, sellable);
            statement.execute();
        }
    }
}
