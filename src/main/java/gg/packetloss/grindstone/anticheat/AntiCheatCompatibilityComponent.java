/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.anticheat;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;

import java.util.logging.Logger;


@ComponentInformation(friendlyName = "Anit-Cheat Compat", desc = "Compatibility layer for Anti-Cheat plugins.")
@Depend(plugins = {"AntiCheat"})
public class AntiCheatCompatibilityComponent extends BukkitComponent /* implements Listener, Runnable*/ {

  private final CommandBook inst = CommandBook.inst();
  private final Logger log = inst.getLogger();
  private final Server server = CommandBook.server();

  //private LocalConfiguration config;
  //private ConcurrentHashMap<String, ConcurrentHashMap<CheckType, Long>> playerList = new ConcurrentHashMap<>();

  @Override
  public void enable() {

        /*
        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 20, 20 * 5);
        */
  }

    /*

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("removal-delay")
        public int removalDelay = 3;
    }

    @Override
    public void run() {

        for (Map.Entry<String, ConcurrentHashMap<CheckType, Long>> e : playerList.entrySet()) {

            Player player = Bukkit.getPlayerExact(e.getKey());
            if (player == null) {
                playerList.remove(e.getKey());
                continue;
            }

            e.getValue().entrySet().stream().filter(p -> (System.currentTimeMillis() - p.getValue()) / 1000 > config.removalDelay).forEach(p -> {
                unexempt(player, p.getKey());
                e.getValue().remove(p.getKey());
            });
        }
    }

    public void exempt(Player player, CheckType checkType) {

        AntiCheatAPI.exemptPlayer(player, checkType);
    }

    public void unexempt(Player player, CheckType checkType) {

        AntiCheatAPI.unexemptPlayer(player, checkType);
    }

    public void bypass(Player player, CheckType[] checkTypes) {

        ConcurrentHashMap<CheckType, Long> hashMap;
        if (playerList.containsKey(player.getName())) hashMap = playerList.get(player.getName());
        else hashMap = new ConcurrentHashMap<>();

        for (CheckType checkType : checkTypes) {
            if (AntiCheatAPI.isExempt(player, checkType) && !hashMap.containsKey(checkType)) continue;
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
                AntiCheatAPI.unexemptPlayer(player, e.getKey());
            }
            playerList.remove(player.getName());
        }
    }

    private static final CheckType[] playerThrowCheckTypes = new CheckType[]{
            CheckType.FLY, CheckType.ZOMBE_FLY, CheckType.SPEED, CheckType.SNEAK, CheckType.SPIDER,
            CheckType.WATER_WALK
    };
    private static final CheckType[] fallBlockerCheckTypes = new CheckType[]{CheckType.NOFALL};
    private static final CheckType[] rapidHitCheckTypes = new CheckType[]{
            CheckType.NO_SWING, CheckType.FORCEFIELD, CheckType.LONG_REACH, CheckType.AUTOTOOL
    };

    @EventHandler
    public void onPlayerThrow(ThrowPlayerEvent event) {

        bypass(event.getPlayer(), playerThrowCheckTypes);
    }

    @EventHandler
    public void onFallBlocker(FallBlockerEvent event) {

        bypass(event.getPlayer(), fallBlockerCheckTypes);
    }

    @EventHandler
    public void onRapidHit(RapidHitEvent event) {

        bypass(event.getPlayer(), rapidHitCheckTypes);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        List<CheckType> checkTypes = new ArrayList<>();
        switch (event.getCause().getEffect().getType()) {
            case TNT:
            case ROCKET:
            case SLAP:
            case DOOM:
                checkTypes.add(CheckType.FLY);
                checkTypes.add(CheckType.ZOMBE_FLY);
                checkTypes.add(CheckType.SPEED);
                checkTypes.add(CheckType.SNEAK);
                checkTypes.add(CheckType.SPIDER);
            case MERLIN:
            case BUTTERFINGERS:
                checkTypes.add(CheckType.ITEM_SPAM);
                break;
            default:
                return;
        }
        bypass(event.getPlayer(), checkTypes.toArray(new CheckType[checkTypes.size()]));
    }
    */
}
