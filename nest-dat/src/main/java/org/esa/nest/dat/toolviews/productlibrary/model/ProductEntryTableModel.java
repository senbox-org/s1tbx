/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.toolviews.productlibrary.model;

import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.toolviews.productlibrary.model.dataprovider.DataProvider;
import org.esa.nest.dat.toolviews.productlibrary.model.dataprovider.IDProvider;
import org.esa.nest.dat.toolviews.productlibrary.model.dataprovider.PropertiesProvider;
import org.esa.nest.dat.toolviews.productlibrary.model.dataprovider.QuicklookProvider;
import org.esa.nest.db.ProductEntry;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.util.ArrayList;
import java.util.List;

public class ProductEntryTableModel extends AbstractTableModel {

    private final ProductEntry[] productEntryList;
    final List<DataProvider> dataProviders = new ArrayList<DataProvider>(5);
    private final List<TableColumn> columnList = new ArrayList<TableColumn>();

    public ProductEntryTableModel(final ProductEntry[] productList) {
        this.productEntryList = productList;
        dataProviders.add(new IDProvider());
        dataProviders.add(new PropertiesProvider());
        try {
            dataProviders.add(new QuicklookProvider());
        } catch(Exception e) {
            e.printStackTrace();
            if(VisatApp.getApp() != null) {
                VisatApp.getApp().showErrorDialog(e.getMessage());
            }
        }
        for (final DataProvider provider : dataProviders) {
            final TableColumn tableColumn = provider.getTableColumn();
            tableColumn.setModelIndex(getColumnCount());
            columnList.add(tableColumn);
        }
    }

    public DataProvider getDataProvider(final int columnIndex) {
        if(columnIndex >= 0 && columnIndex < dataProviders.size()) {
            return dataProviders.get(columnIndex);
        }
        return null;
    }

    public TableColumnModel getColumnModel() {
        final TableColumnModel columnModel = new DefaultTableColumnModel();
        for (TableColumn aColumnList : columnList) {
            columnModel.addColumn(aColumnList);
        }
        return columnModel;
    }

    public int getRowCount() {
        return productEntryList != null ? productEntryList.length : 0;
    }

    public int getColumnCount() {
        return columnList.size();
    }

 /*   @Override
    public Class getColumnClass(final int columnIndex) {
        if (repository != null) {
            if (repository.getEntryCount() > 0) {
                final Object data = repository.getEntry(0).getData(columnIndex);
                if (data != null) {
                    return data.getClass();
                }
            }
        }
        return Object.class;
    }   */

    public Object getValueAt(final int rowIndex, final int columnIndex) {
        if (productEntryList != null) {
            final ProductEntry entry = productEntryList[rowIndex];
            if(entry != null)
                return entry;
        }
        return null;
    }

    @Override
    public String getColumnName(final int columnIndex) {
        if (columnIndex >= 0 && columnIndex < columnList.size()) {
            final TableColumn column = columnList.get(columnIndex);
            return column.getHeaderValue().toString();
        }
        return "";
    }

    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        if (columnIndex >= columnList.size()) {
            return false;
        }
        final TableColumn column = columnList.get(columnIndex);
        return column.getCellEditor() != null;
    }

}