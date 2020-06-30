package gg.packetloss.grindstone.jail;

public class PrisonIdentifier {
    private final String name;

    public PrisonIdentifier(String name) {
        this.name = name;
    }

    public String get() {
        return name;
    }
}
