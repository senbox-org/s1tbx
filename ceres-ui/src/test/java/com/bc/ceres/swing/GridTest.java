package com.bc.ceres.swing;

import org.junit.Test;

import javax.swing.*;

import static org.junit.Assert.*;

public class GridTest {
    @Test
    public void testEmptyGrid() throws Exception {
        Grid grid = new Grid(4, true);

        assertEquals(4, grid.getColumnCount());
        assertEquals(0, grid.getDataRowCount());
        assertEquals(1, grid.getRowCount());
        assertEquals(null, grid.getComponent(0, 0));

        assertEquals(false, grid.isDataRowSelected(0));
        assertEquals(0, grid.getSelectedDataRowCount());
        assertArrayEquals(asArray(), grid.getSelectedDataRowIndexes());
    }

    @Test
    public void testSetHeaderRow() throws Exception {
        Grid grid = new Grid(4, true);

        JLabel c1 = new JLabel();
        JLabel c2 = new JLabel();
        JLabel c3 = new JLabel();

        grid.setHeaderRow(c1, c2, c3);

        assertEquals(4, grid.getColumnCount());
        assertEquals(0, grid.getDataRowCount());
        assertEquals(1, grid.getRowCount());
        assertNotNull(grid.getComponent(0, 0));
        assertEquals(JCheckBox.class, grid.getComponent(0, 0).getClass());
        assertSame(c1, grid.getComponent(0, 1));
        assertSame(c2, grid.getComponent(0, 2));
        assertSame(c3, grid.getComponent(0, 3));
    }

    @Test
    public void testAddDataRow() throws Exception {
        Grid grid = new Grid(4, true);

        JLabel c1 = new JLabel();
        JLabel c2 = new JLabel();
        JLabel c3 = new JLabel();
        JLabel c4 = new JLabel();
        JLabel c5 = new JLabel();
        JLabel c6 = new JLabel();

        grid.addDataRow(c1, c2, c3);

        assertEquals(1, grid.getDataRowCount());
        assertEquals(2, grid.getRowCount());
        assertEquals(JCheckBox.class, grid.getComponent(1, 0).getClass());
        assertSame(c1, grid.getComponent(1, 1));
        assertSame(c2, grid.getComponent(1, 2));
        assertSame(c3, grid.getComponent(1, 3));

        grid.addDataRow(c4, c5, c6);

        assertEquals(2, grid.getDataRowCount());
        assertEquals(3, grid.getRowCount());
        assertEquals(JCheckBox.class, grid.getComponent(1, 0).getClass());
        assertSame(c1, grid.getComponent(1, 1));
        assertSame(c2, grid.getComponent(1, 2));
        assertSame(c3, grid.getComponent(1, 3));
        assertEquals(JCheckBox.class, grid.getComponent(2, 0).getClass());
        assertSame(c4, grid.getComponent(2, 1));
        assertSame(c5, grid.getComponent(2, 2));
        assertSame(c6, grid.getComponent(2, 3));
    }


    @Test
    public void testRemoveDataRow() throws Exception {
        Grid grid = new Grid(4, true);

        JLabel c1 = new JLabel();
        JLabel c2 = new JLabel();
        JLabel c3 = new JLabel();
        JLabel c4 = new JLabel();
        JLabel c5 = new JLabel();
        JLabel c6 = new JLabel();

        grid.addDataRow(c1, c2, c3);
        grid.addDataRow(c4, c5, c6);

        grid.removeDataRow(0);

        assertEquals(1, grid.getDataRowCount());
        assertEquals(2, grid.getRowCount());

        assertEquals(JCheckBox.class, grid.getComponent(1, 0).getClass());
        assertSame(c4, grid.getComponent(1, 1));
        assertSame(c5, grid.getComponent(1, 2));
        assertSame(c6, grid.getComponent(1, 3));

        grid.removeDataRow(0);

        assertEquals(0, grid.getDataRowCount());
        assertEquals(1, grid.getRowCount());
    }

