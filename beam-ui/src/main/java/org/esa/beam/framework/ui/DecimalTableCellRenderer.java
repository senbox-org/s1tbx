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

package org.esa.beam.framework.ui;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.text.DecimalFormat;

/**
 * TableCellRenderer which renders {@link Float float} and {@link Double double}
 * values with the given {@link DecimalFormat format}.
 * The cell value is right aligned.
 */
public class DecimalTableCellRenderer extends DefaultTableCellRenderer {

    private DecimalFormat format;

    /**
     * Creates a new TableCellRenderer with the given format
     *
     * @param format the format in which the cell values shall be rendered.
     *
     * @see DecimalFormat
     */
    public DecimalTableCellRenderer(DecimalFormat format) {
        this.format = format;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (comp instanceof JLabel) {
            JLabel label = (JLabel) comp;
            label.setHorizontalAlignment(JLabel.RIGHT);
            if (value instanceof Float && !Float.isNaN((Float) value) ||
                value instanceof Double && !Double.isNaN((Double) value)) {
                label.setText(format.format(value));
            } else {
                label.setText("n/a");
            }
        }
        return comp;
    }
}
