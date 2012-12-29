package us.arrowcraft.aurora.duel;
import org.bukkit.entity.Player;
import us.arrowcraft.aurora.util.player.GenericWealthStore;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: Turtle9598
 */
public class DuelTeam {

    private String teamName;
    private Set<Player> players = new HashSet<>();
    private Set<GenericWealthStore> stakes = new HashSet<>();
    private boolean neutralTeam = false;


    public DuelTeam(String teamName, Set<Player> players) {

        this.teamName = teamName;
        this.players = players;
    }

    public DuelTeam(String teamName, Set<Player> players, boolean isNeutralTeam) {

        this.teamName = teamName;
        this.players = players;
        this.neutralTeam = isNeutralTeam;
    }

    public DuelTeam(String teamName, Set<Player> players, Set<GenericWealthStore> stakes, boolean isNeutralTeam) {

        this.teamName = teamName;
        this.players = players;
        this.stakes = stakes;
        this.neutralTeam = isNeutralTeam;
    }

    public String getTeamName() {

        return teamName;
    }

    public void setTeamName(String teamName) {

        this.teamName = teamName;
    }

    public Set<Player> getPlayers() {

        return players;
    }

    public Set<GenericWealthStore> getStakes() {

        return stakes;
    }

    public boolean isNeutralTeam() {

        return neutralTeam;
    }

    public boolean hasStakes() {

        return stakes != null;
    }

}
