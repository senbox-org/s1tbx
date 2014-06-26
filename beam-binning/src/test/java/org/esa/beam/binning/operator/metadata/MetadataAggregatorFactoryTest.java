package org.esa.beam.binning.operator.metadata;


import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class MetadataAggregatorFactoryTest {

    @Test
    public void testCreate() {
        MetadataAggregator aggregator = MetadataAggregatorFactory.create("NAME");
        assertNotNull(aggregator);
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
