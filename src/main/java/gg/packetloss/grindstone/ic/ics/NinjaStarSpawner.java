/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.ic.ics;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.mechanics.ic.*;
import com.sk89q.craftbook.util.LocationUtil;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;

import java.util.logging.Logger;

public class NinjaStarSpawner extends AbstractSelfTriggeredIC {

  private static final CommandBook INST = CommandBook.inst();
  private static final Logger LOG = INST.getLogger();
  private static final Server SERVER = CommandBook.server();

  private int quantity;

  public NinjaStarSpawner(Server server, ChangedSign block, ICFactory factory) {

    super(server, block, factory);
  }

  @Override
  public void load() {

    try {
      quantity = Integer.parseInt(getSign().getLine(2));
    } catch (NumberFormatException ex) {
      quantity = 1;
    }
  }

  @Override
  public String getTitle() {

    return "Star Spawner";
  }

  @Override
  public String getSignTitle() {

    return "STAR SPAWNER";
  }

  @Override
  public void trigger(ChipState chip) {

    drop();
  }

  @Override
  public void think(ChipState chip) {

    if (!chip.getInput(0)) {
      trigger(chip);
    }
  }

  public void drop() {

    Location k = LocationUtil.getCenterOfBlock(LocationUtil.getNextFreeSpace(getBackBlock(), BlockFace.UP));
    final Item item = k.getWorld().dropItem(k, CustomItemCenter.build(CustomItems.NINJA_STAR, quantity));
    SERVER.getScheduler().runTaskLater(INST, () -> {
      if (item.isValid()) {
        item.remove();
      }
    }, 20 * 15);
  }

  public static class Factory extends AbstractICFactory implements RestrictedIC {

    public Factory(Server server) {

      super(server);
    }

    @Override
    public IC create(ChangedSign sign) {

      return new NinjaStarSpawner(getServer(), sign, this);
    }

    @Override
    public String getShortDescription() {

      return "Spawns Ninja Stars.";
    }

    @Override
    public String[] getLineHelp() {

      return new String[] {"Quantity", ""};
    }
  }
}