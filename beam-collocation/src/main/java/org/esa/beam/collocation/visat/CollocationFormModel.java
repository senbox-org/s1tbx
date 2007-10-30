package org.esa.beam.collocation.visat;

import com.bc.ceres.binding.*;

import org.esa.beam.collocation.ResamplingType;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
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

    public CollocationFormModel(TargetProductSelectorModel targetProductSelectorModel) {
        createNewProduct = true;
        renameMasterComponents = true;
        renameSlaveComponents = true;
        masterComponentPattern = "${ORIGINAL_NAME}";
        slaveComponentPattern = "${ORIGINAL_NAME}";

        this.targetProductSelectorModel = targetProductSelectorModel;
        resamplingComboBoxModel = new DefaultComboBoxModel(ResamplingType.values());

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

    public String getTargetProductName() {
        return targetProductSelectorModel.getProductName();
    }

    public File getTargetFile() {
        return targetProductSelectorModel.getProductFile();
    }

    public String getTargetFilePath() {
        return targetProductSelectorModel.getProductFile().getPath();
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

    public ResamplingType getResamplingType() {
        return (ResamplingType) resamplingComboBoxModel.getSelectedItem();
    }

    public void setMasterProduct(Product product) {
        setValueContainerValue("masterProduct", product);
    }

    public void setSlaveProduct(Product product) {
        setValueContainerValue("slaveProduct", product);
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

    public void setResamplingType(ResamplingType resamplingType) {
        resamplingComboBoxModel.setSelectedItem(resamplingType);
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
                resamplingComboBoxModel.removeElement(ResamplingType.CUBIC_CONVOLUTION);
                resamplingComboBoxModel.removeElement(ResamplingType.BILINEAR_INTERPOLATION);
                resamplingComboBoxModel.setSelectedItem(ResamplingType.NEAREST_NEIGHBOUR);
            }
        } else {
            if (resamplingComboBoxModel.getSize() == 1) {
                resamplingComboBoxModel.addElement(ResamplingType.BILINEAR_INTERPOLATION);
                resamplingComboBoxModel.addElement(ResamplingType.CUBIC_CONVOLUTION);
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
