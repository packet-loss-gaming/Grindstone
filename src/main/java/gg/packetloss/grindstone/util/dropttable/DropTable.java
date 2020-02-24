package gg.packetloss.grindstone.util.dropttable;

import java.util.function.Consumer;

public interface DropTable<T extends KillInfo> {
    void getDrops(T info, Consumer<Drop> drops);
}
