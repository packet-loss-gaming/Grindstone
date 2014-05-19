/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util;

import com.skelril.aurora.util.checker.Checker;

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
     * Obtains a random element from the provided array
     */
    public static <T> T getElement(T[] arr) {
        return arr[ChanceUtil.getRandom(arr.length) - 1];
    }

    /**
     * Removes elements where the checker evaluates true
     */
    public static <T> List<T> removalAll(List<T> collection, Checker<?, T> checker) {
        return collection.stream().filter(element -> !checker.check(element)).collect(Collectors.toList());
    }
}
