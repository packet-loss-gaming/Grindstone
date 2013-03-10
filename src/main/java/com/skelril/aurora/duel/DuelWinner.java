package com.skelril.aurora.duel;
/**
 * Author: Turtle9598
 */
public class DuelWinner {

    private boolean isTeam;
    private Object winner;

    public DuelWinner(boolean isTeam, Object winner) {

        this.isTeam = isTeam;
        this.winner = winner;
    }

    public boolean isTeam() {

        return isTeam;
    }

    public Object getWinner() {

        return winner;
    }

}
