package gg.packetloss.grindstone.util.dropttable;

import java.util.function.Consumer;

public interface DropTable {
    void getDrops(KillInfo info, Consumer<Drop> drops);
}
