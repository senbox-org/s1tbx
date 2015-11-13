package org.esa.snap.core.datamodel;

import com.bc.ceres.glevel.MultiLevelImage;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.junit.Test;
import org.opengis.referencing.operation.MathTransform;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import static junit.framework.TestCase.assertEquals;

public class ModelTransformationTest {

    private static final int subSampling = 4;
    private static final int tiePointGridWidth = 3;
    private static final int tiePointGridHeight = 5;
    private static final int productWidth = 10;
    private static final int productHeight = 20;
    private static final float LAT_1 = 53.0f;
    private static final float LAT_2 = 50.0f;
    private static final float LON_1 = 10.0f;
    private static final float LON_2 = 15.0f;

    @Test
    public void testModelTransformations() throws NoninvertibleTransformException {
        Product product = new Product("test", "test", productWidth, productHeight);
        TiePointGrid latGrid = new TiePointGrid("latGrid", tiePointGridWidth, tiePointGridHeight, 0.5, 0.5, subSampling, subSampling, createLatGridData());
        TiePointGrid lonGrid = new TiePointGrid("lonGrid", tiePointGridWidth, tiePointGridHeight, 0.5, 0.5, subSampling, subSampling, createLonGridData());
        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));

        final Band band1 = product.addBand("band1", "(X * 10) + Y", ProductData.TYPE_INT8);

        Band band2 = new Band("band2", ProductData.TYPE_INT8, productWidth - 2, productHeight - 4);
        product.addBand(band2);
        final MultiLevelImage sourceImage2 = VirtualBand.createSourceImage(band2, "(X * 8) + Y");
        band2.setSourceImage(sourceImage2);
        final AffineTransform affineTransform2 = new AffineTransform();
        affineTransform2.translate(1, 2);
        final SubTiePointGeoCoding geoCoding2 =
                new SubTiePointGeoCoding(latGrid, lonGrid, new AffineTransform2D(affineTransform2));
        band2.setGeoCoding(geoCoding2);

        Band band3 = new Band("band3", ProductData.TYPE_INT8, 4, 4);
        product.addBand(band3);
        final MultiLevelImage sourceImage3 = VirtualBand.createSourceImage(band3, "(X * 4) + Y");
        band3.setSourceImage(sourceImage3);
        final AffineTransform affineTransform3 = new AffineTransform();
        affineTransform3.translate(2, 4);
        affineTransform3.scale(2, 4);
        final SubTiePointGeoCoding geoCoding3 =
                new SubTiePointGeoCoding(latGrid, lonGrid, new AffineTransform2D(affineTransform3));
        band3.setGeoCoding(geoCoding3);

        final AffineTransform imageToModelTransform1 = Product.findImageToModelTransform(band1.getGeoCoding());
        assertEquals(new Point2D.Double(0, 0),
                     imageToModelTransform1.transform(new Point2D.Double(0, 0), null));
        assertEquals(new Point2D.Double(0, productHeight - 1),
                     imageToModelTransform1.transform(new Point2D.Double(0, productHeight - 1), null));
        assertEquals(new Point2D.Double(productWidth - 1, 0),
                     imageToModelTransform1.transform(new Point2D.Double(productWidth - 1, 0), null));
        assertEquals(new Point2D.Double(productWidth - 1, productHeight - 1),
                     imageToModelTransform1.transform(new Point2D.Double(productWidth - 1, productHeight - 1), null));

        final AffineTransform imageToModelTransform2 = Product.findImageToModelTransform(band2.getGeoCoding());
        assertEquals(new Point2D.Double(1, 2),
                     imageToModelTransform2.transform(new Point2D.Double(0, 0), null));
        assertEquals(new Point2D.Double(1, 17),
                     imageToModelTransform2.transform(new Point2D.Double(0, 15), null));
        assertEquals(new Point2D.Double(8, 2),
                     imageToModelTransform2.transform(new Point2D.Double(7, 0), null));
        assertEquals(new Point2D.Double(8, 17),
                     imageToModelTransform2.transform(new Point2D.Double(7, 15), null));

        final AffineTransform imageToModelTransform3 = Product.findImageToModelTransform(band3.getGeoCoding());
        assertEquals(new Point2D.Double(2, 4),
                     imageToModelTransform3.transform(new Point2D.Double(0, 0), null));
        assertEquals(new Point2D.Double(2, 16),
                     imageToModelTransform3.transform(new Point2D.Double(0, 3), null));
        assertEquals(new Point2D.Double(8, 4),
                     imageToModelTransform3.transform(new Point2D.Double(3, 0), null));
        assertEquals(new Point2D.Double(8, 16),
                     imageToModelTransform3.transform(new Point2D.Double(3, 3), null));
    }

    //todo add test for getting pixel info -> product.getPixelInfoString
    //todo add test for reprojection

    private float[] createLatGridData() {
        return createLatGridData(LAT_1, LAT_2);
    }

    private float[] createLonGridData() {
        return createLonGridData(LON_1, LON_2);
    }

    private static float[] createLatGridData(float lat0, float lat1) {
        float[] floats = new float[tiePointGridWidth * tiePointGridHeight];

        for (int j = 0; j < tiePointGridHeight; j++) {
            for (int i = 0; i < tiePointGridWidth; i++) {
                float x = i / (tiePointGridWidth - 1.0f);
                float y = j / (tiePointGridHeight - 1.0f);
                floats[j * tiePointGridWidth + i] = lat0 + (lat1 - lat0) * y * y + 0.1f * (lat1 - lat0) * x * x;
            }
        }
        return floats;
    }

    private static float[] createLonGridData(float lon0, float lon1) {
        float[] floats = new float[tiePointGridWidth * tiePointGridHeight];

        for (int j = 0; j < tiePointGridHeight; j++) {
            for (int i = 0; i < tiePointGridWidth; i++) {
                float x = i / (tiePointGridWidth - 1.0f);
                float y = j / (tiePointGridHeight - 1.0f);
                final int index = j * tiePointGridWidth + i;
                floats[(index)] = lon0 + (lon1 - lon0) * x * x + 0.1f * (lon1 - lon0) * y * y;
            }
        }
        return floats;
    }

    private class SubTiePointGeoCoding extends TiePointGeoCoding {

        private final AffineTransform2D transform;
        private final AffineTransform inverse;

        public SubTiePointGeoCoding(TiePointGrid latGrid, TiePointGrid lonGrid, AffineTransform2D transform)
                throws NoninvertibleTransformException {
            super(latGrid, lonGrid);
            this.transform = transform;
            inverse = transform.createInverse();
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            PixelPos transformedPixelPos = new PixelPos();
            transform.transform(pixelPos, transformedPixelPos);
            return super.getGeoPos(transformedPixelPos, geoPos);
        }

        @Override
        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            pixelPos = super.getPixelPos(geoPos, pixelPos);
            PixelPos transformedPixelPos = new PixelPos();
            inverse.transform(pixelPos, transformedPixelPos);
            pixelPos.setLocation(transformedPixelPos);
            return transformedPixelPos;
        }

        @Override
        public MathTransform getImageToMapTransform() {
            return transform;
        }
    }

} 