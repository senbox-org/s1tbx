package org.esa.beam.gpf.common.mosaic.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class MosaicFormModel {

    private PropertyContainer container;
    public static final String PROPERTY_SOURCE_PRODUCT_FILES = "sourceProductFiles";
    public static final String PROPERTY_UPDATE_PRODUCT = "updateProduct";
    public static final String PROPERTY_UPDATE_MODE = "updateMode";
    private Product refProduct;
    private File refProductFile;

    MosaicFormModel() {
        Map<String, Object> parameterMap = new HashMap<String, Object>();

        container = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer("Mosaic", parameterMap);
        final PropertyDescriptor sourceFilesDescriptor = new PropertyDescriptor(PROPERTY_SOURCE_PRODUCT_FILES,
                                                                                new File[0].getClass());
        container.addProperty(new Property(sourceFilesDescriptor,
                                           new MapEntryAccessor(parameterMap, PROPERTY_SOURCE_PRODUCT_FILES)));
        container.addProperty(new Property(new PropertyDescriptor(PROPERTY_UPDATE_PRODUCT, Product.class),
                                           new MapEntryAccessor(parameterMap, PROPERTY_UPDATE_PRODUCT)));
        container.addProperty(new Property(new PropertyDescriptor(PROPERTY_UPDATE_MODE, Boolean.class),
                                           new MapEntryAccessor(parameterMap, PROPERTY_UPDATE_MODE)));
        container.setValue(PROPERTY_UPDATE_MODE, false);
        try {
            container.setDefaultValues();
        } catch (ValidationException ignore) {
        }

    }

    public PropertyContainer getPropertyContainer() {
        return container;
    }

    public Product getReferenceProduct() throws IOException {
        if (container.getValue(PROPERTY_SOURCE_PRODUCT_FILES) != null) {
            final File[] files = (File[]) container.getValue(PROPERTY_SOURCE_PRODUCT_FILES);
            if (files.length > 0) {
                try {
                    if (!files[0].equals(refProductFile)) {
                        refProductFile = files[0];
                        refProduct.dispose();
                        refProduct = null;
                        refProduct = ProductIO.readProduct(refProductFile, null);
                    }
                } catch (IOException e) {
                    final String msg = String.format("Cannot read product '%s'", files[0].getPath());
                    throw new IOException(msg, e);
                }
            }else {
                refProduct.dispose();
                refProduct = null;
            }
        }
        if (refProduct == null) {
            final String msg = String.format("No reference product available.");
            throw new IOException(msg);
        }
        return refProduct;
    }
}
