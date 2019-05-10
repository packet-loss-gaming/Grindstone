/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.minigame;

import gg.packetloss.grindstone.util.player.PlayerState;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.Serializable;

public class PlayerGameState extends PlayerState implements Serializable {

    private int teamNumber = 0;

    private String worldName;
    private double x, y, z;
    private float yaw, pitch;

    public PlayerGameState(PlayerState state, int teamNumber) {
        super(state.getOwnerName(), state.getInventoryContents(), state.getArmourContents(), state.getHealth(),
                state.getHunger(), state.getSaturation(), state.getExhaustion(), state.getLevel(),
                state.getExperience());
        this.teamNumber = teamNumber;
        setLocation(state.getLocation());
    }

    public int getTeamNumber() {

        return teamNumber;
    }

    public void setTeamNumber(int teamNumber) {

        this.teamNumber = teamNumber;
    }

    @Override
    public void setLocation(Location location) {

        super.setLocation(location);

        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    @Override
    public Location getLocation() {

        Location k = super.getLocation();

        if (k == null) {
            try {
                k = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
            } catch (Exception ex) {
                k = Bukkit.getWorlds().get(0).getSpawnLocation();
            }

            super.setLocation(k);
        }

        return k;
    }
}
