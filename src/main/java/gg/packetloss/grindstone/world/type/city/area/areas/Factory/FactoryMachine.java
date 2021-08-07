/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Factory;

import gg.packetloss.grindstone.util.functional.TriConsumer;

import java.util.UUID;

public interface FactoryMachine {
    public void detectNewJobs(TriConsumer<UUID, String, Integer> jobDeclarationConsumer);
}
