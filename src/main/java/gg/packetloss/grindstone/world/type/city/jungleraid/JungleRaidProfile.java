package gg.packetloss.grindstone.world.type.city.jungleraid;

public class JungleRaidProfile {
    private JungleRaidClass combatClass = JungleRaidClass.BALANCED;
    private JungleRaidTeam team = null;
    private int points = 0;

    public JungleRaidProfile() { }

    public JungleRaidClass getCombatClass() {
        return combatClass;
    }

    public void setCombatClass(JungleRaidClass combatClass) {
        this.combatClass = combatClass;
    }

    public JungleRaidTeam getTeam() {
        return team;
    }

    public void setTeam(JungleRaidTeam team) {
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
