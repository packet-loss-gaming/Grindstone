/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.portal;

import org.bukkit.Location;
import org.bukkit.TravelAgent;
import org.bukkit.event.player.PlayerPortalEvent;

public class NoOPTravelAgent implements TravelAgent {
    @Override
    public TravelAgent setSearchRadius(int radius) {
        return this;
    }

    @Override
    public int getSearchRadius() {
        return 0;
    }

    @Override
    public TravelAgent setCreationRadius(int radius) {
        return this;
    }

    @Override
    public int getCreationRadius() {
        return 0;
    }

    @Override
    public boolean getCanCreatePortal() {
        return false;
    }

    @Override
    public void setCanCreatePortal(boolean create) {

    }

    @Override
    public Location findOrCreate(Location location) {
        return location;
    }

    @Override
    public Location findPortal(Location location) {
        return location;
    }

    @Override
    public boolean createPortal(Location location) {
        return false;
    }

    public static void overwriteDestination(PlayerPortalEvent event, Location location) {
        event.useTravelAgent(true);
        event.setPortalTravelAgent(new NoOPTravelAgent());

        event.setTo(location.add(0, 1, 0));
    }
}
