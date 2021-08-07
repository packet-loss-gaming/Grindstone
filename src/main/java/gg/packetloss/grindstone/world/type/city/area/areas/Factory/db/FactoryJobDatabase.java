/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Factory.db;

import gg.packetloss.grindstone.world.type.city.area.areas.Factory.FactoryJob;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FactoryJobDatabase {
    public Optional<FactoryJob> getJob(UUID playerID, String itemName);
    public void updateJobs(List<FactoryJob> factoryJobs);
    public List<FactoryJob> getJobs(List<UUID> activePlayers);
}
