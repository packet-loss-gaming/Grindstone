package gg.packetloss.grindstone.city.engine.skywars;

public class SkyWarsProfile {
    private SkyWarsTeam team = null;
    private int points = 0;

    public SkyWarsProfile() { }

    public SkyWarsTeam getTeam() {
        return team;
    }

    public void setTeam(SkyWarsTeam team) {
        this.team = team;
    }

    public int getPoints() {
        return points;
    }

    public void adjustPoints(int points) {
        this.points += points;

        // You must have at least 0 points
        if (this.points < 0) {
            this.points = 0;
        }
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
