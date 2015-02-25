package org.esa.beam.timeseries.core;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;

import static org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries.*;

public class TimeSeriesModule {

    // todo (mp) - @OnStart and @OnStop need to be enabled when module is moved into snap-desktop

    private static final ProductManager.Listener productManagerListener = createProductManagerListener();

    //    @OnStart
    public static class StartOp implements Runnable {

        @Override
        public void run() {
//            SnapApp.getDefault().getProductManager().addListener(productManagerListener);
        }
    }

    //    @OnStop
    public static class StopOp implements Runnable {

        @Override
        public void run() {
//            SnapApp.getDefault().getProductManager().removeListener(productManagerListener);
        }
    }

    private static ProductManager.Listener createProductManagerListener() {
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
//                    final ProductManager productManager = SnapApp.getApp().getProductManager();
//                    for (Product sourceProduct : sourceProducts) {
//                        if (!productManager.contains(sourceProduct)) {
//                            sourceProduct.dispose();
//                        }
//                    }
                    timeSeriesMapper.remove(product);
                }
            }
        };
    }
}
