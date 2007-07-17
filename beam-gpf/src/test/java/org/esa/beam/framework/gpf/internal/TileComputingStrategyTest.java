package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.awt.Rectangle;


public class TileComputingStrategyTest extends TestCase {


    public void testMethodDetection() {
        assertTrue(new TileComputingStrategy.ImplementationInfo(Op2.class).isBandMethodImplemented());
        assertTrue(new TileComputingStrategy.ImplementationInfo(Op1.class).isBandMethodImplemented());
        assertFalse(new TileComputingStrategy.ImplementationInfo(AbstractOperator.class).isBandMethodImplemented());
        assertFalse(new TileComputingStrategy.ImplementationInfo(Operator.class).isBandMethodImplemented());
        assertFalse(new TileComputingStrategy.ImplementationInfo(String.class).isBandMethodImplemented());

        assertTrue(new TileComputingStrategy.ImplementationInfo((Op2.class)).isAllBandsMethodImplemented());
        assertFalse(new TileComputingStrategy.ImplementationInfo((Op1.class)).isAllBandsMethodImplemented());
        assertFalse(new TileComputingStrategy.ImplementationInfo((AbstractOperator.class)).isAllBandsMethodImplemented());
        assertFalse(new TileComputingStrategy.ImplementationInfo((Operator.class)).isAllBandsMethodImplemented());
        assertFalse(new TileComputingStrategy.ImplementationInfo((String.class)).isAllBandsMethodImplemented());
    }

    private static abstract class Op1 extends AbstractOperator {

        protected Op1(OperatorSpi spi) {
            super(spi);
        }

        @Override
        public void computeBand(Raster targetRaster, ProgressMonitor pm) throws OperatorException {
            // used to test GraphProcessor.overridesComputeTileMethod
        }
    }

    // todo - this calls may serve as a template for new Operator implementations (Maven archetype)
    private static class Op2 extends Op1 {

        @SourceProduct
        private Product sourceProduct;
        @TargetProduct
        private Product targetProduct;
        @Parameter
        private String sourceBandName;
        @Parameter
        private String targetBandName1;
        @Parameter
        private String targetBandName2;
        private Band sourceBand;
        private Band targetBand1;
        private Band targetBand2;

        public Op2(OperatorSpi spi) {
            super(spi);
        }

        @Override
        protected Product initialize(ProgressMonitor pm) throws OperatorException {
            targetProduct = new Product(TileComputingStrategyTest.class.getName(), TileComputingStrategyTest.class.getName(), 256, 256);
            sourceBand = sourceProduct.getBand(sourceBandName);
            targetBand1 = targetProduct.addBand(targetBandName1, sourceBand.getDataType());
            targetBand2 = targetProduct.addBand(targetBandName2, sourceBand.getDataType());
            return targetProduct;
        }

        @Override
        public void computeBand(Raster targetRaster,
                                ProgressMonitor pm) throws OperatorException {

            Rectangle targetTileRectangle = targetRaster.getRectangle();
            Raster sourceRaster = getRaster(sourceBand, targetTileRectangle);

            int x0 = targetTileRectangle.x;
            int y0 = targetTileRectangle.y;
            int w = targetTileRectangle.width;
            int h = targetTileRectangle.height;
            for (int y =y0; y < y0+h; y++) {
                for (int x =x0; x < x0+w; x++) {
                    GeoCoding geoCoding = sourceProduct.getGeoCoding();
                    GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(x, y), null);

                    double v = sourceRaster.getDouble(x, y);
                    if (targetRaster.getRasterDataNode() == targetBand1) {
                        double v1 = 0.1 * v; // Place your transformation math here
                        targetRaster.setDouble(x, y, v1);
                    } else if (targetRaster.getRasterDataNode() == targetBand2) {
                        double v2 = 0.2 * v; // Place your transformation math here
                        targetRaster.setDouble(x, y, v2);
                    }
                }
            }
        }

        @Override
        public void computeAllBands(Rectangle targetTileRectangle,
                                 ProgressMonitor pm) throws OperatorException {

            Raster sourceRaster = getRaster(sourceBand, targetTileRectangle);
            Raster targetRaster1 = getRaster(targetBand1, targetTileRectangle);
            Raster targetRaster2 = getRaster(targetBand2, targetTileRectangle);

            int x0 = targetTileRectangle.x;
            int y0 = targetTileRectangle.y;
            int w = targetTileRectangle.width;
            int h = targetTileRectangle.height;
            for (int y = y0; y < y0+h; y++) {
                for (int x = x0; x < x0+w; x++) {
                    GeoCoding geoCoding = sourceProduct.getGeoCoding();
                    GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(x, y), null);

                    double v = sourceRaster.getDouble(x, y);
                    double v1 = 0.1 * v; // Place your transformation math here
                    double v2 = 0.2 * v; // Place your transformation math here
                    targetRaster1.setDouble(x, y, v1);
                    targetRaster2.setDouble(x, y, v2);
                }
            }
        }
    }
}

