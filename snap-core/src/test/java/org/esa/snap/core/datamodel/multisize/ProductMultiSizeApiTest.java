package org.esa.snap.core.datamodel.multisize;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Norman
 */
public class ProductMultiSizeApiTest {

    @Test(expected = NullPointerException.class)
    public void testModelCrsCannotBeNull() throws Exception {
        Product product = new Product("N", "T");
        product.setSceneCRS(null);
    }

    @Test
    public void testModelCrsDefault() throws Exception {
        Product product = new Product("N", "T");
        assertEquals(Product.DEFAULT_IMAGE_CRS, product.getSceneCRS());
    }

    @Test
    public void testModelCrsSetterGetter() throws Exception {
        Product product = new Product("N", "T");
        product.setSceneCRS(DefaultGeographicCRS.WGS84);
        assertEquals(DefaultGeographicCRS.WGS84, product.getSceneCRS());
    }

    @Test
    public void testModelCrsWithMapCSGeoCoding() throws Exception {
        Product product = new Product("N", "T");
        DefaultProjectedCRS biboCrs = new DefaultProjectedCRS("bibo", DefaultGeographicCRS.WGS84, IdentityTransform.create(2), DefaultCartesianCS.PROJECTED);
        product.setSceneGeoCoding(new CrsGeoCoding(biboCrs, 10, 10, 0, 0, 1, 1));
        assertEquals(biboCrs, product.getSceneCRS());
    }

    @Test
    public void testModelCrsWithSatelliteCSGeoCoding() throws Exception {
        Product product = new Product("N", "T");
        TiePointGrid lat = new TiePointGrid("lat", 4, 4, 0, 0, 4, 4, new float[16]);
        TiePointGrid lon = new TiePointGrid("lon", 4, 4, 0, 0, 4, 4, new float[16]);
        product.addTiePointGrid(lat);
        product.addTiePointGrid(lon);
        product.setSceneGeoCoding(new TiePointGeoCoding(lat, lon));
        assertEquals(product.getSceneGeoCoding().getImageCRS(), product.getSceneCRS());
    }
}
