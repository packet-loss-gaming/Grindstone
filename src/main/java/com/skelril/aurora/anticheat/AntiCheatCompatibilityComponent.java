package com.skelril.aurora.anticheat;

import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.events.FallBlockerEvent;
import com.skelril.aurora.events.PrayerApplicationEvent;
import com.skelril.aurora.events.ThrowPlayerEvent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import net.h31ix.anticheat.api.AnticheatAPI;
import net.h31ix.anticheat.manage.CheckType;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Anit-Cheat Compat", desc = "Compatibility layer for Anti-Cheat plugins.")
@Depend(plugins = {"AntiCheat"})
public class AntiCheatCompatibilityComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private LocalConfiguration config;
    private ConcurrentHashMap<String, ConcurrentHashMap<CheckType, Long>> playerList = new ConcurrentHashMap<>();

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 20, 20 * 5);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("removal-delay")
        public int removalDelay = 5;
    }

    @Override
    public void run() {

        for (Map.Entry<String, ConcurrentHashMap<CheckType, Long>> e : playerList.entrySet()) {

            Player player = Bukkit.getPlayerExact(e.getKey());
            if (player == null) {
                playerList.remove(e.getKey());
                continue;
            }

            for (Map.Entry<CheckType, Long> p : e.getValue().entrySet()) {
                if (System.currentTimeMillis() - p.getValue() / 1000 > config.removalDelay) {
                    unexempt(player, p.getKey());
                    e.getValue().remove(p.getKey());
                }
            }
        }
    }

    public void exempt(Player player, CheckType checkType) {

        AnticheatAPI.exemptPlayer(player, checkType);
    }

    public void unexempt(Player player, CheckType checkType) {

        AnticheatAPI.unexemptPlayer(player, checkType);
    }

    public void bypass(Player player, CheckType[] checkTypes) {

        ConcurrentHashMap<CheckType, Long> hashMap;
        if (playerList.containsKey(player.getName())) hashMap = playerList.get(player.getName());
        else hashMap = new ConcurrentHashMap<>();

        for (CheckType checkType : checkTypes) {
            if (AnticheatAPI.isExempt(player, checkType) && !hashMap.containsKey(checkType)) continue;
            hashMap.put(checkType, System.currentTimeMillis());
            exempt(player, checkType);
        }
        playerList.put(player.getName(), hashMap);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();
        if (playerList.containsKey(player.getName())) {
            for (Map.Entry<CheckType, Long> e : playerList.get(player.getName()).entrySet()) {
                AnticheatAPI.unexemptPlayer(player, e.getKey());
            }
            playerList.remove(player.getName());
        }
    }

    private static final CheckType[] playerThrowCheckTypes = new CheckType[] {
            CheckType.FLY, CheckType.ZOMBE_FLY, CheckType.SPEED
    };
    private static final CheckType[] fallBlockerCheckTypes = new CheckType[] {CheckType.NOFALL};

    @EventHandler
    public void onPlayerThrow(ThrowPlayerEvent event) {

        bypass(event.getPlayer(), playerThrowCheckTypes);
    }

    @EventHandler
    public void onFallBlocker(FallBlockerEvent event) {

        bypass(event.getPlayer(), fallBlockerCheckTypes);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        List<CheckType> checkTypes = new ArrayList<>();
        switch (event.getCause().getEffect().getType()) {
            case ROCKET:
            case SLAP:
            case DOOM:
                checkTypes.add(CheckType.FLY);
                checkTypes.add(CheckType.ZOMBE_FLY);
                checkTypes.add(CheckType.SPEED);
            case ALONZO:
            case BUTTERFINGERS:
                checkTypes.add(CheckType.ITEM_SPAM);
                break;
            default:
                return;
        }
        bypass(event.getPlayer(), checkTypes.toArray(new CheckType[checkTypes.size()]));
    }
}
