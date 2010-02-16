package com.bc.ceres.jai.tilecache;

import java.awt.image.RenderedImage;

public interface SwapSpace {

    boolean storeTile(MemoryTile memoryTile);

    MemoryTile restoreTile(RenderedImage owner, int tileX, int tileY);

    boolean deleteTile(RenderedImage owner, int tileX, int tileY);
}
