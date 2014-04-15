/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.validators.NotNullValidator;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@SuppressWarnings({"UnusedDeclaration"})
public class TargetProductSelectorModel {

    public static final String PROPERTY_PRODUCT_NAME = "productName";
    public static final String PROPERTY_SAVE_TO_FILE_SELECTED = "saveToFileSelected";
    public static final String PROEPRTY_OPEN_IN_APP_SELECTED = "openInAppSelected";
    public static final String PROPERTY_PRODUCT_DIR = "productDir";
    public static final String PROPERTY_FORMAT_NAME = "formatName";

    private static final String ENVISAT_FORMAT_NAME = "ENVISAT";

    // used by object binding
    private String productName;
    private boolean saveToFileSelected;
    private boolean openInAppSelected;
    private File productDir;
    private String formatName;
    private String[] formatNames;

    private final PropertyContainer propertyContainer;

    public TargetProductSelectorModel() {
        this(ProductIOPlugInManager.getInstance().getAllProductWriterFormatStrings());
    }

    public TargetProductSelectorModel(String[] formatNames) {
        propertyContainer = PropertyContainer.createObjectBacked(this);
        PropertyDescriptor productNameDescriptor = propertyContainer.getDescriptor(PROPERTY_PRODUCT_NAME);
        productNameDescriptor.setValidator(new ProductNameValidator());
        productNameDescriptor.setDisplayName("target product name");

        PropertyDescriptor productDirDescriptor = propertyContainer.getDescriptor("productDir");
        productDirDescriptor.setValidator(new NotNullValidator());
        productDirDescriptor.setDisplayName("target product directory");

        setOpenInAppSelected(true);
        setSaveToFileSelected(true);

        this.formatNames = formatNames;
        if (StringUtils.contains(this.formatNames, ProductIO.DEFAULT_FORMAT_NAME)) {
            setFormatName(ProductIO.DEFAULT_FORMAT_NAME);
        } else {
            setFormatName(formatNames[0]);
        }

        propertyContainer.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                switch (propertyName) {
                    case "saveToFileSelected": {
                        boolean changesToDeselected = !(Boolean) evt.getNewValue();
                        if (changesToDeselected) {
                            setOpenInAppSelected(true);
                        } else if (!canReadOutputFormat()) {
                            setOpenInAppSelected(false);
                        }
                        break;
                    }
                    case "openInAppSelected": {
                        boolean changesToDeselected = !(Boolean) evt.getNewValue();
                        if (changesToDeselected) {
                            setSaveToFileSelected(true);
                        }
                        break;
                    }
                    case "formatName":
                        if (!canReadOutputFormat()) {
                            setOpenInAppSelected(false);
                        }
                        break;
                }
            }
        });

    }

    public static TargetProductSelectorModel createEnvisatTargetProductSelectorModel() {
        return new EnvisatTargetProductSelectorModel();
    }

    public String getProductName() {
        return productName;
    }

    public boolean isSaveToFileSelected() {
        return saveToFileSelected;
    }

    public boolean isOpenInAppSelected() {
        return openInAppSelected;
    }

    public File getProductDir() {
        return productDir;
    }

    public File getProductFile() {
        return new File(productDir, getProductFileName());
    }

    String getProductFileName() {
        String productFileName = productName;
        Iterator<ProductWriterPlugIn> iterator = ProductIOPlugInManager.getInstance().getWriterPlugIns(formatName);
        if (iterator.hasNext()) {
            final ProductWriterPlugIn writerPlugIn = iterator.next();

            boolean ok = false;
            for (String extension : writerPlugIn.getDefaultFileExtensions()) {
                if (productFileName.endsWith(extension)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                productFileName = productFileName.concat(writerPlugIn.getDefaultFileExtensions()[0]);
            }
        }
        return productFileName;
    }

    public String getFormatName() {
        return formatName;
    }

    public String[] getFormatNames() {
        return formatNames;
    }

    public boolean canReadOutputFormat() {
        return ProductIOPlugInManager.getInstance().getReaderPlugIns(formatName).hasNext();
    }

    public void setProductName(String productName) {
        setValueContainerValue(PROPERTY_PRODUCT_NAME, productName);
    }

    public void setSaveToFileSelected(boolean saveToFileSelected) {
        setValueContainerValue(PROPERTY_SAVE_TO_FILE_SELECTED, saveToFileSelected);
    }

    public void setOpenInAppSelected(boolean openInAppSelected) {
        setValueContainerValue(PROEPRTY_OPEN_IN_APP_SELECTED, openInAppSelected);
    }

    public void setProductDir(File productDir) {
        setValueContainerValue(PROPERTY_PRODUCT_DIR, productDir);
    }

    public void setFormatName(String formatName) {
        setValueContainerValue(PROPERTY_FORMAT_NAME, formatName);
    }

    public PropertyContainer getValueContainer() {
        return propertyContainer;
    }

    private void setValueContainerValue(String name, Object value) {
        propertyContainer.setValue(name, value);
    }

    private static class ProductNameValidator implements Validator {

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            final String name = (String) value;
            if (!ProductNode.isValidNodeName(name)) {
                final String message = MessageFormat.format("The product name ''{0}'' is not valid.\n\n"
                                                            + "Names must not start with a dot and must not\n"
                                                            + "contain any of the following characters: \\/:*?\"<>|",
                                                            name);
                throw new ValidationException(message);
            }
        }
    }

    public static class EnvisatTargetProductSelectorModel extends TargetProductSelectorModel {

        private EnvisatTargetProductSelectorModel() {
            super(createFormats());
        }

        @Override
        public File getProductFile() {
            if (!ENVISAT_FORMAT_NAME.equals(getFormatName())) {
                return super.getProductFile();
            }
            final String productName = getProductName();
            return new File(getProductDir(), FileUtils.ensureExtension(productName, ".N1"));

        }

        private static String[] createFormats() {
            final String[] productWriterFormatStrings = ProductIOPlugInManager.getInstance().getAllProductWriterFormatStrings();
            final String[] formatNames = Arrays.copyOf(productWriterFormatStrings,
                                                       productWriterFormatStrings.length + 1);
            formatNames[formatNames.length - 1] = ENVISAT_FORMAT_NAME;
            return formatNames;
        }
    }
}
