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
package com.bc.ceres.swing;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TableLayout implements LayoutManager2 {
    public enum Fill {
        /**
         * Do not resize the component.
         */
        NONE(GridBagConstraints.NONE), //
        /**
         * Make the component wide enough to fill its display area horizontally, but do not change its height.
         */
        HORIZONTAL(GridBagConstraints.HORIZONTAL),
        /**
         * Make the component tall enough to fill its display area vertically, but do not change its width.
         */
        VERTICAL(GridBagConstraints.VERTICAL),
        /**
         * Make the component tall enough to fill its display area vertically, but do not change its width.
         */
        BOTH(GridBagConstraints.BOTH);

        private final int value;

        private Fill(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public enum Anchor {
        // Absolute
        CENTER(GridBagConstraints.CENTER),
        NORTH(GridBagConstraints.NORTH),
        NORTHEAST(GridBagConstraints.NORTHEAST),
        EAST(GridBagConstraints.EAST),
        SOUTHEAST(GridBagConstraints.SOUTHEAST),
        SOUTH(GridBagConstraints.SOUTH),
        SOUTHWEST(GridBagConstraints.SOUTHWEST),
        WEST(GridBagConstraints.WEST),
        NORTHWEST(GridBagConstraints.NORTHWEST),
        // Orientation relative
        PAGE_START(GridBagConstraints.PAGE_START),
        PAGE_END(GridBagConstraints.PAGE_END),
        LINE_START(GridBagConstraints.LINE_START),
        LINE_END(GridBagConstraints.LINE_END),
        FIRST_LINE_START(GridBagConstraints.FIRST_LINE_START),
        FIRST_LINE_END(GridBagConstraints.FIRST_LINE_END),
        LAST_LINE_START(GridBagConstraints.LAST_LINE_START),
        LAST_LINE_END(GridBagConstraints.LAST_LINE_END),
        // Baseline relvative
        BASELINE(GridBagConstraints.BASELINE),
        BASELINE_LEADING(GridBagConstraints.BASELINE_LEADING),
        BASELINE_TRAILING(GridBagConstraints.LAST_LINE_END),
        ABOVE_BASELINE(GridBagConstraints.ABOVE_BASELINE),
        ABOVE_BASELINE_LEADING(GridBagConstraints.ABOVE_BASELINE_LEADING),
        ABOVE_BASELINE_TRAILING(GridBagConstraints.ABOVE_BASELINE_TRAILING),
        BELOW_BASELINE(GridBagConstraints.BELOW_BASELINE),
        BELOW_BASELINE_LEADING(GridBagConstraints.BELOW_BASELINE_LEADING),
        BELOW_BASELINE_TRAILING(GridBagConstraints.BELOW_BASELINE_TRAILING);


        private final int value;

        private Anchor(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }


    private GridBagLayout gbl;
    private HashMap<String, Object> propertyMap;
    private int columnCount;
    private Cell currentCell;

    public TableLayout() {
        this(1);
    }

    public TableLayout(int columnCount) {
        this.gbl = new GridBagLayout();
        this.propertyMap = new HashMap<String, Object>(32);
        this.columnCount = columnCount;
        this.currentCell = new Cell();
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    /////////////////////////////////////////////////////////////////////////
    // gridwidth

    public void setCellColspan(int row, int col, int colspan) {
        setCellValue("gridwidth", row, col, colspan);
    }

    /////////////////////////////////////////////////////////////////////////
    // gridheight

    public void setCellRowspan(int row, int col, int rowspan) {
        setCellValue("gridheight", row, col, rowspan);
    }

    /////////////////////////////////////////////////////////////////////////
    // insets

    public void setTablePadding(int hpadding, int vpadding) {
        setTablePadding(new Insets(0, 0, vpadding, hpadding));
        setColumnPadding(0, new Insets(0, hpadding, vpadding, hpadding));
        setRowPadding(0, new Insets(vpadding, 0, vpadding, hpadding));
        setCellPadding(0, 0, new Insets(vpadding, hpadding, vpadding, hpadding));
    }

    public void setTablePadding(Insets insets) {
        setValue("insets", insets);
    }

    public void setRowPadding(int row, Insets insets) {
        setRowValue("insets", row, insets);
    }

    public void setColumnPadding(int col, Insets insets) {
        setColumnValue("insets", col, insets);
    }

    public void setCellPadding(int row, int col, Insets insets) {
        setCellValue("insets", row, col, insets);
    }

    /////////////////////////////////////////////////////////////////////////
    // weighty

    public void setTableWeightX(double weightx) {
        setValue("weightx", weightx);
    }

    public void setRowWeightX(int row, double weightx) {
        setRowValue("weightx", row, weightx);
    }

    public void setColumnWeightX(int col, double weightx) {
        setColumnValue("weightx", col, weightx);
    }

    public void setCellWeightX(int row, int col, double weightx) {
        setCellValue("weightx", row, col, weightx);
    }

    /////////////////////////////////////////////////////////////////////////
    // weighty

    public void setTableWeightY(double weightx) {
        setValue("weighty", weightx);
    }

    public void setRowWeightY(int row, double weighty) {
        setRowValue("weighty", row, weighty);
    }

    public void setColumnWeightY(int col, double weighty) {
        setColumnValue("weighty", col, weighty);
    }

    public void setCellWeightY(int row, int col, double weighty) {
        setCellValue("weighty", row, col, weighty);
    }

    /////////////////////////////////////////////////////////////////////////
    // fill

    public void setTableFill(Fill fill) {
        setValue("fill", fill);
    }

    public void setRowFill(int row, Fill fill) {
        setRowValue("fill", row, fill);
    }

    public void setColumnFill(int col, Fill fill) {
        setColumnValue("fill", col, fill);
    }

    public void setCellFill(int row, int col, Fill fill) {
        setCellValue("fill", row, col, fill);
    }

    /////////////////////////////////////////////////////////////////////////
    // anchor

    public void setTableAnchor(Anchor anchor) {
        setValue("anchor", anchor);
    }

    public void setRowAnchor(int row, Anchor anchor) {
        setRowValue("anchor", row, anchor);
    }

    public void setColumnAnchor(int col, Anchor anchor) {
        setColumnValue("anchor", col, anchor);
    }

    public void setCellAnchor(int row, int col, Anchor anchor) {
        setCellValue("anchor", row, col, anchor);
    }


    public Component createVerticalSpacer() {
        setCellColspan(currentCell.row, 0, columnCount);
        setRowFill(currentCell.row, Fill.BOTH);
        setRowWeightY(currentCell.row, 1.0);
        return new JPanel();
    }

    public Component createHorizontalSpacer() {
        setColumnFill(currentCell.col, Fill.BOTH);
        setColumnWeightX(currentCell.col, 1.0);
        return new JPanel();
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * Returns the alignment along the x axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     */
    @Override
    public float getLayoutAlignmentX(Container target) {
        return gbl.getLayoutAlignmentX(target);
    }

    /**
     * Returns the alignment along the y axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     */
    @Override
    public float getLayoutAlignmentY(Container target) {
        return gbl.getLayoutAlignmentY(target);
    }

    /**
     * Invalidates the layout, indicating that if the layout manager
     * has cached information it should be discarded.
     */
    @Override
    public void invalidateLayout(Container target) {
        gbl.invalidateLayout(target);
    }

    /**
     * Adds the specified component to the layout, using the specified
     * constraint object.
     *
     * @param comp        the component to be added
     * @param constraints where/how the component is added to the layout.
     */
    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints == null) {
            addLayoutComponent(comp, this.currentCell);
        } else if (constraints instanceof Cell) {
            addLayoutComponent(comp, (Cell) constraints);
        } else {
            throw new IllegalArgumentException("cannot add to layout: constraints must be null or a TableLayout.Cell");
        }
    }

    /**
     * Has no effect, since this layout manager does not use a per-component string.
     */
    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Removes the specified component from the layout.
     *
     * @param comp the component to be removed
     */
    @Override
    public void removeLayoutComponent(Component comp) {
        gbl.removeLayoutComponent(comp);
    }

    /**
     * Lays out the specified container.
     *
     * @param parent the container to be laid out
     */
    @Override
    public void layoutContainer(Container parent) {
        gbl.layoutContainer(parent);
    }

    /**
     * Calculates the minimum size dimensions for the specified
     * container, given the components it contains.
     *
     * @param parent the component to be laid out
     *
     * @see #preferredLayoutSize
     */
    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return gbl.minimumLayoutSize(parent);
    }

    /**
     * Calculates the preferred size dimensions for the specified
     * container, given the components it contains.
     *
     * @param parent the container to be laid out
     *
     * @see #minimumLayoutSize
     */
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return gbl.preferredLayoutSize(parent);
    }

    /**
     * Calculates the maximum size dimensions for the specified container,
     * given the components it contains.
     *
     * @see java.awt.Component#getMaximumSize
     * @see java.awt.LayoutManager
     */
    @Override
    public Dimension maximumLayoutSize(Container parent) {
        return gbl.maximumLayoutSize(parent);
    }

    /////////////////////////////////////////////////////////////////////////

    private void addLayoutComponent(Component comp, final Cell cell) {
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = cell.col;
        gbc.gridy = cell.row;
        gbc.gridwidth = cell.colspan;
        gbc.gridheight = cell.rowspan;
        setField(gbc, "gridwidth", cell);
        setField(gbc, "gridheight", cell);
        setField(gbc, "weightx", cell);
        setField(gbc, "weighty", cell);
        setField(gbc, "fill", cell);
        setField(gbc, "anchor", cell);
        setField(gbc, "insets", cell);

        gbl.addLayoutComponent(comp, gbc);

        this.currentCell.col = gbc.gridx + gbc.gridwidth;
        if (this.currentCell.col >= columnCount) {
            this.currentCell.col = 0;
            this.currentCell.row++;
            // todo - consider gbc.gridheight
        }
    }

    private void setField(final Object object, final String name, final Cell cell) {
        Object value = getValue(name, cell);
        if (value != null) {
            final Field field;
            try {
                field = object.getClass().getField(name);
                field.set(object, value);
            } catch (Exception e) {
                throw new IllegalStateException(name, e);
            }
        }
    }

    private Object getValue(String name, Cell cell) {
        Object value;
        value = getCellValue(name, cell.row, cell.col);
        if (value == null) {
            value = getColumnValue(name, cell.col);
            if (value == null) {
                value = getRowValue(name, cell.row);
                if (value == null) {
                    value = getValue(name);
                }
            }
        }
        if (value instanceof Fill) {
            value = ((Fill) value).value();
        }
        if (value instanceof Anchor) {
            value = ((Anchor) value).value();
        }
        return value;
    }

    private Object getCellValue(String name, int row, int col) {
        return getValue(getCellName(name, row, col));
    }

    private void setCellValue(String name, int row, int col, Object value) {
        setValue(getCellName(name, row, col), value);
    }

    private Object getRowValue(String name, int row) {
        return getValue(getCellName(name, row, -1));
    }

    private void setRowValue(String name, int row, Object value) {
        setValue(getCellName(name, row, -1), value);
    }

    private Object getColumnValue(String name, int col) {
        return getValue(getCellName(name, -1, col));
    }

    private void setColumnValue(String name, int col, Object value) {
        setValue(getCellName(name, -1, col), value);
    }

    private Object getValue(final String name) {
        return propertyMap.get(name);
    }

    private void setValue(String name, Object value) {
        propertyMap.put(name, value);
    }

    private static String getCellName(String name, int row, int col) {
        StringBuilder sb = new StringBuilder(name);
        sb.append('(');
        appendIndex(sb, row);
        sb.append(',');
        appendIndex(sb, col);
        sb.append(')');
        return sb.toString();
    }

    private static void appendIndex(StringBuilder sb, int index) {
        if (index >= 0) {
            sb.append(index);
        } else {
            sb.append('*');
        }
    }

    public static class Cell {
        public int row = 0;
        public int col = 0;
        public int rowspan = 1;
        public int colspan = 1;

        public Cell() {
        }

        public Cell(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public Cell(int row, int col, int rowspan, int colspan) {
            this.row = row;
            this.col = col;
            this.rowspan = rowspan;
            this.colspan = colspan;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName());
        sb.append('[');
        Set<Map.Entry<String, Object>> entries = propertyMap.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            sb.append(',');
        }
        sb.append(']');
        return sb.toString();
    }


}
