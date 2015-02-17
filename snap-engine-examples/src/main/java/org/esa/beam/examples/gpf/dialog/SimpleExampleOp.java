package org.esa.beam.examples.gpf.dialog;


import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.ProductConfigurer;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.framework.gpf.pointop.SampleOperator;
import org.esa.beam.framework.gpf.pointop.WritableSample;

@OperatorMetadata(alias = "Simple",
                  category = "Optical Processing",
                  version = "1.0",
                  description = "An simple operator which does nothing meaningful. " +
                          "It is just a coding example.")
public class SimpleExampleOp extends SampleOperator {
    private static final String OUTPUT_BAND_NAME = "output";
    private static final String[] REQUIRED_RASTER_NAMES = new String[]{
            "radiance_1", "radiance_2", "radiance_11", "radiance_12",
            "altitude", "sun_zenith", "view_zenith"};

    @SourceProduct(label = "Select source product",
                   description = "The source product used for the processing.")
    Product source;

    @Parameter(label = "Switch between THIS and THAT", valueSet = {"THIS", "THAT"}, defaultValue = "THIS",
               description = "Switch between THIS and THAT algorithm.")
    private Algorithm doThisOrThat;

    @Parameter(label = "Apply factor on input", defaultValue = "false", description = "Apply the factor on input values or not.")
    private boolean includeInputFactor;

    @Parameter(label = "Input factor", defaultValue = "1.04", interval = "(0,2)",
               description = "Factor applied to the input.")
    private double inputFactor;

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        validateSourceProduct();
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        productConfigurer.addBand(OUTPUT_BAND_NAME, ProductData.TYPE_FLOAT64, Double.NaN);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        int index = 0;
        for (String rasterName : REQUIRED_RASTER_NAMES) {
            sampleConfigurer.defineSample(index++, rasterName);
        }
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(0, OUTPUT_BAND_NAME);
    }


    @Override
    protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
        double[] radiances = new double[4];
        for (int i = 0; i < radiances.length; i++) {
            Sample sourceSample = sourceSamples[i];
            radiances[i] = sourceSample.getFloat() * (includeInputFactor ? inputFactor : 1.0);
        }
        double altitude = sourceSamples[4].getDouble();
        double sunZenith = sourceSamples[5].getDouble();
        double viewZenith = sourceSamples[6].getDouble();
        Algo algo = doThisOrThat.createAlgo();
        double result = algo.compute(radiances, altitude, sunZenith, viewZenith);
        targetSample.set(result);

    }

    private void validateSourceProduct() {
        checkForRequiredRaster();
        if (isGeoCodingMissing()) {
            throw new OperatorException("The source product must be geo-coded.");
        }
    }

    private boolean isGeoCodingMissing() {
        return getSourceProduct().getGeoCoding() == null;
    }

    private void checkForRequiredRaster() {
        for (String requiredRaster : REQUIRED_RASTER_NAMES) {
            if (isRequiredRasterMissing(requiredRaster)) {
                String message = String.format("Required raster '%s' is missing.", requiredRaster);
                throw new OperatorException(message);
            }
        }
    }

    private boolean isRequiredRasterMissing(String requiredGrid) {
        return !getSourceProduct().getTiePointGridGroup().contains(requiredGrid);
    }

    public static interface Algo {
        double compute(double[] radiances, double altitude, double sunZenith, double viewZenith);
    }

    private static class ThisAlgo implements Algo {
        @Override
        public double compute(double[] radiances, double altitude, double sunZenith, double viewZenith) {
            double sum = 0.0;
            for (double radiance : radiances) {
                sum += radiance;
            }
            return sum;
        }
    }

    private static class ThatAlgo implements Algo {
        @Override
        public double compute(double[] radiances, double altitude, double sunZenith, double viewZenith) {
            return Math.cos(viewZenith) * radiances[0];
        }
    }

    public enum Algorithm {
        THIS {
            public Algo createAlgo() {
                return new SimpleExampleOp.ThisAlgo();
            }
        },
        THAT {
            public Algo createAlgo() {
                return new SimpleExampleOp.ThatAlgo();
            }
        };

        public abstract Algo createAlgo();
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SimpleExampleOp.class);
        }
    }

}
