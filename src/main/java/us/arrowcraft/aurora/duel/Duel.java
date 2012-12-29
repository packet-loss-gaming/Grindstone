package us.arrowcraft.aurora.duel;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: Turtle9598
 */
public class Duel {

    private DuelTeam duelTeamOne;
    private DuelTeam duelTeamTwo;
    private DuelTeam neutralPlayers;
    private boolean stakedFight = false;
    private boolean active = false;


    public Duel(DuelTeam duelTeamOne, DuelTeam duelTeamTwo) {

        this.duelTeamOne = duelTeamOne;
        this.duelTeamTwo = duelTeamTwo;
    }

    public Duel(DuelTeam duelTeamOne, DuelTeam duelTeamTwo,
                DuelTeam neutralPlayers) {

        this.duelTeamOne = duelTeamOne;
        this.duelTeamTwo = duelTeamTwo;
        this.neutralPlayers = neutralPlayers;
    }


    public Duel(DuelTeam duelTeamOne, DuelTeam duelTeamTwo,
                DuelTeam neutralPlayers, boolean stakedFight) {

        this.duelTeamOne = duelTeamOne;
        this.duelTeamTwo = duelTeamTwo;
        this.neutralPlayers = neutralPlayers;
        this.stakedFight = stakedFight;
    }

    public Set<DuelTeam> getTeams() {

        Set<DuelTeam> duelTeams = new HashSet<>();
        duelTeams.add(duelTeamOne);
        duelTeams.add(duelTeamTwo);
        duelTeams.add(neutralPlayers);

        return duelTeams;
    }

    public DuelTeam getTeamOne() {

        return duelTeamOne;
    }

    public DuelTeam getTeamTwo() {

        return duelTeamTwo;
    }

    public DuelTeam getNeutralPlayers() {

        return neutralPlayers;
    }

    public Set<Player> getDuelPlayers() {

        DuelTeam[] duelTeams = new DuelTeam[3];
        duelTeams[0] = duelTeamOne;
        duelTeams[1] = duelTeamTwo;
        duelTeams[2] = neutralPlayers;

        Set<Player> players = new HashSet<>();
        for (DuelTeam duelTeam : duelTeams) {
            for (Player player : duelTeam.getPlayers()) {
                players.add(player);
            }
        }

        return players;
    }

    public boolean isStakedFight() {

        return stakedFight;
    }

    public boolean isActive() {

        return active;
    }

    public void setActive(boolean active) {

        this.active = active;
    }
}
