package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Tonio Fincke
 */
@Ignore
public class ResampleTest_Aggregate {

    @Test
    public void testCreateAggregatedMultiLevelImage_First() throws Exception {
        final Band sourceBand = createSourceBand();
        final Band referenceBand = createReferenceBand();

        final MultiLevelImage aggregatedMultiLevelImage =
                Resample.createAggregatedMultiLevelImage(sourceBand, referenceBand, Resample.Type.FIRST, Resample.Type.MEDIAN);

        assertNotNull(aggregatedMultiLevelImage);
        assertEquals(2, aggregatedMultiLevelImage.getModel().getLevelCount());

        final RenderedImage image0 = aggregatedMultiLevelImage.getImage(0);
        assertEquals(2, image0.getWidth());
        assertEquals(2, image0.getHeight());
        final Raster imageData0 = image0.getData();
        assertEquals(5.0, imageData0.getSampleDouble(0, 0, 0), 1e-8);
        assertEquals(7.0, imageData0.getSampleDouble(0, 1, 0), 1e-8);
        assertEquals(13.0, imageData0.getSampleDouble(1, 0, 0), 1e-8);
        assertEquals(15.0, imageData0.getSampleDouble(1, 1, 0), 1e-8);

        final RenderedImage image1 = aggregatedMultiLevelImage.getImage(1);
        assertEquals(1, image1.getWidth());
        assertEquals(1, image1.getHeight());
        final Raster imageData1 = image1.getData();
        assertEquals(5.0, imageData1.getSampleDouble(0, 0, 0), 1e-8);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Min() throws Exception {
        final Band sourceBand = createSourceBand();
        final Band referenceBand = createReferenceBand();

        final MultiLevelImage aggregatedMultiLevelImage =
                Resample.createAggregatedMultiLevelImage(sourceBand, referenceBand, Resample.Type.MIN, Resample.Type.MEDIAN);

        assertNotNull(aggregatedMultiLevelImage);
        assertEquals(2, aggregatedMultiLevelImage.getModel().getLevelCount());

        final RenderedImage image0 = aggregatedMultiLevelImage.getImage(0);
        assertEquals(2, image0.getWidth());
        assertEquals(2, image0.getHeight());
        final Raster imageData0 = image0.getData();
        assertEquals(5.0, imageData0.getSampleDouble(0, 0, 0), 1e-8);
        assertEquals(7.0, imageData0.getSampleDouble(0, 1, 0), 1e-8);
        assertEquals(13.0, imageData0.getSampleDouble(1, 0, 0), 1e-8);
        assertEquals(15.0, imageData0.getSampleDouble(1, 1, 0), 1e-8);

        final RenderedImage image1 = aggregatedMultiLevelImage.getImage(1);
        assertEquals(1, image1.getWidth());
        assertEquals(1, image1.getHeight());
        final Raster imageData1 = image1.getData();
        assertEquals(5.0, imageData1.getSampleDouble(0, 0, 0), 1e-8);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Max() throws Exception {
        final Band sourceBand = createSourceBand();
        final Band referenceBand = createReferenceBand();

        final MultiLevelImage aggregatedMultiLevelImage =
                Resample.createAggregatedMultiLevelImage(sourceBand, referenceBand, Resample.Type.MAX, Resample.Type.MEDIAN);

        assertNotNull(aggregatedMultiLevelImage);
        assertEquals(2, aggregatedMultiLevelImage.getModel().getLevelCount());

        final RenderedImage image0 = aggregatedMultiLevelImage.getImage(0);
        assertEquals(2, image0.getWidth());
        assertEquals(2, image0.getHeight());
        final Raster imageData0 = image0.getData();
        assertEquals(10.0, imageData0.getSampleDouble(0, 0, 0), 1e-8);
        assertEquals(11.0, imageData0.getSampleDouble(0, 1, 0), 1e-8);
        assertEquals(14.0, imageData0.getSampleDouble(1, 0, 0), 1e-8);
        assertEquals(15.0, imageData0.getSampleDouble(1, 1, 0), 1e-8);

        final RenderedImage image1 = aggregatedMultiLevelImage.getImage(1);
        assertEquals(1, image1.getWidth());
        assertEquals(1, image1.getHeight());
        final Raster imageData1 = image1.getData();
        assertEquals(10.0, imageData1.getSampleDouble(0, 0, 0), 1e-8);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Median() throws Exception {
        final Band sourceBand = createSourceBand_Median();
        final Band referenceBand = createReferenceBand();

        final MultiLevelImage aggregatedMultiLevelImage =
                Resample.createAggregatedMultiLevelImage(sourceBand, referenceBand, Resample.Type.MEDIAN, Resample.Type.MEDIAN);

        assertNotNull(aggregatedMultiLevelImage);
        assertEquals(2, aggregatedMultiLevelImage.getModel().getLevelCount());

        final RenderedImage image0 = aggregatedMultiLevelImage.getImage(0);
        assertEquals(2, image0.getWidth());
        assertEquals(2, image0.getHeight());
        final Raster imageData0 = image0.getData();
        assertEquals(4.0, imageData0.getSampleDouble(0, 0, 0), 1e-8);
        assertEquals(5.5, imageData0.getSampleDouble(0, 1, 0), 1e-8);
        assertEquals(5.5, imageData0.getSampleDouble(1, 0, 0), 1e-8);
        assertEquals(7.0, imageData0.getSampleDouble(1, 1, 0), 1e-8);

        final RenderedImage image1 = aggregatedMultiLevelImage.getImage(1);
        assertEquals(1, image1.getWidth());
        assertEquals(1, image1.getHeight());
        final Raster imageData1 = image1.getData();
        assertEquals(4.0, imageData1.getSampleDouble(0, 0, 0), 1e-8);
    }

    @Test
    public void testCreateAggregatedMultiLevelImage_Mean() throws Exception {
        final Band sourceBand = createSourceBand();
        final Band referenceBand = createReferenceBand();

        final MultiLevelImage aggregatedMultiLevelImage =
                Resample.createAggregatedMultiLevelImage(sourceBand, referenceBand, Resample.Type.MEAN, Resample.Type.MEDIAN);

        assertNotNull(aggregatedMultiLevelImage);
        assertEquals(2, aggregatedMultiLevelImage.getModel().getLevelCount());

        final RenderedImage image0 = aggregatedMultiLevelImage.getImage(0);
        assertEquals(2, image0.getWidth());
        assertEquals(2, image0.getHeight());
        final Raster imageData0 = image0.getData();
        assertEquals(7.5, imageData0.getSampleDouble(0, 0, 0), 1e-8);
        assertEquals(9.0, imageData0.getSampleDouble(0, 1, 0), 1e-8);
        assertEquals(13.5, imageData0.getSampleDouble(1, 0, 0), 1e-8);
        assertEquals(15.0, imageData0.getSampleDouble(1, 1, 0), 1e-8);

        final RenderedImage image1 = aggregatedMultiLevelImage.getImage(1);
        assertEquals(1, image1.getWidth());
        assertEquals(1, image1.getHeight());
        final Raster imageData1 = image1.getData();
        assertEquals(7.5, imageData1.getSampleDouble(0, 0, 0), 1e-8);
    }

    private Band createSourceBand_Median() {
        int sourceWidth = 4;
        int sourceHeight = 4;
        int sourceScaleX = 2;
        int sourceScaleY = 2;
        int sourceTranslateX = 2;
        int sourceTranslateY = 2;
        final Product sourceProduct = new Product("dummy", "dummy", sourceWidth, sourceHeight);
        final Band sourceBand = sourceProduct.addBand("sourceBand", "X + Y");
        sourceBand.setNoDataValue(-23);
        sourceBand.setImageToModelTransform(new AffineTransform(sourceScaleX, 0, 0, sourceScaleY, sourceTranslateX, sourceTranslateY));
        return sourceBand;
    }

    private Band createSourceBand() {
        int sourceWidth = 4;
        int sourceHeight = 4;
        int sourceScaleX = 2;
        int sourceScaleY = 2;
        int sourceTranslateX = 2;
        int sourceTranslateY = 2;
        final Product sourceProduct = new Product("dummy", "dummy", sourceWidth, sourceHeight);
        final Band sourceBand = sourceProduct.addBand("sourceBand", "((X - 0.5) * " +  sourceWidth + ") + (Y - 0.5)");
        sourceBand.setNoDataValue(-23);
        sourceBand.setImageToModelTransform(new AffineTransform(sourceScaleX, 0, 0, sourceScaleY, sourceTranslateX, sourceTranslateY));
        return sourceBand;
    }

    private Band createReferenceBand() {
        int referenceWidth = 2;
        int referenceHeight = 2;
        int referenceScaleX = 4;
        int referenceScaleY = 4;
        int referenceTranslateX = 4;
        int referenceTranslateY = 4;
        int referenceLevelCount = 2;
        final AffineTransform imageToModelTransform = new AffineTransform(referenceScaleX, 0, 0, referenceScaleY,
                                                                          referenceTranslateX, referenceTranslateY);
        final Band referenceBand = new Band("referenceBand", ProductData.TYPE_INT8, referenceWidth, referenceHeight);
        final DefaultMultiLevelModel referenceModel = new DefaultMultiLevelModel(referenceLevelCount, imageToModelTransform,
                                                                                 referenceWidth, referenceHeight);
        referenceBand.setSourceImage(new DefaultMultiLevelImage(new AbstractMultiLevelSource(referenceModel) {
            @Override
            protected RenderedImage createImage(int level) {
                return new BufferedImage(referenceWidth / (1 + level), referenceHeight / (1 + level), ProductData.TYPE_INT8);
            }
        }));
        return referenceBand;
    }

}