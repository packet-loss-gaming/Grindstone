/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.checker;

public abstract class Checker<T, K> {

    private T main;

    public Checker(T main) {
        this.main = main;
    }

    public T getHeld() {
        return main;
    }

    public abstract boolean check(K k);//
}
