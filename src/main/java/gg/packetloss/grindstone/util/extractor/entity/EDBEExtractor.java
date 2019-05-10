/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.extractor.entity;

import gg.packetloss.grindstone.util.extractor.Extractor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

@SuppressWarnings("unchecked")
public class EDBEExtractor<Attacker extends Entity, Defender extends Entity, Thrown extends Projectile>
       implements Extractor<CombatantPair<Attacker, Defender, Thrown>, EntityDamageByEntityEvent> {

    private final Class<Attacker> attackerType;
    private final Class<Defender> defenderType;
    private final Class<Thrown> thrownClass;

    public EDBEExtractor(Class<Attacker> attackerType, Class<Defender> defenderType, Class<Thrown> thrownClass) {
        this.attackerType = attackerType;
        this.defenderType = defenderType;
        this.thrownClass = thrownClass;
    }

    @Override
    public CombatantPair<Attacker, Defender, Thrown> extractFrom(EntityDamageByEntityEvent event) {
        Entity defender = event.getEntity();
        Entity attacker = event.getDamager();
        Projectile projectile = null;

        if (attacker instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) attacker).getShooter();
            projectile = (Projectile) attacker;
            if (shooter == null || !attackerType.isInstance(shooter) || !thrownClass.isInstance(projectile)) {
                return null;
            }
            attacker = (Entity) shooter;
        }

        if (defenderType.isInstance(defender) && attackerType.isInstance(attacker)) {
            if (projectile == null) {
                return new CombatantPair<>((Attacker) attacker, (Defender) defender);
            }
            return new CombatantPair<>((Attacker) attacker, (Defender) defender, (Thrown) projectile);
        }
        return null;
    }
}
