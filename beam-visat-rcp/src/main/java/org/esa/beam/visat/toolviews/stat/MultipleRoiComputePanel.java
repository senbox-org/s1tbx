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

package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * A panel which performs the 'compute' action.
 *
 * @author Marco Zuehlke
 */
class MultipleRoiComputePanel extends JPanel {

    interface ComputeMasks {
        void compute(Mask[] selectedMasks);
    }

    private final ProductNodeListener productNodeListener;

    private final JButton computeButton;
    private final JCheckBox useRoiCheckBox;
    private final JComboBox maskNameComboBox;
    private final JRadioButton iterateButton;
    private final JRadioButton singleButton;

    private RasterDataNode raster;
    private Product product;

    MultipleRoiComputePanel(final ComputeMasks method, final RasterDataNode rasterDataNode) {
        productNodeListener = new PNL();
        final Icon icon = UIUtils.loadImageIcon("icons/Gears20.gif");

        computeButton = new JButton("Compute");     /*I18N*/
        computeButton.setMnemonic('A');
        computeButton.setEnabled(rasterDataNode != null);
        computeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean useRoi = useRoiCheckBox.isSelected();
                Mask[] selectedMasks;
                if (useRoi) {
                    if (iterateButton.isEnabled() && iterateButton.isSelected()) {
                        ProductNodeGroup<Mask> roiMaskGroup = raster.getRoiMaskGroup();
                        selectedMasks = roiMaskGroup.toArray(new Mask[roiMaskGroup.getNodeCount()]);
                    } else {
                        String maskName = (String) maskNameComboBox.getSelectedItem();
                        ProductNodeGroup<Mask> roiMaskGroup = raster.getRoiMaskGroup();
                        Mask mask = roiMaskGroup.get(maskName);
                        selectedMasks = new Mask[] {mask};
                    }
                } else {
                    selectedMasks = new Mask[] {null};
                }
                method.compute(selectedMasks);
            }
        });
        computeButton.setIcon(icon);

        useRoiCheckBox = new JCheckBox("Use ROI-Mask");
        useRoiCheckBox.setMnemonic('R');
        useRoiCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnablement();
            }
        });

        maskNameComboBox = new JComboBox();
        iterateButton = new JRadioButton("Iterate over all");
        singleButton = new JRadioButton();
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(iterateButton);
        buttonGroup.add(singleButton);

        final TableLayout tableLayoutSingle = new TableLayout(2);
        tableLayoutSingle.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayoutSingle.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayoutSingle.setTableWeightX(1.0);

        JPanel singlePanel = new JPanel(tableLayoutSingle);
        singlePanel.add(singleButton);
        singlePanel.add(maskNameComboBox);

        final TableLayout tableLayoutRoi = new TableLayout(1);
        tableLayoutRoi.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayoutRoi.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayoutRoi.setTableWeightX(1.0);

        JPanel roiPanel = new JPanel(tableLayoutRoi);
        roiPanel.add(singlePanel);
        roiPanel.add(iterateButton);

        ActionListener actionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnablement();
            }
        };
        iterateButton.addActionListener(actionListener);
        singleButton.addActionListener(actionListener);

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.SOUTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setCellPadding(2, 0, new Insets(4, 10, 4, 0));
        setLayout(tableLayout);

        add(computeButton);
        add(useRoiCheckBox);
        add(roiPanel);

        setRaster(rasterDataNode);
    }

    void setRaster(final RasterDataNode newRaster) {
        if (this.raster != newRaster) {
            this.raster = newRaster;
            if (newRaster == null) {
                if (product != null) {
                    product.removeProductNodeListener(productNodeListener);
                }
                product = null;
            } else if (product != newRaster.getProduct()) {
                if (product != null) {
                    product.removeProductNodeListener(productNodeListener);
                }
                product = newRaster.getProduct();
                if (product != null) {
                    product.addProductNodeListener(productNodeListener);
                }
            }
            updateMaskListState();
        }
    }

    private void updateMaskListState() {
        boolean hasRaster = (raster != null);
        computeButton.setEnabled(hasRaster);
        int roiCount = 0;
        if (hasRaster) {
            roiCount = raster.getRoiMaskGroup().getNodeCount();
        }
        boolean hasRois = (hasRaster && roiCount > 0);
        useRoiCheckBox.setEnabled(hasRois);
        if (hasRois) {
            String[] nodeNames = raster.getRoiMaskGroup().getNodeNames();
            maskNameComboBox.setModel(new DefaultComboBoxModel(nodeNames));
            maskNameComboBox.setSelectedIndex(0);
        } else {
            maskNameComboBox.setModel(new DefaultComboBoxModel());
            useRoiCheckBox.setSelected(false);
        }
        updateEnablement();
    }

    private void updateEnablement() {
        boolean useRoi = useRoiCheckBox.isSelected() && useRoiCheckBox.isEnabled();

        singleButton.setEnabled(useRoi);
        iterateButton.setEnabled(useRoi);
        if (useRoi && !singleButton.isSelected() && !iterateButton.isSelected()) {
            singleButton.setSelected(true);
        }
        boolean useSingleRoi = singleButton.isSelected();
        maskNameComboBox.setEnabled(useRoi && useSingleRoi);
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
                updateMaskListState();
            }
        }
    }
}
