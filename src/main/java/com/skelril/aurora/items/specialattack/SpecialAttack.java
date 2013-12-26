package com.skelril.aurora.items.specialattack;

import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Created by wyatt on 12/26/13.
 */
public abstract class SpecialAttack {

    protected static final CommandBook inst = CommandBook.inst();
    protected static final Logger log = inst.getLogger();
    protected static final Server server = CommandBook.server();

    protected LivingEntity owner;

    public SpecialAttack(LivingEntity owner) {
        this.owner = owner;
    }

    public abstract void activate();

    public abstract LivingEntity getTarget();

    public abstract Location getLocation();

    public LivingEntity getOwner() {

        return owner;
    }

    protected void inform(String message) {

        if (owner instanceof Player) {
            ChatUtil.sendNotice((Player) owner, message);
        }
    }

    public long coolDown() {

        return 0;
    }
}
