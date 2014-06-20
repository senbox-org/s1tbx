package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ConvolutionFilterBand;
import org.esa.beam.framework.datamodel.Kernel;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.VisatPlugIn;

/**
 * @author Norman Fomferra
 */
public class FakeUncertaintyGeneratorVPI implements VisatPlugIn {
    @Override
    public void start(VisatApp visatApp) {
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

    @Override
    public void stop(VisatApp visatApp) {
    }

    @Override
    public void updateComponentTreeUI() {
    }

    private void addUncertaintyBands(Product product) {
        String mode = System.getProperty("uncertainty.test");
        if (mode == null) {
            return;
        }
        Band[] bands = product.getBands();
        for (Band band : bands) {
            String bandName = band.getName();
            if (bandName.startsWith("radiance")
                && !bandName.endsWith("_blur")
                && !bandName.endsWith("_variance")
                && !bandName.endsWith("_confidence")) {
                Band varianceBand = product.getBand(bandName + "_variance");
                Band confidenceBand = product.getBand(bandName + "_confidence");
                if (confidenceBand == null) {
                    if ("1".equals(mode)) {
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

                    } else if ("2".equals(mode)) {
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
        return varianceBand;
    }

    private Band addConfidenceBand(Product product, Band sourceBand, Band varianceBand) {
        Band confidenceBand;
        Stx varStx = varianceBand.getStx();
        double minVar = Math.max(varStx.getMean() - 3 * varStx.getStandardDeviation(), varStx.getMinimum());
        double maxVar = Math.min(varStx.getMean() + 3 * varStx.getStandardDeviation(), varStx.getMaximum());
        double absVar = maxVar - minVar;

        confidenceBand = product.addBand(sourceBand.getName() + "_confidence", String.format("255 * min(max((1 - (%s - %s) / %s), 0), 1)", varianceBand.getName(), minVar, absVar), ProductData.TYPE_UINT8);
        confidenceBand.setUnit("dl");
        return confidenceBand;
    }
}

