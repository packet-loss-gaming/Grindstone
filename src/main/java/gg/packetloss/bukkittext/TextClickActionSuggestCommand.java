package gg.packetloss.bukkittext;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

class TextClickActionSuggestCommand extends TextClickAction {
    protected final String command;

    protected TextClickActionSuggestCommand(String command) {
        this.command = command;
    }

    @Override
    protected void apply(TextComponent component) {
        component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, this.command));
    }
}
