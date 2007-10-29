package org.esa.beam.framework.gpf.operators.common;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
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

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@OperatorMetadata(alias = "ProductWriter", // todo - rename to "Write"
                  description = "Writes a product to disk.")
public class WriteOp extends Operator {

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
    public void initialize() throws OperatorException {
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
    }


    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
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
                Tile tile = getSourceTile(band, rectangle, pm);
                ProductData dataBuffer = tile.getRawSamples();
                band.writeRasterData(rectangle.x, rectangle.y,
                                     rectangle.width, rectangle.height, dataBuffer, pm);
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

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(WriteOp.class);
        }
    }
}