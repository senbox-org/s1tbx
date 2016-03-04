package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.ProductData;

/**
 * @author Tonio Fincke
 */
public class AggregatorFactory {

    public static Aggregator createAggregator(String method, int dataType) {
        if (dataType == ProductData.TYPE_FLOAT32 || dataType == ProductData.TYPE_FLOAT64) {
            switch (method) {
                case "Mean":
                    return new DoubleDataAggregator.Mean();
                case "Median":
                    return new DoubleDataAggregator.Median();
                case "And":
                    return new DoubleDataAggregator.Min();
                case "Or":
                    return new DoubleDataAggregator.Max();
                case "First":
                    return new DoubleDataAggregator.First();
            }
        } else {
            switch (method) {
                case "Mean":
                    return new LongDataAggregator.Mean();
                case "Median":
                    return new LongDataAggregator.Median();
                case "And":
                    return new LongDataAggregator.Min();
                case "Or":
                    return new LongDataAggregator.Max();
                case "First":
                    return new LongDataAggregator.First();
                case "FlagAnd":
                    return new LongDataAggregator.FlagAnd();
                case "FlagOr":
                    return new LongDataAggregator.FlagOr();
                case "FlagMedianAnd":
                    return new LongDataAggregator.FlagMedianAnd();
                case "FlagMedianOr":
                    return new LongDataAggregator.FlagMedianOr();
            }
        }
        throw new IllegalArgumentException("Aggregation method not supported (maybe invalid datatype)");
    }

}
