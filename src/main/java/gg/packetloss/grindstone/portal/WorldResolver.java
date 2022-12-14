/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.portal;

import gg.packetloss.grindstone.util.task.promise.TaskFuture;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface WorldResolver {
    public boolean accepts(Player player);
    public TaskFuture<Optional<Location>> getLastExitLocation(Player player);
    public TaskFuture<Location> getDefaultLocationForPlayer(Player player);
    default public TaskFuture<Location> getDestinationFor(Player player) {
        return getLastExitLocation(player).thenCompose((optLastLoc) -> {
            return optLastLoc.map(TaskFuture::completed).orElseGet(() -> getDefaultLocationForPlayer(player));
        });
    }
}
