package org.esa.snap.examples.multilevel;

import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.jai.ImageManager;
import org.esa.snap.jai.RasterDataNodeOpImage;
import org.esa.snap.jai.ResolutionLevel;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * Coding example that shows how to use the {@link RasterDataNodeOpImage} abstract base class.
 *
 * @author Norman
 */
public class MultiLevelSourceExample {
    static final int L = 6;         // # resolution levels for the image pyramid
    static final int T = 1024;      // Image tile size at highest resolution level (=0)
    static final int W = 5 * T;     // Image width at highest resolution level (=0)
    static final int H = 4 * T;     // Image height at highest resolution level (=0)

    public static void main(String[] args) throws IOException {
        Product product = createTestProduct(W, H, T, L);
        Band outputBand = product.getBand("magnitude");

        System.out.println("Accuracy test...");
        double[] pixels = new double[6];
        outputBand.readPixels(100, 120, 3, 2, pixels);
        assertEqualDoubles(pixels[0], getExpectedValue(100, 120), 1e-8);
        assertEqualDoubles(pixels[1], getExpectedValue(101, 120), 1e-8);
        assertEqualDoubles(pixels[2], getExpectedValue(102, 120), 1e-8);
        assertEqualDoubles(pixels[3], getExpectedValue(100, 121), 1e-8);
        assertEqualDoubles(pixels[4], getExpectedValue(101, 121), 1e-8);
        assertEqualDoubles(pixels[5], getExpectedValue(102, 121), 1e-8);
        System.out.println("Success.");

        int pixelsCount = W * H;
        double[] allPixels = new double[pixelsCount];

        System.out.println("Performance test...");
        long t0 = System.currentTimeMillis();
        outputBand.readPixels(0, 0, W, H, allPixels);
        long dt = System.currentTimeMillis() - t0;
        System.out.println(pixelsCount + " pixels processed in " + dt + " ms or " + (double) pixelsCount / dt + " pixels/ms");
    }

    private static double getExpectedValue(int x, int y) {
        double X = x + 0.5;
        double Y = y + 0.5;
        double real = X * X - Y * Y;
        double imag = 2 * X * Y;
        return Math.sqrt(real * real + imag * imag);
    }

    private static void assertEqualDoubles(double expected, double actual, double eps) {
        if (Math.abs(expected - actual) > eps) {
            throw new IllegalStateException("Expected " + expected + " but got " + actual);
        }
    }

    private static Product createTestProduct(int width, int height, int tileSize, int numResolutionsMax) {
        Product product = new Product("test", "test", width, height);
        product.setPreferredTileSize(tileSize, tileSize);
        product.setNumResolutionsMax(numResolutionsMax);

        Band realBand = product.addBand("real", "X * X - Y * Y", ProductData.TYPE_FLOAT64);
        Band imagBand = product.addBand("imag", "2 * X * Y", ProductData.TYPE_FLOAT64);
        Band magnitudeBand = product.addBand("magnitude", ProductData.TYPE_FLOAT64);

        final MultiLevelModel multiLevelModel = ImageManager.getMultiLevelModel(realBand);
        final MultiLevelSource multiLevelSource = new AbstractMultiLevelSource(multiLevelModel) {

            @Override
            public void reset() {
                super.reset();
                magnitudeBand.fireProductNodeDataChanged();
            }

            @Override
            public RenderedImage createImage(int level) {
                return new MagnitudeOpImage(magnitudeBand, ResolutionLevel.create(getModel(), level),
                                            realBand, imagBand);
            }
        };

        magnitudeBand.setSourceImage(new DefaultMultiLevelImage(multiLevelSource));
        return product;
    }

    private static class MagnitudeOpImage extends RasterDataNodeOpImage {

        private final Band realBand;
        private final Band imagBand;

        public MagnitudeOpImage(Band outputBand, ResolutionLevel level, Band realBand, Band imagBand) {
            super(outputBand, level);
            this.realBand = realBand;
            this.imagBand = imagBand;
        }

        @Override
        protected void computeProductData(ProductData outputData, Rectangle region) throws IOException {
            ProductData realData = getRawProductData(realBand, region);
            ProductData imagData = getRawProductData(imagBand, region);
            int numElems = region.width * region.height;
            for (int i = 0; i < numElems; i++) {
                double real = realData.getElemDoubleAt(i);
                double imag = imagData.getElemDoubleAt(i);
                double result = Math.sqrt(real * real + imag * imag);
                outputData.setElemDoubleAt(i, result);
            }
        }
    }
}
