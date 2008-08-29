package org.esa.beam.jai;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ExpDescriptor;
import javax.media.jai.operator.FormatDescriptor;
import javax.media.jai.operator.RescaleDescriptor;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

/**
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class ImageManagerTest extends TestCase {
    private static final double EPS = 1e-8;

    public void testBandWithNoScaling() {
        Band band = createBand(1.0, 0.0, false);
        testTargetImageSampleValues(band);
    }

    public void testBandWithLinearScaling() {
        Band band = createBand(0.1, 0.5, false);
        testTargetImageSampleValues(band);
    }

    public void testBandWithLog10Scaling() {
        Band band = createBand(1.0, 0.0, true);
        testTargetImageSampleValues(band);
    }

    public void testBandWithLinearAndLog10Scaling() {
        Band band = createBand(0.1, 0.5, true);
        testTargetImageSampleValues(band);
    }

    private Band createBand(double factor, double offset, boolean log10Scaled) {
        Band band = new Band("b", ProductData.TYPE_INT8, 2, 2);
        band.setScalingFactor(factor);
        band.setScalingOffset(offset);
        band.setLog10Scaled(log10Scaled);
        return band;
    }

    private void testTargetImageSampleValues(Band band) {
        PlanarImage image = createTargetImage(band);
        assertEquals(band.scale(0), getSample(image, 0, 0), EPS);
        assertEquals(band.scale(1), getSample(image, 1, 0), EPS);
        assertEquals(band.scale(2), getSample(image, 0, 1), EPS);
        assertEquals(band.scale(3), getSample(image, 1, 1), EPS);
    }

    private double getSample(PlanarImage image, int x, int y) {
        return image.getData().getSampleDouble(x, y, 0);
    }

    private static PlanarImage createTargetImage(RasterDataNode rdn) {
        PlanarImage image = createSourceImage();
        return createdTargetImage(rdn, image);
    }

    private static PlanarImage createdTargetImage(RasterDataNode rdn, PlanarImage image) {
        if (!rdn.isScalingApplied()) {
            return image;
        } else if (!rdn.isLog10Scaled()) {
            image = toDouble(image);
            image = rescale(image, rdn.getScalingFactor(), rdn.getScalingOffset());
        } else {
            image = toDouble(image);
            image = rescale(image, Math.log(10) * rdn.getScalingFactor(), Math.log(10) * rdn.getScalingOffset());
            image = ExpDescriptor.create(image, null);
        }
        return image;
    }

    private static PlanarImage rescale(PlanarImage image, double factor, double offset) {
        image = RescaleDescriptor.create(image,
                                         new double[]{factor},
                                         new double[]{offset}, null);
        return image;
    }

    private static PlanarImage toDouble(PlanarImage image) {
        if (image.getSampleModel().getDataType() != DataBuffer.TYPE_DOUBLE) {
            image = FormatDescriptor.create(createSourceImage(), DataBuffer.TYPE_DOUBLE, null);
        }
        return image;
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
