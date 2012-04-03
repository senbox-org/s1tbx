package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.visat.VisatApp;
import org.opengis.feature.type.AttributeDescriptor;

import javax.swing.*;
import java.awt.*;

public class RoiMaskSelector {
    public final static String PROPERTY_NAME_USE_ROI_MASK = "useRoiMask";
    public final static String PROPERTY_NAME_SELECTED_ROI_MASK = "selectedRoiMask";

    public final JCheckBox checkBoxUseRoiMask;
    public final JLabel labelRoiMask;
    public final JComboBox comboRoiMask;
    public final AbstractButton buttonMaskManager;

    private final BindingContext bindingContext;

    private Product product;
    private Enablement useRoiEnablement;

    public RoiMaskSelector(BindingContext bindingContext) {
        this.bindingContext = bindingContext;
        checkBoxUseRoiMask = new JCheckBox("Use ROI mask");
        labelRoiMask = new JLabel("ROI mask:");
        comboRoiMask = new JComboBox();
        comboRoiMask.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    this.setText(((Mask) value).getName());
                }
                return this;
            }
        });

        final Command command = VisatApp.getApp().getCommandManager().getCommand("org.esa.beam.visat.toolviews.mask.MaskManagerToolView.showCmd");
        buttonMaskManager = command.createToolBarButton();

        bindingContext.bind(PROPERTY_NAME_USE_ROI_MASK, checkBoxUseRoiMask);
        bindingContext.bind(PROPERTY_NAME_SELECTED_ROI_MASK, comboRoiMask).addComponent(labelRoiMask);

        bindingContext.bindEnabledState(PROPERTY_NAME_USE_ROI_MASK, true, newUseRoiCondition());
        bindingContext.bindEnabledState(PROPERTY_NAME_SELECTED_ROI_MASK, true, PROPERTY_NAME_USE_ROI_MASK, true);
    }

    public void updateMaskSource(Product product) {
        this.product = product;
        if (useRoiEnablement != null) {
            useRoiEnablement.apply();
        }
        if (product != null) {
            final Property property = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_SELECTED_ROI_MASK);
            property.getDescriptor().setValueSet(new ValueSet(product.getMaskGroup().toArray()));
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
        };
    }
}
