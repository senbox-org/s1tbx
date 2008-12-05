package org.esa.beam.smos.visat;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapshotSelectionService {
    private final ProductManager productManager;
    private final List<SnapshotIdChangeListener> snapshotIdChangeListeners;
    private final Map<Product, Integer> snapshotIds;
    private final ProductManagerHandler productManagerHandler;

    public SnapshotSelectionService(ProductManager productManager) {
        this.snapshotIdChangeListeners = new ArrayList<SnapshotIdChangeListener>();
        this.snapshotIds = new HashMap<Product, Integer>();
        this.productManagerHandler = new ProductManagerHandler();
        this.productManager = productManager;
        this.productManager.addListener(productManagerHandler);
    }

    public void dispose() {
        synchronized (this) {
            snapshotIds.clear();
            snapshotIdChangeListeners.clear();
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
        synchronized (this) {
            int oldSnapshotId = getSelectedSnapshotId(product);
            if (oldSnapshotId != snapshotId) {
                snapshotIds.put(product, snapshotId);
                for (SnapshotIdChangeListener snapshotIdChangeListener : snapshotIdChangeListeners) {
                    snapshotIdChangeListener.handleSnapshotIdChanged(product, oldSnapshotId, snapshotId);
                }
            }
        }
    }

    public void addSnapshotIdChangeListener(SnapshotIdChangeListener listener) {
        synchronized (this) {
            snapshotIdChangeListeners.add(listener);
        }
    }

    public void removeSnapshotIdChangeListener(SnapshotIdChangeListener listener) {
        synchronized (this) {
            snapshotIdChangeListeners.remove(listener);
        }
    }

    public interface SnapshotIdChangeListener {
        void handleSnapshotIdChanged(Product product, int oldSnapshotId, int newSnapshotId);
    }

    private class ProductManagerHandler implements ProductManager.Listener {
        public void productAdded(ProductManager.Event event) {
        }

        public void productRemoved(ProductManager.Event event) {
            snapshotIds.remove(event.getProduct());
        }
    }
}
