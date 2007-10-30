package org.esa.beam.collocation.visat;

import com.bc.ceres.binding.*;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.ui.io.TargetProductSelectorModel;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.Field;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class CollocationFormModel {

    private Product masterProduct;
    private Product slaveProduct;

    private boolean createNewProduct;
    private boolean renameMasterComponents;
    private boolean renameSlaveComponents;

    private String masterComponentPattern;
    private String slaveComponentPattern;

    private TargetProductSelectorModel targetProductSelectorModel;
    private DefaultComboBoxModel resamplingComboBoxModel;

    private final ValueContainer valueContainer;

    public CollocationFormModel() {
        createNewProduct = true;
        renameMasterComponents = true;
        renameSlaveComponents = true;
        masterComponentPattern = "${ORIGINAL_NAME}";
        slaveComponentPattern = "${ORIGINAL_NAME}";

        targetProductSelectorModel = new TargetProductSelectorModel(true);
        resamplingComboBoxModel = new DefaultComboBoxModel(new Resampling[]{
                Resampling.NEAREST_NEIGHBOUR,
                Resampling.BILINEAR_INTERPOLATION,
                Resampling.CUBIC_CONVOLUTION});
        
        final ValueContainerFactory factory = new ValueContainerFactory(new ValueDescriptorFactory() {
            public ValueDescriptor createValueDescriptor(Field field) {
                return new ValueDescriptor(field.getName(), field.getType());
            }
        });

        valueContainer = factory.createObjectBackedValueContainer(this);
    }

    public ValueContainer getValueContainer() {
        return valueContainer;
    }

    public Product getMasterProduct() {
        return masterProduct;
    }

    public Product getSlaveProduct() {
        return slaveProduct;
    }

    private boolean isCreateNewProductSelected() {
        return createNewProduct;
    }

    public String getTargetProductName() {
        return targetProductSelectorModel.getProductName();
    }

    public File getTargetFile() {
        return targetProductSelectorModel.getFile();
    }

    public String getTargetFilePath() {
        return targetProductSelectorModel.getFilePath();
    }

    public String getTargetFormatName() {
        return targetProductSelectorModel.getFormatName();
    }

    public boolean isSaveToFileSelected() {
        return targetProductSelectorModel.isSaveToFileSelected();
    }

    public boolean isOpenInVisatSelected() {
        return targetProductSelectorModel.isOpenInAppSelected();
    }

    public boolean isRenameMasterComponentsSelected() {
        return renameMasterComponents;
    }

    public boolean isRenameSlaveComponentsSelected() {
        return renameSlaveComponents;
    }

    public String getMasterComponentPattern() {
        return masterComponentPattern;
    }

    public String getSlaveComponentPattern() {
        return slaveComponentPattern;
    }

    public Resampling getResampling() {
        return (Resampling) resamplingComboBoxModel.getSelectedItem();
    }

    public void setMasterProduct(Product product) {
        setValueContainerValue("masterProduct", product);
    }

    public void setSlaveProduct(Product product) {
        setValueContainerValue("slaveProduct", product);
    }

    private void setCreateNewProduct(boolean createNewProduct) {
        setValueContainerValue("createNewProduct", createNewProduct);
    }

    public void setRenameMasterComponents(boolean renameMasterComponents) {
        setValueContainerValue("renameMasterComponents", renameMasterComponents);
    }

    public void setRenameSlaveComponents(boolean renameSlaveComponents) {
        setValueContainerValue("renameSlaveComponents", renameSlaveComponents);
    }

    public void setMasterComponentPattern(String pattern) {
        setValueContainerValue("masterComponentPattern", pattern);
    }

    public void setSlaveComponentPattern(String pattern) {
        setValueContainerValue("slaveComponentPattern", pattern);
    }

    public void setResampling(Resampling resampling) {
        resamplingComboBoxModel.setSelectedItem(resampling);
    }

    TargetProductSelectorModel getTargetProductSelectorModel() {
        return targetProductSelectorModel;
    }

    ComboBoxModel getResamplingComboBoxModel() {
        return resamplingComboBoxModel;
    }

    void adaptResamplingComboBoxModel() {
        if (isValidPixelExpressionUsed()) {
            if (resamplingComboBoxModel.getSize() == 3) {
                resamplingComboBoxModel.removeElement(Resampling.CUBIC_CONVOLUTION);
                resamplingComboBoxModel.removeElement(Resampling.BILINEAR_INTERPOLATION);
                resamplingComboBoxModel.setSelectedItem(Resampling.NEAREST_NEIGHBOUR);
            }
        } else {
            if (resamplingComboBoxModel.getSize() == 1) {
                resamplingComboBoxModel.addElement(Resampling.BILINEAR_INTERPOLATION);
                resamplingComboBoxModel.addElement(Resampling.CUBIC_CONVOLUTION);
            }
        }
    }

    private void setValueContainerValue(String name, Object value) {
        try {
            valueContainer.setValue(name, value);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private boolean isValidPixelExpressionUsed() {
        if (slaveProduct != null) {
            for (final Band band : slaveProduct.getBands()) {
                final String expression = band.getValidPixelExpression();
                if (expression != null && !expression.trim().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
