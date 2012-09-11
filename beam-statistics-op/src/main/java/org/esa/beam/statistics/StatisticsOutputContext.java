package org.esa.beam.statistics;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

/**
 * The context used to initialise instances of {@link StatisticsOutputter}.
 */
public class StatisticsOutputContext {

    /**
     * The statistics' source products.
     */
    public final Product[] sourceProducts;

    /**
     * The names of the bands considered in the statistics.
     */
    public final String[] bandNames;

    /**
     * The names of the algorithms considered in the statistics.
     */
    public final String[] algorithmNames;

    /**
     * The start date of the statistics.
     */
    public final ProductData.UTC startDate;

    /**
     * The end data of the statistics.
     */
    public final ProductData.UTC endDate;

    /**
     * The ids of the regions where statistics are computed.
     */
    public final String[] regionIds;


    // todo doku ... add context object to factory ... initialize Context object
    /**
     * Factory method for creating a fully specified instance.
     *
     * @param sourceProducts The statistics' source products.
     * @param bandNames      The names of the bands considered in the statistics.
     * @param algorithmNames The names of the algorithms considered in the statistics.
     * @param startDate      The start date of the statistics.
     * @param endDate        The end data of the statistics.
     * @param regionIds      The ids of the regions where statistics are computed.
     *
     * @return An instance of {@link StatisticsOutputContext}.
     */
    public static StatisticsOutputContext create(Product[] sourceProducts, String[] bandNames, String[] algorithmNames, ProductData.UTC startDate, ProductData.UTC endDate, String[] regionIds) {
        return new StatisticsOutputContext(sourceProducts, bandNames, algorithmNames, startDate, endDate, regionIds);
    }


    /**
     * Convenience factory method for creating an instance which does not use all possible fields.
     *
     * @param sourceProducts The statistics' source products.
     * @param algorithmNames The names of the algorithms considered in the statistics.
     * @param regionIds      The ids of the regions where statistics are computed.
     *
     * @return An instance of {@link StatisticsOutputContext}.
     */
    public static StatisticsOutputContext create(Product[] sourceProducts, String[] algorithmNames, String[] regionIds) {
        return new StatisticsOutputContext(sourceProducts, null, algorithmNames, null, null, regionIds);
    }

    /**
     *
     * Convenience factory method for creating an instance which does not use all possible fields.
     * @param bandNames      The names of the bands considered in the statistics.
     * @param algorithmNames The names of the algorithms considered in the statistics.
     *
     * @return An instance of {@link StatisticsOutputContext}.
     */
    public static StatisticsOutputContext create(String[] bandNames, String[] algorithmNames) {
        return new StatisticsOutputContext(null, bandNames, algorithmNames, null, null, null);
    }

    private StatisticsOutputContext(Product[] sourceProducts, String[] bandNames, String[] algorithmNames, ProductData.UTC startDate, ProductData.UTC endDate, String[] regionIds) {
        this.sourceProducts = sourceProducts;
        this.bandNames = bandNames;
        this.algorithmNames = algorithmNames;
        this.startDate = startDate;
        this.endDate = endDate;
        this.regionIds = regionIds;
    }
}
