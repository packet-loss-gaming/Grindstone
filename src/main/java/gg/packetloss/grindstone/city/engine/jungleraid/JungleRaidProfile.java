package gg.packetloss.grindstone.city.engine.jungleraid;

public class JungleRaidProfile {
    private JungleRaidClass combatClass = JungleRaidClass.BALANCED;
    private JungleRaidTeam team = null;

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
}
