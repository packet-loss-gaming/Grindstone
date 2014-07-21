/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.bosses.detail;

import com.skelril.OSBL.entity.EntityDetail;

public class WBossDetail implements EntityDetail {

    private int level;

    public WBossDetail(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
