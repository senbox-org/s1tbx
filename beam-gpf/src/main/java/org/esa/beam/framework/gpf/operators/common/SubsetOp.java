package org.esa.beam.framework.gpf.operators.common;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.awt.*;
import java.io.IOException;

@OperatorMetadata(alias = "Subset",
                  description = "Create a spatial and/or spectral subset of the source product.")
public class SubsetOp extends Operator {

    private ProductReader subsetReader;
    private ProductSubsetDef subsetDef = null;

    @SourceProduct(alias="input")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
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
    public void initialize() throws OperatorException {
        subsetReader = new ProductSubsetBuilder();
        initSubsetDef();

        try {
            targetProduct = subsetReader.readProductNodes(sourceProduct, subsetDef);
        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        ProductData destBuffer = targetTile.getRawSamples();
        Rectangle rectangle = targetTile.getRectangle();
        try {
            subsetReader.readBandRasterData(band, rectangle.x, rectangle.y, rectangle.width,
                                            rectangle.height, destBuffer, pm);
            targetTile.setRawSamples(destBuffer);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    private void initSubsetDef() {
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
        Product sourceProduct = getSourceProduct("input");
        // set source dependent default values
        if (region == null) {
            region = new Rectangle(0, 0, sourceProduct.getSceneRasterWidth(),
                                   sourceProduct.getSceneRasterWidth());
        }
        bandList = sourceProduct.getBandNames();
        flagBandList = sourceProduct.getAllFlagNames();
        tiePointGridList = sourceProduct.getTiePointGridNames();
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SubsetOp.class);
        }
    }
}
