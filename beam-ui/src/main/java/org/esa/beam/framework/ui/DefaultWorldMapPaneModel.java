package org.esa.beam.framework.ui;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class DefaultWorldMapPaneModel extends WorldMapPaneModel {

    private static final LayerType layerType = LayerType.getLayerType("org.esa.beam.worldmap.BlueMarbleLayerType");

    private Layer worldMapLayer;
    private Product selectedProduct;
    private ArrayList<Product> productList;
    private ArrayList<GeoPos[]> additionalGeoBoundaryList;

    public DefaultWorldMapPaneModel() {
        productList = new ArrayList<Product>();
        additionalGeoBoundaryList = new ArrayList<GeoPos[]>();
    }

    @Override
    public Layer getWorldMapLayer(LayerContext context) {
        if (worldMapLayer == null) {
            worldMapLayer = layerType.createLayer(context, new ValueContainer());
        }
        return worldMapLayer;
    }

    @Override
    public void setSelectedProduct(Product product) {
        Product oldSelectedProduct = selectedProduct;
        if (oldSelectedProduct != product) {
            selectedProduct = product;
            firePropertyChange(PROPERTY_SELECTED_PRODUCT, oldSelectedProduct, selectedProduct);
        }
    }

    @Override
    public Product getSelectedProduct() {
        return selectedProduct;
    }

    @Override
    public Product[] getProducts() {
        return productList.toArray(new Product[productList.size()]);
    }

    @Override
    public void setProducts(Product[] products) {
        final Product[] oldProducts = getProducts();
        productList.clear();
        if (products != null) {
            productList.addAll(Arrays.asList(products));
        }
        firePropertyChange(PROPERTY_PRODUCTS, oldProducts, getProducts());
    }

    public void addProduct(Product product) {
        if (!productList.contains(product)) {
            final Product[] oldProducts = getProducts();
            if (productList.add(product)) {
                firePropertyChange(PROPERTY_PRODUCTS, oldProducts, getProducts());
            }
        }
    }

    public void removeProduct(Product product) {
        if (productList.contains(product)) {
            final Product[] oldProducts = getProducts();
            if (productList.remove(product)) {
                firePropertyChange(PROPERTY_PRODUCTS, oldProducts, getProducts());
            }
        }
    }

    @Override
    public GeoPos[][] getAdditionalGeoBoundaries() {
        return additionalGeoBoundaryList.toArray(new GeoPos[additionalGeoBoundaryList.size()][]);
    }

    @Override
    public void setAdditionalGeoBoundaries(GeoPos[][] geoBoundarys) {
        final GeoPos[][] oldGeoBoundarys = getAdditionalGeoBoundaries();
        additionalGeoBoundaryList.clear();
        if (geoBoundarys != null) {
            additionalGeoBoundaryList.addAll(Arrays.asList(geoBoundarys));
        }
        firePropertyChange(PROPERTY_ADDITIONAL_GEO_BOUNDARIES, oldGeoBoundarys, additionalGeoBoundaryList);
    }
}
