package org.esa.beam.tutorials;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

/**
 * Example taken from the tutorial given at Sentinel-3 OLCI/SLSTR and MERIS/(A)ATSR workshop 2012.
 * See http://www.brockmann-consult.de/cms/web/beam/tutorials
 * <p/>
 * <p/>
 * The FLH is computed as follows:
 * <pre>
 *    FLH = L2 - k * [L1 + (L3 - L1) * (wl2 - wl1) / (wl3 - wl1)]
 * </pre>
 * <p/>
 * where:<br/>
 * L1 is the radiance in band 7
 * L2 is the radiance in band 8
 * L3 is the radiance in band 9
 * k is a user-supplied processing parameter.
 */
public class FluorescenceLineHeightProcessor {

    public static void main(String[] args) {
        if (!(args.length == 1 || args.length == 2)) {
            System.out.println("Usage: <meris-l1-file> [<k>]");
            System.exit(1);
        }
        String inputFile = args[0];
        float k = args.length == 2 ? Float.parseFloat(args[1]) : 1.005F;
        try {
            computeFLH(inputFile, k);
        } catch (IOException e) {
            System.err.println("I/O Error: " + e.getMessage());
            System.exit(2);
        }
    }

    private static void computeFLH(String inputFile, float k) throws IOException {

        // Open input product file
        Product product = ProductIO.readProduct(inputFile);
        Band band1 = product.getBand("radiance_7");
        Band band2 = product.getBand("radiance_8");
        Band band3 = product.getBand("radiance_9");

        // Dimension of the raster data grid
        int width = product.getSceneRasterWidth();
        int height = product.getSceneRasterHeight();

        // Create a new product object and add a band "FLH"
        Product flhProduct = new Product("FLH", "FLH_TYPE", width, height);
        Band flhBand = flhProduct.addBand("FLH", ProductData.TYPE_FLOAT32);

        ProductWriter productWriter = ProductIO.getProductWriter("BEAM-DIMAP");
        productWriter.writeProductNodes(flhProduct, "FLH.dim");

        // Compute  ratio of wavelengths differences
        float wl1 = band1.getSpectralWavelength();
        float wl2 = band2.getSpectralWavelength();
        float wl3 = band3.getSpectralWavelength();
        float a = (wl2 - wl1) / (wl3 - wl1);

        // scan-line buffers
        float[] band1Samples = new float[width];
        float[] band2Samples = new float[width];
        float[] band3Samples = new float[width];
        float[] flhSamples = new float[width];

        // Loop through all x and y of the raster data grid
        for (int y = 0; y < height; y++) {
            band1.readPixels(0, y, width, 1, band1Samples);
            band2.readPixels(0, y, width, 1, band2Samples);
            band3.readPixels(0, y, width, 1, band3Samples);
            for (int x = 0; x < width; x++) {
                float L1 = band1Samples[x];
                float L2 = band2Samples[x];
                float L3 = band3Samples[x];
                float FLH = L2 - k * (L1 + (L3 - L1) * a);
                flhSamples[x] = FLH;
            }

            flhBand.writePixels(0, y, width, 1, flhSamples);
        }
    }

}
