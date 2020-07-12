/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.hackbook;

import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
import net.minecraft.server.v1_16_R1.AttributeBase;
import net.minecraft.server.v1_16_R1.EntityInsentient;
import net.minecraft.server.v1_16_R1.GenericAttributes;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;

public class AttributeBook {

    public enum Attribute {

        MAX_HEALTH(GenericAttributes.MAX_HEALTH),
        FOLLOW_RANGE(GenericAttributes.FOLLOW_RANGE),
        KNOCKBACK_RESISTANCE(GenericAttributes.KNOCKBACK_RESISTANCE),
        MOVEMENT_SPEED(GenericAttributes.MOVEMENT_SPEED),
        ATTACK_KNOCKBACK(GenericAttributes.ATTACK_KNOCKBACK),
        ATTACK_DAMAGE(GenericAttributes.ATTACK_DAMAGE);

        public final AttributeBase attribute;

        Attribute(AttributeBase attribute) {
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
