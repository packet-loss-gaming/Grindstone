package gg.packetloss.grindstone.util.chat;

import java.util.List;
import java.util.function.Consumer;

public class CollectionPaginationProvider<T> implements PaginationProvider<T> {
    private List<T> items;
    private static final int ITEMS_PER_PAGE = 8;

    public CollectionPaginationProvider(List<T> items) {
        this.items = items;
    }

    // Returns true if we're not going to display the pager, but we are going to
    // fil an extra row in its place.
    public boolean isExtendedPageWithoutPager() {
        return items.size() == ITEMS_PER_PAGE + 1;
    }

    @Override
    public int getNumberOfPages() {
        int pageCount = (items.size() / ITEMS_PER_PAGE);
        if (!isExtendedPageWithoutPager() && items.size() % ITEMS_PER_PAGE != 0) {
            ++pageCount;
        }

        return pageCount;
    }

    @Override
    public void forEachItemInPage(int pageNumber, Consumer<T> consumer) {
        // Reduce the page number to base 0
        --pageNumber;

        for (int i = pageNumber * ITEMS_PER_PAGE; i < items.size() && i < (pageNumber + 1) * ITEMS_PER_PAGE; ++i) {
            consumer.accept(items.get(i));
        }

        if (isExtendedPageWithoutPager()) {
            consumer.accept(items.get(ITEMS_PER_PAGE));
        }
    }
}
