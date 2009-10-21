package org.esa.beam.visat.toolviews.mask;

import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.MouseEvent;


class TableToolTipHandler extends MouseInputAdapter {

    private final JTable table;
    private int currentRowIndex;

    TableToolTipHandler(JTable table) {
        this.table = table;
        currentRowIndex = -1;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        currentRowIndex = -1;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int rowIndex = table.rowAtPoint(e.getPoint());
        if (rowIndex != this.currentRowIndex) {
            this.currentRowIndex = rowIndex;
            if (this.currentRowIndex >= 0 && this.currentRowIndex < table.getRowCount()) {
                table.setToolTipText(((MaskTableModel) table.getModel()).getToolTipText(rowIndex));
            }
        }
    }
}
