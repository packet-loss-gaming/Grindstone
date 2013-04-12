package com.skelril.aurora.anticheat;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.skelril.aurora.jail.JailComponent;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Suspicious Activity", desc = "Find bugs and abusers")
public class SuspiciousActivityComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    JailComponent jail;

    private ConcurrentHashMap<String, Profile> profileMap = new ConcurrentHashMap<>();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        //inst.registerEvents(this);

        //server.getScheduler().runTaskTimer(inst, this, 20 * 15, 20 * 15);
    }

    @Override
    public void run() {

        for (Profile profile : profileMap.values()) {

            if (!profile.poll()) {
                if (profile.isMajor()) {
                    jail.jail(profile.getPlayer(), TimeUnit.MINUTES.toMillis(15));
                    server.broadcastMessage(ChatColor.RED + profile.getPlayer() + " jailed for hacking!");
                    //noinspection StatementWithEmptyBody
                    while (!profile.poll());
                } else {
                    for (Player player : server.getOnlinePlayers()) {

                        if (inst.hasPermission(player, "aurora.alarm.mining")) {

                            ChatUtil.sendWarning(player, "[WARNING] Suspicious activity has been detected, "
                                    + "player: " + profile.getPlayer() + ".");
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {

        String player = event.getPlayer().getName();

        profileMap.put(player, new Profile(player));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        String player = event.getPlayer().getName();

        if (profileMap.containsKey(player)) profileMap.remove(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        int type = event.getBlock().getTypeId();
        if (event.getBlock().getWorld().getName().contains("Wilderness")) {

            if (!watched.contains(type)) return;

            String player = event.getPlayer().getName();
            Profile playerProfile = profileMap.get(player);
            playerProfile.add(type, 1);
        }
    }

    private static List<Integer> watched = new ArrayList<>();

    static {
        watched.add(BlockID.DIAMOND_ORE);
        watched.add(BlockID.GOLD_ORE);

        watched.add(BlockID.LAPIS_LAZULI_ORE);
    }

    private static int DIAMOND_CAP = 14;
    private static int GOLD_CAP = 24;

    private static int LAPIS_CAP = 14;

    private class Profile {

        private final String player;

        private int diamond = 0;
        private int gold = 0;

        private int lapis = 0;

        public Profile(String player)  {

            this.player = player;
        }

        public String getPlayer() {

            return player;
        }

        public void add(int id, int amt) {

            switch (id) {
                case BlockID.DIAMOND_ORE:
                    diamond += amt;
                    break;
                case BlockID.GOLD_ORE:
                    gold += amt;
                    break;
                case BlockID.LAPIS_LAZULI_ORE:
                    lapis += amt;
                    break;
            }
        }

        public boolean poll() {

            diamond--;
            gold--;
            lapis--;

            return diamond < (DIAMOND_CAP / 2) && gold < (GOLD_CAP / 2) && lapis < (LAPIS_CAP / 2);
        }

        public boolean isMajor() {

            return diamond >= DIAMOND_CAP || gold >= GOLD_CAP || lapis >= LAPIS_CAP;
        }
    }
}