package org.esa.beam.framework.ui.io;

import com.bc.ceres.binding.Factory;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDefinition;
import com.bc.ceres.binding.ValueDefinitionFactory;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.StringUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Iterator;

/**
 * Target product selector model.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@SuppressWarnings({"UnusedDeclaration"})
public class TargetProductSelectorModel {

    // used by object binding
    private String productName;
    private boolean saveToFileSelected;
    private boolean openInVisatSelected;
    private File directory;
    private String formatName;
    private String[] formatNames;
    private final ValueContainer valueContainer;

    public TargetProductSelectorModel(boolean saveToFile) {
        final Factory factory = new Factory(new ValueDefinitionFactory() {
            public ValueDefinition createValueDefinition(Field field) {
                return new ValueDefinition(field.getName(), field.getType());
            }
        });
        valueContainer = factory.createObjectBackedValueContainer(this);
        valueContainer.addPropertyChangeListener("saveToFileSelected", new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (!(Boolean) evt.getNewValue()) {
                    setOpenInVisatSelected(true);
                }
            }
        });
        valueContainer.addPropertyChangeListener("openInVisatSelected", new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (!(Boolean) evt.getNewValue()) {
                    setSaveToFileSelected(true);
                }
            }
        });
        setSaveToFileSelected(saveToFile);
        formatNames = ProductIOPlugInManager.getInstance().getAllProductWriterFormatStrings();
        if (StringUtils.contains(formatNames, ProductIO.DEFAULT_FORMAT_NAME)) {
            setFormatName(ProductIO.DEFAULT_FORMAT_NAME);
        } else {
            setFormatName(formatNames[0]);
        }
    }

    public String getProductName() {
        return productName;
    }

    public boolean isSaveToFileSelected() {
        return saveToFileSelected;
    }

    public boolean isOpenInVisatSelected() {
        return openInVisatSelected;
    }

    public File getDirectory() {
        return directory;
    }

    public File getFile() {
        return new File(directory, getFileName());
    }

    public String getFileName() {
        String productFileName = productName;
        Iterator<ProductWriterPlugIn> iterator = ProductIOPlugInManager.getInstance().getWriterPlugIns(formatName);
        if (iterator.hasNext()) {
            ProductWriterPlugIn writerPlugIn = iterator.next();

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


    public String[] getFormatNames() {
        return formatNames;
    }

    public void setProductName(String productName) {
        setValueContainerValue("productName", productName);
    }

    public void setSaveToFileSelected(boolean saveToFileSelected) {
        setValueContainerValue("saveToFileSelected", saveToFileSelected);
    }

    public void setOpenInVisatSelected(boolean openInVisatSelected) {
        setValueContainerValue("openInVisatSelected", openInVisatSelected);
    }

    public void setDirectory(File directory) {
        setValueContainerValue("directory", directory);
    }

    public void setFormatName(String formatName) {
        setValueContainerValue("formatName", formatName);
    }

    public ValueContainer getValueContainer() {
        return valueContainer;
    }

    private void setValueContainerValue(String name, Object value) {
        try {
            valueContainer.setValue(name, value);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
