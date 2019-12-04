/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

public class CustomItemCloneVisitor implements CustomItemVisitor {

    private CustomItem cloned = null;

    public void visit(CustomEquipment item) {
        cloned = new CustomEquipment(item);
    }

    public void visit(CustomItem item) {
        cloned = new CustomItem(item);
    }

    public void visit(CustomPotion item) {
        cloned = new CustomPotion(item);
    }

    public void visit(CustomWeapon item) {
        cloned = new CustomWeapon(item);
    }

    public CustomItem out() {
        return cloned;
    }
}
