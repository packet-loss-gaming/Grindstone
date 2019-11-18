package gg.packetloss.grindstone.warps;

import com.sk89q.commandbook.CommandBook;

import java.util.UUID;

public class WarpQualifiedName {
    protected static final UUID GLOBAL_UUID = UUID.fromString("1d77f345-9cc6-45b2-9e85-260c25a9b38f");

    private final UUID namespace;
    private final String name;

    public WarpQualifiedName(String name) {
        this(GLOBAL_UUID, name);
    }

    public WarpQualifiedName(UUID namespace, String name) {
        this.namespace = namespace;
        this.name = name.toUpperCase();
    }

    public boolean isGlobal() {
        return namespace.equals(GLOBAL_UUID);
    }

    public UUID getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return name;
    }

    public String getFriendlyNamespaceName() {
        if (isGlobal()) {
            return "global".toUpperCase();
        }
        return CommandBook.server().getOfflinePlayer(namespace).getName();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WarpQualifiedName)) {
            return false;
        }

        WarpQualifiedName other = (WarpQualifiedName) o;
        return namespace.equals(other.namespace) && name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode() {
        return namespace.hashCode() + name.hashCode();
    }

    @Override
    public String toString() {
        return getFriendlyNamespaceName() + ":" + getName();
    }
}
