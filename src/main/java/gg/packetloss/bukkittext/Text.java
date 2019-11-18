package gg.packetloss.bukkittext;

public abstract class Text implements BukkitTextConvertible {
    protected Text() { }

    public static Text of(Object... objects) {
        TextBase text = new TextBase();
        text.append(objects);
        return text;
    }

    public static TextBuilder builder() {
        return new TextBase();
    }
}
