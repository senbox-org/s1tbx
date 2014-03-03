package org.esa.pfa.ordering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Norman Fomferra
 */
public class ProductOrderBasket {

    final Map<String, ProductOrder> productOrderMap;
    final List<ProductOrder> productOrderList;
    final List<Listener> listenerList;

    public ProductOrderBasket() {
        productOrderMap = new HashMap<>();
        productOrderList = new ArrayList<>();
        listenerList = new ArrayList<>();
    }

    public void addProductOrder(ProductOrder productOrder) {
        String productName = productOrder.getProductName();
        ProductOrder oldProductOrder = getProductOrder(productName);
        if (oldProductOrder == null) {
            productOrderMap.put(productName, productOrder);
            productOrderList.add(productOrder);
            fireBasketChanged();
        }
    }

    public void removeProductOrder(ProductOrder productOrder) {
        productOrderMap.remove(productOrder.getProductName());
        productOrderList.remove(productOrder);
        fireBasketChanged();
    }

    public void fireOrderStateChanged(ProductOrder productOrder) {
        for (Listener listener : listenerList) {
            listener.orderStateChanged(this, productOrder);
        }
    }

    public void fireBasketChanged() {
        for (Listener listener : listenerList) {
            listener.basketChanged(this);
        }
    }

    public void addListener(Listener listener) {
        listenerList.add(listener);
    }

    public int getProductOrderCount() {
        return productOrderMap.size();
    }

    public ProductOrder getProductOrder(String productName) {
          return productOrderMap.get(productName);
    }

    public ProductOrder getProductOrder(int index) {
        return productOrderList.get(index);
    }

    public int getProductOrderIndex(ProductOrder order) {
        return productOrderList.indexOf(order);
    }


    public interface Listener {
        void basketChanged(ProductOrderBasket basket);
        void orderStateChanged(ProductOrderBasket basket, ProductOrder order);
    }
}
