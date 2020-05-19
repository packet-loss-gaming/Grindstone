package gg.packetloss.grindstone.admin;

import gg.packetloss.grindstone.items.custom.CustomItems;

import java.util.List;

public class CustomItemBundle {
    private List<CustomItems> items;

    public CustomItemBundle(List<CustomItems> items) {
        this.items = items;
    }

    public List<CustomItems> getItems() {
        return items;
    }
}
