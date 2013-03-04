package org.esa.beam.binning;

import org.esa.beam.binning.aggregators.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AggregatorDescriptorRegistryTest {
    private MyVariableContext ctx = new MyVariableContext("x", "y", "z");

    @Test
    public void testDefaultAggregatorIsRegistered_Average() {
        AggregatorDescriptor descriptor = assertRegistered("AVG");
        Aggregator aggregator = descriptor.createAggregator(ctx, new AggConf("x", 0.2, -999.9F));
        assertNotNull(aggregator);
        assertEquals(AggregatorAverage.class, aggregator.getClass());
    }

    @Test
    public void testDefaultAggregatorIsRegistered_AverageML() {
        AggregatorDescriptor descriptor = assertRegistered("AVG_ML");
        Aggregator aggregator = descriptor.createAggregator(ctx, new AggConf("x", 0.2, -999.9F));
        assertNotNull(aggregator);
        assertEquals(AggregatorAverageML.class, aggregator.getClass());
    }

    @Test
    public void testDefaultAggregatorIsRegistered_MinMax() {
        AggregatorDescriptor descriptor = assertRegistered("MIN_MAX");
        Aggregator aggregator = descriptor.createAggregator(ctx, new AggConf("x", 0.2, -999.9F));
        assertNotNull(aggregator);
        assertEquals(AggregatorMinMax.class, aggregator.getClass());
    }

    @Test
    public void testDefaultAggregatorIsRegistered_Percentile() {
        AggregatorDescriptor descriptor = assertRegistered("PERCENTILE");
        Aggregator aggregator = descriptor.createAggregator(ctx, new AggConf("x", 75, -1.0F));
        assertNotNull(aggregator);
        assertEquals(AggregatorPercentile.class, aggregator.getClass());
    }

    @Test
    public void testDefaultAggregatorIsRegistered_OnMaxSet() {
        AggregatorDescriptor descriptor = assertRegistered("ON_MAX_SET");
        Aggregator aggregator = descriptor.createAggregator(ctx, new AggConf("x", "y", "z"));
        assertNotNull(aggregator);
        assertEquals(AggregatorOnMaxSet.class, aggregator.getClass());
    }

    @Test
    public void testGetAllRegisteredAggregatorDescriptors() throws Exception {
        final AggregatorDescriptorRegistry registry = AggregatorDescriptorRegistry.getInstance();
        final AggregatorDescriptor[] aggregatorDescriptors = registry.getAggregatorDescriptors();
        assertEquals(6, aggregatorDescriptors.length);
    }

    private AggregatorDescriptor assertRegistered(String name) {
        AggregatorDescriptorRegistry registry = AggregatorDescriptorRegistry.getInstance();
        AggregatorDescriptor descriptor = registry.getAggregatorDescriptor(name);
        assertNotNull(descriptor);
        assertEquals(name, descriptor.getName());
        return descriptor;
    }


    public static final class AggConf extends AggregatorConfig {

        public String varName;
        public String[] varNames;
        public int percentage = -1;
        public double weightCoeff = -1;
        public float fillValue;

        public AggConf(String varName, double weightCoeff, float fillValue) {
            this.varName = varName;
            this.weightCoeff = weightCoeff;
            this.fillValue = fillValue;
        }

        public AggConf(String... varNames) {
            this.varNames = varNames;
        }

        public AggConf(String varName, int percentage, float fillValue) {
            this.varName = varName;
            this.percentage = percentage;
            this.fillValue = fillValue;
        }

        @Override
        public String[] getVarNames() {
            return varNames != null ? varNames : new String[]{varName};
        }
    }
}
