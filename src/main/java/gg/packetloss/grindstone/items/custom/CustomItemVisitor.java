/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

public interface CustomItemVisitor {
    public void visit(CustomEquipment item);
    public void visit(CustomItem item);
    public void visit(CustomPotion item);
    public void visit(CustomWeapon item);
}
