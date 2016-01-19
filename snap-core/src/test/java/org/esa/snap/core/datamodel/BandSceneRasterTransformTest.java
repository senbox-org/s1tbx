package org.esa.snap.core.datamodel;

import org.esa.snap.core.transform.MathTransform2D;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.referencing.operation.NoninvertibleTransformException;

import java.awt.geom.AffineTransform;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class BandSceneRasterTransformTest {

    @Test(expected = NullPointerException.class)
    public void testNotNull() throws Exception {
        Band band1 = new VirtualBand("B1", ProductData.TYPE_INT16, 10, 20, "X");
        band1.setSceneRasterTransform(null);
    }

    @Test
    public void testNoProduct() throws Exception {
        Band band1 = new VirtualBand("B1", ProductData.TYPE_INT16, 10, 20, "X");

        assertSame(SceneRasterTransform.IDENTITY, band1.getSceneRasterTransform());

        DefaultSceneRasterTransform transform = new DefaultSceneRasterTransform(new AffineMathTransform2D(1, 2, 3, 4, 5, 6), new AffineMathTransform2D(1, 2, 3, 4, 5, 6));
        band1.setSceneRasterTransform(transform);

        assertSame(transform, band1.getSceneRasterTransform());
    }

    @Test
    public void testWithoutGeoCoding() throws Exception {

        Product product = new Product("A", "B", 10, 20);
        Band band1 = new VirtualBand("B1", ProductData.TYPE_INT16, 10, 20, "X");
        Band band2 = new VirtualBand("B2", ProductData.TYPE_INT16, 5, 10, "Y");
        product.addBand(band1);
        product.addBand(band2);

        SceneRasterTransform t1 = band1.getSceneRasterTransform();
        SceneRasterTransform t2 = band2.getSceneRasterTransform();
        assertSame(SceneRasterTransform.IDENTITY, t1);
        assertSame(SceneRasterTransform.IDENTITY, t2);
    }

    @Test
    @Ignore
    // test does not work anymore, as a change of geocodings would only work when the geocodings are crs geocodings
    public void testWithGeoCoding() throws Exception {

        Product product = new Product("A", "B", 10, 20);
        TiePointGrid lat = new TiePointGrid("lat", 2, 2, 0, 0, 10 - 1, 20 - 1, new float[]{0f, 1f, 2f, 3f});
        TiePointGrid lon = new TiePointGrid("lon", 2, 2, 0, 0, 10 - 1, 20 - 1, new float[]{1f, 2f, 3f, 4f});
        product.addTiePointGrid(lat);
        product.addTiePointGrid(lon);
        product.setSceneGeoCoding(new TiePointGeoCoding(lat, lon));

        Band band1 = new VirtualBand("B1", ProductData.TYPE_INT16, 10, 20, "X");
        Band band2 = new VirtualBand("B2", ProductData.TYPE_INT16, 5, 10, "Y");
        product.addBand(band1);
        product.addBand(band2);

        assertSame(SceneRasterTransform.IDENTITY, band1.getSceneRasterTransform());
        assertSame(SceneRasterTransform.IDENTITY, band2.getSceneRasterTransform());

        band2.setGeoCoding(new TiePointGeoCoding(lon, lat));
        assertSame(SceneRasterTransform.IDENTITY, band1.getSceneRasterTransform());
        assertNotNull(band2.getSceneRasterTransform());
        assertNotSame(SceneRasterTransform.IDENTITY, band2.getSceneRasterTransform());
    }

    @Test
    public void testCustomTransform() throws Exception {

        Product product = new Product("A", "B", 10, 20);

        Band band1 = new VirtualBand("B1", ProductData.TYPE_INT16, 10, 20, "X");
        product.addBand(band1);

        assertSame(SceneRasterTransform.IDENTITY, band1.getSceneRasterTransform());
        assertSame(SceneRasterTransform.IDENTITY, band1.getSceneRasterTransform());

        DefaultSceneRasterTransform transform = new DefaultSceneRasterTransform(new AffineMathTransform2D(1, 2, 3, 4, 5, 6), new AffineMathTransform2D(1, 2, 3, 4, 5, 6));
        band1.setSceneRasterTransform(transform);

        assertSame(transform, band1.getSceneRasterTransform());
    }

    private class AffineMathTransform2D extends AffineTransform2D implements MathTransform2D {

        private final AffineTransform transform;

        /**
         * Constructs a new affine transform with the same coefficient than the specified transform.
         */
        public AffineMathTransform2D(AffineTransform transform) {
            super(transform);
            this.transform = transform;
        }

        /**
         * Constructs a new {@code AffineTransform2D} from 6 values representing the 6 specifiable
         * entries of the 3&times;3 transformation matrix. Those values are given unchanged to the
         * {@link AffineTransform#AffineTransform(double, double, double, double, double, double) super
         * class constructor}.
         *
         * @since 2.5
         */
        public AffineMathTransform2D(double m00, double m10, double m01, double m11, double m02, double m12) {
            super(m00, m10, m01, m11, m02, m12);
            transform = new AffineTransform(m00, m10, m01, m11, m02, m12);
        }

        /**
         * Creates the inverse transform of this object.
         *
         * @throws NoninvertibleTransformException if this transform can't be inverted.
         */
        @Override
        public MathTransform2D inverse() throws NoninvertibleTransformException {
            try {
                return new AffineMathTransform2D(transform.createInverse());
            } catch (java.awt.geom.NoninvertibleTransformException e) {
                throw new NoninvertibleTransformException(e.getMessage());
            }
        }
    }
}
