package us.arrowcraft.aurora.guilds;
import com.petrifiednightmares.pitfall.PitfallEvent;
import com.sk89q.worldedit.blocks.BlockID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.arrowcraft.aurora.events.PrayerApplicationEvent;
import us.arrowcraft.aurora.util.ChatUtil;
import us.arrowcraft.aurora.util.player.GeneralPlayerUtil;

import java.util.*;

/**
 * Author: Turtle9598
 */
public class NinjaGuild extends AbstractGuild implements Runnable, Listener {

    private static final PotionEffect[] guildEffects = new PotionEffect[] {
            new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 600, 2),
            new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 600, 2),
            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 600, 2)
    };
    private static final int WATCH_DISTANCE_SQ = 10 * 10;
    private static final int SNEAK_WATCH_DISTANCE_SQ = 3 * 3;

    public NinjaGuild() {

        super("Ninja");
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        server.getScheduler().runTaskTimer(inst, this, 20 * 2, 11);
    }

    @Override
    public void run() {

        boostOnline();
        updateHidden();
    }


    /**
     * This method is used to get a list of player's that can be hidden from.
     *
     * @return - List of players which can be hidden from
     */
    public Player[] hideable() {

        List<Player> returnedList = new ArrayList<>();

        for (Player player : server.getOnlinePlayers()) {

            if (!inst.hasPermission(player, getGroupPermissionString() + ".vision")) returnedList.add(player);
        }
        return returnedList.toArray(new Player[returnedList.size()]);
    }

    /**
     * This method is used to boost online members.
     */
    public void boostOnline() {

        for (Player player : getActivePlayers()) {

            // Remove outdated
            for (PotionEffect guildEffect : guildEffects) player.removePotionEffect(guildEffect.getType());

            // Refresh
            List<PotionEffect> effectCollection = new ArrayList<>();
            Collections.addAll(effectCollection, guildEffects);
            player.addPotionEffects(effectCollection);
        }
    }

    /**
     * This method is used to update hidden states
     */
    public void updateHidden() {

        for (Player ninja : getActivePlayers()) {

            Player[] checked;
            if (hasFlag(ninja, 'g')) checked = getOnlineNonmembers();
            else checked = hideable();

            Set<Player> invisibleNewCount = new HashSet<>();
            Set<Player> visibleNewCount = new HashSet<>();

            for (Player aPlayer : checked) {

                if (aPlayer == null || !aPlayer.isOnline() || !aPlayer.isValid()) continue;

                if (aPlayer != ninja) {
                    if (aPlayer.getWorld().equals(ninja.getWorld())) {
                        if (ninja.getLocation().distanceSquared(aPlayer.getLocation()) >= WATCH_DISTANCE_SQ) {
                            if (GeneralPlayerUtil.hide(ninja, aPlayer)) invisibleNewCount.add(aPlayer);
                        } else if (ninja.isSneaking()
                                && ninja.getLocation().distanceSquared(aPlayer.getLocation()) >=
                                SNEAK_WATCH_DISTANCE_SQ) {
                            if (GeneralPlayerUtil.hide(ninja, aPlayer)) invisibleNewCount.add(aPlayer);
                        } else if (GeneralPlayerUtil.show(ninja, aPlayer)) visibleNewCount.add(aPlayer);
                    } else GeneralPlayerUtil.show(ninja, aPlayer);
                }
            }

            if (invisibleNewCount.size() > 0) {
                if (invisibleNewCount.size() > 1) {
                    ChatUtil.sendNotice(ninja, "You are now invisible to multiple players.");
                } else {
                    for (Player aPlayer : invisibleNewCount) {
                        ChatUtil.sendNotice(ninja, "You are now invisible to " + aPlayer.getDisplayName() + ".");
                    }
                }
            }

            if (visibleNewCount.size() > 0) {
                if (visibleNewCount.size() > 1) {
                    ChatUtil.sendNotice(ninja, "You are now visible to multiple players.");
                } else {
                    for (Player aPlayer : visibleNewCount) {
                        ChatUtil.sendNotice(ninja, "You are now visible to " + aPlayer.getDisplayName() + ".");
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {

        Entity entity = event.getEntity();
        Entity targetEntity = event.getTarget();

        if (entity instanceof Player || !(targetEntity instanceof Player)) return;

        Player player = (Player) targetEntity;

        if (isActive(player) && player.isSneaking() && !player.getWorld().isThundering()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerRespawnEvent event) {

        for (Player aPlayer : hideable()) GeneralPlayerUtil.show(event.getPlayer(), aPlayer);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPitfallEvent(PitfallEvent event) {

        Entity entity = event.getCause();

        if (entity instanceof Player) {
            Player player = (Player) entity;

            if (isActive(player) && player.isSneaking()
                    && event.getNewTypeIdB() == BlockID.AIR && event.getNewTypeIdH() == BlockID.AIR) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        if (isActive(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
