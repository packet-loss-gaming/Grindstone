package gg.packetloss.grindstone.world.type.city.area.areas.MirageArena;

import java.nio.file.Path;

public class MirageArenaSchematic {
    private final Path schematicFile;
    private final String arenaName;

    public MirageArenaSchematic(Path schematicFile) {
        this.schematicFile = schematicFile;
        this.arenaName = schematicFile.getParent().getFileName().toString().toUpperCase();
    }

    public Path getPath() {
        return schematicFile;
    }

    public String getArenaName() {
        return arenaName;
    }

    @Override
    public int hashCode() {
        return arenaName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MirageArenaSchematic)) {
            return false;
        }

        return arenaName.equals(((MirageArenaSchematic) o).arenaName);
    }
}
