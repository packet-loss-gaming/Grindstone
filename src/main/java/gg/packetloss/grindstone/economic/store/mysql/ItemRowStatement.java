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
    private final boolean infinite;
    private final boolean buyable;
    private final boolean sellable;

    public ItemRowStatement(String name, double price, boolean infinite, boolean buyable, boolean sellable) {
        super(name);
        this.price = price;
        this.infinite = infinite;
        this.buyable = buyable;
        this.sellable = sellable;
    }

    @Override
    public void setConnection(Connection con) {
        this.con = con;
    }

    private String getSQL(boolean infinite) {
        StringBuilder sql = new StringBuilder();

        sql.append("INSERT INTO `market-items` (name, price, `current-price`, stock, buyable, sellable) ");
        sql.append("VALUES (?, ?, ?, ?, ?, ?) ");
        sql.append("ON DUPLICATE KEY UPDATE price=values(price), `current-price`=values(`current-price`), ");
        if (infinite) {
            sql.append("stock=values(stock)");
        } else {
            sql.append("stock=GREATEST(stock, values(stock))");
        }
        sql.append(", buyable=values(buyable), sellable=values(sellable)");

        return sql.toString();
    }

    @Override
    public void executeStatements() throws SQLException {
        try (PreparedStatement statement = con.prepareStatement(getSQL(infinite))) {
            statement.setString(1, name);
            statement.setDouble(2, price);
            statement.setDouble(3, price);
            statement.setDouble(4, infinite ? -1 : 0);
            statement.setBoolean(5, buyable);
            statement.setBoolean(6, sellable);
            statement.execute();
        }
    }
}
