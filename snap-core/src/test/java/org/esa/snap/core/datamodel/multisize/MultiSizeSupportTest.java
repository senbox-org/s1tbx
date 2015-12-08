package org.esa.snap.core.datamodel.multisize;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.util.ProductUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;

public class MultiSizeSupportTest {

    //This class is written to deal with the test cases described here:
    //https://senbox.atlassian.net/wiki/display/SNAP/Test+Case+Descriptions

    private Product product;

    @Before
    public void setUp() {
        product = new Product("du", "mmy");
    }

    @Test
    @Ignore
    public void testThatEachProductHasAnExpectedModelCRS() {
        final CoordinateReferenceSystem productModelCrs = product.getSceneCRS();
        assertNotNull(productModelCrs);
        final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        for (int i = 0; i < bandGroup.getNodeCount(); i++) {
            assertSame(productModelCrs, bandGroup.get(i).getGeoCoding());
        }
    }

    @Test
    @Ignore
    public void testThatImagePixelPositionsAreCorrectlyTransformedIntoModelCoordinates() {
        ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        //todo get model coordinates for corner pixel positions
        PixelPos[][] expectedModelCoordinates = new PixelPos[bandGroup.getNodeCount()][];
        for (int i = 0; i < bandGroup.getNodeCount(); i++) {
            Band band = bandGroup.get(i);
            AffineTransform imageToModelTransform = Product.findImageToModelTransform(band.getGeoCoding());
            double cornerWidth = band.getRasterWidth() - 1.5;
            double cornerHeight = band.getRasterHeight() - 1.5;
            assertEquals(expectedModelCoordinates[i][0], imageToModelTransform.transform(new PixelPos(0.5, 0.5), null));
            assertEquals(expectedModelCoordinates[i][1], imageToModelTransform.transform(new PixelPos(0.5, cornerHeight), null));
            assertEquals(expectedModelCoordinates[i][2], imageToModelTransform.transform(new PixelPos(cornerWidth, 0.5), null));
            assertEquals(expectedModelCoordinates[i][3], imageToModelTransform.transform(new PixelPos(cornerWidth, cornerHeight), null));
        }
    }

    @Test
    @Ignore
    public void testThatModelCoordinatesAreCorrectlyTransformedIntoImagePixelPositions() throws NoninvertibleTransformException {
        ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        //todo get model coordinates for corner pixel positions
        PixelPos[][] modelCoordinates = new PixelPos[bandGroup.getNodeCount()][];
        for (int i = 0; i < bandGroup.getNodeCount(); i++) {
            Band band = bandGroup.get(i);
            AffineTransform modelToImageTransform = Product.findImageToModelTransform(band.getGeoCoding()).createInverse();
            double cornerWidth = band.getRasterWidth() - 1.5;
            double cornerHeight = band.getRasterHeight() - 1.5;
            assertEquals(new PixelPos(0.5, 0.5), modelToImageTransform.transform(modelCoordinates[i][0], null));
            assertEquals(new PixelPos(0.5, cornerHeight), modelToImageTransform.transform(modelCoordinates[i][1], null));
            assertEquals(new PixelPos(cornerWidth, 0.5), modelToImageTransform.transform(modelCoordinates[i][2], null));
            assertEquals(new PixelPos(cornerWidth, cornerHeight), modelToImageTransform.transform(modelCoordinates[i][3], null));
        }
    }

    @Test
    @Ignore
    public void testPixelExtractionAsInSpectrumView() {
        final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        //todo get model coordinates for corner pixel positions
        double[][] expectedGeophysicalValues = new double[bandGroup.getNodeCount()][];
        for (int i = 0; i < bandGroup.getNodeCount(); i++) {
            final Band band = bandGroup.get(i);
            double cornerWidth = band.getRasterWidth() - 1.5;
            double cornerHeight = band.getRasterHeight() - 1.5;
            assertEquals(expectedGeophysicalValues[i][0], readEnergyAsInSpectrumTopComponent(new PixelPos(0.5, 0.5), band, 0), 1e-8);
            assertEquals(expectedGeophysicalValues[i][1], readEnergyAsInSpectrumTopComponent(new PixelPos(0.5, cornerHeight), band, 0), 1e-8);
            assertEquals(expectedGeophysicalValues[i][2], readEnergyAsInSpectrumTopComponent(new PixelPos(cornerWidth, 0.5), band, 0), 1e-8);
            assertEquals(expectedGeophysicalValues[i][3], readEnergyAsInSpectrumTopComponent(new PixelPos(cornerWidth, cornerHeight), band, 0), 1e-8);
        }
    }

    @Test
    @Ignore
    public void testThatBandsWithSameRasterSizeHaveSameMultiLevelModel() {
        final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        for (int i = 0; i < bandGroup.getNodeCount() - 1; i++) {
            final Band band1 = bandGroup.get(i);
            final MultiLevelModel multiLevelModel1 = band1.getMultiLevelModel();
            for (int j = i + 1; j < bandGroup.getNodeCount(); j++) {
                final Band band2 = bandGroup.get(j);
                if (band1.getRasterSize().equals(band2.getRasterSize())) {
                    assertSame(multiLevelModel1, band2.getMultiLevelModel());
                }
            }
        }
    }

    //todo add test to check whether different bands have different tile sizes

    @Test
    @Ignore
    public void testThatValidMasksOfBandsHaveSameMultiLevelModelAsBand() {
        final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        for (int i = 0; i < bandGroup.getNodeCount(); i++) {
            final Band band = bandGroup.get(i);
            final MultiLevelImage validMaskImage = band.getValidMaskImage();
            if (validMaskImage != null) {
                assertSame(validMaskImage.getModel(), band.getMultiLevelModel());
            }
        }
    }

    //todo add test to check whether different bands have different tile sizes

    //method copied from SpectrumTopComponent
    private double readEnergyAsInSpectrumTopComponent(PixelPos pixelPos, Band spectralBand, int level) {
        final MultiLevelModel multiLevelModel = spectralBand.getMultiLevelModel();
        final AffineTransform i2mTransform = multiLevelModel.getImageToModelTransform(0);
        final AffineTransform m2iTransform = multiLevelModel.getModelToImageTransform(level);
        final Point2D modelPixel = i2mTransform.transform(pixelPos, null);
        final Point2D imagePixel = m2iTransform.transform(modelPixel, null);
        int pinPixelX = (int) Math.floor(imagePixel.getX());
        int pinPixelY = (int) Math.floor(imagePixel.getY());
        if (spectralBand.isPixelValid(pinPixelX, pinPixelY)) {
            return ProductUtils.getGeophysicalSampleAsDouble(spectralBand, pinPixelX, pinPixelY, level);
        }
        return spectralBand.getGeophysicalNoDataValue();
    }

} 