package gg.packetloss.bukkittext;

class TextString implements TextStreamPart {
    protected String message;

    protected TextString(String message) {
        this.message = message;
    }
}
