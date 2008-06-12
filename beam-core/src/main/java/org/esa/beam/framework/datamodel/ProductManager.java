/*
 * $Id: ProductManager.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.datamodel;

import java.util.EventObject;
import java.util.Vector;


/**
 * A type-safe container for elements of the type <code>Product</code>. ProductListeners can be added to inform if a
 * <code>Product</code> was added or removed.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ProductManager {

    private final static int _PRODUCT_ADDED = 1;
    private final static int _PRODUCT_REMOVED = 2;

    private Vector<Listener> listeners;

    private final ProductNodeList<Product> products;


    /**
     * Constructs an product manager with an empty list of products.
     */
    public ProductManager() {
        products = new ProductNodeList<Product>();
    }

    /**
     * @deprecated Since BEAM 4.2 use {@link #getProductCount()} instead.
     */
    @Deprecated
    public int getNumProducts() {
        return products.size();
    }

    /**
     * @return The number of products in this product manager.
     */
    public int getProductCount() {
        return products.size();
    }

    /**
     * @deprecated Since BEAM 4.2 use {@link #getProduct(int)} instead.
     */
    @Deprecated
    public Product getProductAt(int index) {
        return products.getAt(index);
    }

    /**
     * Gets the product at the given index.
     * @param index the index
     * @return The product at the given index.
     */
    public Product getProduct(int index) {
        return products.getAt(index);
    }

    /**
     * Returns the display names of all products currently managed.
     *
     * @return an array containing the display names, never <code>null</code>, but the array can have zero length
     * @see ProductNode#getDisplayName()
     */
    public String[] getProductDisplayNames() {
        return products.getDisplayNames();
    }

    /**
     * Returns the names of all products currently managed.
     *
     * @return an array containing the names, never <code>null</code>, but the array can have zero length
     */
    public String[] getProductNames() {
        return products.getNames();
    }

    /**
     * Returns an array of all products currently managed.
     *
     * @return an array containing the products, never <code>null</code>, but the array can have zero length
     */
    public Product[] getProducts() {
        return products.toArray(new Product[getNumProducts()]);
    }

    /**
     * @param displayName The product's display name.
     * @return The product with the given display name.
     */
    public Product getProductByDisplayName(final String displayName) {
        if (displayName == null) {
            return null;
        }
        return products.getByDisplayName(displayName);
    }

    /**
     * @param name The product name.
     * @return The product with the given name.
     */
    public Product getProduct(String name) {
        return products.get(name);
    }

    /**
     * Tests whether a product with the given name is contained in this list.
     *
     * @param name the product name
     * @return true, if so
     */
    public boolean containsProduct(String name) {
        return products.contains(name);
    }

    /**
     * Tests whether the given product is contained in this list.
     *
     * @param product The product.
     * @return {@code true} if so.
     */
    public boolean contains(final Product product) {
        return products.contains(product);
    }

    /**
     * Adds the given product to this product manager if it does not already exists and sets it's reference number one
     * biger than the greatest reference number in this product manager.
     *
     * @param product the product to be added, ignored if <code>null</code>
     */
    public void addProduct(Product product) {
        if (product != null) {
            if (contains(product)) {
                return;
            }
            if (products.add(product)) {
                setProductManager(product);
                product.setRefNo(getNextRefNo() + 1);
                fireEvent(product, _PRODUCT_ADDED);
            }
        }
    }

    /**
     * Removes the given product from this product manager if it exists.
     *
     * @param product the product to be removed, ignored if <code>null</code>
     * @return true, if the product was removed
     */
    public boolean removeProduct(Product product) {
        if (product != null) {
            int index = products.indexOf(product);
            if (index >= 0) {
                if (products.remove(product)) {
                    products.clearRemovedList();
                    product.resetRefNo();
                    clearProductManager(product);
                    fireEvent(product, _PRODUCT_REMOVED);
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
        final int numProducts = getNumProducts();
        int highestRefNo = 0;
        for (int i = 0; i < numProducts; i++) {
            final int refNo = getProductAt(i).getRefNo();
            if (refNo > highestRefNo) {
                highestRefNo = refNo;
            }
        }
        return highestRefNo;
    }

    //////////////////////////////////////////////////////////////////////////
    // Product listener support

    /**
     * Adds a <code>ProductManagerListener</code> to this product manager. The <code>ProductManagerListener</code> is
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
            listeners = new Vector<Listener>(8);
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
     * Removes a <code>ProductManagerListener</code> from this product manager.
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

    /**
     * Removes a <code>ProductManagerListener</code> from this product manager.
     *
     * @param listener The listener.
     * @deprecated use #removeListener
     */
    @Deprecated
    public void removeProductListener(Listener listener) {
        removeListener(listener);
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
            case _PRODUCT_ADDED:
                listener.productAdded(event);
                break;
            case _PRODUCT_REMOVED:
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
         * @param event the product node which the listener to be notified
         */
        void productAdded(Event event);

        /**
         * Notified when a node was removed.
         *
         * @param event the product node which the listener to be notified
         */
        void productRemoved(Event event);
    }

    /**
     * @deprecated use {@link Listener} instead
     */
    @Deprecated
    public interface ProductManagerListener extends Listener {
    }

    /**
     * An event object passed into the {@link Listener} methods.
     */
    public static class Event extends EventObject {

        /**
         * Constructs a productEvent object.
         *
         * @param product the source class where the object originates
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

    /**
     * @deprecated use {@link Event} instead
     */
    @Deprecated
    public static class ProductManagerEvent extends Event {
        public ProductManagerEvent(Product product) {
            super(product);
        }
    }
}

