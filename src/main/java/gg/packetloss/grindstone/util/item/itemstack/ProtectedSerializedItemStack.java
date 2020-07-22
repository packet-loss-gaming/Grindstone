/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item.itemstack;

import java.io.Serializable;
import java.util.UUID;

public class ProtectedSerializedItemStack implements Serializable {
    private final UUID player;
    private final long additionDate;
    private final SerializableItemStack itemStack;

    public ProtectedSerializedItemStack(UUID player, long additionDate, SerializableItemStack itemStack) {
        this.player = player;
        this.additionDate = additionDate;
        this.itemStack = itemStack;
    }

    public UUID getPlayer() {
        return player;
    }

    public long getAdditionDate() {
        return additionDate;
    }

    public SerializableItemStack getItemStack() {
        return itemStack;
    }
}
