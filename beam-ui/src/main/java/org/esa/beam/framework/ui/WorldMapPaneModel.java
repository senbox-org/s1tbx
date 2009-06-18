package org.esa.beam.framework.ui;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * User: Marco
 * Date: 17.06.2009
 */
public abstract class WorldMapPaneModel {

    public static final String PROPERTY_LAYER = "layer";
    public static final String PROPERTY_SELECTED_PRODUCT = "selectedProduct";
    public static final String PROPERTY_PRODUCTS = "products";
    public static final String PROPERTY_ADDITIONAL_GEO_BOUNDARIES = "additionalGeoBoundaries";

    private PropertyChangeSupport changeSupport;

    public abstract Layer getWorldMapLayer(LayerContext context);

    public abstract Product getSelectedProduct();

    public abstract void setSelectedProduct(Product product);

    public abstract Product[] getProducts();

    public abstract void setProducts(Product[] products);

    public abstract GeoPos[][] getAdditionalGeoBoundaries();

    public abstract void setAdditionalGeoBoundaries(GeoPos[][] additionalGeoBoundaries);

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (changeSupport != null) {
            changeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public void addModelChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removeModelChangeListener(PropertyChangeListener listener) {
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }

}
