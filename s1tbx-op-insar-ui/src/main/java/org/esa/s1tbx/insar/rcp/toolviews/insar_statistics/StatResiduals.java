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

import org.esa.s1tbx.insar.rcp.toolviews.InSARStatisticsTopComponent;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.esa.snap.rcp.SnapApp;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Residual Stack Information
 */
public class StatResiduals implements InSARStatistic {

    private JTextArea textarea;
    public static final String EmptyMsg = "This tool window requires a coregistered Stripmap stack product to be selected";

    public StatResiduals() {
    }

    public String getName() {
        return "Coregistration Residuals";
    }

    public Component createPanel() {
        textarea = new JTextArea();
        return new JScrollPane(textarea);
    }

    public void update(final Product product) {
        try {
            if (!InSARStatisticsTopComponent.isValidProduct(product)) {
                textarea.setText(EmptyMsg);
            } else {
                final File residualFile = getResidualFile(product);
                if(!residualFile.exists()) {
                    textarea.setText(EmptyMsg);
                    return;
                }

                final String content = readFile(residualFile);

                textarea.setText(content);
            }
        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to update product", e);
        }
    }

    private static File getResidualFile(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final String mstName = absRoot.getAttributeString(AbstractMetadata.PRODUCT);

        return new File(ResourceUtils.getReportFolder(), mstName + "_residual.txt");
    }

    private String readFile(final File file) {
        final StringBuilder str = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file)) {
            int content;
            while ((content = fis.read()) != -1) {
                str.append((char) content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return str.toString();
    }
}


