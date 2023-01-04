/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.player;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.util.*;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class GeneralPlayerUtil {
    public static List<Item> giveItemToPlayer(Player player, ItemStack... items) {
        List<Item> itemEntities = new ArrayList<>();
        for (ItemStack item : items) {
            ItemStack remainder = player.getInventory().addItem(item).get(0);
            if (remainder != null) {
                itemEntities.add(EntityUtil.spawnProtectedItem(remainder, player));
            }
        }
        return itemEntities;
    }

    public static boolean isFlyingGamemode(GameMode gameMode) {
        List<GameMode> flyingGamemodes = List.of(
                GameMode.CREATIVE,
                GameMode.SPECTATOR
        );

        return flyingGamemodes.contains(gameMode);
    }

    /**
     * Checks if the player has a gamemode that provides flight.
     *
     * @param player The target player
     * @return true - if player has flight from gamemode
     */
    public static boolean hasFlyingGamemode(Player player) {
        return isFlyingGamemode(player.getGameMode());
    }

    /**
     * Removes a player's flight powers, if they're not in a flying gamemode.
     *
     * @param player The target player
     * @return true - if flight was removed
     */
    public static boolean takeFlightSafely(Player player) {
        if (!hasFlyingGamemode(player) && player.getAllowFlight()) {
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setFlySpeed(.1F);
            player.setFallDistance(0F);
            return true;
        }
        return false;
    }

    public static boolean isDamageableGamemode(GameMode gameMode) {
        List<GameMode> damageableGamemodes = List.of(
                GameMode.SURVIVAL,
                GameMode.ADVENTURE
        );

        return damageableGamemodes.contains(gameMode);
    }

    /**
     * Checks if the player has a gamemode that allows them to be injured.
     *
     * @param player The target player
     * @return true - if player can be damaged
     */
    public static boolean hasDamageableGamemode(Player player) {
        return isDamageableGamemode(player.getGameMode());
    }

    public static boolean hasInvulnerableGamemode(Player player) {
        return !hasDamageableGamemode(player);
    }

    public static boolean isInBuildMode(Player player) {
        return player.getGameMode() == GameMode.CREATIVE;
    }

    public static void findSafeSpot(Player player) {

        Location toBlock = LocationUtil.findFreePosition(player.getLocation());

        if (toBlock != null && player.teleport(toBlock)) {
            return;
        } else {
            toBlock = player.getLocation();
        }

        Location working = toBlock.clone();

        List<BlockFace> nearbyBlockFaces = Lists.newArrayList(EnvironmentUtil.getNearbyBlockFaces());
        nearbyBlockFaces.remove(BlockFace.SELF);
        Collections.shuffle(nearbyBlockFaces);

        boolean done = false;

        for (int i = 1; i < 10 && !done; i++) {
            for (BlockFace face : nearbyBlockFaces) {
                working = LocationUtil.findFreePosition(toBlock.getBlock().getRelative(face, i).getLocation(working));

                if (working == null) {
                    working = toBlock.clone();
                    continue;
                }

                done = player.teleport(working);
            }
        }

        if (!done) {
            player.teleport(player.getWorld().getSpawnLocation());
            ChatUtil.sendError(player, "Failed to locate a safe location, teleporting to spawn!");
        }
    }

    /**
     * This method is used to hide a player
     *
     * @param player - The player to hide
     * @param to     - The player who can no longer see the player
     * @return - true if change occurred
     */
    public static boolean hide(Player player, Player to) {

        if (to.canSee(player)) {
            to.hidePlayer(player);
            return true;
        }
        return false;
    }

    /**
     * This method is used to show a player
     *
     * @param player - The player to show
     * @param to     - The player who can now see the player
     * @return - true if change occurred
     */
    public static boolean show(Player player, Player to) {

        if (!to.canSee(player)) {
            to.showPlayer(player);
            return true;
        }
        return false;
    }

    public static Set<UUID> getOnlinePlayerUUIDs() {
        return CommandBook.server().getOnlinePlayers().stream()
          .map(Player::getUniqueId)
          .collect(Collectors.toSet());
    }

    public static boolean isLookingUp(Player player) {
        return player.getEyeLocation().getPitch() < -60;
    }

    public static boolean isLookingDown(Player player) {
        return player.getEyeLocation().getPitch() > 60;
    }

    private static boolean isSolidBlockAt(Location location) {
        return location.getBlock().getType().isSolid();
    }

    public static boolean isStandingOnSolidGround(Player player) {
        Location loc = player.getLocation();

        // Check 1 block down
        if (isSolidBlockAt(loc.add(0, -1, 0))) {
            return true;
        }

        // Check 2 blocks down (maybe they "jumped")
        if (isSolidBlockAt(loc.add(0, -1, 0))) {
            return true;
        }

        return false;
    }

    public static OfflinePlayer findOfflinePlayer(UUID playerID) {
        for (OfflinePlayer player : CommandBook.server().getOfflinePlayers()) {
            if (playerID.equals(player.getUniqueId())) {
                return player;
            }
        }
        return null;
    }

    public static UUID resolveMacroNamespace(CommandSender context, String qualifier) {
        if (qualifier.equals("@")) {
            return NamespaceConstants.GLOBAL;
        } else if (qualifier.equals("$") && context instanceof Player player) {
            return player.getUniqueId();
        } else if (qualifier.startsWith("#")) {
            String name = qualifier.substring(1);

            for (OfflinePlayer player : CommandBook.server().getOfflinePlayers()) {
                if (name.equalsIgnoreCase(player.getName())) {
                    return player.getUniqueId();
                }
            }
        } else {
            try {
                return UUID.fromString(qualifier);
            } catch (IllegalArgumentException ignored) { }
        }

        return null;
    }
}
