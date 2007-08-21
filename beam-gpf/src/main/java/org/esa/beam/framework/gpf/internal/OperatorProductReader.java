package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorContext;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.util.io.BeamFileFilter;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Locale;

/**
 * The <code>OperatorProductReader</code> is an adapter class that wraps
 * <code>Operator</code>s to BEAM <code>ProductReader</code>s. It enables
 * the usage of BEAM <code>Product</code>s inside of this framework without
 * the necessity to make changes on the <code>Product</code>'s signature.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 */
public class OperatorProductReader implements ProductReader {

    private static PlugIn plugIn = new PlugIn();
    private OperatorComputationContext operatorContext;

    /**
     * Creates a <code>OperatorProductReader</code> instance.
     *
     * @param operatorContext the node context
     */
    public OperatorProductReader(OperatorComputationContext operatorContext) {
        this.operatorContext = operatorContext;
    }

    public void close() throws IOException {
        operatorContext = null;
        // note: nodeContext will be disposed by framework
    }

    public Object getInput() {
        return operatorContext.getSourceProducts();
    }

    public ProductReaderPlugIn getReaderPlugIn() {
        return plugIn;
    }

    public ProductSubsetDef getSubsetDef() {
        return null;
    }

    public Product readProductNodes(Object input, ProductSubsetDef subsetDef) throws IOException,
                                                                                     IllegalFileFormatException {
        return operatorContext.getTargetProduct();
    }

    public void readBandRasterData(Band destBand, int destOffsetX, int destOffsetY, int destWidth,
                                   int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {

        Rectangle destTileRectangle = new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight);
        long minimumTileSize = GPF.getDefaultInstance().getTileCache().getMinimumTileSize();
        if (destTileRectangle.height * destTileRectangle.width <= minimumTileSize) {
            readRectangle(destBand, destTileRectangle, destBuffer, pm);
        } else {
            readRectangleTiled(destBand, destTileRectangle, destBuffer, pm);
        }
    }

    // todo - write a test for this method!
    private void readRectangleTiled(Band targetBand, Rectangle targetTileRectangle, ProductData targetBuffer,
                                    ProgressMonitor pm) throws IOException {
        java.util.List<Rectangle> affectedRectangles = operatorContext.getAffectedRectangles(targetTileRectangle);
        if (targetBuffer == null) {
            targetBuffer = ProductData.createInstance(targetBand.getDataType(), targetTileRectangle.width * targetTileRectangle.height);
        }
        pm.beginTask("Reading data...", affectedRectangles.size() * (100));
        try {
            for (Rectangle subRect : affectedRectangles) {
                if(pm.isCanceled()) {
                    return;
                }
                Raster subRaster;
                try {
                    subRaster = operatorContext.getRaster(targetBand, subRect, SubProgressMonitor.create(pm, 50));
                } catch (OperatorException e) {
                    if (e.getCause() instanceof IOException) {
                        throw (IOException) e.getCause();
                    }
                    throw new IOException(e.getMessage(), e);
                }

                readRectangle(targetBand, subRect, subRaster.getDataBuffer(), SubProgressMonitor.create(pm, 50));

                copyIntersection(subRaster.getDataBuffer(), subRect, targetBuffer, targetTileRectangle);
            }
        } finally {
            pm.done();
        }
    }

    // todo - write a test for this method!
    private void copyIntersection(ProductData sourceBuffer, Rectangle sourceRectangle,
                                  ProductData targetBuffer, Rectangle targetRectangle) {

        Rectangle intersection = targetRectangle.intersection(sourceRectangle);
        final int sourceOffsetX = intersection.x - sourceRectangle.x;
        final int sourceOffsetY = intersection.y - sourceRectangle.y;
        final int targetOffsetX = intersection.x - targetRectangle.x;
        final int targetOffsetY = intersection.y - targetRectangle.y;

        int sourceIndex = sourceRectangle.width * sourceOffsetY + sourceOffsetX;
        int targetIndex = targetRectangle.width * targetOffsetY + targetOffsetX;
        if (sourceRectangle.width == targetRectangle.width) {
            System.arraycopy(sourceBuffer.getElems(), sourceIndex,
                             targetBuffer.getElems(), targetIndex,
                             intersection.width * intersection.height);
        } else {
            for (int y = targetOffsetY; y < targetOffsetY + intersection.height; y++) {
                System.arraycopy(sourceBuffer.getElems(), sourceIndex,
                                 targetBuffer.getElems(), targetIndex,
                                 intersection.width);
                sourceIndex += sourceRectangle.width;
                targetIndex += targetRectangle.width;
            }
        }
    }

    private void readRectangle(Band targetBand, Rectangle targetTileRectangle, ProductData targetBuffer,
                               ProgressMonitor pm) throws IOException {
        try {
            TileComputingStrategy.computeBand(operatorContext, targetBand, targetTileRectangle, targetBuffer, pm);
        } catch (OperatorException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return "OperatorProductReader[op=" + operatorContext.getOperator().getSpi().getName() + "]";
    }

    private static class PlugIn implements ProductReaderPlugIn {

        public DecodeQualification getDecodeQualification(Object input) {
            return input instanceof OperatorContext ? DecodeQualification.INTENDED : DecodeQualification.UNABLE;
        }

        public ProductReader createReaderInstance() {
            throw new RuntimeException("not implemented");
        }

        public Class[] getInputTypes() {
            return new Class[]{OperatorContext.class};
        }

        public String[] getDefaultFileExtensions() {
            return new String[0];
        }

        public String getDescription(Locale locale) {
            return "Adapts the Operator interface to a ProductReader interface";
        }

        public String[] getFormatNames() {
            return new String[0];
        }

        public BeamFileFilter getProductFileFilter() {
            return null;
        }
    }
}