/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.warps;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import org.bukkit.Location;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class CSVWarpDatabase implements WarpDatabase {

    private final Logger log = CommandBook.inst().getLogger();
    protected final File warpFile;

    /**
     * Used to lookup warps by namespace.
     */
    protected Map<UUID, List<WarpPoint>> namespaceWarps = new HashMap<>();

    /**
     * A set of all warps.
     */
    protected final List<WarpPoint> warps = new ArrayList<>();

    public CSVWarpDatabase(String name, File storageDir) {
        warpFile = new File(storageDir, name + ".csv");
    }

    @Override
    public List<WarpPoint> getWarpsInNamespace(UUID namespace) {
        return Lists.newArrayList(namespaceWarps.getOrDefault(namespace, new ArrayList<>()));
    }

    private List<WarpPoint> getWarpsForQualifierNamespace(WarpQualifiedName qualifiedName) {
        return namespaceWarps.getOrDefault(qualifiedName.getNamespace(), new ArrayList<>());
    }

    @Override
    public Optional<WarpPoint> getWarp(WarpQualifiedName qualifiedName) {
        return getWarpsForQualifierNamespace(qualifiedName).stream().filter(warp -> {
            return warp.getQualifiedName().equals(qualifiedName);
        }).findAny();
    }

    private void blindAddWarp(WarpPoint warp) {
        UUID namespace = warp.getQualifiedName().getNamespace();

        namespaceWarps.putIfAbsent(namespace, new ArrayList<>());
        namespaceWarps.get(namespace).add(warp);

        warps.add(warp);
    }

    @Override
    public Optional<WarpPoint> setWarp(WarpQualifiedName qualifiedName, Location loc) {
        Optional<WarpPoint> optExistingWarp = destroyWarp(qualifiedName);

        WarpPoint newWarp = new WarpPoint(
                qualifiedName, loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch()
        );
        blindAddWarp(newWarp);

        return optExistingWarp;
    }

    @Override
    public Optional<WarpPoint> destroyWarp(WarpQualifiedName qualifiedName) {
        Optional<WarpPoint> optExistingWarp = getWarp(qualifiedName);

        if (optExistingWarp.isPresent()) {
            getWarpsForQualifierNamespace(qualifiedName).remove(optExistingWarp.get());
            warps.remove(optExistingWarp.get());
        }

        return optExistingWarp;
    }

    @Override
    public synchronized boolean load() {

        boolean successful = true;
        try (FileInputStream input = new FileInputStream(warpFile)) {
            InputStreamReader streamReader = new InputStreamReader(input, "utf-8");
            CSVReader reader = new CSVReader(new BufferedReader(streamReader));
            String[] line;

            while ((line = reader.readNext()) != null) {
                if (line.length < 6) {
                    log.warning("A warp entry with < 8 fields was found!");
                    continue;
                }
                try {
                    String namespaceStr = line[0];
                    UUID namespace = UUID.fromString(namespaceStr);

                    String warpName = line[1];

                    String world = line[2];
                    double x = Double.parseDouble(line[3]);
                    double y = Double.parseDouble(line[4]);
                    double z = Double.parseDouble(line[5]);
                    float yaw = Float.parseFloat(line[6]);
                    float pitch = Float.parseFloat(line[7]);

                    WarpQualifiedName qualifiedName = new WarpQualifiedName(namespace, warpName);
                    WarpPoint warp = new WarpPoint(qualifiedName, world, x, y, z, yaw, pitch);

                    blindAddWarp(warp);
                } catch (NumberFormatException e) {
                    log.warning("Incorrect double found in warp entry!");
                }
            }
            log.info(warps.size() + " warps loaded.");
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            log.warning("Failed to load " + warpFile.getAbsolutePath()
                    + ": " + e.getMessage());
            successful = false;
        }
        return successful;
    }

    @Override
    public synchronized boolean save() {

        boolean successful = true;
        try (FileOutputStream output = new FileOutputStream(warpFile)) {
            CSVWriter writer = new CSVWriter(new BufferedWriter(new OutputStreamWriter(output, "utf-8")));
            String[] line;

            for (WarpPoint warp : warps) {
                line = new String[]{
                        warp.getQualifiedName().getNamespace().toString(),
                        warp.getQualifiedName().getName(),
                        warp.getWorldName(),
                        String.valueOf(warp.getPosition().getX()),
                        String.valueOf(warp.getPosition().getY()),
                        String.valueOf(warp.getPosition().getZ()),
                        String.valueOf(warp.getYaw()),
                        String.valueOf(warp.getPitch())
                };
                writer.writeNext(line);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.warning("Failed to save " + warpFile.getAbsolutePath()
                    + ": " + e.getMessage());
            successful = false;
        }
        return successful;
    }
}
