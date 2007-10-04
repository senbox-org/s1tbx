package org.esa.beam.framework.gpf.operators.common;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.Parameter;

import java.awt.Rectangle;
import java.io.IOException;

/**
 * The <code>SubsetOp</code> will create a subset of its source Product.
 * <p/>
 * Configuration Elements:
 * <ul>
 * <li><b>region</b> a rectangle specifying the subset region
 * <li><b>subSamplingX</b> the subsampling in X directions
 * <li><b>subSamplingY</b> the subsampling in Y directions
 * <li><b>bandList</b> an array of band names that should be part of the
 * subset
 * <li><b>flagBandList</b> an array of flag band names that should be part of
 * the subset
 * <li><b>tiePointGridList</b> an array of tie-point grid names that should be
 * part of the subset
 * <li><b>igonreMetadata</b> a boolean value that signals if the metadata is
 * part of the subset
 * </ul>
 *
 * @author Maximilian Aulinger
 */
public class SubsetOp extends AbstractOperator {

    private ProductReader subsetReader;
    private ProductSubsetDef subsetDef = null;

    @Parameter
    private Rectangle region = null;
    @Parameter
    private int subSamplingX = 1;
    @Parameter
    private int subSamplingY = 1;
    @Parameter
    private String[] bandList = null;
    @Parameter
    private String[] flagBandList = null;
    @Parameter
    private String[] tiePointGridList = null;
    @Parameter
    private boolean ignoreMetadata = false;

    @Override
    protected Product initialize() throws OperatorException {
        subsetReader = new ProductSubsetBuilder();
        createSubsetDef();

        try {
            return subsetReader.readProductNodes(getContext().getSourceProduct("input"), subsetDef);
        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile) throws OperatorException {
        ProductData destBuffer = targetTile.getRawSampleData();
        Rectangle rectangle = targetTile.getRectangle();
        try {
            subsetReader.readBandRasterData(band, rectangle.x, rectangle.y, rectangle.width,
                                            rectangle.height, destBuffer, createProgressMonitor());
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    private void createSubsetDef() {
        setDefaultValues();
        subsetDef = new ProductSubsetDef();
        subsetDef.addNodeNames(bandList);
        subsetDef.addNodeNames(tiePointGridList);
        subsetDef.addNodeNames(flagBandList);
        subsetDef.setRegion(region);
        subsetDef.setSubSampling(subSamplingX, subSamplingY);
        subsetDef.setIgnoreMetadata(ignoreMetadata);
    }

    private void setDefaultValues() {
        Product sourceProduct = getContext().getSourceProduct("input");
        // set source dependent default values
        if (region == null) {
            region = new Rectangle(0, 0, sourceProduct.getSceneRasterWidth(),
                                   sourceProduct.getSceneRasterWidth());
        }
        bandList = sourceProduct.getBandNames();
        flagBandList = sourceProduct.getAllFlagNames();
        tiePointGridList = sourceProduct.getTiePointGridNames();
    }

    public static class Spi extends AbstractOperatorSpi {
        public Spi() {
            super(SubsetOp.class, "Subset");
        }
    }
}
