package gg.packetloss.grindstone.city.engine.area.areas.MirageArena;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;

class MirageEditorState {
    private final MirageArenaSchematic schematic;
    private final EditSession editor;
    private final Clipboard clipboard;

    private final BlockVector3 dimensions;

    private int x;
    private int y;
    private int z;

    public MirageEditorState(MirageArenaSchematic schematic, EditSession editor, Clipboard clipboard) {
        this.schematic = schematic;
        this.editor = editor;
        this.clipboard = clipboard;

        this.dimensions = clipboard.getDimensions();

        this.x = getMaxX();
        this.y = getMaxY();
        this.z = getMaxZ();
    }

    public MirageArenaSchematic getSchematic() {
        return schematic;
    }

    public EditSession getSession() {
        return editor;
    }

    public Clipboard getClipboard() {
        return clipboard;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public int getMaxX() {
        return dimensions.getX();
    }

    public int getMaxY() {
        return dimensions.getY();
    }

    public int getMaxZ() {
        return dimensions.getZ();
    }
}
