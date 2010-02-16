package org.esa.beam.visat.actions.pgrab.ui;

import org.esa.beam.util.Guardian;
import org.esa.beam.visat.actions.pgrab.model.Repository;
import org.esa.beam.visat.actions.pgrab.model.RepositoryEntry;
import org.esa.beam.visat.actions.pgrab.model.dataprovider.DataProvider;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.util.ArrayList;

public class RepositoryTableModel extends AbstractTableModel {

    private Repository repository;
    private ArrayList<TableColumn> columnList;

    public RepositoryTableModel(final Repository repository) {
        Guardian.assertNotNull("repository", repository);
        this.repository = repository;
        this.repository.addListener(new RepositoryHandler());
        columnList = new ArrayList<TableColumn>();
        final DataProvider[] dataProviders = repository.getDataProviders();
        for (int i = 0; i < dataProviders.length; i++) {
            final DataProvider provider = dataProviders[i];
            final TableColumn tableColumn = provider.getTableColumn();
            tableColumn.setModelIndex(getColumnCount());
            columnList.add(tableColumn);
        }
    }

    public TableColumnModel getColumnModel() {
        final TableColumnModel columnModel = new DefaultTableColumnModel();
        for (int i = 0; i < columnList.size(); i++) {
            columnModel.addColumn(columnList.get(i));
        }
        return columnModel;
    }

    public int getRowCount() {
        return repository != null ? repository.getEntryCount() : 0;
    }

    public int getColumnCount() {
        return columnList.size();
    }

    @Override
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
    }

    public Object getValueAt(final int rowIndex, final int columnIndex) {
        if (repository != null) {
            return repository.getEntry(rowIndex).getData(columnIndex);
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

    private class RepositoryHandler implements Repository.RepositoryListener {

        public void handleEntryAdded(final RepositoryEntry entry, final int index) {
            fireTableRowsInserted(index, index);
        }

        public void handleEntryRemoved(final RepositoryEntry entry, final int index) {
            fireTableRowsDeleted(index, index);
        }
    }

}
