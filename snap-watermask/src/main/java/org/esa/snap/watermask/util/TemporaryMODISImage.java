package org.esa.snap.watermask.util;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.image.ImageHeader;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.watermask.operator.WatermaskClassifier;

import javax.media.jai.JAI;
import javax.media.jai.SourcelessOpImage;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;

/**
 */
class TemporaryMODISImage extends SourcelessOpImage {

    private final Product[] products;
    private int count = 0;
    private Strategy strategy;

    public TemporaryMODISImage(ImageHeader imageHeader, Product[] products, int mode) {
        super(imageHeader.getImageLayout(),
              null,
              ImageUtils.createSingleBandedSampleModel(DataBuffer.TYPE_BYTE,
                                                       imageHeader.getImageLayout().getSampleModel(null).getWidth(),
                                                       imageHeader.getImageLayout().getSampleModel(null).getHeight()),
              imageHeader.getImageLayout().getMinX(null),
              imageHeader.getImageLayout().getMinY(null),
              imageHeader.getImageLayout().getWidth(null),
              imageHeader.getImageLayout().getHeight(null));
        this.products = products;
        if (mode == ModisMosaicer.NORTH_MODE) {
            strategy = new NorthStrategy();
        } else {
            strategy = new SouthStrategy();
        }
        setTileCache(JAI.createTileCache(50L * 1024 * 1024));
    }

    @Override
    public Raster computeTile(int tileX, int tileY) {
        count++;
        final int numTiles = getNumXTiles() * getNumYTiles();
        System.out.println("Writing tile '" + tileX + ", " + tileY + "', which is tile " + count + "/" + numTiles + ".");
        Point location = new Point(tileXToX(tileX), tileYToY(tileY));
        WritableRaster dest = createWritableRaster(getSampleModel(), location);
        final PixelPos pixelPos = new PixelPos();

        for (int x = dest.getMinX(); x < dest.getMinX() + dest.getWidth(); x++) {
            for (int y = dest.getMinY(); y < dest.getMinY() + dest.getHeight(); y++) {

                // fill invalid MODIS data areas with land
                if (strategy.isSourceInvalid(x, y)) {
                    dest.setSample(x, y, 0, WatermaskClassifier.LAND_VALUE);
                    continue;
                }

                int xOffset = strategy.xOffset(x);
                int yOffset = strategy.yOffset(y);

                dest.setSample(x, y, 0, WatermaskClassifier.WATER_VALUE);
                final GeoPos geoPos = strategy.getGeoPos(x + xOffset, y + yOffset);
                final Product[] products = getProducts(geoPos);
                for (Product product : products) {
                    product.getSceneGeoCoding().getPixelPos(geoPos, pixelPos);
                    final Band band = product.getBand("water_mask");
                    final MultiLevelImage sourceImage = band.getSourceImage();
                    final Raster tile = sourceImage.getTile(sourceImage.XToTileX((int) pixelPos.x), sourceImage.YToTileY((int) pixelPos.y));
                    final int sample = tile.getSample((int) pixelPos.x, (int) pixelPos.y, 0);
                    if (sample != band.getNoDataValue()) {
                        dest.setSample(x, y, 0, sample);
                        break;
                    }
                }
            }
        }

        return dest;
    }

    private Product[] getProducts(GeoPos geoPos) {
        final java.util.List<Product> result = new ArrayList<Product>();
        for (Product product : products) {
            final PixelPos pixelPos = product.getSceneGeoCoding().getPixelPos(geoPos, null);
            if (pixelPos.isValid() &&
                    pixelPos.x > 0 &&
                    pixelPos.x < product.getSceneRasterWidth() &&
                    pixelPos.y > 0 &&
                    pixelPos.y < product.getSceneRasterHeight()) {
                result.add(product);
            }
        }
        return result.toArray(new Product[result.size()]);
    }

    private interface Strategy {

        GeoPos getGeoPos(int x, int y);

        int xOffset(int x);

        int yOffset(int y);

        boolean isSourceInvalid(int x, int y);
    }

    private class SouthStrategy implements Strategy {

        public int xOffset(int x) {
            int xOffset = 0;
            if (x == 77758) {
                xOffset = -1;
            } else if (x == 77759) {
                xOffset = -2;
            } else if (x == 77760) {
                xOffset = 1;
            }
            return xOffset;
        }

        public int yOffset(int y) {
            int yOffset = 0;
            if (y == 4286 || y == 8601) {
                yOffset = -1;
            }
            return yOffset;
        }

        public boolean isSourceInvalid(int x, int y) {
            return y > 10860 || (y > 10473 && x > 154127) || (y > 10486 && x < 1436)
                    || (y > 10491 && x > 153820)
                    || (y > 10548 && x > 1432 && x < 1675);
        }

        public GeoPos getGeoPos(int x, int y) {
            final double pixelSizeX = 360.0 / ModisMosaicer.MODIS_IMAGE_WIDTH;
            final double pixelSizeY = -30.0 / ModisMosaicer.MODIS_IMAGE_HEIGHT;
            double lon = -180.0 + x * pixelSizeX;
            double lat = -60.0 + y * pixelSizeY;
            return new GeoPos((float) lat, (float) lon);
        }

    }

    private class NorthStrategy implements Strategy {

        public int xOffset(int x) {
            return 0;
        }

        public int yOffset(int y) {
            return 0;
        }

        public boolean isSourceInvalid(int x, int y) {
            return false;
        }

        public GeoPos getGeoPos(int x, int y) {
            final double pixelSizeX = 360.0 / ModisMosaicer.MODIS_IMAGE_WIDTH;
            final double pixelSizeY = 30.0 / ModisMosaicer.MODIS_IMAGE_HEIGHT;
            double lon = -180.0 + x * pixelSizeX;
            double lat = -90.0 + y * pixelSizeY;
            return new GeoPos((float) lat, (float) lon);
        }
    }
}
