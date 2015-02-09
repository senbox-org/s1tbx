package org.esa.beam.binning.operator.metadata;


public class MetadataAggregatorFactory {

    private static final String NAME = "NAME";
    private static final String FIRST_HISTORY = "FIRST_HISTORY";
    private static final String ALL_HISTORIES = "ALL_HISTORIES";

    public static MetadataAggregator create(String name) {
        if (NAME.equalsIgnoreCase(name)) {
            return new ProductNameMetaAggregator();
        } else if (FIRST_HISTORY.equalsIgnoreCase(name)) {
            return new FirstHistoryMetaAggregator();
        } else if (ALL_HISTORIES.equalsIgnoreCase(name)) {
            return new AllHistoriesMetaAggregator();
        }

        throw new IllegalArgumentException("Unknown metadata aggregator name: " + name);
    }
}
