/*
 * $Id: BitmaskOverlayToolView.java,v 1.1 2007/04/19 10:41:38 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.mask;

import com.jidesoft.combobox.ColorComboBox;
import com.jidesoft.grid.ColorCellEditor;
import com.jidesoft.grid.ColorCellRenderer;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class MaskManagerForm {

    private ProductSceneView sceneView;

    private final JTable maskTable;

    private final MaskTableModel maskTableModel;

    private final AbstractButton newButton;
    private final AbstractButton copyButton;
    private final AbstractButton editButton;
    private final AbstractButton removeButton;
    private final AbstractButton importButton;
    private final AbstractButton exportButton;
    private final AbstractButton moveUpButton;
    private final AbstractButton moveDownButton;
    private final AbstractButton helpButton;

    public MaskManagerForm() {

        maskTableModel = new MaskTableModel();

        maskTable = new JTable(maskTableModel);
        maskTable.setName("maskTable");

        maskTable.setPreferredScrollableViewportSize(new Dimension(200, 150));
        maskTable.setDefaultRenderer(Color.class, new ColorCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                ColorComboBox comboBox = (ColorComboBox) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                return configureColorComboBox(comboBox);
            }
        });
        ColorCellEditor cellEditor = new ColorCellEditor() {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                ColorComboBox comboBox = (ColorComboBox) super.getTableCellEditorComponent(table, value, isSelected, row, column);
                return configureColorComboBox(comboBox);
            }
        };

        maskTable.setDefaultEditor(Color.class, cellEditor);
        maskTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        maskTable.getTableHeader().setReorderingAllowed(false);
        maskTable.getTableHeader().setResizingAllowed(true);
        maskTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableToolTipHandler toolTipSetter = new TableToolTipHandler(maskTable);
        maskTable.addMouseListener(toolTipSetter);
        maskTable.addMouseMotionListener(toolTipSetter);
        maskTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                updateState();
                if (e.getClickCount() == 2) {
                    // todo
                }
            }
        });
        maskTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateState();
            }
        });
        maskTableModel.configureColumnModel(maskTable.getColumnModel());

        newButton = createButton("icons/New24.gif");
        newButton.setName("newButton");
        newButton.setToolTipText("Create new mask."); /*I18N*/
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                // todo
            }
        });

        copyButton = createButton("icons/Copy24.gif");
        copyButton.setName("copyButton");
        copyButton.setToolTipText("Copy the selected mask."); /*I18N*/
        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                // todo
            }
        });

        editButton = createButton("icons/Edit24.gif");
        editButton.setName("editButton");
        editButton.setToolTipText("Edit the selected mask."); /*I18N*/
        editButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                // todo
            }
        });

        removeButton = createButton("icons/Remove24.gif");
        removeButton.setName("removeButton");
        removeButton.setToolTipText("Remove the selected masks."); /*I18N*/
        removeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                // todo
            }
        });

        importButton = createButton("icons/Import24.gif");
        importButton.setName("importButton");
        importButton.setToolTipText("Import masks from file."); /*I18N*/
        importButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                // todo
            }
        });

        exportButton = createButton("icons/Export24.gif");
        exportButton.setName("exportButton");
        exportButton.setToolTipText("Export masks to file."); /*I18N*/
        exportButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                // todo
            }
        });

        moveUpButton = createButton("icons/Up24.gif");
        moveUpButton.setName("moveUpButton");
        moveUpButton.setToolTipText("Moves up the selected mask."); /*I18N*/
        moveUpButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                // todo
            }
        });

        moveDownButton = createButton("icons/Down24.gif");
        moveDownButton.setName("moveDownButton");
        moveDownButton.setToolTipText("Moves down the selected mask."); /*I18N*/
        moveDownButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                // todo
            }
        });

        helpButton = createButton("icons/Help24.gif");
        helpButton.setName("helpButton");
    }

    private Component configureColorComboBox(ColorComboBox comboBox) {
        comboBox.setColorValueVisible(false);
        comboBox.setUseAlphaColorButtons(false);
        comboBox.setAllowDefaultColor(false);
        comboBox.setAllowMoreColors(true);
        return comboBox;
    }

    AbstractButton getHelpButton() {
        return helpButton;
    }

    ProductSceneView getSceneView() {
        return sceneView;
    }

    void setSceneView(final ProductSceneView sceneView) {
        if (this.sceneView != sceneView) {
            this.sceneView = sceneView;
            if (this.sceneView != null) {
                ProductNodeGroup<Mask> maskGroup = this.sceneView.getProduct().getMaskGroup();
                RasterDataNode visibleBand = this.sceneView.getRaster();
                reconfigureMasks(maskGroup, visibleBand);
            } else {
                clearMasks();
            }
        }
    }

    void reconfigureMasks(ProductNodeGroup<Mask> maskGroup, RasterDataNode visibleBand) {
        maskTableModel.reconfigure(maskGroup, visibleBand);
        maskTableModel.configureColumnModel(maskTable.getColumnModel());
    }

    void clearMasks() {
        maskTableModel.clear();
        maskTableModel.configureColumnModel(maskTable.getColumnModel());
    }

    void updateState() {

        final int selectedRowCount = maskTable.getSelectedRowCount();
        final boolean maskSinkAvailable = maskTableModel.getMaskGroup() != null;
        final boolean masksSelected = selectedRowCount > 0;
        final boolean singleMaskSelected = selectedRowCount == 1;

        final int rowCount = maskTable.getRowCount();
        final int selectedRow = maskTable.getSelectedRow();

        newButton.setEnabled(maskSinkAvailable);
        copyButton.setEnabled(singleMaskSelected);
        editButton.setEnabled(singleMaskSelected);
        removeButton.setEnabled(masksSelected);
        importButton.setEnabled(maskSinkAvailable);
        exportButton.setEnabled(masksSelected);
        moveUpButton.setEnabled(singleMaskSelected && selectedRow > 0);
        moveDownButton.setEnabled(singleMaskSelected && selectedRow < rowCount - 1);
    }

    static AbstractButton createButton(final String path) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), false);
    }

    JPanel createContentPanel() {

        JPanel buttonPanel = GridBagUtils.createPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.5;

        gbc.insets.bottom = 0;
        gbc.gridwidth = 1;
        buttonPanel.add(newButton, gbc);
        buttonPanel.add(copyButton, gbc);
        gbc.gridy++;
        buttonPanel.add(editButton, gbc);
        buttonPanel.add(removeButton, gbc);
        gbc.gridy++;
        buttonPanel.add(importButton, gbc);
        buttonPanel.add(exportButton, gbc);
        gbc.gridy++;
        buttonPanel.add(moveUpButton, gbc);
        buttonPanel.add(moveDownButton, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        buttonPanel.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.gridwidth = 1;
        buttonPanel.add(helpButton, gbc);

        JPanel tablePanel = new JPanel(new BorderLayout(4, 4));
        tablePanel.add(new JScrollPane(maskTable), BorderLayout.CENTER);

        JPanel contentPane1 = new JPanel(new BorderLayout(4, 4));
        contentPane1.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        contentPane1.add(BorderLayout.CENTER, tablePanel);
        contentPane1.add(BorderLayout.EAST, buttonPanel);

        updateState();

        return contentPane1;
    }
}