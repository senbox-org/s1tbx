package org.esa.beam.framework.gpf.operators.common;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
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
import org.esa.beam.util.math.MathUtils;

import javax.media.jai.JAI;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@OperatorMetadata(alias = "Write",
                  description = "Writes a product to disk.")
public class WriteOp extends Operator {

    @TargetProduct
    private Product targetProduct;
    @SourceProduct(alias = "input")
    private Product sourceProduct;

    @Parameter(description = "The file to which the data product is written.")
    private File file;
    @Parameter(defaultValue = ProductIO.DEFAULT_FORMAT_NAME)
    private String formatName;

    private ProductWriter productWriter;
    private List<Band> bandsToWrite;
    private boolean productFileWritten;

    public WriteOp() {
    }

    public WriteOp(Product product, File file, String formatName) {
        this.sourceProduct = product;
        this.file = file;
        this.formatName = formatName;
    }

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
                productWriter.writeProductNodes(targetProduct, file);
                productFileWritten = true;
            } catch (IOException e) {
                throw new OperatorException(e);
            }
        }
        if (bandsToWrite.contains(band)) {
            Rectangle rectangle = targetTile.getRectangle();
            ProductWriter productWriterOld = null;
            try {
                Tile tile = getSourceTile(band, rectangle, pm);
                ProductData dataBuffer = tile.getRawSamples();
                productWriterOld = targetProduct.getProductWriter();
                targetProduct.setProductWriter(productWriter);
                band.writeRasterData(rectangle.x, rectangle.y,
                                     rectangle.width, rectangle.height, dataBuffer, pm);
            } catch (IOException e) {
                Throwable cause = e.getCause();
                if (cause instanceof OperatorException) {
                    throw (OperatorException) cause;
                }
                throw new OperatorException(e);
            } finally {
                if (productWriterOld != null) {
                    targetProduct.setProductWriter(productWriterOld);
                }
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

    public static void writeProduct(Product product, File file, String formatName, ProgressMonitor pm) {
        WriteOp writeOp = new WriteOp(product, file, formatName);
        Product outputProduct = writeOp.getTargetProduct();

        Dimension defaultTileSize = product.getPreferredTileSize();
        if (defaultTileSize == null) {
            defaultTileSize = JAI.getDefaultTileSize();
        }
        final int rasterHeight = outputProduct.getSceneRasterHeight();
        final int rasterWidth = outputProduct.getSceneRasterWidth();
        Rectangle productBounds = new Rectangle(rasterWidth, rasterHeight);
        int numXTiles = MathUtils.ceilInt(productBounds.width / (double) defaultTileSize.width);
        int numYTiles = MathUtils.ceilInt(productBounds.height / (double) defaultTileSize.height);
        final Band[] bands = outputProduct.getBands();

        pm.beginTask("Writing product...", numXTiles * numYTiles * bands.length * 2);
        try {
            for (int tileY = 0; tileY < numYTiles; tileY++) {
                for (int tileX = 0; tileX < numXTiles; tileX++) {
                    writeOp.checkForCancelation(pm);
                    // todo - delete file(s)  (nf - 2007.10.30)

                    Rectangle tileRectangle = new Rectangle(tileX
                            * defaultTileSize.width, tileY
                            * defaultTileSize.height, defaultTileSize.width,
                                                      defaultTileSize.height);
                    Rectangle intersection = productBounds
                            .intersection(tileRectangle);

                    for (Band band : bands) {
                        Tile tile = writeOp.getSourceTile(band, intersection, SubProgressMonitor.create(pm, 1));
                        writeOp.computeTile(band, tile, SubProgressMonitor.create(pm, 1));
                    }
                }
            }
        } finally {
            pm.done();
        }
    }
}
    