/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.datamodel;

import org.esa.snap.core.dataop.barithm.BandArithmetic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;


/**
 * A type-safe container for elements of the type {@code Product}. ProductListeners can be added to inform if a
 * {@code Product} was added or removed.
 *
 * @author Norman Fomferra
 */
public class ProductManager {

    private static final int PRODUCT_ADDED = 1;
    private static final int PRODUCT_REMOVED = 2;

    private List<Listener> listeners;

    private final ProductNodeList<Product> productList;
    private ProductNodeNameChangeListener productNodeNameChangeListener;


    /**
     * Constructs an product manager with an empty list of products.
     */
    public ProductManager() {
        productList = new ProductNodeList<>();
        productNodeNameChangeListener = new ProductNodeNameChangeListener();
    }

    /**
     * @return The number of products in this product manager.
     */
    public int getProductCount() {
        return productList.size();
    }

    /**
     * Gets the product at the given index.
     *
     * @param index the index
     * @return The product at the given index.
     */
    public Product getProduct(int index) {
        return productList.getAt(index);
    }

    /**
     * Returns the display names of all products currently managed.
     *
     * @return an array containing the display names, never {@code null}, but the array can have zero length
     * @see ProductNode#getDisplayName()
     */
    public String[] getProductDisplayNames() {
        return productList.getDisplayNames();
    }

    /**
     * Returns the names of all products currently managed.
     *
     * @return an array containing the names, never {@code null}, but the array can have zero length
     */
    public String[] getProductNames() {
        return productList.getNames();
    }

    /**
     * Returns an array of all products currently managed.
     *
     * @return an array containing the products, never {@code null}, but the array can have zero length
     */
    public Product[] getProducts() {
        return productList.toArray(new Product[getProductCount()]);
    }

    /**
     * @param displayName The product's display name.
     * @return The product with the given display name.
     */
    public Product getProductByDisplayName(final String displayName) {
        if (displayName == null) {
            return null;
        }
        return productList.getByDisplayName(displayName);
    }

    /**
     * @param refNo The reference number.
     * @return The product with the given reference number.
     */
    public Product getProductByRefNo(final int refNo) {
        for (final Product product : getProducts()) {
            if (refNo == product.getRefNo()) {
                return product;
            }
        }

        return null;
    }

    /**
     * @param name The product name.
     * @return The product with the given name.
     */
    public Product getProduct(String name) {
        return productList.get(name);
    }

    public int getProductIndex(Product product) {
        return productList.indexOf(product);
    }

    /**
     * Tests whether a product with the given name is contained in this list.
     *
     * @param name the product name
     * @return true, if so
     */
    public boolean containsProduct(String name) {
        return productList.contains(name);
    }

    /**
     * Tests whether the given product is contained in this list.
     *
     * @param product The product.
     * @return {@code true} if so.
     */
    public boolean contains(final Product product) {
        return productList.contains(product);
    }

    /**
     * Adds the given product to this product manager if it does not already exists and sets it's reference number one
     * biger than the greatest reference number in this product manager.
     *
     * @param product the product to be added, ignored if {@code null}
     */
    public void addProduct(Product product) {
        if (product != null) {
            if (contains(product)) {
                return;
            }
            if (productList.add(product)) {
                setProductManager(product);
                if (product.getRefNo() <= 0) {
                    product.setRefNo(getNextRefNo() + 1);
                }
                product.addProductNodeListener(productNodeNameChangeListener);
                fireEvent(product, PRODUCT_ADDED);
            }
        }
    }

    private void updateExpressionToRenamedNode(ProductNode renamedNode, String oldName) {
        final Product[] products = getProducts();
        final String oldExternName = BandArithmetic.createExternalName(oldName);
        final String newExternName = BandArithmetic.createExternalName(renamedNode.getName());

        for (Product product : products) {
            if (product != renamedNode.getProduct()) {
                product.acceptVisitor(new ExpressionUpdaterVisitor(oldExternName, newExternName));
            }
        }
    }

