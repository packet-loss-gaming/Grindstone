/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.logging.Logger;

public abstract class SpecialAttack {
    protected static final CommandBook inst = CommandBook.inst();
    protected static final Logger log = inst.getLogger();
    protected static final Server server = CommandBook.server();

    protected LivingEntity owner;
    protected ItemStack usedItem;

    public SpecialAttack(LivingEntity owner, @Nullable ItemStack usedItem) {
        this.owner = owner;
        this.usedItem = usedItem;
    }

    public abstract void activate();

    public abstract LivingEntity getTarget();

    public abstract Location getLocation();

    public LivingEntity getOwner() {
        return owner;
    }

    public ItemStack getUsedItem() {
        return usedItem;
    }

    protected void inform(String message) {
        if (owner instanceof Player) {
            ChatUtil.sendNotice(owner, message);
        }
    }

    public long getCoolDown(SpecType context) {
        return context.getDelay();
    }
}
