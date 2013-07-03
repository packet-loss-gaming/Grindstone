package com.skelril.aurora.pet;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

/**
 * Author: Turtle9598
 */
public class Pet {

    private final String playerName;
    private final EntityType petType;
    private LivingEntity pet;
    private LivingEntity target = null;

    public Pet(String playerName, EntityType petType, LivingEntity pet) {

        this.playerName = playerName;
        this.petType = petType;
        this.pet = pet;
    }

    public String getOwner() {

        return playerName;
    }

    public EntityType getType() {

        return petType;
    }

    public void setPet(LivingEntity pet) {

        this.pet = pet;
    }

    public LivingEntity getPet() {

        return pet;
    }

    public void setTarget(LivingEntity target) {

        this.target = target;
    }

    public LivingEntity getTarget() {

        return target;
    }
}
