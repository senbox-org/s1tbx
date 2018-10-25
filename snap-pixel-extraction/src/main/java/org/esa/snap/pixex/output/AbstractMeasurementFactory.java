package org.esa.snap.pixex.output;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.measurement.Measurement;
import org.esa.snap.measurement.writer.MeasurementFactory;

public abstract class AbstractMeasurementFactory implements MeasurementFactory {

    protected static Measurement createMeasurement(Product product, long productId,
                                                   int coordinateID,
                                                   String coordinateName, Number[] values,
                                                   boolean isValid, int x, int y) {
        final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);
        final GeoCoding geoCoding = product.getSceneGeoCoding();
        final GeoPos currentGeoPos;
        if (geoCoding != null) {
            currentGeoPos = geoCoding.getGeoPos(pixelPos, null);
        } else {
            currentGeoPos = new GeoPos();
        }
        final ProductData.UTC scanLineTime = ProductUtils.getScanLineTime(product, pixelPos.y);
        return new Measurement(coordinateID, coordinateName, productId,
                               pixelPos.x, pixelPos.y, scanLineTime, currentGeoPos, values,
                               isValid);
    }

    protected static void setBandValues(Product product, String[] rasterNames, int x, int y, Number[] bandValues) {
        for (int i = 0; i < rasterNames.length; i++) {
            RasterDataNode raster = product.getRasterDataNode(rasterNames[i]);
            if (raster != null && product.containsPixel(x, y)) {
                if (pixelIsNotInBounds(raster, x, y) || !raster.isPixelValid(x, y)) {
                    bandValues[i] = Double.NaN;
                } else if (raster.isFloatingPointType()) {
                    bandValues[i] = (double) raster.getSampleFloat(x, y);
                } else {
                    int temp = raster.getSampleInt(x, y);
                    if (raster instanceof Mask) {
                        bandValues[i] = temp == 0 ? 0 : 1; // normalize to 0 for false and 1 for true
                    } else {
                        if (raster.getDataType() == ProductData.TYPE_UINT32) {
                            bandValues[i] = temp & 0xffffffffL;
                        } else {
                            bandValues[i] = temp;
                        }
                    }
                }
            }
        }
    }

    private static boolean pixelIsNotInBounds(RasterDataNode raster, int x, int y) {
        int height = raster.getRasterHeight();
        int width = raster.getRasterWidth();
        return x < 0 || x >= width || y < 0 || y >= height;
    }
}
