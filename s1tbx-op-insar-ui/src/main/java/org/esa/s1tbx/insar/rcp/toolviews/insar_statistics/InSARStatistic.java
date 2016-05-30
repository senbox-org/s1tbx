/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.insar.rcp.toolviews.insar_statistics;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.SnapFileChooser;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * tab interface for InSARStatistic TopComponent
 */
public interface InSARStatistic {

    String getName();

    Component createPanel();

    void update(final Product product);

    static boolean isValidProduct(final Product product) {
        return product != null && StackUtils.isCoregisteredStack(product);
    }

    void copyToClipboard();

    void saveToFile();

    default void saveToFile(final String content) {
        final SnapFileChooser fileChooser = new SnapFileChooser();
        fileChooser.setFileFilter(new SnapFileFilter("TXT", new String[]{".txt"}, "TXT files"));
        File outputAsciiFile;
        int result = fileChooser.showSaveDialog(SnapApp.getDefault().getMainFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            outputAsciiFile = fileChooser.getSelectedFile();
        } else {
            return;
        }

        try (PrintStream outputStream = new PrintStream(new FileOutputStream(outputAsciiFile))) {

            outputStream.print(content);

        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to write insar statistics", e);
        }
    }
}
