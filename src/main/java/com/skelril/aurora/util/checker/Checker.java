/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.checker;

public abstract class Checker<T, K> implements Expression<K, Boolean> {

    private T main;

    public Checker(T main) {
        this.main = main;
    }

    public T get() {
        return main;
    }
}
