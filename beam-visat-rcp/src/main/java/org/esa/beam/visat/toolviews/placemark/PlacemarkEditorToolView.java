/*
* Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.placemark;

import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.SelectionManager;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.SimpleFeatureFigure;
import org.esa.beam.framework.ui.product.VectorDataFigureEditor;
import org.esa.beam.visat.VisatApp;
import org.opengis.feature.simple.SimpleFeature;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;

/**
 * A dialog used to manage the list of pins or ground control points associated
 * with a selected product.
 */
public class PlacemarkEditorToolView extends AbstractToolView {

    private VisatApp visatApp;

    private JLabel infoLabel;
    private JTable attributeTable;
    private String titleBase;
    private JScrollPane attributeTablePane;
    private final SCL scl;

    public PlacemarkEditorToolView() {
        visatApp = VisatApp.getApp();
        scl = new SCL();
    }

    @Override
    public JComponent createControl() {
        titleBase = getTitle();
        infoLabel = new JLabel();
        attributeTable = new JTable();
        attributeTablePane = new JScrollPane(attributeTable);
        attributeTablePane.setVisible(false);

        final JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(infoLabel, BorderLayout.NORTH);
        panel.add(attributeTablePane, BorderLayout.CENTER);

        return panel;
    }

    @Override
    public void componentOpened() {
        SelectionManager selectionManager = visatApp.getApplicationPage().getSelectionManager();
        selectionManager.addSelectionChangeListener(scl);
        handleSelectionChange(selectionManager.getSelectionContext(),
                              selectionManager.getSelection());
    }

    @Override
    public void componentHidden() {
        final SelectionManager selectionManager = visatApp.getApplicationPage().getSelectionManager();
        selectionManager.removeSelectionChangeListener(scl);
    }

    private void handleSelectionChange(SelectionContext selectionContext, Selection selection) {

        if (selectionContext == null) {
            return;
        }

        if (selection != null) {
            VectorDataNode vectorDataNode = null;
            if (selectionContext instanceof VectorDataFigureEditor) {
                VectorDataFigureEditor editor = (VectorDataFigureEditor) selectionContext;
                vectorDataNode = editor.getVectorDataNode();
            }
            if (selection instanceof VectorDataFigureEditor) {
            }
            System.out.println("selectionContext = " + selectionContext);

            final Object selectedValue = selection.getSelectedValue();
            System.out.println("selection.selectedValue = " + selectedValue);


            if (vectorDataNode != null) {
                setTitle(titleBase + " - " + vectorDataNode.getName());
            } else {
                setTitle(titleBase);
            }

            if (vectorDataNode != null) {
                infoLabel.setText(String.format("<html>" +
                                                        "Vector data node <b>%s</b><br>" +
                                                        "Placemark descriptor <b>%s</b><br>" +
                                                        "Feature type <b>%s</b><br>" +
                                                        "<br>" +
                                                        "%d placemark(s)<br>" +
                                                        "%d feature(s)<br></html>",
                                                vectorDataNode.getName(),
                                                vectorDataNode.getPlacemarkDescriptor().getClass(),
                                                vectorDataNode.getFeatureType().getTypeName(),
                                                vectorDataNode.getPlacemarkGroup().getNodeCount(),
                                                vectorDataNode.getFeatureCollection().size()));
            } else {
                // ?
                infoLabel.setText(String.format("<html>" +
                                                        "SelectionContext: <b>%s</b><br>" +
                                                        "selectedValue: <b>%s</b><br>",
                                                selectionContext,
                                                selectedValue
                ));
            }

            if (selectedValue instanceof SimpleFeatureFigure) {
                SimpleFeatureFigure figure = (SimpleFeatureFigure) selectedValue;
                attributeTablePane.setVisible(true);
                attributeTable.setModel(new FeatureTableModel(figure.getSimpleFeature()));
            } else {
                attributeTablePane.setVisible(false);
            }
        } else {
            infoLabel.setText("No selection.");
            setTitle(titleBase);
        }

    }

    private static class FeatureTableModel extends AbstractTableModel {
        private final SimpleFeature feature;

        public FeatureTableModel(SimpleFeature feature) {
            this.feature = feature;
        }

        @Override
        public int getRowCount() {
            return feature.getFeatureType().getAttributeCount();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return "Name";
            } else if (columnIndex == 1) {
                return "Type";
            } else {
                return "Value";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return feature.getFeatureType().getDescriptor(rowIndex).getLocalName();
            } else if (columnIndex == 1) {
                return feature.getFeatureType().getDescriptor(rowIndex).getType().getBinding().getSimpleName();
            } else {
                return feature.getAttribute(rowIndex);
            }
        }
    }

    private class SCL extends AbstractSelectionChangeListener {
        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            handleSelectionChange(event.getSelectionContext(), event.getSelection());
        }
    }

}
