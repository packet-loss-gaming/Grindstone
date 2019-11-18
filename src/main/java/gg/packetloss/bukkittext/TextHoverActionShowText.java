package gg.packetloss.bukkittext;

import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

class TextHoverActionShowText extends TextHoverAction {
    protected final Text text;

    protected TextHoverActionShowText(Text text) {
        this.text = text;
    }

    @Override
    protected void apply(TextComponent component) {
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text.build()));
    }
}
