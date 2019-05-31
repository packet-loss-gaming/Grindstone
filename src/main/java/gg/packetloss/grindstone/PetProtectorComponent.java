/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.ItemID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.homes.HomeManagerComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;


@ComponentInformation(friendlyName = "Pet Protector", desc = "Protectin dem petz.")
@Depend(components = {HomeManagerComponent.class})
public class PetProtectorComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private HomeManagerComponent homes;

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        // Prevent safe mobs from being used against fellow players
        if (event.getTarget() instanceof Player && isSafe(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    private static Set<EntityDamageEvent.DamageCause> ignored = new HashSet<>();

    static {
        ignored.add(EntityDamageEvent.DamageCause.FALL);
    }

    private static EDBEExtractor<Player, LivingEntity, Arrow> extractor = new EDBEExtractor<>(
            Player.class,
            LivingEntity.class,
            Arrow.class
    );

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity targetEntity = event.getEntity();

        if (!isSafe(targetEntity) || ignored.contains(event.getCause())) {
            return;
        }

        if (event instanceof EntityDamageByEntityEvent) {
            CombatantPair<Player, LivingEntity, Arrow> result = extractor.extractFrom((EntityDamageByEntityEvent) event);

            // Only block entity damage by entity events where the attacker is a player
            if (result == null) return;

            Player attacker = result.getAttacker();

            String customName = targetEntity.getCustomName();
            String entityName = targetEntity.getType().toString().toLowerCase();

            if (((Tameable) targetEntity).getOwner().getUniqueId().equals(attacker.getUniqueId())) {
                String message = "You're hurting your " + entityName;
                if (customName != null) {
                    message += ", " + customName;
                }
                message += "!";

                ChatUtil.sendWarning(attacker, message);
                return;
            }

            if (customName != null) {
                ChatUtil.sendError(attacker, "How dare you try and harm, " + customName + "!");
            }

            ChatUtil.sendError(attacker, "You cannot hurt a " + entityName + " that you don't own.");
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {

        if (!(event.getRightClicked() instanceof Tameable)) return;

        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        Tameable tameable = (Tameable) entity;

        if (entity instanceof Horse && tameable.isTamed() && player.getItemInHand().getTypeId() == ItemID.RED_APPLE) {
            if (tameable.getOwner() == null) {

                tameable.setOwner(player);
                event.setCancelled(true);

                ItemUtil.removeItemOfType(player, ItemID.RED_APPLE, 1, true);

                ChatUtil.sendNotice(player, "You have gained possession of this horse.");
                return;
            } else if (player.isSneaking() && tameable.getOwner().getUniqueId().equals(player.getUniqueId())) {

                tameable.setOwner(null);
                tameable.setTamed(true);
                event.setCancelled(true);

                ItemUtil.removeItemOfType(player, ItemID.RED_APPLE, 1, true);

                ChatUtil.sendNotice(player, "You have lost possession of this horse.");
                return;
            }
        }

        if (isSafe(entity)) {
            if ((tameable.getOwner() == null || !tameable.getOwner().getUniqueId().equals(player.getUniqueId()))) {
                event.setCancelled(true);
                String entityName = entity.getType().toString().toLowerCase();
                ChatUtil.sendError(player, "You cannot interact with a " + entityName + " that you don't own.");
            } else if (entity instanceof Wolf && !((Wolf) tameable).isSitting() && !homes.isEntityInPlayersHome(player, entity)) {
                event.setCancelled(true);
                ChatUtil.sendError(player, "You cannot make a wolf sit outside of your home!");
            }
        }
    }

    private boolean isSafe(Entity entity) {
        if (entity instanceof Horse) {
            Horse horse = (Horse) entity;
            Entity passenger = horse.getPassenger();
            if (passenger instanceof Player) {
                return true;
            }
        }

        return entity instanceof Tameable && ((Tameable) entity).isTamed() && ((Tameable) entity).getOwner() != null;
    }
}
