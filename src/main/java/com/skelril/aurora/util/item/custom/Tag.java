/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.item.custom;

public class Tag {
    private String key;
    private String prop;

    public Tag(String key, String prop) {
        this.key = key;
        this.prop = prop;
    }

    public String getKey() {
        return key;
    }

    public String getProp() {
        return prop;
    }
}
