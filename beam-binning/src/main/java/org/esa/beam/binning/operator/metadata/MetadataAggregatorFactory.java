package org.esa.beam.binning.operator.metadata;


public class MetadataAggregatorFactory {

    private static final String NAME = "NAME";
    private static final String FIRST_HISTORY = "FIRST_HISTORY";

    public static MetadataAggregator create(String name) {
        if (NAME.equalsIgnoreCase(name)) {
            return new ProductNameMetaAggregator();
        } else if (FIRST_HISTORY.equalsIgnoreCase(name)) {
            return new FirstHistoryMetaAggregator();
        }

        throw new IllegalArgumentException("Unknown metadata aggregator name: " + name);
    }
}
