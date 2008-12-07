package org.esa.beam.smos.visat;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapshotSelectionService {
    private final ProductManager productManager;
    private final List<Listener> listeners;
    private final Map<Product, Integer> snapshotIds;
    private final ProductManagerHandler productManagerHandler;

    public SnapshotSelectionService(ProductManager productManager) {
        this.listeners = new ArrayList<Listener>();
        this.snapshotIds = new HashMap<Product, Integer>();
        this.productManagerHandler = new ProductManagerHandler();
        this.productManager = productManager;
        this.productManager.addListener(productManagerHandler);
    }

    public void dispose() {
        synchronized (this) {
            snapshotIds.clear();
            listeners.clear();
            productManager.addListener(productManagerHandler);
        }
    }

    public int getSelectedSnapshotId(Product product) {
        Integer integer;
        synchronized (this) {
            integer = snapshotIds.get(product);
        }
        return integer != null ? integer : -1;
    }

    public void setSelectedSnapshotId(Product product, int snapshotId) {
        int oldSnapshotId;
        synchronized (this) {
            oldSnapshotId = getSelectedSnapshotId(product);
            if (oldSnapshotId != snapshotId) {
                if (snapshotId >= 0) {
                    snapshotIds.put(product, snapshotId);
                } else {
                    snapshotIds.remove(product);
                }
            }
        }
        if (oldSnapshotId != snapshotId) {
            fireSelectionChange(product, oldSnapshotId, snapshotId);
        }
    }

    private void fireSelectionChange(Product product, int oldSnapshotId, int newSnapshotId) {
        for (Listener listener : listeners) {
            listener.handleSnapshotIdChanged(product, oldSnapshotId, newSnapshotId);
        }
    }

    public void addSnapshotIdChangeListener(Listener listener) {
        synchronized (this) {
            listeners.add(listener);
        }
    }

    public void removeSnapshotIdChangeListener(Listener listener) {
        synchronized (this) {
            listeners.remove(listener);
        }
    }

    public interface Listener {
        void handleSnapshotIdChanged(Product product, int oldSnapshotId, int newSnapshotId);
    }

    private class ProductManagerHandler implements ProductManager.Listener {
        public void productAdded(ProductManager.Event event) {
        }

        public void productRemoved(ProductManager.Event event) {
            setSelectedSnapshotId(event.getProduct(), -1);
        }
    }
}
