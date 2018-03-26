package org.esa.snap.binning.aggregators;

import org.esa.snap.binning.AbstractAggregator;
import org.esa.snap.binning.Aggregator;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.AggregatorDescriptor;
import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.Observation;
import org.esa.snap.binning.VariableContext;
import org.esa.snap.binning.Vector;
import org.esa.snap.binning.WritableVector;
import org.esa.snap.binning.support.GrowableVector;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.StringUtils;

import static java.lang.Float.NaN;

public class AggregatorAverageOutlierAware extends AbstractAggregator {

    private final String vectorName;
    private final int varIndex;
    private final double deviationFactor;

    public AggregatorAverageOutlierAware(VariableContext varCtx, String varName, double deviationFactor) {
        this(varCtx, varName, varName, deviationFactor);
    }

    private AggregatorAverageOutlierAware(VariableContext varCtx, String varName, String targetName, double deviationFactor) {
        super(Descriptor.NAME, new String[0],
                createFeatureNames(varName, "mean", "sigma", "counts"),
                createFeatureNames(targetName, "mean", "sigma", "counts"));

        vectorName = "values." + varName;
        this.deviationFactor = deviationFactor;
        this.varIndex = varCtx.getVariableIndex(varName);
    }

    @Override
    public boolean requiresGrowableSpatialData() {
        return true;
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        // nothing to do here tb 2018-03-12
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        final float value = observationVector.get(varIndex);
        if (!Float.isNaN(value)) {
            ((GrowableVector) spatialVector).add(value);
        }
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        // nothing to do here tb 2018-03-12
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
        vector.set(1, 0.0f);
        vector.set(2, 0.0f);
        ctx.put(vectorName, new GrowableVector(256));   // @todo 3 tb/tb is this a good default? 2018-03-09
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {
        final GrowableVector measurementsVec = ctx.get(vectorName);
        for (int i = 0; i < spatialVector.size(); i++) {
            measurementsVec.add(spatialVector.get(i));
        }
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
        final GrowableVector measurementsVec = ctx.get(vectorName);
        int count = measurementsVec.size();
        if (count == 0) {
            temporalVector.set(0, NaN);
            temporalVector.set(1, NaN);
            temporalVector.set(2, 0);
        }

        double[] statistics = calculateStatistics(measurementsVec);

        if (count > 2) {    // we cannot detect outliers with two or less elements tb 2018-03-12
            final double maxDelta = statistics[1] * deviationFactor;
            final GrowableVector consolidatedMeasurements = new GrowableVector(count);
            for (int i = 0; i < count; i++) {
                final float measurement = measurementsVec.get(i);
                if (Math.abs(measurement - statistics[0]) < maxDelta) {
                    consolidatedMeasurements.add(measurement);
                }
            }
            if (count > consolidatedMeasurements.size()) {
                statistics = calculateStatistics(consolidatedMeasurements);
                count = consolidatedMeasurements.size();
            }
        }

        temporalVector.set(0, (float) statistics[0]);
        temporalVector.set(1, (float) statistics[1]);
        temporalVector.set(2, count);
    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        outputVector.set(0, temporalVector.get(0));
        outputVector.set(1, temporalVector.get(1));
        outputVector.set(2, temporalVector.get(2));
    }

    public static class Config extends AggregatorConfig {
        @Parameter(label = "Source band name", notEmpty = true, notNull = true, description = "The source band used for aggregation.")
        String varName;

        @Parameter(label = "Target band name prefix (optional)", description = "The name prefix for the resulting bands. If empty, the source band name is used.")
        String targetName;

        @Parameter(label = "Deviation Factor", defaultValue = "1.0",
                description = "Factor multiplied with the standard deviation that defines the distance to the mean that serves as threshold for outlier detection.")
        Double deviationFactor;

        public Config() {
            this(null, null, 1.0);
        }

        public Config(String varName, String targetName, double deviationFactor) {
            super(AggregatorAverageOutlierAware.Descriptor.NAME);
            this.varName = varName;
            this.targetName = targetName;
            this.deviationFactor = deviationFactor;
        }
    }

    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "AVG_OUTLIER";

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            final Config config = (Config) aggregatorConfig;

            if (StringUtils.isNullOrEmpty(config.targetName)) {
                return new AggregatorAverageOutlierAware(varCtx, config.varName, config.deviationFactor);
            } else {
                return new AggregatorAverageOutlierAware(varCtx, config.varName, config.targetName, config.deviationFactor);
            }
        }

        @Override
        public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
            final Config config = (Config) aggregatorConfig;

            return new String[]{config.varName};
        }

        @Override
        public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
            final Config config = (Config) aggregatorConfig;
            if (StringUtils.isNullOrEmpty(config.targetName)) {
                return createFeatureNames(config.varName, "mean", "sigma", "counts");
            } else {
                return createFeatureNames(config.targetName, "mean", "sigma", "counts");
            }
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public AggregatorConfig createConfig() {
            return new Config();
        }
    }

    private double[] calculateStatistics(GrowableVector measurementsVec) {
        final int count = measurementsVec.size();

        double sum = 0.0;
        for (int i = 0; i < count; i++) {
            sum += measurementsVec.get(i);
        }
        final double mean = sum / count;

        sum = 0.0;
        for (int i = 0; i < count; i++) {
            final double delta = measurementsVec.get(i) - mean;
            sum += delta * delta;
        }

        final double stdDev = Math.sqrt(sum / count);

        return new double[]{mean, stdDev};
    }
}
