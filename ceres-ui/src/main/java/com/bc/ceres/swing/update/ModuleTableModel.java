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

package com.bc.ceres.swing.update;

import com.bc.ceres.core.Assert;

import javax.swing.table.AbstractTableModel;
import java.util.HashMap;
import java.util.Map;

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
        defineColumn("Funding", String.class, new ModuleValueAccessor() {
            public Object getValue(ModuleItem moduleItem) {
                return ModuleTextFactory.getFundingText(moduleItem);
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
