/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

import com.sk89q.commandbook.component.session.PersistentSession;

import java.util.concurrent.TimeUnit;

public class StoreSession extends PersistentSession {

    private long lastPurchT = 0;
    private String lastPurch = "";
    private long lastSaleT = 0;
    private long recentNotice = 0;

    protected StoreSession() {
        super(TimeUnit.MINUTES.toMicros(10));
    }

    public void setLastPurch(String lastPurch) {
        this.lastPurch = lastPurch;
        lastPurchT = System.currentTimeMillis();
    }

    public String getLastPurch() {
        return lastPurch;
    }

    public boolean recentPurch() {
        return System.currentTimeMillis() - lastPurchT < 10000;
    }

    public void updateSale() {
        lastSaleT = System.currentTimeMillis();
    }

    public boolean recentSale() {
        return System.currentTimeMillis() - lastSaleT < 10000;
    }

    public void updateNotice() {
        recentNotice = System.currentTimeMillis();
    }

    public boolean recentNotice() {
        return System.currentTimeMillis() - recentNotice < 35000;
    }
}
