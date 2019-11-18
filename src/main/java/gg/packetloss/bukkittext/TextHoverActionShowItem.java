package gg.packetloss.bukkittext;

import gg.packetloss.hackbook.ItemSerializer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.inventory.ItemStack;

class TextHoverActionShowItem  extends TextHoverAction {
    protected final ItemStack item;

    protected TextHoverActionShowItem(ItemStack item) {
        this.item = item;
    }

    @Override
    protected void apply(TextComponent component) {
        component.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_ITEM,
                new ComponentBuilder(ItemSerializer.toJSON(item)).create()
        ));
    }
}
