package org.esa.snap.core.gpf.multisize;

import com.bc.ceres.glevel.MultiLevelModel;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.common.BandMathsOp;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.fail;

public class MultiSizeSupportTest {

    private Product product;

    //This class is written to deal with the test cases described here:
    //https://senbox.atlassian.net/wiki/display/SNAP/Test+Case+Descriptions

    @Before
    public void setUp() {
        product = new Product("dummyProduct", "dummyType", 5, 5);
    }

    @Test
    @Ignore
    public void testSubset() {
        final AffineTransform imageToModelTransform = Product.findImageToModelTransform(product.getSceneGeoCoding());
        final int height = product.getSceneRasterHeight();
        final int width = product.getSceneRasterWidth();
        double xCoord1 = width * 0.25;
        double xCoord2 = width * 0.75;
        double yCoord1 = height * 0.75;
        double yCoord2 = height * 0.75;
        final Point2D subsetCoordinate1 = imageToModelTransform.transform(new PixelPos(xCoord1, yCoord1), null);
        final Point2D subsetCoordinate2 = imageToModelTransform.transform(new PixelPos(xCoord1, yCoord2), null);
        final Point2D subsetCoordinate3 = imageToModelTransform.transform(new PixelPos(xCoord2, yCoord1), null);
        final Point2D subsetCoordinate4 = imageToModelTransform.transform(new PixelPos(xCoord2, yCoord2), null);
        final Rectangle2D.Double subsetImageBounds = new Rectangle2D.Double(xCoord1, yCoord1, xCoord2 - xCoord1, yCoord2 - yCoord1);
        final Rectangle2D subsetModelBounds = imageToModelTransform.createTransformedShape(subsetImageBounds).getBounds2D();
        GeometryFactory gf = new GeometryFactory();
        Polygon polygon = gf.createPolygon(gf.createLinearRing(new Coordinate[]{
                new Coordinate(subsetCoordinate1.getX(), subsetCoordinate1.getY()),
                new Coordinate(subsetCoordinate2.getX(), subsetCoordinate2.getY()),
                new Coordinate(subsetCoordinate3.getX(), subsetCoordinate3.getY()),
                new Coordinate(subsetCoordinate4.getX(), subsetCoordinate4.getY()),
                new Coordinate(subsetCoordinate1.getX(), subsetCoordinate1.getY()),
        }), null);

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("geoRegion", polygon.toText());

        final Product subset = GPF.createProduct("Subset", parameters, product);

        assertEquals(width / 2, subset.getSceneRasterWidth());
        assertEquals(height / 2, subset.getSceneRasterHeight());

        final ProductNodeGroup<Band> productBandGroup = product.getBandGroup();
        final Rectangle2D[] expectedImageSizes = new Rectangle2D[productBandGroup.getNodeCount()];
        for (int i = 0; i < productBandGroup.getNodeCount(); i++) {
            final Band band = productBandGroup.get(i);
            final MultiLevelModel multiLevelModel = band.getMultiLevelModel();
            final Rectangle2D modelBounds = multiLevelModel.getModelBounds();
            final Rectangle2D intersection = modelBounds.createIntersection(subsetModelBounds);
            expectedImageSizes[i] = multiLevelModel.getModelToImageTransform(0).createTransformedShape(intersection).getBounds2D();
        }

        final ProductNodeGroup<Band> subsetBandGroup = subset.getBandGroup();
        for (int i = 0; i < subsetBandGroup.getNodeCount(); i++) {
            assertEquals(expectedImageSizes[i].getWidth(), subsetBandGroup.get(i).getRasterWidth());
            assertEquals(expectedImageSizes[i].getHeight(), subsetBandGroup.get(i).getRasterHeight());
        }
    }

    @Test
    @Ignore
    public void testReprojection() {
        final String WGS84_CODE = "EPSG:4326";
        Map<String, Object> parameterMap = new HashMap<String, Object>(5);
        parameterMap.put("crs", WGS84_CODE);
        final Product reprojectedProduct = GPF.createProduct("Reproject", parameterMap, product);
        assertNotNull(reprojectedProduct.getSceneGeoCoding());
        final ProductNodeGroup<Band> productBandGroup = product.getBandGroup();
        final ProductNodeGroup<Band> reprojectedProductBandGroup = reprojectedProduct.getBandGroup();
        for (int i = 0; i < productBandGroup.getNodeCount() - 1; i++) {
            final Band band1 = productBandGroup.get(i);
            final CoordinateReferenceSystem modelCrs1 = Product.findModelCRS(band1.getGeoCoding());
            final Band reprojectedBand1 = reprojectedProductBandGroup.get(band1.getName());
            final CoordinateReferenceSystem reprojectedModelCrs1 = Product.findModelCRS(reprojectedBand1.getGeoCoding());
            for (int j = i + 1; j < productBandGroup.getNodeCount(); j++) {
                final Band band2 = productBandGroup.get(j);
                final CoordinateReferenceSystem modelCrs2 = Product.findModelCRS(band2.getGeoCoding());
                final Band reprojectedBand2 = reprojectedProductBandGroup.get(band2.getName());
                final CoordinateReferenceSystem reprojectedModelCrs2 = Product.findModelCRS(reprojectedBand2.getGeoCoding());
                assertEquals(band1.getRasterSize().equals(band2.getRasterSize()),
                             reprojectedBand1.getRasterSize().equals(reprojectedBand2.getRasterSize()));
                assertEquals(modelCrs1 == modelCrs2, reprojectedModelCrs1 == reprojectedModelCrs2);
            }
        }
    }

    @Test
    @Ignore
    public void testBandMaths() {
        final ProductNodeGroup<Band> bandGroup = product.getBandGroup();
        for (int i = 0; i < bandGroup.getNodeCount() - 1; i++) {
            final Band band1 = bandGroup.get(i);
            for (int j = i; j < bandGroup.getNodeCount(); j++) {
                final Band band2 = bandGroup.get(j);
                Map<String, Object> parameters = new HashMap<>();
                BandMathsOp.BandDescriptor[] bandDescriptors = new BandMathsOp.BandDescriptor[1];
                bandDescriptors[0] = createBandDescription("aBandName", band1.getName() + " + " + band2.getName(),
                                                           ProductData.TYPESTRING_FLOAT32, "bigUnits", null, null);
                parameters.put("targetBands", bandDescriptors);
                try {
                    GPF.createProduct("BandMaths", parameters, product);
                    if (!band1.getRasterSize().equals(band2.getRasterSize())) {
                        fail("Could create band maths for differently sized bands");
                    }
                } catch (OperatorException oe) {
                    if (band1.getRasterSize().equals(band2.getRasterSize())) {
                        fail("Could not create band maths for equally sized bands: " + oe.getMessage());
                    }
                }
            }
        }
    }

    private static BandMathsOp.BandDescriptor createBandDescription(String bandName, String expression, String type, String unit, Double scalingFactor, Double scalingOffset) {
        BandMathsOp.BandDescriptor bandDescriptor = new BandMathsOp.BandDescriptor();
        bandDescriptor.name = bandName;
        bandDescriptor.description = "aDescription";
        bandDescriptor.expression = expression;
        bandDescriptor.type = type;
        bandDescriptor.unit = unit;
        bandDescriptor.scalingFactor = scalingFactor;
        bandDescriptor.scalingOffset = scalingOffset;
        return bandDescriptor;
    }

//    @Test
//    public void testStatisticsOp() {
//        GPF.createProduct("StatisticsOp")
//    }

} 