    /**
     * Removes the given product from this product manager if it exists.
     *
     * @param product the product to be removed, ignored if {@code null}
     * @return true, if the product was removed
     */
    public boolean removeProduct(Product product) {
        if (product != null) {
            int index = productList.indexOf(product);
            if (index >= 0) {
                if (productList.remove(product)) {
                    productList.clearRemovedList();
                    product.removeProductNodeListener(productNodeNameChangeListener);
                    clearProductManager(product);
                    fireEvent(product, PRODUCT_REMOVED);
                    product.resetRefNo();       // don't reset ref no until after event fired
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes all product from this list.
     */
    public void removeAllProducts() {
        final Product[] products = getProducts();
        for (Product product : products) {
            removeProduct(product);
        }
    }

    private void setProductManager(Product product) {
        if (product.getProductManager() != this) {
            product.setProductManager(this);
        }
    }

    private void clearProductManager(Product product) {
        if (product.getProductManager() == this) {
            product.setProductManager(null);
        }
    }

    /**
     * Returns the next reference number for products in this manager.
     *
     * @return the next highest reference number
     */
    private int getNextRefNo() {
        final int numProducts = getProductCount();
        int highestRefNo = 0;
        for (int i = 0; i < numProducts; i++) {
            final int refNo = getProduct(i).getRefNo();
            if (refNo > highestRefNo) {
                highestRefNo = refNo;
            }
        }
        return highestRefNo;
    }

    //////////////////////////////////////////////////////////////////////////
    // Product listener support

    /**
     * Adds a {@code ProductManagerListener} to this product manager. The {@code ProductManagerListener} is
     * informed each time a product was added or removed.
     *
     * @param listener the listener to be added.
     * @return true if the listener was added, otherwise false.
     */
    public synchronized boolean addListener(Listener listener) {
        if (listener == null) {
            return false;
        }
        if (listeners == null) {
            listeners = Collections.synchronizedList(new ArrayList<>());
        }
        for (Listener l : listeners) {
            if (listener == l) {
                return false;
            }
        }
        listeners.add(listener);
        return true;
    }

    /**
     * Removes a {@code ProductManagerListener} from this product manager.
     *
     * @param listener The listener.
     * @return true, if the listener was removed, otherwise false.
     */
    public synchronized boolean removeListener(Listener listener) {
        if (listener != null && listeners != null) {
            for (int i = 0; i < listeners.size(); i++) {
                Listener l = listeners.get(i);
                if (listener == l) {
                    listeners.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasListeners() {
        return listeners != null && !listeners.isEmpty();
    }

    private void fireEvent(Product sourceProduct, int eventId) {
        if (hasListeners()) {
            Event event = new Event(sourceProduct);
            for (Listener listener : listeners) {
                fireEvent(eventId, listener, event);
            }
        }
    }

    private static void fireEvent(int eventId, Listener listener, Event event) {
        switch (eventId) {
            case PRODUCT_ADDED:
                listener.productAdded(event);
                break;
            case PRODUCT_REMOVED:
                listener.productRemoved(event);
                break;
        }
    }

    /**
     * A listener for the product manager.
     */
    public interface Listener {

        /**
         * Notified when a product was added.
         *
         * @param event the event
         */
        void productAdded(Event event);

        /**
         * Notified when a product was removed.
         *
         * @param event the event
         */
        void productRemoved(Event event);
    }

    /**
     * An event object passed into the {@link Listener} methods.
     */
    public static class Event extends EventObject {

        /**
         * @param product The product on which the event initially occurred.
         * @throws IllegalArgumentException if source is null.
         */
        public Event(Product product) {
            super(product);
        }

        /**
         * Retrieves a reference to the originating object, i.e. the one who fired the event.
         *
         * @return the originating object
         */
        public Product getProduct() {
            return (Product) getSource();
        }
    }

    private static class ExpressionUpdaterVisitor extends ProductVisitorAdapter {

        private final String oldExternName;
        private final String newExternName;

        public ExpressionUpdaterVisitor(String oldExternName, String newExternName) {
            this.oldExternName = oldExternName;
            this.newExternName = newExternName;
        }

        @Override
        public void visit(TiePointGrid grid) {
            grid.updateExpression(oldExternName, newExternName);
        }

        @Override
        public void visit(Band band) {
            band.updateExpression(oldExternName, newExternName);
        }

        @Override
        public void visit(VirtualBand virtualBand) {
            virtualBand.updateExpression(oldExternName, newExternName);
        }

        @Override
        public void visit(Mask mask) {
            mask.updateExpression(oldExternName, newExternName);
        }

        @Override
        public void visit(ProductNodeGroup group) {
            group.updateExpression(oldExternName, newExternName);
        }
    }

    private class ProductNodeNameChangeListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (ProductNode.PROPERTY_NAME_NAME.equals(event.getPropertyName())) {
                updateExpressionToRenamedNode(event.getSourceNode(), (String) event.getOldValue());
            }
        }
    }
}

