package com.skelril.aurora.authentication;

/**
 * Author: Turtle9598
 */
public class Character {

    final String playerName;
    String authToken = "";

    public Character(String playerName) {

        this.playerName = playerName.trim();
    }

    public Character(String playerName, String authToken) {

        this.playerName = playerName.trim();
        this.authToken = authToken.trim();
    }

    public String getPlayerName() {

        return playerName.toLowerCase();
    }

    protected String getAuthToken() {

        return authToken;
    }

    public void setAuthToken(String authToken) {

        this.authToken = authToken;
    }
}