/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.warps;


import gg.packetloss.grindstone.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class WarpPoint {
    private WarpQualifiedName qualifiedName;
    private String worldName;
    private Vector position;
    private float yaw;
    private float pitch;

    public WarpPoint(WarpQualifiedName qualifiedName, String worldName, Vector position, float yaw, float pitch) {
        this.qualifiedName = qualifiedName;
        this.worldName = worldName;
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public WarpPoint(WarpQualifiedName qualifiedName, String worldName, double x, double y, double z, float yaw, float pitch) {
        this(qualifiedName, worldName, new Vector(x, y, z), yaw, pitch);
    }

    public WarpQualifiedName getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(WarpQualifiedName qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public Vector getPosition() {
        return position;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(worldName), position.getX(), position.getY(), position.getZ(), getYaw(), getPitch());
    }

    public Location getSafeLocation() {
        return LocationUtil.findFreePosition(getLocation());
    }

    public void setLocation(Location loc) {
        this.worldName = loc.getWorld().getName();
        this.position = new Vector(loc.getX(), loc.getY(), loc.getZ());
    }
}
