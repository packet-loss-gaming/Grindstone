/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.extractor;

public interface Extractor<K, E> {
    public K extractFrom(E e);
}
