package com.skelril.aurora.ic;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.craftbook.bukkit.CircuitCore;
import com.skelril.aurora.ic.ics.GroupSentryGun;
import com.skelril.aurora.ic.ics.NinjaStarSpawner;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;

import java.util.logging.Logger;

/**
 * Created by wyatt on 12/15/13.
 */
@ComponentInformation(friendlyName = "IC Component", desc = "Custom ICs!")
@Depend(plugins = "CraftBook")
public class CustomICComponent extends BukkitComponent {

    private static final CommandBook inst = CommandBook.inst();
    private static final Logger log = inst.getLogger();
    private static final Server server = CommandBook.server();

    private CircuitCore ICCore;

    @Override
    public void enable() {

        server.getScheduler().runTaskLater(inst, new Runnable() {
            @Override
            public void run() {
                ICCore = CircuitCore.inst();
                if (ICCore == null) {
                    log.warning("ICCore is null!");
                }
                registerICs();
            }
        }, 1);

    }

    @SuppressWarnings("AccessStaticViaInstance")
    private void registerICs() {

        ICCore.registerIC("SK1278", "group sentry", new GroupSentryGun.Factory(server), ICCore.FAMILY_SISO, ICCore.FAMILY_AISO);
        ICCore.registerIC("SK9001", "star spawner", new NinjaStarSpawner.Factory(server), ICCore.FAMILY_SI5O, ICCore.FAMILY_AISO);
    }
}
