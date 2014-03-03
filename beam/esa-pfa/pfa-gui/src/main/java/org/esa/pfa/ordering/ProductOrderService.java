package org.esa.pfa.ordering;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fake service for time being.
 */
public class ProductOrderService {

    final ExecutorService executorService = Executors.newFixedThreadPool(3);
    ProductOrderBasket productOrderBasket;

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
        int SUBMIT_DURATION = 2;
        int DOWNLOAD_DURATION = 20;
        try {
            productOrder.setState(ProductOrder.State.REQUEST_SUBMITTED);
            productOrderBasket.fireOrderStateChanged(productOrder);
            Thread.sleep(SUBMIT_DURATION * 1000);
        } catch (InterruptedException e) {
            // ok
        }

        int N = 100;
        int delay = DOWNLOAD_DURATION * 1000 / N;
        for (int i = 0; i < 100; i++) {
            try {
                Thread.sleep(delay);
                productOrder.setState(ProductOrder.State.DOWNLOADING);
                productOrder.setProgress(i + 1);
                productOrderBasket.fireOrderStateChanged(productOrder);
            } catch (InterruptedException e) {
                // ok
            }
        }
        productOrder.setState(ProductOrder.State.DOWNLOADED);
        productOrder.setProgress(N);
        productOrderBasket.fireOrderStateChanged(productOrder);
    }
}
