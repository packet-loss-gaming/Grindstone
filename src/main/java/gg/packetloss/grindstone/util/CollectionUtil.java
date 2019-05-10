/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import com.google.common.collect.Lists;
import gg.packetloss.grindstone.util.checker.Checker;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CollectionUtil {
    /**
     * Obtains a random element from the provided {@link java.util.List}
     */
    public static <T> T getElement(List<T> list) {
        return list.get(ChanceUtil.getRandom(list.size()) - 1);
    }

    /**
     * Obtains a random element from the provided {@link java.util.Collection}
     */
    public static <T> T getElement(Collection<T> collection) {
        return getElement(Lists.newArrayList(collection));
    }

    /**
     * Obtains a random element from the provided array
     */
    public static <T> T getElement(T[] arr) {
        return arr[ChanceUtil.getRandom(arr.length) - 1];
    }

    /**
     * Removes elements where the checker evaluates true
     */
    public static <T> List<T> removalAll(List<T> collection, Checker<?, T> checker) {
        return collection.stream().filter(element -> !checker.evaluate(element)).collect(Collectors.toList());
    }
}
