package gg.packetloss.bukkittext;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

class TextClickActionRunCommand extends TextClickAction {
    protected final String command;

    protected TextClickActionRunCommand(String command) {
        this.command = command;
    }

    @Override
    void apply(TextComponent component) {
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, this.command));
    }
}
