package us.arrowcraft.aurora.anticheat;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import net.h31ix.anticheat.api.AnticheatAPI;
import net.h31ix.anticheat.manage.CheckType;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import us.arrowcraft.aurora.events.FallBlockerEvent;
import us.arrowcraft.aurora.events.JungleFallBlockerEvent;
import us.arrowcraft.aurora.events.PrayerApplicationEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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

    private ConcurrentHashMap<String, ConcurrentHashMap<CheckType, Long>> playerList = new ConcurrentHashMap<>();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 20, 20 * 5);
    }

    @Override
    public void run() {

        for (Map.Entry<String, ConcurrentHashMap<CheckType, Long>> e : playerList.entrySet()) {
            Player player = Bukkit.getPlayerExact(e.getKey());
            if (player == null) continue;
            for (Map.Entry<CheckType, Long> p : e.getValue().entrySet()) {
                if (System.currentTimeMillis() - p.getValue() / TimeUnit.SECONDS.toMillis(1) > 1.75) {
                    AnticheatAPI.unexemptPlayer(player, p.getKey());
                    e.getValue().remove(p.getKey());
                }
            }
        }
    }

    public void bypass(Player player, CheckType[] checkTypes) {

        ConcurrentHashMap<CheckType, Long> hashMap;
        if (playerList.containsKey(player.getName())) hashMap = playerList.get(player.getName());
        else hashMap = new ConcurrentHashMap<>();

        for (CheckType checkType : checkTypes) {
            hashMap.put(checkType, System.currentTimeMillis());
            AnticheatAPI.exemptPlayer(player, checkType);
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

    @EventHandler
    public void onJungleFallBlocker(JungleFallBlockerEvent event) {

        CheckType[] checkTypes = new CheckType[] {CheckType.FLY, CheckType.ZOMBE_FLY, CheckType.NOFALL};
        bypass(event.getPlayer(), checkTypes);
    }

    @EventHandler
    public void onFallBlocker(FallBlockerEvent event) {


        CheckType[] checkTypes = new CheckType[] {CheckType.NOFALL};
        bypass(event.getPlayer(), checkTypes);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        List<CheckType> checkTypes = new ArrayList<>();
        switch (event.getCause().getEffect().getType()) {
            case ALONZO:
            case ROCKET:
            case SLAP:
            case DOOM:
                checkTypes.add(CheckType.FLY);
                checkTypes.add(CheckType.ZOMBE_FLY);
            case BUTTERFINGERS:
                checkTypes.add(CheckType.ITEM_SPAM);
                break;
            default:
                return;
        }
        bypass(event.getPlayer(), checkTypes.toArray(new CheckType[checkTypes.size()]));
    }
}
