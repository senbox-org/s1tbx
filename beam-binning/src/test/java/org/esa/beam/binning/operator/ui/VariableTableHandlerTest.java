package org.esa.beam.binning.operator.ui;

import org.junit.Before;
import org.junit.Test;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import static org.junit.Assert.assertEquals;

/**
 * Created by tonio on 23.04.2014.
 */
public class VariableTableHandlerTest {

    private JTable table;

    @Before
    public void setUp() {
//        Object[][] data = new Object[][]{{"xy", "ab", "ef"}, {1, 2, 4}};
        Object[][] data = new Object[][]{{"xy", 1}, {"ab", 2}, {"ef", 4}};
        Object[] columnNames = new Object[]{"Strings", "Integers"};
        TableModel tableModel = new DefaultTableModel(data, columnNames);
        table = new JTable(tableModel);
    }

    @Test
    public void testAddRow() throws Exception {
        final Object[] row = {"cd", 3};
        VariableTableHandler.addRow(table, row);
        assertEquals(4, table.getRowCount());
        assertEquals("cd", table.getValueAt(3, 0));
        assertEquals(3, table.getValueAt(3, 1));
    }

    @Test
    public void testRemoveRows() throws Exception {
        VariableTableHandler.removeRows(table, new int[]{0, 2});
        assertEquals(1, table.getRowCount());
        assertEquals("ab", table.getValueAt(0, 0));
        assertEquals(2, table.getValueAt(0, 1));
    }

    @Test
    public void testMoveRowsDown() throws Exception {
        VariableTableHandler.moveRowsDown(table, new int[]{0, 1});
        assertEquals("ef", table.getValueAt(0, 0));
        assertEquals(4, table.getValueAt(0, 1));
        assertEquals("xy", table.getValueAt(1, 0));
        assertEquals(1, table.getValueAt(1, 1));
        assertEquals("ab", table.getValueAt(2, 0));
        assertEquals(2, table.getValueAt(2, 1));
    }

    @Test
    public void testMoveRowsUp() throws Exception {
        VariableTableHandler.moveRowsUp(table, new int[]{1, 2});
        assertEquals("ab", table.getValueAt(0, 0));
        assertEquals(2, table.getValueAt(0, 1));
        assertEquals("ef", table.getValueAt(1, 0));
        assertEquals(4, table.getValueAt(1, 1));
        assertEquals("xy", table.getValueAt(2, 0));
        assertEquals(1, table.getValueAt(2, 1));
    }

    @Test
    public void testSelectRowsByArray() throws Exception {
        VariableTableHandler.selectRows(table, new int[]{0, 2});
        assertEquals(2, table.getSelectedRowCount());
        assertEquals(0, table.getSelectedRows()[0]);
        assertEquals(2, table.getSelectedRows()[1]);
    }

    @Test
    public void testSelectRowsByMinMax() throws Exception {
        VariableTableHandler.selectRows(table, 0, 2);
        assertEquals(3, table.getSelectedRowCount());
        assertEquals(0, table.getSelectedRows()[0]);
        assertEquals(1, table.getSelectedRows()[1]);
        assertEquals(2, table.getSelectedRows()[2]);
    }
}
