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

public class FloatTableCellRenderer extends DefaultTableCellRenderer {

    private DecimalFormat format;

    public FloatTableCellRenderer(DecimalFormat format) {
        this.format = format;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (comp instanceof JLabel) {
            JLabel label = (JLabel) comp;
            label.setHorizontalAlignment(JLabel.RIGHT);
            if (value instanceof Float && !Float.isNaN((Float) value)) {
                label.setText(format.format(value));
            } else {
                label.setText("n/a");
            }
        }
        return comp;
    }
}
