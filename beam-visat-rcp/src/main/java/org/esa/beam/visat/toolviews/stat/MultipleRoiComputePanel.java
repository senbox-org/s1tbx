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
import com.jidesoft.list.FilterableCheckBoxList;
import com.jidesoft.list.QuickListFilterField;
import com.jidesoft.swing.SearchableUtils;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.util.Debug;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Position;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;


/**
 * A panel which performs the 'compute' action.
 *
 * @author Marco Zuehlke
 */
class MultipleRoiComputePanel extends JPanel {


    private final QuickListFilterField maskNameSearchField;

    interface ComputeMasks {
        void compute(Mask[] selectedMasks);
    }

    private final ProductNodeListener productNodeListener;

    private final JButton computeButton;
    private final JCheckBox useRoiCheckBox;
    private final FilterableCheckBoxList maskNameList;

    private RasterDataNode raster;
    private Product product;

    MultipleRoiComputePanel(final ComputeMasks method, final RasterDataNode rasterDataNode) {
        productNodeListener = new PNL();
        final Icon icon = UIUtils.loadImageIcon("icons/ViewRefresh16.png");

        DefaultListModel maskNameListModel = new DefaultListModel();

        maskNameSearchField = new QuickListFilterField(maskNameListModel);
        maskNameSearchField.setHintText("Filter masks here");
        //quickSearchPanel.setBorder(new JideTitledBorder(new PartialEtchedBorder(PartialEtchedBorder.LOWERED, PartialSide.NORTH), "QuickListFilterField", JideTitledBorder.LEADING, JideTitledBorder.ABOVE_TOP));

        maskNameList = new FilterableCheckBoxList(maskNameSearchField.getDisplayListModel()) {
            @Override
            public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
                return -1;
            }

            @Override
            public boolean isCheckBoxEnabled(int index) {
                return true;
            }
        };
        SearchableUtils.installSearchable(maskNameList);

        maskNameList.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        maskNameList.getCheckBoxListSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {

                if (!e.getValueIsAdjusting()) {
                    int[] indices = maskNameList.getCheckBoxListSelectedIndices();
                    System.out.println("indices = " + Arrays.toString(indices));
                }
            }
        });

        computeButton = new JButton("Compute");     /*I18N*/
        computeButton.setMnemonic('C');
        computeButton.setEnabled(rasterDataNode != null);
        computeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean useRoi = useRoiCheckBox.isSelected();
                Mask[] selectedMasks;
                if (useRoi) {
                    int[] listIndexes = maskNameList.getCheckBoxListSelectedIndices();
                    selectedMasks = new Mask[listIndexes.length];
                    for (int i = 0; i < listIndexes.length; i++) {
                        int listIndex = listIndexes[i];
                        String maskName = maskNameList.getModel().getElementAt(listIndex).toString();
                        selectedMasks[i] = raster.getProduct().getMaskGroup().get(maskName);
                    }
                } else {
                    selectedMasks = new Mask[]{null};
                }
                method.compute(selectedMasks);
            }
        });
        computeButton.setIcon(icon);

        useRoiCheckBox = new JCheckBox("Use ROI mask(s):");
        useRoiCheckBox.setMnemonic('R');
        useRoiCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnablement();
            }
        });

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.SOUTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTablePadding(new Insets(2, 2, 2, 2));
        setLayout(tableLayout);

        add(computeButton);
        add(useRoiCheckBox);
        add(maskNameSearchField);
        add(new JScrollPane(maskNameList));

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
                updateMaskListState();
            } else if (product != newRaster.getProduct()) {
                if (product != null) {
                    product.removeProductNodeListener(productNodeListener);
                }
                product = newRaster.getProduct();
                if (product != null) {
                    product.addProductNodeListener(productNodeListener);
                }
                updateMaskListState();
            }
        }
    }

    private void updateMaskListState() {

        DefaultListModel maskNameListModel = new DefaultListModel();

        if (product != null) {
            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
            Mask[] masks = maskGroup.toArray(new Mask[0]);
            for (Mask mask : masks) {
                maskNameListModel.addElement(mask.getName());
            }
        }

        try {
            maskNameSearchField.setListModel(maskNameListModel);
            maskNameList.setModel(maskNameSearchField.getDisplayListModel());
        } catch (Throwable e) {
            Debug.trace(e);
        }

        updateEnablement();
    }

    private void updateEnablement() {
        boolean hasRaster = (raster != null);
        boolean hasMasks = (product != null && product.getMaskGroup().getNodeCount() > 0);
        boolean canSelectMasks = hasMasks && useRoiCheckBox.isSelected();

        computeButton.setEnabled(hasRaster);
        useRoiCheckBox.setEnabled(hasMasks);
        maskNameSearchField.setEnabled(canSelectMasks);
        maskNameList.setEnabled(canSelectMasks);
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
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateMaskListState();
                    }
                });
            }
        }
    }
}
