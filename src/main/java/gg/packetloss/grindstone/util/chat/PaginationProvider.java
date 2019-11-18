package gg.packetloss.grindstone.util.chat;

import java.util.function.Consumer;

public interface PaginationProvider<T> {
    int getNumberOfPages();
    void forEachItemInPage(int pageNumber, Consumer<T> consumer);
}
