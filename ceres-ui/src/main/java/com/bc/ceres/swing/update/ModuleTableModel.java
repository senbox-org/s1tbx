package com.bc.ceres.swing.update;

import com.bc.ceres.core.Assert;

import javax.swing.table.AbstractTableModel;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 05.04.2007
 * Time: 16:36:23
 * To change this template use File | Settings | File Templates.
 */
class ModuleTableModel extends AbstractTableModel {

    static Map<String, ModuleTableColumnDef> availableColumnDefs = new HashMap<String, ModuleTableColumnDef>(32);
    private ModuleTableColumnDef[] moduleTableColumns;
    private ModuleItem[] moduleItems;

    static {
        defineColumn("Name", String.class, new ModuleValueAccessor() {
            public Object getValue(ModuleItem moduleItem) {
                return ModuleTextFactory.getNameText(moduleItem);
            }
        });
        defineColumn("Version", String.class, new ModuleValueAccessor() {
            public Object getValue(ModuleItem moduleItem) {
                return ModuleTextFactory.getVersionText(moduleItem);
            }
        });
        defineColumn("New Version", String.class, new ModuleValueAccessor() {
            public Object getValue(ModuleItem moduleItem) {
                return ModuleTextFactory.getUpdateVersionText(moduleItem);
            }
        });
        defineColumn("Date", String.class, new ModuleValueAccessor() {
            public Object getValue(ModuleItem moduleItem) {
                return ModuleTextFactory.getDateText(moduleItem);
            }
        });
        defineColumn("Size", String.class, new ModuleValueAccessor() {
            public Object getValue(ModuleItem moduleItem) {
                return ModuleTextFactory.getSizeText(moduleItem);
            }
        });
        defineColumn("State", String.class, new ModuleValueAccessor() {
            public Object getValue(ModuleItem moduleItem) {
                return ModuleTextFactory.getStateText(moduleItem);
            }
        });
        defineColumn("Action", String.class, new ModuleValueAccessor() {
            public Object getValue(ModuleItem moduleItem) {
                return ModuleTextFactory.getActionText(moduleItem);
            }
        });
    }

    public ModuleTableModel(ModuleItem[] moduleItems, String[] columnNames) {
        Assert.notNull(moduleItems);
        Assert.notNull(columnNames);
        this.moduleItems = moduleItems;
        moduleTableColumns = new ModuleTableColumnDef[columnNames.length];
        for (int i = 0; i < moduleTableColumns.length; i++) {
            ModuleTableColumnDef moduleTableColumn = availableColumnDefs.get(columnNames[i]);
            Assert.notNull(moduleTableColumn);
            moduleTableColumns[i] = moduleTableColumn;
        }
    }

    public int getRowCount() {
        return moduleItems.length;
    }

    public int getColumnCount() {
        return moduleTableColumns.length;
    }

    public ModuleItem[] getModuleItems() {
        return moduleItems;
    }

    public void setModuleItems(ModuleItem[] moduleItems) {
        this.moduleItems = moduleItems;
        fireTableDataChanged();
    }

    public ModuleItem getModuleItem(int rowIndex) {
        return moduleItems[rowIndex];
    }

    @Override
    public String getColumnName(int columnIndex) {
        return getColumn(columnIndex).getName();
    }

    private ModuleTableColumnDef getColumn(int columnIndex) {
        return moduleTableColumns[columnIndex];
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        ModuleItem moduleItem = moduleItems[rowIndex];
        ModuleTableColumnDef column = getColumn(columnIndex);
        return column.getValue(moduleItem);
    }

    private static void defineColumn(String name, Class<?> type, ModuleValueAccessor accessor) {
        ModuleTableColumnDef value = new ModuleTableColumnDef(name, type, accessor);
        availableColumnDefs.put(name, value);
    }
}
