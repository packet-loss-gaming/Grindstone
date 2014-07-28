/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.generic;

import com.skelril.aurora.util.ItemCondenser;

public class AbstractCondenserImpl extends AbstractItemFeatureImpl {
    protected ItemCondenser condenser;

    public AbstractCondenserImpl(ItemCondenser condenser) {
        this.condenser = condenser;
    }
}
