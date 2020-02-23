package gg.packetloss.grindstone.util.dropttable;

public interface DropProvider {
    public void provide(DropTable dropTable, KillInfo killInfo);
}
