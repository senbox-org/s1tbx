package org.esa.beam.visat.toolviews.layermanager.layersrc;

import org.esa.beam.util.io.FileUtils;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;
import java.io.File;

/**
 * A {@link javax.swing.ListCellRenderer} which replaces the beginning of the file path
 * by "..." to ensure the given maximum length..
 * It shows also the complete path as a tool tip.
 */
public class FilePathListCellRenderer extends DefaultListCellRenderer {

    private int maxLength;

    /**
     * Creates an instance of {@link javax.swing.ListCellRenderer}.
     *
     * @param maxLength The maximum length of the file path
     */
    public FilePathListCellRenderer(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        final String filePath = (String) value;
        if (filePath != null) {
            setToolTipText(filePath);
            setText(FileUtils.getDisplayText(new File(filePath), maxLength));
        }
        return this;

    }
}
