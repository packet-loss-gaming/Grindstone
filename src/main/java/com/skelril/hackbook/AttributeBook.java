/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.hackbook;

import com.skelril.hackbook.exceptions.UnsupportedFeatureException;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.minecraft.server.v1_8_R3.GenericAttributes;
import net.minecraft.server.v1_8_R3.IAttribute;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;

public class AttributeBook {

    public enum Attribute {

        MAX_HEALTH(GenericAttributes.maxHealth),
        FOLLOW_RANGE(GenericAttributes.FOLLOW_RANGE),
        KNOCKBACK_RESISTANCE(GenericAttributes.c),
        MOVEMENT_SPEED(GenericAttributes.MOVEMENT_SPEED),
        ATTACK_DAMAGE(GenericAttributes.ATTACK_DAMAGE);

        public IAttribute attribute;

        Attribute(IAttribute attribute) {

            this.attribute = attribute;
        }

    }

    public static double getAttribute(LivingEntity entity, Attribute attribute) throws UnsupportedFeatureException {

        try {
            EntityInsentient nmsEntity = getNMSEntity(entity);

            return nmsEntity.getAttributeInstance(attribute.attribute).getValue();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new UnsupportedFeatureException();
        }
    }

    public static void setAttribute(LivingEntity entity, Attribute attribute, double value) throws UnsupportedFeatureException {

        try {
            EntityInsentient nmsEntity = getNMSEntity(entity);

            nmsEntity.getAttributeInstance(attribute.attribute).setValue(value);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new UnsupportedFeatureException();
        }
    }

    private static EntityInsentient getNMSEntity(LivingEntity entity) throws UnsupportedFeatureException {

        try {
            return ((EntityInsentient) ((CraftLivingEntity) entity).getHandle());
        } catch (Throwable t) {
            t.printStackTrace();
            throw new UnsupportedFeatureException();
        }
    }
}
