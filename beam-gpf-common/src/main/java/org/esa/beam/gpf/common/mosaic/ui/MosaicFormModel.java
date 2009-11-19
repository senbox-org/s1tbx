package org.esa.beam.gpf.common.mosaic.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.ui.WorldMapPaneDataModel;
import org.esa.beam.gpf.common.mosaic.MosaicOp;
import org.esa.beam.util.math.MathUtils;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class MosaicFormModel {

    public static final String PROPERTY_SOURCE_PRODUCT_FILES = "sourceProductFiles";
    public static final String PROPERTY_UPDATE_PRODUCT = "updateProduct";
    public static final String PROPERTY_UPDATE_MODE = "updateMode";
    public static final String PROPERTY_SHOW_SOURCE_PRODUCTS = "showSourceProducts";

    private final PropertyContainer container;
    private final Map<String, Object> parameterMap = new HashMap<String, Object>();
    private final Map<File, Product> fileProductMap;

    private Product refProduct;
    private File refProductFile;


    MosaicFormModel() {
        container = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer("Mosaic", parameterMap);
        final PropertyDescriptor sourceFilesDescriptor = new PropertyDescriptor(PROPERTY_SOURCE_PRODUCT_FILES,
                                                                                File[].class);
        container.addProperty(new Property(sourceFilesDescriptor,
                                           new MapEntryAccessor(parameterMap, PROPERTY_SOURCE_PRODUCT_FILES)));
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

        fileProductMap = Collections.synchronizedMap(new HashMap<File, Product>());
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

    Map<String, Product> getSourceProductMap() throws IOException {
        final HashMap<String, Product> map = new HashMap<String, Product>();
        final File[] files = getSourceProductFiles();
        for (int i = 0; i < files.length; i++) {
            final File file = files[i];
            final Product product;
            if (refProduct == null || !file.equals(refProduct.getFileLocation())) {
                product = ProductIO.readProduct(file, null);
            } else {
                product = refProduct;
            }
            map.put(GPF.SOURCE_PRODUCT_FIELD_NAME + (i + 1), product);
        }
        final Product updateProduct = getUpdateProduct();
        if (updateProduct != null) {
            map.put(PROPERTY_UPDATE_PRODUCT, updateProduct);
        }

        return map;
    }

    File[] getSourceProductFiles() {
        final Object value = getPropertyValue(PROPERTY_SOURCE_PRODUCT_FILES);
        if (value instanceof File[]) {
            return (File[]) value;
        }

        return new File[0];
    }

    Product getUpdateProduct() {
        final Object value = getPropertyValue(PROPERTY_UPDATE_PRODUCT);
        if (value instanceof Product) {
            return (Product) value;
        }

        return null;
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
        if (container.getValue(PROPERTY_SOURCE_PRODUCT_FILES) != null) {
            final File[] files = (File[]) container.getValue(PROPERTY_SOURCE_PRODUCT_FILES);
            if (files.length > 0) {
                try {
                    if (!files[0].equals(refProductFile)) {
                        refProductFile = files[0];
                        if (refProduct != null) {
                            refProduct.dispose();
                            refProduct = null;
                        }
                        refProduct = ProductIO.readProduct(refProductFile, null);
                    }
                } catch (IOException e) {
                    final String msg = String.format("Cannot read product '%s'", files[0].getPath());
                    throw new IOException(msg, e);
                }
            } else {
                if (refProduct != null) {
                    refProduct.dispose();
                    refProduct = null;
                }
            }
        }
        if (refProduct == null) {
            final String msg = String.format("No reference product available.");
            throw new IOException(msg);
        }
        return refProduct;
    }

    public Product getBoundaryProduct() throws FactoryException, TransformException {
        final CoordinateReferenceSystem mapCRS = getTargetCRS();
        if (mapCRS != null) {
            final double pixelSizeX = (Double) getPropertyValue("pixelSizeX");
            final double pixelSizeY = (Double) getPropertyValue("pixelSizeY");
            final GeneralEnvelope generalEnvelope = getGeoEnvelope();

            final Envelope targetEnvelope = CRS.transform(generalEnvelope, mapCRS);
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

    private CoordinateReferenceSystem getTargetCRS() throws FactoryException {
        final String crs = (String) getPropertyValue("crs");
        if (crs == null) {
            return null;
        }
        try {
            return CRS.parseWKT(crs);
        } catch (FactoryException e) {
            return CRS.decode(crs, true);
        }
    }

    GeneralEnvelope getGeoEnvelope() {
        final double west = (Double) getPropertyValue("westBound");
        final double north = (Double) getPropertyValue("northBound");
        final double east = (Double) getPropertyValue("eastBound");
        final double south = (Double) getPropertyValue("southBound");
        final Rectangle2D.Double geoBounds = new Rectangle2D.Double();
        geoBounds.setFrameFromDiagonal(west, north, east, south);
        final GeneralEnvelope generalEnvelope = new GeneralEnvelope(geoBounds);
        generalEnvelope.setCoordinateReferenceSystem(DefaultGeographicCRS.WGS84);
        return generalEnvelope;
    }

    public void updateWithSourceProducts(WorldMapPaneDataModel worldMapModel,
                                         JComponent parentComponent) {
        final File[] productFiles = (File[]) container.getValue(PROPERTY_SOURCE_PRODUCT_FILES);
        final Boolean display = (Boolean) container.getValue(PROPERTY_SHOW_SOURCE_PRODUCTS);
        if (display && productFiles != null && productFiles.length > 0) {
            SwingWorker sw = new ProductLoaderSwingWorker(worldMapModel, productFiles, parentComponent);
            sw.execute();
        } else {
            worldMapModel.setProducts(null);
            for (Map.Entry<File, Product> entry : fileProductMap.entrySet()) {
                entry.getValue().dispose();
            }
            fileProductMap.clear();
        }
    }

    private class ProductLoaderSwingWorker extends ProgressMonitorSwingWorker<Product[], Product> {

        private static final String TITLE = "Loading source products";

        private final WorldMapPaneDataModel worldMapModel;
        private final File[] productFiles;
        private final JComponent component;

        private ProductLoaderSwingWorker(WorldMapPaneDataModel worldMapModel, File[] productFiles,
                                         JComponent parentComponent) {
            super(parentComponent, TITLE);
            component = parentComponent;
            this.worldMapModel = worldMapModel;
            this.productFiles = productFiles.clone();
        }

        @Override
        protected Product[] doInBackground(ProgressMonitor pm) throws Exception {
            for (int i = 0; i < productFiles.length; i++) {
                File productFile = productFiles[i];
                final Product currentProduct = fileProductMap.get(productFile);
                final int refNo = i + 1;
                if (currentProduct == null) {
                    final Product product = ProductIO.readProduct(productFile, null);
                    product.setRefNo(refNo);
                    fileProductMap.put(productFile, product);
                } else {
                    currentProduct.setRefNo(refNo);
                }
            }
            return fileProductMap.values().toArray(new Product[fileProductMap.size()]);
        }

        @Override
        protected void done() {
            try {
                worldMapModel.setProducts(get());
            } catch (Exception e) {
                final String msg = String.format("Cannot display source products.\n%s", e.getMessage());
                JOptionPane.showMessageDialog(component, msg, TITLE, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}
