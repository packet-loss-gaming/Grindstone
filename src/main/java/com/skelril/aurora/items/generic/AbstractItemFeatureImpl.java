/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.generic;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.SessionComponent;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.custom.item.SpecialAttackEvent;
import com.skelril.aurora.items.CustomItemSession;
import com.skelril.aurora.items.specialattack.SpecType;
import com.skelril.aurora.items.specialattack.SpecialAttack;
import com.skelril.aurora.prayer.PrayerComponent;
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

    protected SpecialAttackEvent callSpec(Player owner, SpecType context, SpecialAttack spec) {
        SpecialAttackEvent event = new SpecialAttackEvent(owner, context, spec);
        server.getPluginManager().callEvent(event);
        return event;
    }
}
