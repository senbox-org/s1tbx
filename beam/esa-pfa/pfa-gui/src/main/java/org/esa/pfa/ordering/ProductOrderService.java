package org.esa.pfa.ordering;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fake service for time being.
 */
public class ProductOrderService {
    final static int MEAN_SUBMIT_DURATION = 2;
    final static int MEAN_DOWNLOAD_DURATION = 20;

    final ExecutorService executorService = Executors.newFixedThreadPool(3);
    final ProductOrderBasket productOrderBasket;
    final Random random = new Random();

    public ProductOrderService(ProductOrderBasket productOrderBasket) {
        this.productOrderBasket = productOrderBasket;
    }

    public ProductOrderBasket getProductOrderBasket() {
        return productOrderBasket;
    }

    public void submit(final ProductOrder productOrder) {
        productOrderBasket.addProductOrder(productOrder);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                orderImpl(productOrder);
            }
        });
    }

    private void orderImpl(final ProductOrder productOrder) {
        productOrder.setState(ProductOrder.State.SUBMITTED);
        productOrderBasket.fireOrderStateChanged(productOrder);
        sleep(MEAN_SUBMIT_DURATION * 1000);

        final int numDownloadSteps = 100;
        for (int i = 0; i < 100; i++) {
            sleep(MEAN_DOWNLOAD_DURATION * 1000 / numDownloadSteps);
            productOrder.setState(ProductOrder.State.DOWNLOADING);
            productOrder.setProgress(i + 1);
            productOrderBasket.fireOrderStateChanged(productOrder);
        }
        productOrder.setState(ProductOrder.State.COMPLETED);
        productOrder.setProgress(numDownloadSteps);
        productOrderBasket.fireOrderStateChanged(productOrder);
    }


    void sleep(int meanMillis) {
        try {
            int millis = meanMillis + (int) (0.5 * random.nextGaussian() * meanMillis);
            if (millis > 0) {
                Thread.sleep(millis);
            }
        } catch (InterruptedException e) {
            // ok
        }
    }
}
