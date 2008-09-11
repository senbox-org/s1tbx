package org.esa.beam.jai;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;

/**
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class ImageManagerTest extends TestCase {
    private static final double EPS_L = 1e-3;
    private static final double EPS_H = 1e-6;

    public void testBandWithNoScaling() {
        Band band = createBand(1.0, 0.0, false);
        testTargetImageSampleValues(band, EPS_H);
    }

    public void testBandWithLinearScaling() {
        Band band = createBand(0.1, 0.5, false);
        testTargetImageSampleValues(band, EPS_H);
    }

    public void testBandWithLog10Scaling() {
        Band band = createBand(1.0, 0.0, true);
        testTargetImageSampleValues(band, EPS_L);
    }

    public void testBandWithLinearAndLog10Scaling() {
        Band band = createBand(0.1, 0.5, true);
        testTargetImageSampleValues(band, EPS_H);
    }

    private Band createBand(double factor, double offset, boolean log10Scaled) {
        Band band = new Band("b", ProductData.TYPE_INT8, 2, 2);
        band.setScalingFactor(factor);
        band.setScalingOffset(offset);
        band.setLog10Scaled(log10Scaled);
        band.setSourceImage(createSourceImage());
        return band;
    }

    private void testTargetImageSampleValues(Band band, final double eps) {
        PlanarImage image = createTargetImage(band);
        assertEquals(band.scale(0), getSample(image, 0, 0), eps);
        assertEquals(band.scale(1), getSample(image, 1, 0), eps);
        assertEquals(band.scale(2), getSample(image, 0, 1), eps);
        assertEquals(band.scale(3), getSample(image, 1, 1), eps);
    }

    private double getSample(PlanarImage image, int x, int y) {
        return image.getData().getSampleDouble(x, y, 0);
    }

    private static PlanarImage createTargetImage(RasterDataNode rdn) {
        return PlanarImage.wrapRenderedImage(rdn.getGeophysicalImage());
    }

    private static PlanarImage createSourceImage() {
        final BufferedImage bi = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_GRAY);
        bi.getRaster().setSample(0, 0, 0, 0);
        bi.getRaster().setSample(1, 0, 0, 1);
        bi.getRaster().setSample(0, 1, 0, 2);
        bi.getRaster().setSample(1, 1, 0, 3);
        return PlanarImage.wrapRenderedImage(bi);
    }
}
