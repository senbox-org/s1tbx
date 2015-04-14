package org.esa.snap.binning;

import org.esa.snap.binning.aggregators.AggregatorAverage;
import org.esa.snap.binning.aggregators.AggregatorMinMax;
import org.esa.snap.binning.aggregators.AggregatorOnMaxSet;
import org.esa.snap.binning.aggregators.AggregatorPercentile;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class AggregatorDescriptorRegistryTest {

    private MyVariableContext ctx = new MyVariableContext("x", "y", "z");

    @Test
    public void testDefaultAggregatorIsRegistered_Average() {
        AggregatorDescriptor descriptor = assertRegistered("AVG");
        Aggregator aggregator = descriptor.createAggregator(ctx, new AggregatorAverage.Config("x", "target", 0.2, false, false));
        assertNotNull(aggregator);
        assertEquals(AggregatorAverage.class, aggregator.getClass());
    }

    @Test
    public void testDefaultAggregatorIsRegistered_MinMax() {
        AggregatorDescriptor descriptor = assertRegistered("MIN_MAX");
        Aggregator aggregator = descriptor.createAggregator(ctx, new AggregatorMinMax.Config("x", "y"));
        assertNotNull(aggregator);
        assertEquals(AggregatorMinMax.class, aggregator.getClass());
    }

    @Test
    public void testDefaultAggregatorIsRegistered_Percentile() {
        AggregatorDescriptor descriptor = assertRegistered("PERCENTILE");
        Aggregator aggregator = descriptor.createAggregator(ctx, new AggregatorPercentile.Config("x", "y", 75));
        assertNotNull(aggregator);
        assertEquals(AggregatorPercentile.class, aggregator.getClass());
    }

    @Test
    public void testDefaultAggregatorIsRegistered_OnMaxSet() {
        AggregatorDescriptor descriptor = assertRegistered("ON_MAX_SET");
        AggregatorOnMaxSet.Config config = new AggregatorOnMaxSet.Config("target", "x", "y", "z");
        Aggregator aggregator = descriptor.createAggregator(ctx, config);
        assertNotNull(aggregator);
        assertEquals(AggregatorOnMaxSet.class, aggregator.getClass());
    }

    @Test
    public void testGetAllRegisteredAggregatorDescriptors() throws Exception {
        TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
        List<AggregatorDescriptor> aggregatorDescriptors = registry.getDescriptors(AggregatorDescriptor.class);
        assertEquals(4, aggregatorDescriptors.size());
    }

    private AggregatorDescriptor assertRegistered(String name) {
        TypedDescriptorsRegistry registry = TypedDescriptorsRegistry.getInstance();
        AggregatorDescriptor descriptor = registry.getDescriptor(AggregatorDescriptor.class, name);
        assertNotNull(descriptor);
        assertEquals(name, descriptor.getName());
        return descriptor;
    }

}
