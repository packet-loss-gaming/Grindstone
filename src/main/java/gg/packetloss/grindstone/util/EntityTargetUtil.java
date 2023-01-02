/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.bukkit.event.entity.EntityTargetEvent;

import java.util.List;

public class EntityTargetUtil {
    private static final List<EntityTargetEvent.TargetReason> RETALIATORY_REASONS = List.of(
        EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY,
        EntityTargetEvent.TargetReason.TARGET_ATTACKED_OWNER
    );

    public static boolean isRetaliatoryReason(EntityTargetEvent.TargetReason reason) {
        return RETALIATORY_REASONS.contains(reason);
    }
}
