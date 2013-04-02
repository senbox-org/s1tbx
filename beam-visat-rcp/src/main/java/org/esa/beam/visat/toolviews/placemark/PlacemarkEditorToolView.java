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

import com.bc.ceres.swing.selection.*;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.SimpleFeatureFigure;
import org.esa.beam.framework.ui.product.VectorDataFigureEditor;
import org.esa.beam.visat.VisatApp;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

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

    public static final String ID = PlacemarkEditorToolView.class.getName();

    public static final String NO_SELECTION_TEXT = "<html>No vector data feature selected<br/>Try selecting a geometry in a view.</html>";

    private VisatApp visatApp;

    private JLabel infoLabel;
    private JTable attributeTable;
    private String titleBase;
    private JScrollPane attributeTablePane;
    private final SCL scl;
    private PlacemarkTableModel tableModel;
    private Placemark placemark;

    public PlacemarkEditorToolView() {
        visatApp = VisatApp.getApp();
        scl = new SCL();
    }

    @Override
    public JComponent createControl() {
        titleBase = getTitle();
        infoLabel = new JLabel();
        tableModel = new PlacemarkTableModel();
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
        placemark = null;
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

            if (vectorDataNode != null) {
                placemark = vectorDataNode.getPlacemarkGroup().getPlacemark(feature);
            } else {
                placemark = null;
            }

            if (placemark != null) {
                attributeTablePane.setVisible(true);
                tableModel.setFeature(placemark);
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
                                     feature != null ? String.format("Selected feature <b>%s</b>:",
                                                                     feature.getID()) : "No feature selected.");
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

    private static class PlacemarkTableModel extends AbstractTableModel {
        private Placemark placemark;

        public void setFeature(Placemark newFeature) {
            final Placemark oldFeature = this.placemark;
            if (oldFeature != newFeature) {
                this.placemark = newFeature;
                fireTableDataChanged();
            }
        }

        @Override
        public int getRowCount() {
            return placemark != null ? placemark.getFeature().getFeatureType().getAttributeCount() : 0;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (placemark != null && columnIndex == 2) {
                final AttributeDescriptor attributeDescriptor
                        = placemark.getFeature().getFeatureType().getAttributeDescriptors().get(rowIndex);
                final Class<?> binding = attributeDescriptor.getType().getBinding();
                if (String.class.isAssignableFrom(binding) || Geometry.class.isAssignableFrom(binding)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (placemark != null) {
                final AttributeDescriptor attributeDescriptor
                        = placemark.getFeature().getFeatureType().getAttributeDescriptors().get(rowIndex);
                final Class<?> binding = attributeDescriptor.getType().getBinding();
                if (String.class.isAssignableFrom(binding)) {
                    placemark.setAttributeValue(attributeDescriptor.getLocalName(), aValue);
                } else if (Geometry.class.isAssignableFrom(binding)) {
                    WKTReader reader = new WKTReader();
                    try {
                        final Geometry geometry = reader.read((String) aValue);
                        placemark.setAttributeValue(attributeDescriptor.getLocalName(), geometry);
                    } catch (ParseException e) {
                        // No way to handle this any further   :(
                        e.printStackTrace();
                    }
                }
            }
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
            if (placemark != null) {
                if (columnIndex == 0) {
                    return placemark.getFeature().getFeatureType().getDescriptor(rowIndex).getLocalName();
                } else if (columnIndex == 1) {
                    return placemark.getFeature().getFeatureType().getDescriptor(rowIndex).getType().getBinding().getSimpleName();
                } else {
                    return placemark.getFeature().getAttribute(rowIndex);
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
