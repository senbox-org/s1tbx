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
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
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
