package org.esa.beam.framework.gpf.operators.common;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.OperatorAlias;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The <code>WriteProductOp</code> writes
 * the in-memory representation of its input product.
 * <p/>
 * Configuration Elements:
 * <ul>
 * <li><b>filePath</b> the path of the product file to write to
 * <li><b>formatName</b> the format of the file
 * </ul>
 *
 * @author Maximilian Aulinger
 * @author Marco Zuehlke
 */
@OperatorAlias("ProductWriter")
public class WriteProductOp extends AbstractOperator {

    @TargetProduct
    private Product targetProduct;
    @SourceProduct(alias = "input")
    private Product sourceProduct;

    @Parameter
    private String filePath = null;
    @Parameter
    private String formatName = ProductIO.DEFAULT_FORMAT_NAME;

    private ProductWriter productWriter;
    private List<Band> bandsToWrite;
    private boolean productFileWritten;

    @Override
    public Product initialize() throws OperatorException {
        targetProduct = sourceProduct;
        productWriter = ProductIO.getProductWriter(formatName);
        if (productWriter == null) {
            throw new OperatorException("No product writer for the '" + formatName + "' format available");
        }
        productWriter.setIncrementalMode(false);
        Band[] bands = targetProduct.getBands();
        bandsToWrite = new ArrayList<Band>(bands.length);
        for (Band band : bands) {
            if (productWriter.shouldWrite(band)) {
                bandsToWrite.add(band);
            }
        }
        return targetProduct;
    }


    @Override
    public void computeTile(Band band, Tile targetTile) throws OperatorException {
        if (!productFileWritten) {
            try {
                productWriter.writeProductNodes(targetProduct, filePath);
                productFileWritten = true;
            } catch (IOException e) {
                throw new OperatorException(e);
            }
        }
        if (bandsToWrite.contains(band)) {
            Rectangle rectangle = targetTile.getRectangle();
            try {
                Tile tile = getSourceTile(band, rectangle);
                ProductData dataBuffer = tile.getRawSampleData();
                band.writeRasterData(rectangle.x, rectangle.y, 
                        rectangle.width, rectangle.height, dataBuffer, createProgressMonitor());
            } catch (IOException e) {
                Throwable cause = e.getCause();
                if (cause instanceof OperatorException) {
                    throw (OperatorException) cause;
                }
                throw new OperatorException(e);
            }
        }
    }

    @Override
    public void dispose() {
        try {
            targetProduct.closeIO();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Spi extends AbstractOperatorSpi {
        public Spi() {
            super(WriteProductOp.class, "WriteProduct");
        }
    }
}