/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.player;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.io.Serializable;

public class PlayerState extends GenericWealthStore implements Serializable {

    private double health = 20;
    private int hunger = 20;
    private float saturation = 5;
    private float exhaustion = 0;
    private int level = 0;
    private float experience = 0;
    private transient Location location = null;

    public PlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents, int level,
                       float experience) {

        super(ownerName, inventoryContents, armourContents);
        this.level = level;
        this.experience = experience;
    }

    public PlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents, double health,
                       int hunger, float saturation, float exhaustion, int level, float experience) {

        super(ownerName, inventoryContents, armourContents);
        this.health = health;
        this.hunger = hunger;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.level = level;
        this.experience = experience;
    }

    public PlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents, double health,
                       int hunger, float saturation, float exhaustion, Location location) {

        super(ownerName, inventoryContents, armourContents);
        this.health = health;
        this.hunger = hunger;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.location = location == null ? null : location.clone();
    }

    public PlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents, double health,
                       int hunger, float saturation, float exhaustion, int level, float experience, Location location) {

        super(ownerName, inventoryContents, armourContents);
        this.health = health;
        this.hunger = hunger;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.level = level;
        this.experience = experience;
        this.location = location == null ? null : location.clone();
    }


    public double getHealth() {

        return health;
    }

    public void setHealth(double health) {

        this.health = health;
    }

    public int getHunger() {

        return hunger;
    }

    public void setHunger(int hunger) {

        this.hunger = hunger;
    }

    public float getSaturation() {

        return saturation;
    }

    public void setSaturation(float saturation) {

        this.saturation = saturation;
    }

    public float getExhaustion() {

        return exhaustion;
    }

    public void setExhaustion(float exhaustion) {

        this.exhaustion = exhaustion;
    }

    public int getLevel() {

        return level;
    }

    public void setLevel(int level) {

        this.level = level;
    }

    public float getExperience() {

        return experience;
    }

    public void setExperience(float experience) {

        this.experience = experience;
    }

    public Location getLocation() {

        return location;
    }

    public void setLocation(Location location) {

        this.location = location == null ? null : location.clone();
    }

}
