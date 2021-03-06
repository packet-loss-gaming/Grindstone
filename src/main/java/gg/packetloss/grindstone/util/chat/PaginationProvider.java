/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.chat;

import java.util.function.Consumer;

public interface PaginationProvider<T> {
    int getPageForIndex(int elementIndex);
    int getNumberOfPages();
    void forEachItemInPage(int pageNumber, Consumer<T> consumer);
}
