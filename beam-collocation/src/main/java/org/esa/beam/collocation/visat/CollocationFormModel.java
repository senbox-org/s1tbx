/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.collocation.visat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import org.esa.beam.collocation.CollocateOp;
import org.esa.beam.collocation.ResamplingType;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import java.io.File;

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

    private final PropertyContainer propertyContainer;

    public CollocationFormModel(TargetProductSelectorModel targetProductSelectorModel) {
        this.targetProductSelectorModel = targetProductSelectorModel;
        this.createNewProduct = true;
        this.resamplingComboBoxModel = new DefaultComboBoxModel(ResamplingType.values());
        this.propertyContainer = createValueContainer(CollocateOp.Spi.class.getName(), this);
    }

    // todo - this is a generally useful helper method!
    public static PropertyContainer createValueContainer(String operatorName, Object object) {
        PropertyContainer vc1 = PropertyContainer.createObjectBacked(object);
        PropertyContainer vc0 = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer(operatorName);
        vc0.setDefaultValues();

        Property[] vma0 = vc0.getProperties();
        for (Property vm0 : vma0) {
            Property vm1 = vc1.getProperty(vm0.getDescriptor().getName());
//            System.out.println("vm0 = " + vm0);
//            System.out.println("vm1 = " + vm1);
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

    public PropertyContainer getValueContainer() {
        return propertyContainer;
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
        propertyContainer.setValue(name, value);
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
