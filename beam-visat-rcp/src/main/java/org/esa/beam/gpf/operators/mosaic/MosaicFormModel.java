package org.esa.beam.gpf.operators.mosaic;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.ui.WorldMapPaneDataModel;
import org.esa.beam.gpf.operators.standard.MosaicOp;
import org.esa.beam.util.math.MathUtils;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Marco Peters
 * @author Ralf Quast
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class MosaicFormModel {

    public static final String PROPERTY_UPDATE_PRODUCT = "updateProduct";
    public static final String PROPERTY_UPDATE_MODE = "updateMode";
    public static final String PROPERTY_SHOW_SOURCE_PRODUCTS = "showSourceProducts";
    
    public static final String PROPERTY_WEST_BOUND = "westBound";
    public static final String PROPERTY_NORTH_BOUND = "northBound";
    public static final String PROPERTY_EAST_BOUND = "eastBound";
    public static final String PROPERTY_SOUTH_BOUND = "southBound";

    private final PropertyContainer container;
    private final Map<String, Object> parameterMap = new HashMap<String, Object>();
    private final Map<File, Product> sourceProductMap = Collections.synchronizedMap(new HashMap<File, Product>());
    private final WorldMapPaneDataModel worldMapModel = new WorldMapPaneDataModel();

    MosaicFormModel() {
        container = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer("Mosaic", parameterMap);
        container.addProperty(new Property(new PropertyDescriptor(PROPERTY_UPDATE_PRODUCT, Product.class),
                                           new MapEntryAccessor(parameterMap, PROPERTY_UPDATE_PRODUCT)));
        container.addProperty(new Property(new PropertyDescriptor(PROPERTY_UPDATE_MODE, Boolean.class),
                                           new MapEntryAccessor(parameterMap, PROPERTY_UPDATE_MODE)));
        container.addProperty(new Property(new PropertyDescriptor(PROPERTY_SHOW_SOURCE_PRODUCTS, Boolean.class),
                                           new MapEntryAccessor(parameterMap, PROPERTY_SHOW_SOURCE_PRODUCTS)));

        try {
            container.setDefaultValues();
        } catch (ValidationException ignore) {
        }
        container.setValue(PROPERTY_UPDATE_MODE, false);
        container.setValue(PROPERTY_SHOW_SOURCE_PRODUCTS, false);
        container.addPropertyChangeListener(PROPERTY_SHOW_SOURCE_PRODUCTS, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (Boolean.TRUE.equals(evt.getNewValue())) {
                    final Collection<Product> products = sourceProductMap.values();
                    worldMapModel.setProducts(products.toArray(new Product[products.size()]));
                } else {
                    worldMapModel.setProducts(null);
                }
            }
        });
    }

    void setSourceProducts(File[] files) throws IOException {
        final List<File> fileList = Arrays.asList(files);
        final Iterator<Map.Entry<File, Product>> iterator = sourceProductMap.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<File, Product> entry = iterator.next();
            if (!fileList.contains(entry.getKey())) {
                final Product product = entry.getValue();
                worldMapModel.removeProduct(product);
                iterator.remove();
                product.dispose();
            }
        }
        for (int i = 0; i < files.length; i++) {
            final File file = files[i];
            Product product = sourceProductMap.get(file);
            if (product == null) {
                product = ProductIO.readProduct(file);
                sourceProductMap.put(file, product);
                if (Boolean.TRUE.equals(getPropertyValue(PROPERTY_SHOW_SOURCE_PRODUCTS))) {
                    worldMapModel.addProduct(product);
                }
            }
            final int refNo = i + 1;
            if (product.getRefNo() != refNo) {
                product.resetRefNo();
                product.setRefNo(refNo);
            }
        }
    }

    Map<String, Object> getParameterMap() {
        final Map<String, Object> map = new HashMap<String, Object>(parameterMap.size());
        for (final String key : parameterMap.keySet()) {
            for (final Field field : MosaicOp.class.getDeclaredFields()) {
                final Parameter annotation = field.getAnnotation(Parameter.class);
                if (annotation != null) {
                    if (key.equals(field.getName()) || key.equals(annotation.alias())) {
                        map.put(key, parameterMap.get(key));
                    }
                }
            }
        }
        return map;
    }

    Map<String, Product> getSourceProductMap() {
        final HashMap<String, Product> map = new HashMap<String, Product>(sourceProductMap.size());
        for (final Product product : sourceProductMap.values()) {
            map.put(GPF.SOURCE_PRODUCT_FIELD_NAME + product.getRefNo(), product);
        }
        if (Boolean.TRUE.equals(container.getValue(PROPERTY_UPDATE_MODE))) {
            final Product updateProduct = getUpdateProduct();
            if (updateProduct != null) {
                map.put(PROPERTY_UPDATE_PRODUCT, updateProduct);
            }
        }
        return map;
    }

    boolean isUpdateMode() {
        return Boolean.TRUE.equals(getPropertyValue(PROPERTY_UPDATE_MODE));
    }

    Product getUpdateProduct() {
        final Object value = getPropertyValue(PROPERTY_UPDATE_PRODUCT);
        if (value instanceof Product) {
            return (Product) value;
        }

        return null;
    }

    void setUpdateProduct(Product product) {
        setPropertyValue(PROPERTY_UPDATE_PRODUCT, product);
        if (product != null && product.getGeoCoding() != null && product.getGeoCoding().getMapCRS() != null) {
            setTargetCRS(product.getGeoCoding().getMapCRS().toWKT());
        }
    }

    MosaicOp.Variable[] getVariables() {
        return (MosaicOp.Variable[]) getPropertyValue("variables");
    }

    MosaicOp.Condition[] getConditions() {
        return (MosaicOp.Condition[]) getPropertyValue("conditions");
    }

    PropertyContainer getPropertyContainer() {
        return container;
    }

    public Object getPropertyValue(String propertyName) {
        return container.getValue(propertyName);
    }

    public void setPropertyValue(String propertyName, Object value) {
        container.setValue(propertyName, value);
    }

    public Product getReferenceProduct() throws IOException {
        for (Product product : sourceProductMap.values()) {
            if (product.getRefNo() == 1) {
                return product;
            }
        }
        return null;
    }

    public Product getBoundaryProduct() throws FactoryException, TransformException {
        final CoordinateReferenceSystem mapCRS = getTargetCRS();
        if (mapCRS != null) {
            final double pixelSizeX = (Double) getPropertyValue("pixelSizeX");
            final double pixelSizeY = (Double) getPropertyValue("pixelSizeY");
            final Envelope envelope = getTargetEnvelope();

            final Envelope targetEnvelope = CRS.transform(envelope, mapCRS);
            final int sceneRasterWidth = MathUtils.floorInt(targetEnvelope.getSpan(0) / pixelSizeX);
            final int sceneRasterHeight = MathUtils.floorInt(targetEnvelope.getSpan(1) / pixelSizeY);
            final Product outputProduct = new Product("mosaic", "MosaicBounds",
                                                      sceneRasterWidth, sceneRasterHeight);
            final Rectangle imageRect = new Rectangle(0, 0, sceneRasterWidth, sceneRasterHeight);
            final AffineTransform i2mTransform = new AffineTransform();
            i2mTransform.translate(targetEnvelope.getMinimum(0), targetEnvelope.getMinimum(1));
            i2mTransform.scale(pixelSizeX, pixelSizeY);
            i2mTransform.translate(-0.5, -0.5);
            outputProduct.setGeoCoding(new CrsGeoCoding(mapCRS, imageRect, i2mTransform));
            return outputProduct;
        }
        return null;
    }

    void setTargetCRS(String crs) {
        setPropertyValue("crs", crs);
    }

    CoordinateReferenceSystem getTargetCRS() throws FactoryException {
        final String crs = (String) getPropertyValue("crs");
        if (crs == null) {
            return null;
        }
        try {
            return CRS.parseWKT(crs);
        } catch (FactoryException ignored) {
            return CRS.decode(crs, true);
        }
    }

    GeneralEnvelope getTargetEnvelope() {
        final double west = (Double) getPropertyValue(PROPERTY_WEST_BOUND);
        final double north = (Double) getPropertyValue(PROPERTY_NORTH_BOUND);
        final double east = (Double) getPropertyValue(PROPERTY_EAST_BOUND);
        final double south = (Double) getPropertyValue(PROPERTY_SOUTH_BOUND);

        final Rectangle2D bounds = new Rectangle2D.Double();
        bounds.setFrameFromDiagonal(west, north, east, south);

        final GeneralEnvelope envelope = new GeneralEnvelope(bounds);
        envelope.setCoordinateReferenceSystem(DefaultGeographicCRS.WGS84);

        return envelope;
    }

    public WorldMapPaneDataModel getWorldMapModel() {
        return worldMapModel;
    }
}
