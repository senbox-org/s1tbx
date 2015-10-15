package org.esa.snap.pixex.output;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.pixex.aggregators.AggregatorStrategy;
import org.esa.snap.pixex.calvalus.ma.Record;
import org.junit.Before;
import org.junit.Test;

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
    }

    @Test
    public void testGetRasterNamesToBeExported_exportAll() {
        // preparation
        final boolean exportBands = true;
        final boolean exportTiePoints = true;
        final boolean exportMasks = true;
        final PixExRasterNamesFactory pixExRasterNamesFactory = new PixExRasterNamesFactory(exportBands,
                                                                                            exportTiePoints,
                                                                                            exportMasks, null);

        // execution
        final String[] rasterNames = pixExRasterNamesFactory.getRasterNames(product);

        // verifying
        final String[] expected = {"val1", "val2", "val3", "tp1", "tp2", "tp3", "mask1", "mask2", "mask3"};
        assertThat(rasterNames, equalTo(expected));
    }

    @Test
    public void testGetRasterNamesToBeExported_exportBandsOnly() {
        // preparation
        final boolean exportBands = true;
        final PixExRasterNamesFactory pixExRasterNamesFactory = new PixExRasterNamesFactory(exportBands, false, false,
                                                                                            null);

        // execution
        final String[] rasterNames = pixExRasterNamesFactory.getRasterNames(product);

        // verifying
        final String[] expected = {"val1", "val2", "val3"};
        assertThat(rasterNames, equalTo(expected));
    }

    @Test
    public void testGetRasterNamesToBeExported_exportTiepointsOnyl() {
        // preparation
        final boolean exportTiePoints = true;
        final PixExRasterNamesFactory pixExRasterNamesFactory = new PixExRasterNamesFactory(false, exportTiePoints,
                                                                                            false, null);

        // execution
        final String[] rasterNames = pixExRasterNamesFactory.getRasterNames(product);

        // verifying
        final String[] expected = {"tp1", "tp2", "tp3"};
        assertThat(rasterNames, equalTo(expected));
    }

    @Test
    public void testGetRasterNamesToBeExported_exportMasksOnly() {
        // preparation
        final boolean exportMasks = true;
        final PixExRasterNamesFactory pixExRasterNamesFactory = new PixExRasterNamesFactory(false, false, exportMasks,
                                                                                            null);

        // execution
        final String[] rasterNames = pixExRasterNamesFactory.getRasterNames(product);

        // verifying
        final String[] expected = {"mask1", "mask2", "mask3"};
        assertThat(rasterNames, equalTo(expected));
    }

    @Test
    public void testGetRasterNamesWithAggregationStrategy() {
        // preparation
        final AggregatorStrategy aggregatorStrategy = new AggregatorStrategy() {

            @Override
            public Number[] getValues(Record record, int rasterIndex) {
                return new Number[0];
            }

            @Override
            public int getValueCount() {
                return 3;
            }

            @Override
            public String[] getSuffixes() {
                return new String[]{"first", "second", "last"};
            }

        };
        final PixExRasterNamesFactory pixExRasterNamesFactory = new PixExRasterNamesFactory(true, false, false,
                                                                                            aggregatorStrategy);

        // execution
        final String[] rasterNames = pixExRasterNamesFactory.getRasterNames(product);

        // verifying
        final String[] expected = {
                "val1_first",
                "val1_second",
                "val1_last",
                "val2_first",
                "val2_second",
                "val2_last",
                "val3_first",
                "val3_second",
                "val3_last"
        };
        assertThat(rasterNames, equalTo(expected));
    }

    ///////////////////////////////
    ////  Test Helper Methods  ////
    ///////////////////////////////

    private TiePointGrid newTiePointGrid(String name) {
        return new TiePointGrid(name, 10, 10, 0, 0, 1, 1, new float[10 * 10]);
    }

    private Mask newMask(String name) {
        return new Mask(name, 10, 10, new TestImageType());
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
