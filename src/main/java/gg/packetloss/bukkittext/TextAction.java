package gg.packetloss.bukkittext;

import net.md_5.bungee.api.chat.TextComponent;

public abstract class TextAction {
    protected TextAction() { }

    abstract void apply(TextComponent component);

    public static class Click {
        private Click() { }

        public static TextAction runCommand(String command) {
            return new TextClickActionRunCommand(command);
        }
        public static TextAction suggestCommand(String command) {
            return new TextClickActionSuggestCommand(command);
        }
    }

    public static class Hover {
        private Hover() { }

        public static TextAction showText(Text text) {
            return new TextHoverActionShowText(text);
        }
    }
}
