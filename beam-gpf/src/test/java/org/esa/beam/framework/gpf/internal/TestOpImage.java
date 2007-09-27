package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.util.HashMap;

class TestOpImage extends RasterDataNodeOpImage {

    private HashMap<Rectangle, TileImpl> tileImplMap = new HashMap<Rectangle, TileImpl>(4);

    public TestOpImage(Band band) {
        super(band);
    }

    public int getNumTileImpls() {
        return tileImplMap.size();
    }

    public TileImpl getTileImpl(Rectangle rect) {
        return tileImplMap.get(rect);
    }

    @Override
    protected void computeRect(PlanarImage[] planarImages, WritableRaster writableRaster, Rectangle rectangle) {
        int x1 = writableRaster.getMinX();
        int x2 = writableRaster.getMinX() + writableRaster.getWidth() - 1;
        int y1 = writableRaster.getMinY();
        int y2 = writableRaster.getMinY() + writableRaster.getHeight() - 1;
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                double sample = getSampleDouble(x, y);
                writableRaster.setSample(x, y, 0, sample);
            }
        }
        tileImplMap.put(rectangle, new TileImpl(getRasterDataNode(), writableRaster, rectangle));
    }

    public static double getSampleDouble(int x, int y) {
        return 10.0 * x + y + 0.5;
    }
}
