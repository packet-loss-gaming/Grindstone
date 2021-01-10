/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.arena.factory;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SmeltableResource {
    private final Material input;
    private final Material output;
    private final int outputQuantity;

    public SmeltableResource(Material input, Material output, int outputQuantity) {
        this.input = input;
        this.output = output;
        this.outputQuantity = outputQuantity;
    }

    public boolean matches(ItemStack input) {
        return this.input == input.getType();
    }

    public Material getInputMaterial() {
        return input;
    }

    public Material getOutputMaterial() {
        return output;
    }

    public int getOutputQuantity() {
        return outputQuantity;
    }
}
