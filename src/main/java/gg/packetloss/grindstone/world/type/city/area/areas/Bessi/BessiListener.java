/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Bessi;

import gg.packetloss.grindstone.events.apocalypse.GemOfLifeUsageEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.world.type.city.area.AreaListener;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

import java.io.IOException;

public class BessiListener extends AreaListener<Bessi> {
    public BessiListener(Bessi parent) {
        super(parent);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        if (parent.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private static EDBEExtractor<Player, Ravager, Arrow> extractor = new EDBEExtractor<>(
        Player.class,
        Ravager.class,
        Arrow.class
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageByEntityEvent event) {
        // If the parent doesn't contain the target, ignore.
        if (!parent.contains(event.getEntity())) {
            return;
        }

        CombatantPair<Player, Ravager, Arrow> result = extractor.extractFrom(event);
        if (result == null) {
            return;
        }

        Player attacker = result.getAttacker();
        if (!parent.isParticipant(attacker)) {
            ChatUtil.sendNotice(attacker, "Hello there friend... I see you want to harm my cows too!");
            attacker.teleport(parent.boss.getLocation());
            attacker.damage(100, parent.boss);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCowKilled(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!parent.contains(entity)) {
            return;
        }

        if (entity.getType() == EntityType.COW) {
            parent.markCowKilled();
            return;
        }

        if (parent.isBessi(entity)) {
            parent.bessiWasKilled(entity.getLocation());;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGemOfLifeUsage(GemOfLifeUsageEvent event) {
        if (parent.isParticipant(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (parent.isParticipant(player, true)) {
            try {
                parent.playerState.pushState(PlayerStateKind.BESSI, player);
                event.getDrops().clear();
                event.setDroppedExp(0);
            } catch (ConflictingPlayerStateException | IOException e) {
                e.printStackTrace();
            }

            event.setDeathMessage(player.getName() + " found the beef");
        }
    }
}
