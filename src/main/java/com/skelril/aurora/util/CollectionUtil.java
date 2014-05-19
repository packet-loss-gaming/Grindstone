/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util;

import java.util.List;

public class CollectionUtil {
    public static <T> T getElement(List<T> list) {
        return list.get(ChanceUtil.getRandom(list.size()) - 1);
    }

    public static <T> T getElement(T[] arr) {
        return arr[ChanceUtil.getRandom(arr.length) - 1];
    }
}
