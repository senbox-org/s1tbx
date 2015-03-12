package org.esa.beam.framework.ui.color;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * A table cell renderer for color values.
 *
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
public class ColorTableCellRenderer implements TableCellRenderer {
    private ColorLabel colorLabel;

    public ColorTableCellRenderer() {
        this(new ColorLabel());
    }

    public ColorTableCellRenderer(ColorLabel colorLabel) {
        this.colorLabel = colorLabel;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        colorLabel.setHighlighted(isSelected);
        colorLabel.setColor((Color) value);
        return colorLabel;
    }
}
