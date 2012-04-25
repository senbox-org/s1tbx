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
package org.esa.beam.framework.ui.product;

import com.jidesoft.grid.SortableTable;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.BasicView;
import org.esa.beam.framework.ui.PopupMenuHandler;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

/**
 * A view component used to display a product's metadata in tabular form.
 */
public class ProductPlacemarkView extends BasicView implements ProductNodeView {

    private VectorDataNode vectorDataNode;
    private SortableTable placemarkTable;
    private final PlacemarkTableModel tableModel;

    public ProductPlacemarkView(VectorDataNode vectorDataNode) {
        this.vectorDataNode = vectorDataNode;
        this.vectorDataNode.getProduct().addProductNodeListener(new PNL());
        placemarkTable = new SortableTable();
        placemarkTable.addMouseListener(new PopupMenuHandler(this));

        tableModel = new PlacemarkTableModel();
        placemarkTable.setModel(tableModel);

        final TableCellRenderer defaultRenderer = placemarkTable.getTableHeader().getDefaultRenderer();

        Enumeration<TableColumn> columns = placemarkTable.getColumnModel().getColumns();
        while (columns.hasMoreElements()) {
            TableColumn tableColumn = columns.nextElement();
            tableColumn.setHeaderRenderer(defaultRenderer);
            tableColumn.sizeWidthToFit();
            tableColumn.setMaxWidth(Integer.MAX_VALUE);
        }

        // the following 4 lines are needed, therewith the scroll pane displays
        // not only a vertical scroll bar if needed but also a horizontal scroll bar if needed.
        final JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(placemarkTable, BorderLayout.CENTER);
        final JScrollPane scrollPane = new JScrollPane(tablePanel);
        scrollPane.setColumnHeaderView(placemarkTable.getTableHeader());

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }

    public VectorDataNode getVectorDataNode() {
        return vectorDataNode;
    }

    /**
     * Returns the currently visible product node.
     */
    @Override
    public ProductNode getVisibleProductNode() {
        return vectorDataNode;
    }

    @Override
    public JPopupMenu createPopupMenu(Component component) {
        JPopupMenu popupMenu = new JPopupMenu();
        if (getCommandUIFactory() != null) {
            getCommandUIFactory().addContextDependentMenuItems("placemark", popupMenu);
        }
        popupMenu.add(new HelloAction());
        return popupMenu;
    }

    @Override
    public JPopupMenu createPopupMenu(MouseEvent event) {
        return null;
    }

    private void onNodeChange(ProductNodeEvent event) {
        ProductNode sourceNode = event.getSourceNode();
        if (sourceNode == vectorDataNode) {
            updateTable();
        } else if (sourceNode.getOwner() == vectorDataNode.getPlacemarkGroup()) {
            updateTable();
        }
    }

    private class PlacemarkTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return vectorDataNode.getPlacemarkGroup().getNodeCount();
        }

        @Override
        public int getColumnCount() {
            return vectorDataNode.getFeatureType().getAttributeCount();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return vectorDataNode.getFeatureType().getDescriptor(columnIndex).getLocalName();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return vectorDataNode.getFeatureType().getDescriptor(columnIndex).getType().getBinding();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return vectorDataNode.getPlacemarkGroup().get(rowIndex).getFeature().getAttribute(columnIndex);
        }
    }

    private class PNL implements ProductNodeListener {
        @Override
        public void nodeChanged(ProductNodeEvent event) {
            onNodeChange(event);
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            onNodeChange(event);
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            onNodeChange(event);
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            onNodeChange(event);
        }
    }

    private void updateTable() {
        tableModel.fireTableDataChanged();
    }

    private class HelloAction extends AbstractAction {

        public HelloAction() {
            super("Hello");
            putValue(SHORT_DESCRIPTION, "Says hello.");
        }


        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }

}
