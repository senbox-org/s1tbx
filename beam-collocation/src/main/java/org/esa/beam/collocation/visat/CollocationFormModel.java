package org.esa.beam.collocation.visat;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueContainerFactory;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueDescriptorFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.ui.io.TargetProductSelectorModel;

import java.lang.reflect.Field;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class CollocationFormModel {

    private TargetProductSelectorModel targetProductSelectorModel;

    private Product masterProduct;
    private Product slaveProduct;

    private boolean createNewProduct;
    private boolean renameMasterComponents;
    private boolean renameSlaveComponents;

    private String masterComponentPattern;
    private String slaveComponentPattern;

    private Resampling resampling;

    private final ValueContainer valueContainer;

    public CollocationFormModel() {
        targetProductSelectorModel = new TargetProductSelectorModel(true);

        createNewProduct = true;
        renameMasterComponents = true;
        renameSlaveComponents = true;
        masterComponentPattern = "${ORIGINAL_NAME}";
        slaveComponentPattern = "${ORIGINAL_NAME}";
        resampling = Resampling.BILINEAR_INTERPOLATION;

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

    public boolean isCreateNewProductSelected() {
        return createNewProduct;
    }

    public void setCreateNewProduct(boolean createNewProduct) {
        setValueContainerValue("createNewProduct", createNewProduct);
    }

    public String getTargetProductName() {
        return targetProductSelectorModel.getProductName();
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
        return targetProductSelectorModel.isOpenInVisatSelected();
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
        return resampling;
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

    public void setResampling(Resampling resampling) {
        setValueContainerValue("resampling", resampling);
    }

    TargetProductSelectorModel getTargetProductSelectorModel() {
        return targetProductSelectorModel;
    }

    Resampling[] getResamplings() {
        return new Resampling[]{
                Resampling.BILINEAR_INTERPOLATION,
                Resampling.CUBIC_CONVOLUTION,
                Resampling.NEAREST_NEIGHBOUR,
        };
    }

    private void setValueContainerValue(String name, Object value) {
        try {
            valueContainer.setValue(name, value);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
