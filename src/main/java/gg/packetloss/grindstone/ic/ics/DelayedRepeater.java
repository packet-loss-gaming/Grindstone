/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.ic.ics;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.mechanics.ic.*;
import org.bukkit.Server;

import java.util.logging.Logger;

public class DelayedRepeater extends AbstractIC {

  private static final CommandBook INST = CommandBook.inst();
  private static final Logger LOG = INST.getLogger();
  private static final Server SERVER = CommandBook.server();

  private long delay;

  public DelayedRepeater(Server server, ChangedSign block, ICFactory factory) {
    super(server, block, factory);
  }

  @Override
  public void load() {
    try {
      delay = Long.parseLong(getLine(2));
    } catch (Exception ex) {
      delay = 20;
    }
  }

  @Override
  public String getTitle() {

    return "Delay Repeater";
  }

  @Override
  public String getSignTitle() {

    return "DELAY REPEATER";
  }

  @Override
  public void trigger(final ChipState chip) {

    final boolean trigger = chip.getInput(0);
    SERVER.getScheduler().runTaskLater(INST, () -> chip.setOutput(0, trigger), delay);
  }

  public static class Factory extends AbstractICFactory implements RestrictedIC {

    public Factory(Server server) {

      super(server);
    }

    @Override
    public IC create(ChangedSign sign) {

      return new DelayedRepeater(getServer(), sign, this);
    }

    @Override
    public String getShortDescription() {

      return "Delays a current by x ticks.";
    }

    @Override
    public String[] getLineHelp() {

      return new String[] {"delay", ""};
    }
  }
}
