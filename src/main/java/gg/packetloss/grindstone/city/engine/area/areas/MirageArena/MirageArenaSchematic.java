package gg.packetloss.grindstone.city.engine.area.areas.MirageArena;

import java.nio.file.Path;

public class MirageArenaSchematic {
    private final Path schematicFile;

    public MirageArenaSchematic(Path schematicFile) {
        this.schematicFile = schematicFile;
    }

    public Path getPath() {
        return schematicFile;
    }

    public String getArenaName() {
        return schematicFile.getParent().getFileName().toString().toUpperCase();
    }
}
