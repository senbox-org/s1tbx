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
 * A tool windows that lets users edit the attribute values of selected vector data features.
 * <p/>
 * <i>Note: the editor functionality is not implemented yet. Instead it is used as a tool windows that
 * displays the attributes of selected vector data features.
 * </i>
 * <p/>
 * <i>Implementation idea: Wrap the entire feature attribute set in a {@link com.bc.ceres.binding.PropertySet}
 * so that we have {@link com.bc.ceres.swing.binding.BindingContext} and can then create a
 * {@link com.bc.ceres.swing.binding.PropertyPane} for editing (or use the JIDE Property Pane). Furthermore, we
 * will have to make better use of the {@link com.bc.ceres.core.Extensible} interface, since {@link Selection} already
 * implements it. But currently the are no factories registered that will return a SimpleFeature or VectorDataNode
 * instances for a given {@link Selection} object. (nf - 2012-04-25)</i>
 *
 * @author Norman
 */
public class PlacemarkEditorToolView extends AbstractToolView {

    public static final String NO_SELECTION_TEXT = "<html>No vector data feature selected<br/>Try selecting a geometry in a view.</html>";

    private VisatApp visatApp;

    private JLabel infoLabel;
    private JTable attributeTable;
    private String titleBase;
    private JScrollPane attributeTablePane;
    private final SCL scl;
    private FeatureTableModel tableModel;

    public PlacemarkEditorToolView() {
        visatApp = VisatApp.getApp();
        scl = new SCL();
    }

    @Override
    public JComponent createControl() {
        titleBase = getTitle();
        infoLabel = new JLabel();
        tableModel = new FeatureTableModel();
        attributeTable = new JTable(tableModel);
        attributeTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        attributeTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        attributeTable.getColumnModel().getColumn(2).setPreferredWidth(320);
        attributeTablePane = new JScrollPane(attributeTable);
        attributeTablePane.setVisible(false);

        final JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(infoLabel, BorderLayout.NORTH);
        panel.add(attributeTablePane, BorderLayout.CENTER);

        return panel;
    }

    @Override
    public void componentShown() {
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
            final VectorDataNode vectorDataNode;
            if (selectionContext instanceof VectorDataFigureEditor) {
                vectorDataNode = ((VectorDataFigureEditor) selectionContext).getVectorDataNode();
            } else {
                vectorDataNode = null;
            }

            final Object selectedValue = selection.getSelectedValue();
            final SimpleFeature feature;
            if (selectedValue instanceof SimpleFeatureFigure) {
                SimpleFeatureFigure figure = (SimpleFeatureFigure) selectedValue;
                feature = figure.getSimpleFeature();
            } else {
                feature = null;
            }

            setTitle(getWindowTitle(vectorDataNode, feature));
            infoLabel.setText(getInfoText(vectorDataNode, feature));

            if (feature != null) {
                attributeTablePane.setVisible(true);
                tableModel.setFeature(feature);
            } else {
                attributeTablePane.setVisible(false);
            }
        } else {
            setTitle(getWindowTitle(null, null));
            infoLabel.setText(getInfoText(null, null));
        }

    }

    private String getInfoText(VectorDataNode vectorDataNode, SimpleFeature feature) {
        final String infoText;
        if (vectorDataNode != null) {
            infoText = String.format("<html>" +
                                             "Vector data node: <b>%s</b> with %d feature(s)<br>" +
                                             "Feature type: <b>%s (%s)</b><br>" +
                                             "Geometry attribute: <b>%s</b><br>" +
                                             "<br>" +
                                             "%s<br>" +
                                             "</html>",
                                     vectorDataNode.getName(),
                                     vectorDataNode.getFeatureCollection().size(),
                                     vectorDataNode.getFeatureType().getTypeName(),
                                     vectorDataNode.getPlacemarkDescriptor().getClass().getSimpleName(),
                                     vectorDataNode.getFeatureType().getGeometryDescriptor().getLocalName(),
                                     feature != null ? String.format("Selected feature <b>%s</b>:", feature.getID()) : "No feature selected.");
            infoLabel.setText(infoText);
        } else {
            infoText = NO_SELECTION_TEXT;
            // infoText = getSelectionDebugText(selectionContext, selectedValue);
        }
        return infoText;
    }

    private String getSelectionDebugText(SelectionContext selectionContext, Object selectedValue) {
        return String.format("<html>"
                                     + "SelectionContext: <b>%s</b><br>"
                                     + "SelectionContext.class: <b>%s</b><br>"
                                     + "selectedValue: <b>%s</b><br>"
                                     + "selectedValue.class: <b>%s</b><br>",
                             selectionContext,
                             selectionContext.getClass(),
                             selectedValue,
                             selectedValue != null ? selectedValue.getClass() : "-"
        );
    }

    private String getWindowTitle(VectorDataNode vectorDataNode, SimpleFeature feature) {
        final String titleText;
        if (vectorDataNode != null) {
            if (feature != null) {
                titleText = titleBase + " - " + vectorDataNode.getName() + " - " + feature.getID();
            } else {
                titleText = titleBase + " - " + vectorDataNode.getName();
            }
        } else {
            titleText = titleBase;
        }
        return titleText;
    }

    private static class FeatureTableModel extends AbstractTableModel {
        private SimpleFeature feature;

        public void setFeature(SimpleFeature newFeature) {
            final SimpleFeature oldFeature = this.feature;
            if (oldFeature != newFeature) {
                this.feature = newFeature;
                fireTableDataChanged();
            }
        }

        @Override
        public int getRowCount() {
            return feature != null ? feature.getFeatureType().getAttributeCount() : 0;
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
            if (feature != null) {
                if (columnIndex == 0) {
                    return feature.getFeatureType().getDescriptor(rowIndex).getLocalName();
                } else if (columnIndex == 1) {
                    return feature.getFeatureType().getDescriptor(rowIndex).getType().getBinding().getSimpleName();
                } else {
                    return feature.getAttribute(rowIndex);
                }
            }
            return null;
        }
    }

    private class SCL extends AbstractSelectionChangeListener {
        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            handleSelectionChange(event.getSelectionContext(), event.getSelection());
        }
    }

}
