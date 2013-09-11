package org.esa.beam.binning.aggregators;


import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.MyVariableContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class AggregatorBiasCorrectDescriptorTest {

    private AggregatorDescriptor descriptor;

    @Before
    public void setUp() {
        descriptor = new AggregatorBiasCorrect.Descriptor();
    }

    @Test
    public void testGetName() {
        assertEquals("OC-CCI-BIAS", descriptor.getName());
    }

    @Test
    public void testCreateConfig() {
        final AggregatorConfig config = descriptor.createConfig();
        assertNotNull(config);
        assertEquals("OC-CCI-BIAS", config.getName());
    }

    @Test
    public void testCreateAggregator() {
        MyVariableContext ctx = new MyVariableContext("x", "y", "z");
        final AggregatorConfig config = descriptor.createConfig();

        final Aggregator aggregator = descriptor.createAggregator(ctx, config);
        assertNotNull(aggregator);
    }
}
