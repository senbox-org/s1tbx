package org.esa.beam.visat.toolviews.mask;

import org.esa.beam.framework.ui.UIUtils;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

class VisibleFlagHeaderRenderer extends JLabel implements TableCellRenderer {

    VisibleFlagHeaderRenderer() {
        ImageIcon icon = UIUtils.loadImageIcon("icons/EyeIcon10.gif");
        this.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        this.setText(null);
        this.setIcon(icon);
        this.setHorizontalAlignment(SwingConstants.CENTER);
        this.setPreferredSize(this.getPreferredSize());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        return this;
    }
}