    @Test
    public void testRemoveDataRows() throws Exception {
        Grid grid = new Grid(4, true);

        JLabel c1 = new JLabel();
        JLabel c2 = new JLabel();
        JLabel c3 = new JLabel();
        JLabel c4 = new JLabel();
        JLabel c5 = new JLabel();
        JLabel c6 = new JLabel();

        grid.addDataRow(c1, c2, c3);
        grid.addDataRow(new JLabel(), new JLabel(), new JLabel());
        grid.addDataRow(c4, c5, c6);
        grid.addDataRow(new JLabel(), new JLabel(), new JLabel());

        grid.removeDataRows(1, 3);

        assertEquals(2, grid.getDataRowCount());
        assertEquals(3, grid.getRowCount());

        assertEquals(JCheckBox.class, grid.getComponent(1, 0).getClass());
        assertSame(c1, grid.getComponent(1, 1));
        assertSame(c2, grid.getComponent(1, 2));
        assertSame(c3, grid.getComponent(1, 3));

        assertEquals(JCheckBox.class, grid.getComponent(2, 0).getClass());
        assertSame(c4, grid.getComponent(2, 1));
        assertSame(c5, grid.getComponent(2, 2));
        assertSame(c6, grid.getComponent(2, 3));
    }

    @Test
    public void testSelectionState_1() throws Exception {
        Grid grid = new Grid(4, true);

        MySelectionListener listener = new MySelectionListener();
        grid.getSelectionModel().addListener(listener);

        grid.addDataRow(new JLabel(), new JLabel(), new JLabel());
        grid.addDataRow(new JLabel(), new JLabel(), new JLabel());
        grid.addDataRow(new JLabel(), new JLabel(), new JLabel());

        assertEquals(0, grid.getSelectedDataRowCount());
        assertEquals(-1, grid.getSelectedDataRowIndex());
        assertArrayEquals(asArray(), grid.getSelectedDataRowIndexes());

        ((JCheckBox) grid.getComponent(2, 0)).doClick();

        assertEquals(1, grid.getSelectedDataRowCount());
        assertEquals(1, grid.getSelectedDataRowIndex());
        assertArrayEquals(asArray(1), grid.getSelectedDataRowIndexes());

        ((JCheckBox) grid.getComponent(1, 0)).doClick();
        ((JCheckBox) grid.getComponent(3, 0)).doClick();

        assertEquals(3, grid.getSelectedDataRowCount());
        assertEquals(0, grid.getSelectedDataRowIndex());
        assertArrayEquals(asArray(0, 1, 2), grid.getSelectedDataRowIndexes());

        assertEquals(3, listener.count);
    }

    @Test
    public void testSelectionState_2() throws Exception {
        Grid grid = new Grid(4, true);

        MySelectionListener listener = new MySelectionListener();
        grid.getSelectionModel().addListener(listener);

        grid.addDataRow(new JLabel(), new JLabel(), new JLabel());
        grid.addDataRow(new JLabel(), new JLabel(), new JLabel());
        grid.addDataRow(new JLabel(), new JLabel(), new JLabel());

        assertEquals(0, grid.getSelectedDataRowCount());
        assertEquals(-1, grid.getSelectedDataRowIndex());
        assertArrayEquals(asArray(), grid.getSelectedDataRowIndexes());

        grid.setSelectedDataRowIndexes(2);

        assertEquals(1, grid.getSelectedDataRowCount());
        assertEquals(2, grid.getSelectedDataRowIndex());
        assertArrayEquals(asArray(2), grid.getSelectedDataRowIndexes());

        grid.setSelectedDataRowIndexes(1, 2, 3);

        assertEquals(3, grid.getSelectedDataRowCount());
        assertEquals(1, grid.getSelectedDataRowIndex());
        assertArrayEquals(asArray(1, 2, 3), grid.getSelectedDataRowIndexes());

        assertEquals(2, listener.count);
    }

   static  int [] asArray(int ... array) {
        return array;
    }

    private static class MySelectionListener implements GridSelectionModel.Listener {
        int count;

        @Override
        public void gridSelectionChanged(GridSelectionModel.Event event) {
            count++;
        }
    }
}
