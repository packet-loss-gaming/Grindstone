package gg.packetloss.bukkittext;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TextBase extends Text implements TextStreamPart, TextBuilder {
    protected TextClickAction clickAction = null;
    protected TextHoverAction hoverAction = null;
    protected List<TextStreamPart> messageParts = new ArrayList<>();

    protected TextBase() {
        super();
    }

    private void append(Object obj) {
        if (obj instanceof TextStreamPart) {
            messageParts.add((TextStreamPart) obj);
        } else if (obj instanceof TextClickAction) {
            clickAction = (TextClickAction) obj;
        } else if (obj instanceof TextHoverAction) {
            hoverAction = (TextHoverAction) obj;
        } else if (obj instanceof ChatColor) {
            messageParts.add(new TextChatColor((ChatColor) obj));
        } else if (obj instanceof Iterable) {
            append((Iterable) obj);
        } else {
            messageParts.add(new TextString(obj.toString()));
        }
    }

    private void append(Iterable<Object> objects) {
        for (Object obj : objects) {
            append(obj);
        }
    }

    @Override
    public TextBuilder append(Object... objects) {
        append(Arrays.asList(objects));
        return this;
    }

    public BaseComponent[] build() {
        TextComponent resultComponent = new TextComponent();

        if (clickAction != null) {
            clickAction.apply(resultComponent);
        }
        if (hoverAction != null) {
            hoverAction.apply(resultComponent);
        }

        ChatColor curColor = ChatColor.WHITE;
        for (TextStreamPart part : messageParts) {
            if (part instanceof TextChatColor) {
                // If we've got a color update the color for subsequent string parts.
                curColor = ((TextChatColor) part).color;
            } else if (part instanceof TextString) {
                // Build the string part with the specified color.
                TextComponent component = new TextComponent(((TextString) part).message);
                component.setColor(net.md_5.bungee.api.ChatColor.getByChar(curColor.getChar()));
                resultComponent.addExtra(component);
            } else if (part instanceof TextBase) {
                // Add the parts built by the sub text.
                for (BaseComponent baseComponent : ((TextBase) part).build()) {
                    resultComponent.addExtra(baseComponent);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported text stream part.");
            }
        }

        return new BaseComponent[] { resultComponent };
    }
}
