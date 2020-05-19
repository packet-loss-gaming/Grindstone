package gg.packetloss.grindstone.util.dropttable;

public interface DropProvider {
    public <T extends KillInfo> void provide(DropTable<T> dropTable, T killInfo);
}
