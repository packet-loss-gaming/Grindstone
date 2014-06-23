/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.helper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class Response {

    private final Pattern pattern;
    private final List<String> response;

    public Response(Pattern pattern, List<String> response) {
        this.pattern = pattern;
        this.response = response;
    }

    public String getPattern() {
        return pattern.pattern();
    }

    public List<String> getResponse() {
        return Collections.unmodifiableList(response);
    }

    public boolean accept(Player player, String string) {
        if (!pattern.matcher(string).matches()) return false;

        Bukkit.broadcastMessage(ChatColor.YELLOW + "[Auto Reply] @" + player.getName());
        response.forEach(msg -> Bukkit.broadcastMessage("   " + ChatColor.YELLOW + msg));
        return true;
    }
}
