package gg.packetloss.grindstone.world.type.city.minigame;

public class Win {
    private String name;
    private WinType winType;

    public Win(String name, WinType winType) {
        this.name = name;
        this.winType = winType;
    }

    public String getName() {
        return name;
    }

    public WinType getWinType() {
        return winType;
    }
}
