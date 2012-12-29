package us.arrowcraft.aurora.admin;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import us.arrowcraft.aurora.util.player.GenericWealthStore;

/**
 * Author: Turtle9598
 */
public class AdminPlayerState extends GenericWealthStore {

    AdminState adminState;
    int health = 20;
    int hunger = 20;
    float saturation = 5;
    float exhaustion = 0;
    int level = 0;
    float experience = 0;
    Location location = null;

    public AdminPlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents,
                            AdminState adminState, int level, float experience) {

        super(ownerName, inventoryContents, armourContents);
        this.adminState = adminState;
        this.level = level;
        this.experience = experience;
    }

    public AdminPlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents,
                            AdminState adminState, int health, int hunger, float saturation,
                            float exhaustion, int level, float experience) {

        super(ownerName, inventoryContents, armourContents);
        this.adminState = adminState;
        this.health = health;
        this.hunger = hunger;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.level = level;
        this.experience = experience;
    }

    public AdminPlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents,
                            AdminState adminState, int health, int hunger, float saturation, float exhaustion,
                            Location location) {

        super(ownerName, inventoryContents, armourContents);
        this.adminState = adminState;
        this.health = health;
        this.hunger = hunger;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.location = location;
    }

    public AdminPlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents,
                            AdminState adminState, int health, int hunger, float saturation, float exhaustion,
                            int level, float experience, Location location) {

        super(ownerName, inventoryContents, armourContents);
        this.adminState = adminState;
        this.health = health;
        this.hunger = hunger;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.level = level;
        this.experience = experience;
        this.location = location;
    }

    public AdminState getAdminState() {

        return adminState;
    }

    public void setAdminState(AdminState adminState) {

        this.adminState = adminState;
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
