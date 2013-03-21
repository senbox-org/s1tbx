/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.GridBagConstraints;


class RoiMaskSelector {
    public final static String PROPERTY_NAME_USE_ROI_MASK = "useRoiMask";
    public final static String PROPERTY_NAME_ROI_MASK = "roiMask";

    final JCheckBox useRoiMaskCheckBox;
    final JComboBox roiMaskComboBox;
    final AbstractButton showMaskManagerButton;

    private final BindingContext bindingContext;
    private final ProductNodeListener productNodeListener;

    private Product product;
    private Enablement useRoiEnablement;
    private Enablement roiMaskEnablement;

    public RoiMaskSelector(BindingContext bindingContext) {
        this(bindingContext, VisatApp.getApp().getCommandManager().getCommand("org.esa.beam.visat.toolviews.mask.MaskManagerToolView.showCmd").createToolBarButton());
    }

    public RoiMaskSelector(BindingContext bindingContext, AbstractButton showMaskManagerButton) {
        final Property useRoiMaskProperty = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_USE_ROI_MASK);
        Assert.argument(useRoiMaskProperty != null, "bindingContext");
        Assert.argument(useRoiMaskProperty.getType().equals(Boolean.class) || useRoiMaskProperty.getType() == Boolean.TYPE, "bindingContext");
        Assert.argument(bindingContext.getPropertySet().getProperty(PROPERTY_NAME_ROI_MASK) != null, "bindingContext");
        Assert.argument(bindingContext.getPropertySet().getProperty(PROPERTY_NAME_ROI_MASK).getType().equals(Mask.class), "bindingContext");

        this.productNodeListener = new PNL();
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
        bindingContext.bind(PROPERTY_NAME_ROI_MASK, roiMaskComboBox);

        bindingContext.bindEnabledState(PROPERTY_NAME_USE_ROI_MASK, true, createUseRoiCondition());
        bindingContext.bindEnabledState(PROPERTY_NAME_ROI_MASK, true, createEnableMaskDropDownCondition());
    }

    public JPanel createPanel() {
        final JPanel roiMaskPanel = GridBagUtils.createPanel();
        GridBagConstraints roiMaskPanelConstraints = GridBagUtils.createConstraints("anchor=SOUTHWEST,fill=HORIZONTAL,insets.top=2");
        GridBagUtils.addToPanel(roiMaskPanel, useRoiMaskCheckBox, roiMaskPanelConstraints,
                                ",gridy=0,gridx=0,weightx=1");
        GridBagUtils.addToPanel(roiMaskPanel, roiMaskComboBox, roiMaskPanelConstraints,
                                "gridy=1,insets.left=4");
        GridBagUtils.addToPanel(roiMaskPanel, showMaskManagerButton, roiMaskPanelConstraints,
                                "gridheight=2,gridy=0,gridx=1,weightx=0,ipadx=5,insets.left=0");
        return roiMaskPanel;
    }

    public void updateMaskSource(Product newProduct) {
        if (product != newProduct) {
            if (product != null) {
                product.removeProductNodeListener(productNodeListener);
            }
            if (newProduct != null) {
                newProduct.addProductNodeListener(productNodeListener);
            }
            this.product = newProduct;
            updateRoiMasks();
        }
    }

    private void updateRoiMasks() {
        final Property property = bindingContext.getPropertySet().getProperty(PROPERTY_NAME_ROI_MASK);
        if (product != null) {
            property.getDescriptor().setValueSet(new ValueSet(product.getMaskGroup().toArray()));
        } else {
            property.getDescriptor().setValueSet(new ValueSet(new Mask[0]));
        }
        useRoiEnablement.apply();
        roiMaskEnablement.apply();
    }

    private Enablement.Condition createUseRoiCondition() {
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

    private Enablement.Condition createEnableMaskDropDownCondition() {
        return new Enablement.Condition() {

            @Override
            public boolean evaluate(BindingContext bindingContext) {
                Boolean propertyValue = bindingContext.getPropertySet().getValue(PROPERTY_NAME_USE_ROI_MASK);
                if (roiMaskComboBox.getItemCount() > 0 && roiMaskComboBox.getSelectedIndex() < 0) {
                    roiMaskComboBox.setSelectedIndex(0);
                }
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

    private class PNL implements ProductNodeListener {

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            handleEvent(event);
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            handleEvent(event);
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            handleEvent(event);
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            handleEvent(event);
        }

        private void handleEvent(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Mask) {
                updateRoiMasks();
            }
        }
    }
}
