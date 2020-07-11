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

    private int getPageCount(int numItems) {
        int pageCount = (numItems / ITEMS_PER_PAGE);
        if (!isExtendedPageWithoutPager() && numItems % ITEMS_PER_PAGE != 0) {
            ++pageCount;
        }

        return pageCount;
    }

    @Override
    public int getPageForIndex(int elementIndex) {
        // Trick the page count algorithm into telling us what page this item is on
        // by pretending this item's index is the number of items we need pages for
        return getPageCount(elementIndex + 1);
    }

    @Override
    public int getNumberOfPages() {
        return getPageCount(items.size());
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
