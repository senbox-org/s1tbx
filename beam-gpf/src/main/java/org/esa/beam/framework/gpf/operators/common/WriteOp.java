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
import java.awt.*;
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
    private List<Band> writableBands;
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
        final Band[] bands = targetProduct.getBands();
        writableBands = new ArrayList<Band>(bands.length);
        for (final Band band : bands) {
            if (productWriter.shouldWrite(band)) {
                writableBands.add(band);
            }
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        if (!productFileWritten) {
            try {
                productWriter.writeProductNodes(targetProduct, file);
                productFileWritten = true;
            } catch (IOException e) {
                throw new OperatorException(e);
            }
        }
        if (writableBands.contains(targetBand)) {
            final Rectangle rectangle = targetTile.getRectangle();
            final ProductWriter oldProductWriter = targetProduct.getProductWriter();

            try {
                final Tile tile = getSourceTile(targetBand, rectangle, pm);
                final ProductData rawSamples = tile.getRawSamples();
                targetProduct.setProductWriter(productWriter);
                targetBand.writeRasterData(rectangle.x, rectangle.y, rectangle.width, rectangle.height, rawSamples, pm);
            } catch (IOException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof OperatorException) {
                    throw (OperatorException) cause;
                }
                throw new OperatorException(e);
            } finally {
                targetProduct.setProductWriter(oldProductWriter);
            }
        }
    }

    @Override
    public void dispose() {
        try {
            productWriter.close();
        } catch (IOException e) {
            // ignore
        }
        writableBands.clear();
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(WriteOp.class);
        }
    }

    public static void writeProduct(Product product, File file, String formatName, ProgressMonitor pm) {
        final WriteOp writeOp = new WriteOp(product, file, formatName);
        final Product targetProduct = writeOp.getTargetProduct();

        Dimension tileSize = product.getPreferredTileSize();
        if (tileSize == null) {
            tileSize = JAI.getDefaultTileSize();
        }

        final int rasterHeight = targetProduct.getSceneRasterHeight();
        final int rasterWidth = targetProduct.getSceneRasterWidth();
        final Rectangle boundary = new Rectangle(rasterWidth, rasterHeight);
        final int tileCountX = MathUtils.ceilInt(boundary.width / (double) tileSize.width);
        final int tileCountY = MathUtils.ceilInt(boundary.height / (double) tileSize.height);
        final Band[] targetBands = targetProduct.getBands();

        try {
            pm.beginTask("Writing product...", tileCountX * tileCountY * targetBands.length * 2);

            for (int tileY = 0; tileY < tileCountY; tileY++) {
                for (int tileX = 0; tileX < tileCountX; tileX++) {
                    writeOp.checkForCancelation(pm);

                    final Rectangle tileRectangle = new Rectangle(tileX * tileSize.width,
                                                                  tileY * tileSize.height,
                                                                  tileSize.width,
                                                                  tileSize.height);
                    final Rectangle intersection = boundary.intersection(tileRectangle);

                    for (final Band band : targetBands) {
                        final Tile tile = writeOp.getSourceTile(band, intersection, new SubProgressMonitor(pm, 1));
                        writeOp.computeTile(band, tile, new SubProgressMonitor(pm, 1));
                    }
                }
            }
        } catch (OperatorException e) {
            if ("Operation canceled.".equals(e.getMessage())) {
                try {
                    writeOp.productWriter.deleteOutput();
                } catch (IOException ignored) {
                }
            }
            throw e;
        } finally {
            try {
                writeOp.productWriter.close();
            } catch (IOException ignored) {
            }
            pm.done();
        }
    }
}
    