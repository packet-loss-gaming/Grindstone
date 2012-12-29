package us.arrowcraft.aurora.anticheat;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
@Depend(plugins = {"NoCheatPlus"})
public class AntiCheatCompatibilityComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private ConcurrentHashMap<Player, ConcurrentHashMap<CheckType, Long>> playerList = new ConcurrentHashMap<>();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 20, 20 * 5);
    }

    @Override
    public void run() {

        for (Map.Entry<Player, ConcurrentHashMap<CheckType, Long>> e : playerList.entrySet()) {
            for (Map.Entry<CheckType, Long> p : e.getValue().entrySet()) {
                if (System.currentTimeMillis() - p.getValue() / TimeUnit.SECONDS.toMillis(1) > 1.75) {
                    NCPExemptionManager.unexempt(e.getKey(), p.getKey());
                    e.getValue().remove(p.getKey());
                }
            }
        }
    }

    public void bypass(Player player, CheckType[] checkTypes) {

        ConcurrentHashMap<CheckType, Long> hashMap;
        if (playerList.containsKey(player)) hashMap = playerList.get(player);
        else hashMap = new ConcurrentHashMap<>();

        for (CheckType checkType : checkTypes) {
            hashMap.put(checkType, System.currentTimeMillis());
            NCPExemptionManager.exemptPermanently(player, checkType);
        }
    }

    @EventHandler
    public void onJungleFallBlocker(JungleFallBlockerEvent event) {

        CheckType[] checkTypes = new CheckType[] {CheckType.MOVING_SURVIVALFLY, CheckType.MOVING_NOFALL};
        bypass(event.getPlayer(), checkTypes);
    }

    @EventHandler
    public void onFallBlocker(FallBlockerEvent event) {


        CheckType[] checkTypes = new CheckType[] {CheckType.MOVING_NOFALL};
        bypass(event.getPlayer(), checkTypes);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        List<CheckType> checkTypes = new ArrayList<>();
        switch (event.getPrayerType()) {
            case ALONZO:
            case ROCKET:
            case SLAP:
            case DOOM:
                checkTypes.add(CheckType.MOVING_SURVIVALFLY);
            case BUTTERFINGERS:
                checkTypes.add(CheckType.INVENTORY_DROP);
                break;
            default:
                return;
        }
        bypass(event.getPlayer(), checkTypes.toArray(new CheckType[checkTypes.size()]));
    }
}
