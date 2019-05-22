/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.homes;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.sk89q.commandbook.CommandBook;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class CSVHomeDatabase implements HomeDatabase {

  private static final SimpleDateFormat dateFormat =
      new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
  protected final File homeFile;
  /**
   * A set of all homes.
   */
  protected final Set<Home> homes = new HashSet<>();
  private final Logger log = CommandBook.inst().getLogger();
  /**
   * Used to lookup homes by name.
   */
  protected Map<UUID, Home> playerIDHome = new HashMap<>();

  public CSVHomeDatabase(String name, File storageDir) {
    homeFile = new File(storageDir, name + ".csv");
  }

  @Override
  public synchronized boolean load() {

    FileInputStream input = null;
    boolean successful = true;

    try {
      input = new FileInputStream(homeFile);
      InputStreamReader streamReader = new InputStreamReader(input, StandardCharsets.UTF_8);
      CSVReader reader = new CSVReader(new BufferedReader(streamReader));
      String[] line;

      while ((line = reader.readNext()) != null) {
        if (line.length < 5) {
          log.warning("A home entry with < 5 fields was found!");
          continue;
        }
        try {
          String playerIDStr = line[0];
          UUID playerID = UUID.fromString(playerIDStr);

          String world = line[1];
          int x = Integer.parseInt(line[2]);
          int y = Integer.parseInt(line[3]);
          int z = Integer.parseInt(line[4]);

          Home home = new Home(playerID, world, x, y, z);

          playerIDHome.put(playerID, home);
          homes.add(home);
        } catch (NumberFormatException e) {
          log.warning("Incorrect int found in home entry!");
        } catch (Exception e) {
          log.warning("The home's world could not be found!");
        }
      }
      log.info(homes.size() + " houses loaded.");
    } catch (FileNotFoundException ignored) {
    } catch (IOException e) {
      playerIDHome = new HashMap<>();
      log.warning("Failed to load " + homeFile.getAbsolutePath()
          + ": " + e.getMessage());
      successful = false;
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException ignored) {
        }
      }
    }
    return successful;
  }

  @Override
  public synchronized boolean save() {

    FileOutputStream output = null;
    boolean successful = true;

    try {
      output = new FileOutputStream(homeFile);
      CSVWriter writer = new CSVWriter(new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8)));
      String[] line;

      for (Home home : homes) {
        line = new String[] {
            home.getPlayerID().toString(),
            home.getWorldName(),
            String.valueOf(home.getX()),
            String.valueOf(home.getY()),
            String.valueOf(home.getZ())
        };
        writer.writeNext(line);
      }
      writer.flush();
      writer.close();
    } catch (IOException e) {
      log.warning("Failed to save " + homeFile.getAbsolutePath()
          + ": " + e.getMessage());
      successful = false;
    } finally {
      if (output != null) {
        try {
          output.close();
        } catch (IOException ignored) {
        }
      }
    }
    return successful;
  }

  @Override
  public boolean houseExist(UUID playerID) {
    return playerIDHome.containsKey(playerID);
  }

  @Override
  public void saveHouse(Player player, String world, int x, int y, int z) {
    // Remove the old home if it exists.
    Home oldHome = playerIDHome.remove(player.getUniqueId());
    if (oldHome != null) {
      homes.remove(oldHome);
    }

    Home home = new Home(player.getUniqueId(), world, x, y, z);
    playerIDHome.put(player.getUniqueId(), home);
    homes.add(home);
  }

  @Override
  public boolean deleteHouse(UUID playerID) {
    Home home = playerIDHome.remove(playerID);
    if (home != null) {
      homes.remove(home);
      return true;
    }
    return false;
  }

  @Override
  public Home getHouse(UUID playerID) {
    return playerIDHome.get(playerID);
  }
}
