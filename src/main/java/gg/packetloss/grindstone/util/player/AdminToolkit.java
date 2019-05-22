/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.player;

import gg.packetloss.grindstone.admin.AdminComponent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.stream.Collectors;

public class AdminToolkit {

  private AdminComponent admin;

  public AdminToolkit(AdminComponent admin) {
    this.admin = admin;
  }

  public <T extends Entity> Collection<T> removeAdmin(Collection<T> entities) {
    return entities.stream()
        .filter(e -> !(e instanceof Player && admin.isAdmin((Player) e)))
        .collect(Collectors.toSet());
  }
}
