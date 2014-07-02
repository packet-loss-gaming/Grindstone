/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.checker;

public interface Expression<I, R> {
    public R evaluate(I input);
}
