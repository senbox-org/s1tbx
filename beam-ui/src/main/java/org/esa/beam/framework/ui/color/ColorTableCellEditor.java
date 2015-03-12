package org.esa.beam.framework.ui.color;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A table cell editor for color values.
 *
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
public class ColorTableCellEditor extends AbstractCellEditor implements TableCellEditor, PropertyChangeListener {
    private ColorComboBox colorComboBox;
    private boolean adjusting;

    public ColorTableCellEditor() {
        this(new ColorComboBox());
    }

    public ColorTableCellEditor(ColorComboBox colorComboBox) {
        this.colorComboBox = colorComboBox;
        this.colorComboBox.addPropertyChangeListener(ColorComboBox.SELECTED_COLOR_PROPERTY, this);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        adjusting = true;
        colorComboBox.setSelectedColor((Color) value);
        adjusting = false;
        return colorComboBox;
    }

    @Override
    public Object getCellEditorValue() {
        return colorComboBox.getSelectedColor();
    }


    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!adjusting) {
            stopCellEditing();
        }
    }
}
