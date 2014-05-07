/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.item.custom;

import org.bukkit.ChatColor;

public class Tag {
    private ChatColor color;
    private String key;
    private String prop;

    public Tag(ChatColor color, String key, String prop) {
        this.color = color;
        this.key = key;
        this.prop = prop;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getKey() {
        return key;
    }

    public String getProp() {
        return prop;
    }

}
