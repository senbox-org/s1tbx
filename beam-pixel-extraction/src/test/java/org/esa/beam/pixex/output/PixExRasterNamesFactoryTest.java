package org.esa.beam.pixex.output;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.junit.*;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class PixExRasterNamesFactoryTest {

    private Product product;

    @Before
    public void setUp() throws Exception {
        product = new Product("name", "type", 10, 10);
        product.addBand("val1", ProductData.TYPE_INT16);
        product.addBand("val2", ProductData.TYPE_INT16);
        product.addBand("val3", ProductData.TYPE_INT16);
        product.addTiePointGrid(newTiePointGrid("tp1"));
        product.addTiePointGrid(newTiePointGrid("tp2"));
        product.addTiePointGrid(newTiePointGrid("tp3"));
        final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        maskGroup.add(newMask("mask1"));
        maskGroup.add(newMask("mask2"));
        maskGroup.add(newMask("mask3"));
        maskGroup.remove(maskGroup.get(Product.PIN_MASK_NAME));
        maskGroup.remove(maskGroup.get(Product.GCP_MASK_NAME));
    }

    @Test
    public void testGetRasterNamesToBeExported_exportAll() {
        // preparation
        final boolean exportBands = true;
        final boolean exportTiePoints = true;
        final boolean exportMasks = true;
        final PixExRasterNamesFactory pixExRasterNamesFactory = new PixExRasterNamesFactory(exportBands, exportTiePoints, exportMasks);

        // execution
        final String[] rasterNames = pixExRasterNamesFactory.getRasterNames(product);
        System.out.println("rasterNames = " + Arrays.toString(rasterNames));

        // verifying
        final String[] expected = {"val1", "val2", "val3", "tp1", "tp2", "tp3", "mask1", "mask2", "mask3"};
        assertThat(rasterNames, equalTo(expected));
    }
    @Test
    public void testGetRasterNamesToBeExported_exportBandsOnly() {
        // preparation
        final boolean exportBands = true;
        final boolean exportTiePoints = false;
        final boolean exportMasks = false;
        final PixExRasterNamesFactory pixExRasterNamesFactory = new PixExRasterNamesFactory(exportBands, exportTiePoints, exportMasks);

        // execution
        final String[] rasterNames = pixExRasterNamesFactory.getRasterNames(product);
        System.out.println("rasterNames = " + Arrays.toString(rasterNames));

        // verifying
        final String[] expected = {"val1", "val2", "val3"};
        assertThat(rasterNames, equalTo(expected));
    }
    @Test
    public void testGetRasterNamesToBeExported_exportTiepointsOnyl() {
        // preparation
        final boolean exportBands = false;
        final boolean exportTiePoints = true;
        final boolean exportMasks = false;
        final PixExRasterNamesFactory pixExRasterNamesFactory = new PixExRasterNamesFactory(exportBands, exportTiePoints, exportMasks);

        // execution
        final String[] rasterNames = pixExRasterNamesFactory.getRasterNames(product);
        System.out.println("rasterNames = " + Arrays.toString(rasterNames));

        // verifying
        final String[] expected = {"tp1", "tp2", "tp3"};
        assertThat(rasterNames, equalTo(expected));
    }
    @Test
    public void testGetRasterNamesToBeExported_exportMasksOnly() {
        // preparation
        final boolean exportBands = false;
        final boolean exportTiePoints = false;
        final boolean exportMasks = true;
        final PixExRasterNamesFactory pixExRasterNamesFactory = new PixExRasterNamesFactory(exportBands, exportTiePoints, exportMasks);

        // execution
        final String[] rasterNames = pixExRasterNamesFactory.getRasterNames(product);
        System.out.println("rasterNames = " + Arrays.toString(rasterNames));

        // verifying
        final String[] expected = {"mask1", "mask2", "mask3"};
        assertThat(rasterNames, equalTo(expected));
    }

    ///////////////////////////////
    ////  Test Helper Methods  ////
    ///////////////////////////////

    private TiePointGrid newTiePointGrid(String name) {
        return new TiePointGrid(name, 10, 10, 0, 0, 1, 1, new float[10 * 10]);
    }

    private Mask newMask(String name) {
        return new Mask(name, 10,10, new TestImageType());
    }

    private static class TestImageType extends Mask.ImageType {

        public TestImageType() {
            super("type");
        }

        @Override
        public MultiLevelImage createImage(Mask mask) {
            return null;
        }

    }
}
