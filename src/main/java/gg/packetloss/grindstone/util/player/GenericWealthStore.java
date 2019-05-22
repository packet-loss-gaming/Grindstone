/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.player;

import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.item.itemstack.SerializableItemStack;
import org.apache.commons.lang.Validate;
import org.bukkit.inventory.ItemStack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class GenericWealthStore implements Serializable {

  private String ownerName;
  // For Serialization
  private SerializableItemStack[] armourContents = null;
  private SerializableItemStack[] inventoryContents = null;
  // For Usage
  private transient ItemStack[] cacheArmourContents = null;
  private transient ItemStack[] cacheInventoryContents = null;
  private transient List<ItemStack> itemStacks = new ArrayList<>();
  private int value = 0;

  public GenericWealthStore(String ownerName, ItemStack[] inventoryContents) {

    Validate.notNull(inventoryContents);

    this.ownerName = ownerName;
    this.inventoryContents = ItemUtil.serialize(inventoryContents);
  }

  public GenericWealthStore(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents) {

    Validate.notNull(inventoryContents);
    Validate.notNull(armourContents);

    this.ownerName = ownerName;
    this.inventoryContents = ItemUtil.serialize(inventoryContents);
    this.armourContents = ItemUtil.serialize(armourContents);
  }

  public GenericWealthStore(String ownerName, List<ItemStack> itemStacks) {

    this.ownerName = ownerName;
    this.itemStacks = itemStacks;
  }

  public GenericWealthStore(String ownerName, List<ItemStack> itemStacks, int value) {

    this.ownerName = ownerName;
    this.itemStacks = itemStacks;
    this.value = value;
  }

  public GenericWealthStore(String ownerName, int value) {

    this.ownerName = ownerName;
    this.value = value;
  }

  public String getOwnerName() {

    return ownerName;
  }

  public void setOwnerName(String ownerName) {

    this.ownerName = ownerName;
  }

  public ItemStack[] getArmourContents() {

    if (cacheArmourContents == null) {
      cacheArmourContents = ItemUtil.unSerialize(armourContents);
    }
    return ItemUtil.clone(cacheArmourContents);
  }

  public void setArmourContents(ItemStack[] armourContents) {

    Validate.notNull(armourContents);

    this.armourContents = ItemUtil.serialize(armourContents);
  }

  public ItemStack[] getInventoryContents() {

    if (cacheInventoryContents == null) {
      cacheInventoryContents = ItemUtil.unSerialize(inventoryContents);
    }
    return ItemUtil.clone(cacheInventoryContents);
  }

  public void setInventoryContents(ItemStack[] inventoryContents) {

    Validate.notNull(inventoryContents);

    this.inventoryContents = ItemUtil.serialize(inventoryContents);
  }

  public List<ItemStack> getItemStacks() {

    return itemStacks;
  }

  public void setItemStacks(List<ItemStack> itemStacks) {

    this.itemStacks = itemStacks;
  }

  public int getValue() {

    return value;
  }

  public void setValue(int value) {

    this.value = value;
  }

}
