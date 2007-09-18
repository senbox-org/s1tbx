package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.*;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorContext;
import org.esa.beam.util.io.BeamFileFilter;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
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
        Rectangle destRectangle = new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight);
        readRectangle(destBand, destRectangle, destBuffer);
    }

    private void readRectangle(Band targetBand,
                               Rectangle targetRect,
                               ProductData targetBuffer) {
        final RenderedImage image = targetBand.getImage();
        /////////////////////////////////////////////////////////////////////
        //
        // GPF pull-processing is triggered here!!!
        //
        java.awt.image.Raster data = image.getData(targetRect);
        //
        /////////////////////////////////////////////////////////////////////
        data.getDataElements(targetRect.x, targetRect.y, targetRect.width, targetRect.height, targetBuffer.getElems());
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