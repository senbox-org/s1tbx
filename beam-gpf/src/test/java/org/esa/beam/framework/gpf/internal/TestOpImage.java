package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.util.HashMap;

class TestOpImage extends RasterDataNodeOpImage {

    private HashMap<Rectangle, TileImpl> gpfTiles = new HashMap<Rectangle, TileImpl>(4);

    public TestOpImage(Band band) {
        super(band);
    }

    public HashMap<Rectangle, TileImpl> getGpfTiles() {
        return gpfTiles;
    }

    @Override
    protected void computeRect(PlanarImage[] planarImages, WritableRaster writableRaster, Rectangle rectangle) {
        gpfTiles.put(rectangle, new TileImpl(getRasterDataNode(), writableRaster, rectangle));
    }
}
