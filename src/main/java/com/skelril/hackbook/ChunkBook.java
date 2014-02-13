package com.skelril.hackbook;

import com.skelril.hackbook.exceptions.UnsupportedFeatureException;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.v1_7_R1.CraftChunk;

/**
 * Created by Wyatt on 2/13/14.
 */
public class ChunkBook {

    public static void relight(Chunk chunk) throws UnsupportedFeatureException {

        try {
            ((CraftChunk) chunk).getHandle().initLighting();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new UnsupportedFeatureException();
        }
    }
}
