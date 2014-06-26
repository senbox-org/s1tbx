package org.esa.beam.binning.operator.metadata;


public class MetadataAggregatorFactory {

    private static final String NAME = "NAME";

    public static MetadataAggregator create(String name) {
        if (NAME.equalsIgnoreCase(name)) {
            return new ProductNameMetaAggregator();
        }

        throw new IllegalArgumentException("Unknown metadata aggregator name: " + name);
    }
}
