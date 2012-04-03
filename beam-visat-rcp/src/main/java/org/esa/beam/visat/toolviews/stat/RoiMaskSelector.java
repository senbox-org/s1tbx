package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.*;

public class RoiMaskSelector {
    public final static String PROPERTY_NAME_USE_ROI_MASK = "useRoiMask";
    public final static String PROPERTY_NAME_SELECTED_ROI_MASK = "roiMask";

    final JCheckBox useRoiMaskCheckBox;
    final JComboBox roiMaskComboBox;
    final AbstractButton showMaskManagerButton;

    private final BindingContext bindingContext;

    private Product product;
    private Enablement useRoiEnablement;
    private Enablement roiMaskEnablement;

    public RoiMaskSelector(BindingContext bindingContext, AbstractButton showMaskManagerButton) {
        this.bindingContext = bindingContext;
        useRoiMaskCheckBox = new JCheckBox("Use ROI mask:");
        roiMaskComboBox = new JComboBox();
        roiMaskComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    this.setText(((Mask) value).getName());
                }
                return this;
            }
        });

        this.showMaskManagerButton = showMaskManagerButton;

        bindingContext.bind(PROPERTY_NAME_USE_ROI_MASK, useRoiMaskCheckBox);
        bindingContext.bind(PROPERTY_NAME_SELECTED_ROI_MASK, roiMaskComboBox);

        bindingContext.bindEnabledState(PROPERTY_NAME_USE_ROI_MASK, true, newUseRoiCondition());
        bindingContext.bindEnabledState(PROPERTY_NAME_SELECTED_ROI_MASK, true, newEnableMaskDropDownCondition());
    }

    public JPanel getUI() {
        final JPanel roiMaskPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints roiMaskGbc = new GridBagConstraints();
        roiMaskGbc.anchor = GridBagConstraints.SOUTHWEST;
        roiMaskGbc.fill = GridBagConstraints.HORIZONTAL;
        roiMaskGbc.gridx = 0;
        roiMaskGbc.gridy = 0;
        roiMaskGbc.weightx = 1;
        roiMaskPanel.add(useRoiMaskCheckBox, roiMaskGbc);
        roiMaskGbc.gridy++;
        roiMaskPanel.add(roiMaskComboBox, roiMaskGbc);
        roiMaskGbc.gridheight = 2;
        roiMaskGbc.gridx = 1;
        roiMaskGbc.gridy = 0;
        roiMaskGbc.weightx = 0;
        roiMaskGbc.ipadx = 5;
        roiMaskPanel.add(showMaskManagerButton, roiMaskGbc);
        return roiMaskPanel;
    }

    public void updateMaskSource(Product product) {
        this.product = product;
        if (useRoiEnablement != null) {
            useRoiEnablement.apply();
        }

        if (roiMaskEnablement != null) {
            roiMaskEnablement.apply();
        }

        final Property property = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_SELECTED_ROI_MASK);
        if (product != null) {
            property.getDescriptor().setValueSet(new ValueSet(product.getMaskGroup().toArray()));
        } else {
            property.getDescriptor().setValueSet(new ValueSet(new Mask[0]));
        }
    }

    private Enablement.Condition newUseRoiCondition() {
        return new Enablement.Condition() {
            @Override
            public boolean evaluate(BindingContext bindingContext) {
                return product != null && product.getMaskGroup().getNodeCount() > 0;
            }

            @Override
            public void install(BindingContext bindingContext, Enablement enablement) {
                useRoiEnablement = enablement;
            }

            @Override
            public void uninstall(BindingContext bindingContext, Enablement enablement) {
                useRoiEnablement = null;
            }
        };
    }

    private Enablement.Condition newEnableMaskDropDownCondition() {
        return new Enablement.Condition() {

            @Override
            public boolean evaluate(BindingContext bindingContext) {
                Boolean propertyValue = bindingContext.getPropertySet().getValue(PROPERTY_NAME_USE_ROI_MASK);
                return Boolean.TRUE.equals(propertyValue)
                        && product != null
                        && product.getMaskGroup().getNodeCount() > 0;
            }

            @Override
            public void install(BindingContext bindingContext, Enablement enablement) {
                bindingContext.addPropertyChangeListener(PROPERTY_NAME_USE_ROI_MASK, enablement);
                roiMaskEnablement = enablement;
            }

            @Override
            public void uninstall(BindingContext bindingContext, Enablement enablement) {
                bindingContext.removePropertyChangeListener(PROPERTY_NAME_USE_ROI_MASK, enablement);
                roiMaskEnablement = null;
            }
        };
    }
}
