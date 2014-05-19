package org.esa.beam.dataio.avhrr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.avhrr.noaa.PodAvhrrFile;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.File;
import java.io.IOException;

/**
 * @author Ralf Quast
 */
public final class PodAvhrrReader extends AbstractProductReader {

    private PodAvhrrFile avhrrFile;

    public PodAvhrrReader(PodAvhrrReaderPlugIn plugIn) {
        super(plugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File file = PodAvhrrReaderPlugIn.getInputFile(getInput());

        final Product product;
        try {
            avhrrFile = new PodAvhrrFile(file);
            product = avhrrFile.createProduct();
            product.setFileLocation(file);
        } catch (IOException e) {
            try {
                close();
            } catch (IOException ignored) {
            }
            throw e;
        }

        return product;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX,
                                          int sourceOffsetY,
                                          int sourceWidth,
                                          int sourceHeight,
                                          int sourceStepX,
                                          int sourceStepY,
                                          Band destBand,
                                          int destOffsetX,
                                          int destOffsetY,
                                          int destWidth,
                                          int destHeight,
                                          ProductData destBuffer, ProgressMonitor pm) throws IOException {
        final BandReader bandReader = avhrrFile.getBandReader(destBand);
        if (bandReader == null) {
            throw new IllegalStateException("no band reader available");
        }

        bandReader.readBandRasterData(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, sourceStepX,
                                      sourceStepY, destBuffer, pm);
    }

    @Override
    public void close() throws IOException {
        super.close();
        avhrrFile.dispose();
    }

}
