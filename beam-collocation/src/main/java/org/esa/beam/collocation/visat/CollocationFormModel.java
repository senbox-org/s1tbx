package org.esa.beam.collocation.visat;

import com.bc.ceres.binding.*;
import org.esa.beam.collocation.CollocateOp;
import org.esa.beam.collocation.ResamplingType;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import java.io.File;
import java.lang.reflect.Field;

/**
 * @author Ralf Quast
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
        this.targetProductSelectorModel = targetProductSelectorModel;
        this.createNewProduct = true;
        this.resamplingComboBoxModel = new DefaultComboBoxModel(ResamplingType.values());
        this.valueContainer = createValueContainer(CollocateOp.Spi.class.getName(), this);
    }

    // todo - this is a generally useful helper method!
    public static ValueContainer createValueContainer(String operatorName, Object object)  {
        ValueContainer vc1 = ValueContainer.createObjectBacked(object);
        ValueContainer vc0 = ParameterDescriptorFactory.createMapBackedOperatorValueContainer(operatorName);
        try {
            vc0.setDefaultValues();
        } catch (ValidationException e) {
            // todo - ok here?
            e.printStackTrace();
        }

        ValueModel[] vma0 = vc0.getModels();
        for (ValueModel vm0 : vma0) {
            ValueModel vm1 = vc1.getModel(vm0.getDescriptor().getName());
            System.out.println("vm0 = " + vm0);
            System.out.println("vm1 = " + vm1);
            if (vm1 != null) {
                try {
                    vm1.setValue(vm0.getValue());
                } catch (ValidationException e) {
                    // todo - ok here?
                    e.printStackTrace();
                }
            }
        }

        return vc1;
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
