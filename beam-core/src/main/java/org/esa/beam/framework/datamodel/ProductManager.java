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
 * @version $Revision: 1.1.1.1 $ $Date: 2006/09/11 08:16:45 $
 */
public class ProductManager {

    private final static int _PRODUCT_ADDED = 1;
    private final static int _PRODUCT_REMOVED = 2;

    private Vector<ProductManagerListener> _listeners;

    private final ProductNodeList<Product> _products;


    /**
     * Constructs an product manager with an empty list of products.
     */
    public ProductManager() {
        _products = new ProductNodeList<Product>();
    }

    /**
     * Returns the number of products in this product manager.
     */
    public int getNumProducts() {
        return _products.size();
    }

    /**
     * Returns the product at the given index.
     */
    public Product getProductAt(int index) {
        return _products.getAt(index);
    }

    /**
     * Returns the display names of all products currently managed.
     *
     * @return an array containing the display names, never <code>null</code>, but the array can have zero length
     * @see ProductNode#getDisplayName()
     */
    public String[] getProductDisplayNames() {
        return _products.getDisplayNames();
    }

    /**
     * Returns the names of all products currently managed.
     *
     * @return an array containing the names, never <code>null</code>, but the array can have zero length
     */
    public String[] getProductNames() {
        return _products.getNames();
    }

    /**
     * Returns an array of all products currently managed.
     *
     * @return an array containing the products, never <code>null</code>, but the array can have zero length
     */
    public Product[] getProducts() {
        return _products.toArray(new Product[getNumProducts()]);
    }

    /**
     * Returns the product with the given display name.
     */
    public Product getProductByDisplayName(final String displayName) {
        if (displayName == null) {
            return null;
        }
        return _products.getByDisplayName(displayName);
    }

    /**
     * Returns the product with the given name.
     */
    public Product getProduct(String name) {
        return _products.get(name);
    }

    /**
     * Tests whether a product with the given name is contained in this list.
     */
    public boolean containsProduct(String name) {
        return _products.contains(name);
    }

    /**
     * Tests whether the given product is contained in this list.
     *
     * @param product The product.
     * @return {@code true} if so.
     */
    public boolean contains(final Product product) {
        return _products.contains(product);
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
            if (_products.add(product)) {
                setProductManager(product);
                product.setRefNo(getHighestRefNo() + 1);
                fireEvent(product, _PRODUCT_ADDED);
            }
        }
    }

    /**
     * Removes the given product from this product manager if it exists.
     *
     * @param product the product to be removed, ignored if <code>null</code>
     */
    public boolean removeProduct(Product product) {
        if (product != null) {
            int index = _products.indexOf(product);
            if (index >= 0) {
                if (_products.remove(product)) {
                    _products.clearRemovedList();
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
     * Returns the greatest reference number of the products in this manager.
     */
    private int getHighestRefNo() {
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
    public boolean addListener(ProductManagerListener listener) {
        if (listener != null) {
            if (_listeners == null) {
                _listeners = new Vector<ProductManagerListener>();
            }
            if (!_listeners.contains(listener)) {
                _listeners.add(listener);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes a <code>ProductManagerListener</code> from this product manager.
     */
    public void removeProductListener(ProductManagerListener listener) {
        if (listener != null && _listeners != null) {
            _listeners.remove(listener);
        }
    }

    private boolean hasListeners() {
        return _listeners != null && _listeners.size() > 0;
    }

    private void fireEvent(Product sourceProduct, int eventId) {
        if (hasListeners()) {
            ProductManagerEvent event = new ProductManagerEvent(sourceProduct);
            for (ProductManagerListener listener : _listeners) {
                fireEvent(eventId, listener, event);
            }
        }
    }

    private void fireEvent(int eventId, ProductManagerListener listener, ProductManagerEvent event) {
        switch (eventId) {
            case _PRODUCT_ADDED:
                listener.productAdded(event);
                break;
            case _PRODUCT_REMOVED:
                listener.productRemoved(event);
                break;
        }
    }

    public interface ProductManagerListener {

        /**
         * Notified when a product was added.
         *
         * @param event the product node which the listener to be notified
         */
        void productAdded(ProductManagerEvent event);

        /**
         * Notified when a node was removed.
         *
         * @param event the product node which the listener to be notified
         */
        void productRemoved(ProductManagerEvent event);
    }

    public class ProductManagerEvent extends EventObject {

        /**
         * Constructs a productEvent object.
         *
         * @param product the source class where the object originates
         */
        public ProductManagerEvent(Product product) {
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
}

