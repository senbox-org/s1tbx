package org.esa.beam.dataio.avhrr.noaa.pod;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.File;
import java.io.IOException;

/**
 * A reader for NOAA Polar Orbiter Data (POD) products (currently AVHRR HRPT only).
 *
 * @author Ralf Quast
 * @see <a href="http://www.ncdc.noaa.gov/oa/pod-guide/ncdc/docs/podug/index.htm">NOAA Polar Orbiter Data User's Guide</a>
 */
final class PodAvhrrReader extends AbstractProductReader {

    private PodAvhrrFile avhrrFile;

    PodAvhrrReader(PodAvhrrReaderPlugIn plugIn) {
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
                                          Band targetBand,
                                          int targetOffsetX,
                                          int targetOffsetY,
                                          int targetWidth,
                                          int targetHeight,
                                          ProductData targetBuffer, ProgressMonitor pm) throws IOException {
        final BandReader bandReader = avhrrFile.getBandReader(targetBand);
        if (bandReader == null) {
            throw new IllegalStateException("No band reader available.");
        }

        bandReader.readBandRasterData(sourceOffsetX,
                                      sourceOffsetY,
                                      sourceWidth,
                                      sourceHeight,
                                      sourceStepX,
                                      sourceStepY,
                                      targetBuffer, pm);
    }

    @Override
    public void close() throws IOException {
        super.close();
        avhrrFile.dispose();
    }

}
