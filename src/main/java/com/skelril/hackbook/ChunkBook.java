/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.hackbook;

import com.skelril.hackbook.exceptions.UnsupportedFeatureException;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.v1_7_R4.CraftChunk;

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
