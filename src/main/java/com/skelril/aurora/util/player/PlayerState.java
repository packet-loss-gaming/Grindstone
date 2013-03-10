package com.skelril.aurora.util.player;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * Author: Turtle9598
 */
public class PlayerState extends GenericWealthStore {

    int health = 20;
    int hunger = 20;
    float saturation = 5;
    float exhaustion = 0;
    int level = 0;
    float experience = 0;
    Location location = null;

    public PlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents, int level,
                       float experience) {

        super(ownerName, inventoryContents, armourContents);
        this.level = level;
        this.experience = experience;
    }

    public PlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents, int health,
                       int hunger, float saturation, float exhaustion, int level, float experience) {

        super(ownerName, inventoryContents, armourContents);
        this.health = health;
        this.hunger = hunger;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.level = level;
        this.experience = experience;
    }

    public PlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents, int health,
                       int hunger, float saturation, float exhaustion, Location location) {

        super(ownerName, inventoryContents, armourContents);
        this.health = health;
        this.hunger = hunger;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.location = location;
    }

    public PlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents, int health,
                       int hunger, float saturation, float exhaustion, int level, float experience, Location location) {

        super(ownerName, inventoryContents, armourContents);
        this.health = health;
        this.hunger = hunger;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.level = level;
        this.experience = experience;
        this.location = location;
    }


    public int getHealth() {

        return health;
    }

    public void setHealth(int health) {

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

        this.location = location;
    }

}
