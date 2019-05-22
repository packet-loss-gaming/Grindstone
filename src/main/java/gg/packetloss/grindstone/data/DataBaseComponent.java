/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.data;

import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

@ComponentInformation(friendlyName = "Database", desc = "MySQL database handler.")
public class DataBaseComponent extends BukkitComponent {

  private LocalConfiguration config;

  @Override
  public void enable() {
    config = configure(new LocalConfiguration());
    updateHandle();
  }

  @Override
  public void reload() {
    super.reload();
    configure(config);
    updateHandle();
  }

  public void updateHandle() {
    MySQLHandle.setDatabase(config.database);
    MySQLHandle.setUsername(config.username);
    MySQLHandle.setPassword(config.password);
  }

  public class LocalConfiguration extends ConfigurationBase {
    @Setting("database")
    public String database = "";
    @Setting("username")
    public String username = "";
    @Setting("password")
    public String password = "";
  }
}
