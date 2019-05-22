/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.homes;

import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public class HomeManager {
  private HomeDatabase homeDatabase;

  public HomeManager(HomeDatabase homeDatabase) {
    this.homeDatabase = homeDatabase;
  }

  public Optional<Location> getPlayerHome(Player player) {
    Home home = homeDatabase.getHouse(player.getUniqueId());
    return home != null ? Optional.of(home.getLocation()) : Optional.empty();
  }

  public Optional<Location> getSafePlayerHome(Player player) {
    Optional<Location> optPlayerHome = getPlayerHome(player);
    if (optPlayerHome.isPresent()) {
      return Optional.ofNullable(LocationUtil.findFreePosition(optPlayerHome.get()));
    }
    return Optional.empty();
  }

  public void setPlayerHome(Player player, Location loc) {
    homeDatabase.saveHouse(player, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    homeDatabase.save();
  }

  public void setPlayerHomeAndNotify(Player player, Location loc) {
    boolean hadHouse = homeDatabase.houseExist(player.getUniqueId());
    setPlayerHome(player, loc);

    if (hadHouse) {
      ChatUtil.sendNotice(player, "Your bed location has been updated.");
    } else {
      ChatUtil.sendNotice(player, "Your bed location has been set.");
    }
  }
}
