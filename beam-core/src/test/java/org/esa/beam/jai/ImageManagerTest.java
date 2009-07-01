package org.esa.beam.jai;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ROIDefinition;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

/**
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class ImageManagerTest extends TestCase {

    private static final double EPS_L = 1.0e-3;
    private static final double EPS_H = 1.0e-6;

    public void testBandWithNoScaling() {
        Band band = createBand(1.0, 0.0, false);
        checkTargetImageSampleValues(band, EPS_H);
    }

    public void testBandWithLinearScaling() {
        Band band = createBand(0.1, 0.5, false);
        checkTargetImageSampleValues(band, EPS_H);
    }

    public void testBandWithLog10Scaling() {
        Band band = createBand(1.0, 0.0, true);
        checkTargetImageSampleValues(band, EPS_L);
    }

    public void testBandWithLinearAndLog10Scaling() {
        Band band = createBand(0.1, 0.5, true);
        checkTargetImageSampleValues(band, EPS_H);
    }

    public void testCreateRoiImageWithValueRange() {
        final Product product = new Product("p", "t", 2, 2);
        Band band = createBand(1.0, 0, false);
        product.addBand(band);
        final ROIDefinition roiDef = new ROIDefinition();
        roiDef.setValueRangeEnabled(true);
        roiDef.setValueRangeMin(1);
        roiDef.setValueRangeMax(2);
        band.setROIDefinition(roiDef);

        final RenderedImage roiMaskImage = ImageManager.getInstance().createRoiMaskImage(band, 0);
        final DataBuffer dataBuffer = roiMaskImage.getData().getDataBuffer();
        assertEquals(0, dataBuffer.getElem(0));
        assertEquals(255, dataBuffer.getElem(1));
        assertEquals(255, dataBuffer.getElem(2));
        assertEquals(0, dataBuffer.getElem(3));
    }

    public void testCreateRoiImageWithValueRangeAndNoDataValue() {
        final Product product = new Product("p", "t", 2, 2);
        Band band = createBand(1.0, 0, false);
        product.addBand(band);
        final ROIDefinition roiDef = new ROIDefinition();
        roiDef.setValueRangeEnabled(true);
        roiDef.setValueRangeMin(1);
        roiDef.setValueRangeMax(2);
        band.setROIDefinition(roiDef);

        band.setNoDataValueUsed(true);
        band.setNoDataValue(1.0);

        final RenderedImage roiMaskImage = ImageManager.getInstance().createRoiMaskImage(band, 0);
        final DataBuffer dataBuffer = roiMaskImage.getData().getDataBuffer();
        assertEquals(0, dataBuffer.getElem(0));
        assertEquals(0, dataBuffer.getElem(1));
        assertEquals(255, dataBuffer.getElem(2));
        assertEquals(0, dataBuffer.getElem(3));
    }

    private Band createBand(double factor, double offset, boolean log10Scaled) {
        Band band = new Band("b", ProductData.TYPE_INT8, 2, 2);
        band.setScalingFactor(factor);
        band.setScalingOffset(offset);
        band.setLog10Scaled(log10Scaled);
        band.setSourceImage(createSourceImage());
        return band;
    }

    private static void checkTargetImageSampleValues(Band band, final double eps) {
        PlanarImage image = createTargetImage(band);
        assertEquals(band.scale(0), getSample(image, 0, 0), eps);
        assertEquals(band.scale(1), getSample(image, 1, 0), eps);
        assertEquals(band.scale(2), getSample(image, 0, 1), eps);
        assertEquals(band.scale(3), getSample(image, 1, 1), eps);
    }

    private static double getSample(PlanarImage image, int x, int y) {
        return image.getData().getSampleDouble(x, y, 0);
    }

    private static PlanarImage createTargetImage(RasterDataNode rdn) {
        return rdn.getGeophysicalImage();
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
