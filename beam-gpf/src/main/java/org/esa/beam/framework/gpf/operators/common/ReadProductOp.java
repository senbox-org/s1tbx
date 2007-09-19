package org.esa.beam.framework.gpf.operators.common;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.awt.Rectangle;
import java.io.IOException;

/**
 * The <code>LoadProductOperator</code> wrapps the BEAM {@link ProductIO} to load any kind of Product
 * from the file system into an <code>Operator</code>.</p>
 * <p/>
 * Configuration elements:
 * <ul>
 * <li><b>filePath:</b> The path of the file to read the Product from</th>
 * </ul>
 *
 * @author Maximilian Aulinger
 */
public class ReadProductOp extends AbstractOperator {

    private ProductReader beamReader;

    /**
     * The path to the data product file to open and read.
     */
    @Parameter
    private String filePath = null;
    @TargetProduct
    private Product targetProduct;

    public ReadProductOp(OperatorSpi spi) {
        super(spi);
    }

    @Override
    public Product initialize(ProgressMonitor pm) throws OperatorException {
        try {
            targetProduct = ProductIO.readProduct(filePath, null);
            if (targetProduct == null) {
                throw new OperatorException("No product reader found for file " + filePath);
            }
            beamReader = targetProduct.getProductReader();
        } catch (IOException e) {
            throw new OperatorException(e);
        }
        return targetProduct;
    }

    @Override
    public void computeBand(Band band, Raster targetRaster,
                            ProgressMonitor pm) throws OperatorException {

        ProductData dataBuffer = targetRaster.getDataBuffer();
        Rectangle rectangle = targetRaster.getRectangle();
        try {
            beamReader.readBandRasterData(band, rectangle.x, rectangle.y, rectangle.width,
                                          rectangle.height, dataBuffer, pm);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    public static class Spi extends AbstractOperatorSpi {
        public Spi() {
            super(ReadProductOp.class, "ReadProduct");
        }
    }
}
