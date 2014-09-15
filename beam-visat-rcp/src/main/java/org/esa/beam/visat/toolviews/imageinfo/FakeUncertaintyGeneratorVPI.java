package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.VisatPlugIn;

/**
 * @author Norman Fomferra
 */
public class FakeUncertaintyGeneratorVPI implements VisatPlugIn {

    public static final int UNCERTAINTY_KIND_COUNT = 2;

    @Override
    public void start(VisatApp visatApp) {
        if (Boolean.getBoolean("s3tbx.uncertainty.test")) {
            visatApp.getProductManager().addListener(new ProductManager.Listener() {
                @Override
                public void productAdded(ProductManager.Event event) {
                    addUncertaintyBands(event.getProduct());
                }

                @Override
                public void productRemoved(ProductManager.Event event) {
                }
            });
        }
    }

    @Override
    public void stop(VisatApp visatApp) {
    }

    @Override
    public void updateComponentTreeUI() {
    }

    int bandCount;

    private void addUncertaintyBands(Product product) {
        Band[] bands = product.getBands();
        product.setAutoGrouping("radiance_*_variance:radiance_*_confidence:radiance_*_blur:radiance");
        for (Band band : bands) {
            bandCount++;
            String bandName = band.getName();
            if (bandName.startsWith("radiance")
                    && !bandName.endsWith("_blur")
                    && !bandName.endsWith("_variance")
                    && !bandName.endsWith("_confidence")) {
                Band varianceBand = product.getBand(bandName + "_variance");
                Band confidenceBand = product.getBand(bandName + "_confidence");
                if (confidenceBand == null) {
                    if (bandCount % UNCERTAINTY_KIND_COUNT == 0) {
                        ConvolutionFilterBand blurredBand = new ConvolutionFilterBand(bandName + "_blur", band, new Kernel(11, 11, new double[]{
                                0 / 720.0, 0 / 720.0, 1 / 720.0, 1 / 720.0, 2 / 720.0, 2 / 720.0, 2 / 720.0, 1 / 720.0, 1 / 720.0, 0 / 720.0, 0 / 720.0,
                                0 / 720.0, 1 / 720.0, 2 / 720.0, 3 / 720.0, 4 / 720.0, 5 / 720.0, 4 / 720.0, 3 / 720.0, 2 / 720.0, 1 / 720.0, 0 / 720.0,
                                1 / 720.0, 2 / 720.0, 4 / 720.0, 6 / 720.0, 9 / 720.0, 9 / 720.0, 9 / 720.0, 6 / 720.0, 4 / 720.0, 2 / 720.0, 1 / 720.0,
                                1 / 720.0, 3 / 720.0, 6 / 720.0, 11 / 720.0, 14 / 720.0, 16 / 720.0, 14 / 720.0, 11 / 720.0, 6 / 720.0, 3 / 720.0, 1 / 720.0,
                                2 / 720.0, 4 / 720.0, 9 / 720.0, 14 / 720.0, 20 / 720.0, 22 / 720.0, 20 / 720.0, 14 / 720.0, 9 / 720.0, 4 / 720.0, 2 / 720.0,
                                2 / 720.0, 5 / 720.0, 9 / 720.0, 16 / 720.0, 22 / 720.0, 24 / 720.0, 22 / 720.0, 16 / 720.0, 9 / 720.0, 5 / 720.0, 2 / 720.0,
                                2 / 720.0, 4 / 720.0, 9 / 720.0, 14 / 720.0, 20 / 720.0, 22 / 720.0, 20 / 720.0, 14 / 720.0, 9 / 720.0, 4 / 720.0, 2 / 720.0,
                                1 / 720.0, 3 / 720.0, 6 / 720.0, 11 / 720.0, 14 / 720.0, 16 / 720.0, 14 / 720.0, 11 / 720.0, 6 / 720.0, 3 / 720.0, 1 / 720.0,
                                1 / 720.0, 2 / 720.0, 4 / 720.0, 6 / 720.0, 9 / 720.0, 9 / 720.0, 9 / 720.0, 6 / 720.0, 4 / 720.0, 2 / 720.0, 1 / 720.0,
                                0 / 720.0, 1 / 720.0, 2 / 720.0, 3 / 720.0, 4 / 720.0, 5 / 720.0, 4 / 720.0, 3 / 720.0, 2 / 720.0, 1 / 720.0, 0 / 720.0,
                                0 / 720.0, 0 / 720.0, 1 / 720.0, 1 / 720.0, 2 / 720.0, 2 / 720.0, 2 / 720.0, 1 / 720.0, 1 / 720.0, 0 / 720.0, 0 / 720.0,
                        }), 1);
                        product.addBand(blurredBand);

                        String varianceExpr = String.format("0.1 * (1 + 0.1 * min(max(random_gaussian(), 0), 10)) * %s", blurredBand.getName());
                        varianceBand = addVarianceBand(product, band, varianceExpr);
                        confidenceBand = addConfidenceBand(product, band, varianceBand);
                    } else if (bandCount % UNCERTAINTY_KIND_COUNT == 1) {
                        int w2 = product.getSceneRasterWidth() / 2;
                        int h2 = product.getSceneRasterHeight() / 2;
                        int s = Math.min(w2, h2);

                        String varianceExpr = String.format("100 * 0.5 * (1 + sin(4 * PI * sqrt(sqr(X-%d) + sqr(Y-%d)) / %d))", w2, h2, s);
                        varianceBand = addVarianceBand(product, band, varianceExpr);
                        confidenceBand = addConfidenceBand(product, band, varianceBand);
                    }
                }
                band.setAncillaryBand("variance", varianceBand);
                band.setAncillaryBand("confidence", confidenceBand);
            }
        }
    }

    private Band addVarianceBand(Product product, Band sourceBand, String varianceExpr) {
        Band varianceBand;
        varianceBand = product.addBand(sourceBand.getName() + "_variance", varianceExpr, ProductData.TYPE_FLOAT32);
        varianceBand.setUnit(sourceBand.getUnit());
        ProductUtils.copySpectralBandProperties(sourceBand, varianceBand);
        return varianceBand;
    }

    private Band addConfidenceBand(Product product, Band sourceBand, Band varianceBand) {
        Band confidenceBand;
        Stx varStx = varianceBand.getStx();
        double minVar = Math.max(varStx.getMean() - 3 * varStx.getStandardDeviation(), varStx.getMinimum());
        double maxVar = Math.min(varStx.getMean() + 3 * varStx.getStandardDeviation(), varStx.getMaximum());
        double absVar = maxVar - minVar;

        confidenceBand = product.addBand(sourceBand.getName() + "_confidence", String.format("min(max((1 - (%s - %s) / %s), 0), 1)", varianceBand.getName(), minVar, absVar), ProductData.TYPE_FLOAT32);
        confidenceBand.setUnit("dl");
        return confidenceBand;
    }
}

