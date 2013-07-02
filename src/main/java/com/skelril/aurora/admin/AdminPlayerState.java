package com.skelril.aurora.admin;

import com.skelril.aurora.util.player.PlayerState;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * Author: Turtle9598
 */
public class AdminPlayerState extends PlayerState {

    AdminState adminState;

    public AdminPlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents,
                            AdminState adminState, int level, float experience) {

        super(ownerName, inventoryContents, armourContents, level, experience);
        this.adminState = adminState;
    }

    public AdminPlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents,
                            AdminState adminState, double health, int hunger, float saturation,
                            float exhaustion, int level, float experience) {

        super(ownerName, inventoryContents, armourContents, health, hunger, saturation, exhaustion, level, experience);
        this.adminState = adminState;
    }

    public AdminPlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents,
                            AdminState adminState, double health, int hunger, float saturation, float exhaustion,
                            Location location) {

        super(ownerName, inventoryContents, armourContents, health, hunger, saturation, exhaustion, location);
        this.adminState = adminState;
    }

    public AdminPlayerState(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents,
                            AdminState adminState, double health, int hunger, float saturation, float exhaustion,
                            int level, float experience, Location location) {

        super(ownerName, inventoryContents, armourContents, health, hunger, saturation, exhaustion, level, experience,
                location);
        this.adminState = adminState;
    }

    public AdminState getAdminState() {

        return adminState;
    }

    public void setAdminState(AdminState adminState) {

        this.adminState = adminState;
    }
}
