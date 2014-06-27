package org.esa.beam.binning.operator.metadata;


import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MetadataAggregatorFactoryTest {

    @Test
    public void testCreate() {
        MetadataAggregator aggregator = MetadataAggregatorFactory.create("NAME");
        assertNotNull(aggregator);
        assertTrue(aggregator instanceof ProductNameMetaAggregator);

        aggregator = MetadataAggregatorFactory.create("FIRST_HISTORY");
        assertNotNull(aggregator);
        assertTrue(aggregator instanceof FirstHistoryMetaAggregator);

        aggregator = MetadataAggregatorFactory.create("ALL_HISTORIES");
        assertNotNull(aggregator);
        assertTrue(aggregator instanceof AllHistoriesMetaAggregator);
    }

    @SuppressWarnings("EmptyCatchBlock")
    @Test
    public void testCreate_invalidName() {
        try {
            MetadataAggregatorFactory.create("blanker Unsinn");
            fail("IllegalArgumentException expected");
        } catch(IllegalArgumentException expected) {
        }
    }
}
