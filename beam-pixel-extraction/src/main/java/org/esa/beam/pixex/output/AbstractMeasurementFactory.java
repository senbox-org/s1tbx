package org.esa.beam.pixex.output;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.measurement.Measurement;
import org.esa.beam.measurement.writer.MeasurementFactory;
import org.esa.beam.util.ProductUtils;

import java.awt.image.Raster;

public abstract class AbstractMeasurementFactory implements MeasurementFactory {

    protected static Measurement createMeasurement(Product product, long productId,
                                                   int coordinateID,
                                                   String coordinateName, Number[] values,
                                                   Raster validData, int x, int y) {
        final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);
        final GeoCoding geoCoding = product.getGeoCoding();
        final GeoPos currentGeoPos;
        if (geoCoding != null) {
            currentGeoPos = geoCoding.getGeoPos(pixelPos, null);
        } else {
            currentGeoPos = new GeoPos();
        }
        final boolean isValid = validData == null || validData.getSample(x, y, 0) != 0;
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
                            bandValues[i] = temp & 0xffffL;
                        } else {
                            bandValues[i] = temp;
                        }
                    }
                }
            }
        }
    }

    private static boolean pixelIsNotInBounds(RasterDataNode raster, int x, int y) {
        int height = raster.getSceneRasterHeight();
        int width = raster.getSceneRasterWidth();
        return x < 0 || x >= width || y < 0 || y >= height;
    }
}
