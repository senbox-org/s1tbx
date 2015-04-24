package org.esa.snap.framework.datamodel;

import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.junit.Test;

import java.awt.geom.AffineTransform;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class BandSceneRasterTransformTest {
    @Test
    public void testNoProduct() throws Exception {
        Band band1 = new VirtualBand("B1", ProductData.TYPE_INT16, 10, 20, "X");

        assertNull(band1.getSceneRasterTransform());

        DefaultSceneRasterTransform transform = new DefaultSceneRasterTransform(new AffineTransform2D(1, 2, 3, 4, 5, 6), new AffineTransform2D(1, 2, 3, 4, 5, 6));
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
    public void testWithGeoCoding() throws Exception {

        Product product = new Product("A", "B", 10, 20);
        TiePointGrid lat = new TiePointGrid("lat", 2, 2, 0, 0, 10 - 1, 20 - 1, new float[]{0f, 1f, 2f, 3f});
        TiePointGrid lon = new TiePointGrid("lon", 2, 2, 0, 0, 10 - 1, 20 - 1, new float[]{1f, 2f, 3f, 4f});
        product.addTiePointGrid(lat);
        product.addTiePointGrid(lon);
        product.setGeoCoding(new TiePointGeoCoding(lat, lon));

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
    public void testCustomTransfoprm() throws Exception {

        Product product = new Product("A", "B", 10, 20);

        Band band1 = new VirtualBand("B1", ProductData.TYPE_INT16, 10, 20, "X");
        product.addBand(band1);

        assertSame(SceneRasterTransform.IDENTITY, band1.getSceneRasterTransform());

        DefaultSceneRasterTransform transform = new DefaultSceneRasterTransform(new AffineTransform2D(1, 2, 3, 4, 5, 6), new AffineTransform2D(1, 2, 3, 4, 5, 6));
        band1.setSceneRasterTransform(transform);

        assertSame(transform, band1.getSceneRasterTransform());
    }
}
