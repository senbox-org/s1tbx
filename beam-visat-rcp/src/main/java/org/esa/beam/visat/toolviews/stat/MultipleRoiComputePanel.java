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

import com.jidesoft.list.QuickListFilterField;
import com.jidesoft.swing.CheckBoxList;
import com.jidesoft.swing.SearchableUtils;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Position;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Enumeration;


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

    private final AbstractButton refreshButton;
    private final JCheckBox useRoiCheckBox;
    private final CheckBoxList maskNameList;
    private final JCheckBox selectAllCheckBox;
    private final JCheckBox selectNoneCheckBox;

    private RasterDataNode raster;
    private Product product;

    MultipleRoiComputePanel(final ComputeMasks method, final RasterDataNode rasterDataNode) {
        productNodeListener = new PNL();

        DefaultListModel maskNameListModel = new DefaultListModel();

        maskNameSearchField = new QuickListFilterField(maskNameListModel);
        maskNameSearchField.setHintText("Filter masks here");
        maskNameSearchField.setPreferredSize(new Dimension(150, 21));

        maskNameList = new CheckBoxList(maskNameSearchField.getDisplayListModel()) {
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
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateEnablement();
//                refreshButton.setEnabled(true);
                if (!e.getValueIsAdjusting()) {
                    selectAndEnableCheckBoxes();
                }
            }
        });

        useRoiCheckBox = new JCheckBox("Use ROI mask(s):");
        useRoiCheckBox.setMnemonic('R');
        useRoiCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnablement();
            }
        });

        final JPanel topPanel = new JPanel(new BorderLayout());
        refreshButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/ViewRefresh22.png"),
                false);
        refreshButton.setEnabled(rasterDataNode != null);
        refreshButton.setToolTipText("Refresh View");
        refreshButton.setName("refreshButton");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean useRoi = useRoiCheckBox.isSelected();
                Mask[] selectedMasks;
                if (useRoi) {
                    int[] listIndexes = maskNameList.getCheckBoxListSelectedIndices();
                    if (listIndexes.length > 0) {
                        selectedMasks = new Mask[listIndexes.length];
                        for (int i = 0; i < listIndexes.length; i++) {
                            int listIndex = listIndexes[i];
                            String maskName = maskNameList.getModel().getElementAt(listIndex).toString();
                            selectedMasks[i] = raster.getProduct().getMaskGroup().get(maskName);
                        }
                    } else {
                        selectedMasks = new Mask[]{null};
                    }
                } else {
                    selectedMasks = new Mask[]{null};
                }
                method.compute(selectedMasks);
                refreshButton.setEnabled(false);
            }
        });
        topPanel.add(refreshButton, BorderLayout.WEST);

        AbstractButton showMaskManagerButton = VisatApp.getApp().getCommandManager().getCommand("org.esa.beam.visat.toolviews.mask.MaskManagerToolView.showCmd").createToolBarButton();

        selectAllCheckBox = new JCheckBox("Select all");
        selectAllCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (selectAllCheckBox.isSelected()) {
                    maskNameList.selectAll();
                }
                selectAndEnableCheckBoxes();
            }
        });
        selectNoneCheckBox = new JCheckBox("Select none");
        selectNoneCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (selectNoneCheckBox.isSelected()) {
                    maskNameList.selectNone();
                }
                selectAndEnableCheckBoxes();
            }
        });
        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.add(selectAllCheckBox);
        checkBoxPanel.add(selectNoneCheckBox);

        final JPanel multiRoiComputePanel = GridBagUtils.createPanel();
        GridBagConstraints multiRoiComputePanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,weightx=1");
        GridBagUtils.addToPanel(multiRoiComputePanel, topPanel, multiRoiComputePanelConstraints, "gridx=0,gridy=0,gridwidth=3");
        GridBagUtils.addToPanel(multiRoiComputePanel, new JSeparator(), multiRoiComputePanelConstraints, "gridy=1,fill=HORIZONTAL");
        GridBagUtils.addToPanel(multiRoiComputePanel, useRoiCheckBox, multiRoiComputePanelConstraints, "gridy=2");
        GridBagUtils.addToPanel(multiRoiComputePanel, maskNameSearchField, multiRoiComputePanelConstraints, "gridy=3,weightx=0,gridwidth=2");
        GridBagUtils.addToPanel(multiRoiComputePanel, showMaskManagerButton, multiRoiComputePanelConstraints, "gridy=3,gridx=2,weightx=0,gridwidth=1");
        GridBagUtils.addToPanel(multiRoiComputePanel, new JScrollPane(maskNameList), multiRoiComputePanelConstraints, "gridy=4,gridx=0,fill=HORIZONTAL,gridwidth=3");
        GridBagUtils.addToPanel(multiRoiComputePanel, checkBoxPanel, multiRoiComputePanelConstraints, "gridy=5,weighty=1,gridwidth=3");
        add(multiRoiComputePanel);

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
            refreshButton.setEnabled(raster != null);
        }
    }

    private void selectAndEnableCheckBoxes() {
        final int numEntries = maskNameList.getModel().getSize();
        final int numSelected = maskNameList.getCheckBoxListSelectedIndices().length;
        selectNoneCheckBox.setEnabled(numSelected > 0);
        selectAllCheckBox.setEnabled(numSelected < numEntries);
        selectNoneCheckBox.setSelected(numSelected == 0);
        selectAllCheckBox.setSelected(numSelected == numEntries);
    }

    private String[] getSelectedMaskNames() {
        final Object[] selectedValues = maskNameList.getCheckBoxListSelectedValues();
        return StringUtils.toStringArray(selectedValues);
    }

    private void updateMaskListState() {

        final DefaultListModel maskNameListModel = new DefaultListModel();
        final String[] currentSelectedMaskNames = getSelectedMaskNames();

        if (product != null) {
            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
            final Mask[] masks = maskGroup.toArray(new Mask[maskGroup.getNodeCount()]);
            for (Mask mask : masks) {
                maskNameListModel.addElement(mask.getName());
            }
        }

        try {
            maskNameSearchField.setListModel(maskNameListModel);
            if (product != null) {
                maskNameList.setModel(maskNameSearchField.getDisplayListModel());
            }
        } catch (Throwable e) {

            /*
            We catch everything here, because there seems to be a bug in the combination of
            JIDE QuickListFilterField and FilteredCheckBoxList:

             java.lang.IndexOutOfBoundsException: bitIndex < 0: -1
             	at java.util.BitSet.get(BitSet.java:441)
             	at javax.swing.DefaultListSelectionModel.clear(DefaultListSelectionModel.java:257)
             	at javax.swing.DefaultListSelectionModel.setState(DefaultListSelectionModel.java:567)
             	at javax.swing.DefaultListSelectionModel.removeIndexInterval(DefaultListSelectionModel.java:635)
             	at com.jidesoft.list.CheckBoxListSelectionModelWithWrapper.removeIndexInterval(Unknown Source)
/             */
            Debug.trace(e);
        }

        final String[] allNames = StringUtils.toStringArray(maskNameListModel.toArray());
        for (int i = 0; i < allNames.length; i++) {
            String name = allNames[i];
            if (StringUtils.contains(currentSelectedMaskNames, name)) {
                maskNameList.getCheckBoxListSelectionModel().addSelectionInterval(i,i);
            }
        }

        updateEnablement();
    }

    void updateEnablement() {
        boolean hasMasks = (product != null && product.getMaskGroup().getNodeCount() > 0);
        boolean canSelectMasks = hasMasks && useRoiCheckBox.isSelected();
        useRoiCheckBox.setEnabled(hasMasks);
        maskNameSearchField.setEnabled(canSelectMasks);
        maskNameList.setEnabled(canSelectMasks);
        selectAllCheckBox.setEnabled(canSelectMasks && maskNameList.getCheckBoxListSelectedIndices().length < maskNameList.getModel().getSize());
        selectNoneCheckBox.setEnabled(canSelectMasks && maskNameList.getCheckBoxListSelectedIndices().length > 0);
        refreshButton.setEnabled(raster != null);
    }

    private class PNL implements ProductNodeListener {

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            handleEvent(event);
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
//            handleEvent(event);
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            if (!useRoiCheckBox.isSelected()) {
                return;
            }
            final ProductNode sourceNode = event.getSourceNode();
            if (!(sourceNode instanceof Mask)) {
                return;
            }
            final String maskName = ((Mask) sourceNode).getName();
            final String[] selectedNames = getSelectedMaskNames();

            if (StringUtils.contains(selectedNames, maskName)) {
                updateEnablement();
            }
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
