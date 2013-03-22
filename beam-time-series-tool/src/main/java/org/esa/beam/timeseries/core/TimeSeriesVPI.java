package org.esa.beam.timeseries.core;

import static org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.VisatPlugIn;

public class TimeSeriesVPI implements VisatPlugIn {

    private final ProductManager.Listener productManagerListener = createProductManagerListener();

    @Override
    public void start(VisatApp visatApp) {
        visatApp.getProductManager().addListener(productManagerListener);
    }

    @Override
    public void stop(VisatApp visatApp) {
        visatApp.getProductManager().removeListener(productManagerListener);
    }

    @Override
    public void updateComponentTreeUI() {
    }

    private ProductManager.Listener createProductManagerListener() {
        return new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event event) {
            }

            @Override
            public void productRemoved(ProductManager.Event event) {
                final Product product = event.getProduct();
                if (product.getProductType().equals(TIME_SERIES_PRODUCT_TYPE)) {
                    final TimeSeriesMapper timeSeriesMapper = TimeSeriesMapper.getInstance();
                    final AbstractTimeSeries timeSeries = timeSeriesMapper.getTimeSeries(product);
                    final Product[] sourceProducts = timeSeries.getSourceProducts();
                    final ProductManager productManager = VisatApp.getApp().getProductManager();
                    for (Product sourceProduct : sourceProducts) {
                        if (!productManager.contains(sourceProduct)) {
                            sourceProduct.dispose();
                        }
                    }
                    timeSeriesMapper.remove(product);
                }
            }
        };
    }
}
