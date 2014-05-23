/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.economic.store.mysql;

import com.skelril.aurora.data.MySQLPreparedStatement;

public abstract class ItemStatement implements MySQLPreparedStatement {
    protected final String name;

    public ItemStatement(String name) {
        this.name = name.toUpperCase();
    }
}
