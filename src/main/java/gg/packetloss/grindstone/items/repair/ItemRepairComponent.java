/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.repair;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.custom.ItemFamily;
import gg.packetloss.grindstone.items.repair.listener.ItemBreakWarningListener;
import gg.packetloss.grindstone.items.repair.listener.SacrificeRepairsListener;
import gg.packetloss.grindstone.items.repair.profile.RepairProfile;
import gg.packetloss.grindstone.items.repair.profile.SacrificeItemRepairProfile;
import gg.packetloss.grindstone.items.repair.profile.SacrificeRepairProfile;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@ComponentInformation(friendlyName = "Item Repair Component", desc = "Fix custom items")
public class ItemRepairComponent extends BukkitComponent {
    private final List<RepairProfile> repairProfiles = new ArrayList<>();

    @Override
    public void enable() {
        setupWeapons();
        setupArmors();

        CommandBook.registerEvents(new ItemBreakWarningListener(repairProfiles));
        CommandBook.registerEvents(new SacrificeRepairsListener(repairProfiles));
    }

    private static final ItemStack IMBUED_CRYSTAL = CustomItemCenter.build(CustomItems.IMBUED_CRYSTAL);
    private static final ItemStack GEM_OF_DARKNESS = CustomItemCenter.build(CustomItems.GEM_OF_DARKNESS);
    private static final ItemStack BAMBOO = new ItemStack(Material.BAMBOO);

    private void registerSacrifice(ItemFamily itemFamily, float repairPercentage) {
        repairProfiles.add(new SacrificeRepairProfile(itemFamily, repairPercentage));
    }

    private void registerSacrifice(ItemFamily itemFamily, ItemStack repairItem, float repairPercentage) {
        repairProfiles.add(new SacrificeItemRepairProfile(itemFamily, repairItem, repairPercentage));
    }

    private void setupWeapons() {
        // Master Weapons
        registerSacrifice(ItemFamily.MASTER, .8F);

        // Unleashed Weapons
        registerSacrifice(ItemFamily.UNLEASHED, IMBUED_CRYSTAL, .06F);

        // Fear Weapons
        registerSacrifice(ItemFamily.FEAR, GEM_OF_DARKNESS, .06F);
    }

    private void setupArmors() {
        registerSacrifice(ItemFamily.PEACEFUL_WARRIOR, BAMBOO, 0.78125F);
    }
}
