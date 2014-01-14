package com.skelril.aurora.city.engine.minigame;

import com.skelril.aurora.util.player.PlayerState;

/**
 * User: Wyatt Childers
 * Date: 1/13/14
 */
public class PlayerGameState extends PlayerState {

    private int teamNumber = 0;

    public PlayerGameState(PlayerState state, int teamNumber) {
        super(state.getOwnerName(), state.getInventoryContents(), state.getArmourContents(), state.getHealth(),
                state.getHunger(), state.getSaturation(), state.getExhaustion(), state.getLevel(),
                state.getExperience(), state.getLocation());
        this.teamNumber = teamNumber;
    }

    public int getTeamNumber() {

        return teamNumber;
    }

    public void setTeamNumber(int teamNumber) {

        this.teamNumber = teamNumber;
    }
}
