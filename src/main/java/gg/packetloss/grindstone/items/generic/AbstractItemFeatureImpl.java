/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.generic;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.SessionComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.items.CustomItemSession;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.logging.Logger;

public abstract class AbstractItemFeatureImpl implements Listener {

    protected final CommandBook inst = CommandBook.inst();
    protected final Logger log = CommandBook.logger();
    protected final Server server = CommandBook.server();

    protected static AdminComponent admin;
    protected static SessionComponent sessions;
    protected static PrayerComponent prayers;

    public static void applyResource(AdminComponent admin) {
        AbstractItemFeatureImpl.admin = admin;
    }
    public static void applyResource(SessionComponent sessions) {
        AbstractItemFeatureImpl.sessions = sessions;
    }
    public static void applyResource(PrayerComponent prayers) {
        AbstractItemFeatureImpl.prayers = prayers;
    }

    public CustomItemSession getSession(Player player) {
        return sessions.getSession(CustomItemSession.class, player);
    }

    // Use SpecialAttackFactory
    @Deprecated
    protected SpecialAttackEvent callSpec(Player owner, SpecType context, SpecialAttack spec) {
        SpecialAttackEvent event = new SpecialAttackEvent(owner, context, spec);
        server.getPluginManager().callEvent(event);
        return event;
    }
}
