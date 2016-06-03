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
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.esa.snap.rcp.SnapApp;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;

/**
 * Residual Stack Information
 */
public class StatResiduals implements InSARStatistic {

    private JTextArea textarea;
    private final InSARStatisticsTopComponent parent;

    private static final String EmptyMsg = "This tool window requires a coregistered Stripmap stack product to be selected";

    public StatResiduals(final InSARStatisticsTopComponent parent) {
        this.parent = parent;
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
            if (!InSARStatistic.isValidProduct(product)) {
                textarea.setText(EmptyMsg);
            } else {
                String content = "";
                final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

                final File residualFile = getResidualFile(absRoot);
                if (residualFile.exists()) {
                    content = readFile(residualFile);
                } else {
                    content = readFromMetadata(product, absRoot);
                }

                textarea.setText(content);
            }
        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to update product", e);
        }
    }

    public void copyToClipboard() {
        SystemUtils.copyToClipboard(textarea.getText());
    }

    public void saveToFile() {
        saveToFile(textarea.getText());
    }

    private static File getResidualFile(final MetadataElement absRoot) {
        final String mstName = absRoot.getAttributeString(AbstractMetadata.PRODUCT);
        return new File(ResourceUtils.getReportFolder(), mstName + "_residual.txt");
    }

    private static String readFile(final File file) {
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

    private static String readFromMetadata(final Product product, final MetadataElement absRoot) {
        final StringBuilder str = new StringBuilder();
        final Formatter formatter = new Formatter(str, Locale.US);

        final Band[] bands = product.getBands();
        for (Band band : bands) {
            final MetadataElement bandElem = AbstractMetadata.getBandAbsMetadata(absRoot, band.getName(), false);
            if (bandElem != null) {
                MetadataElement warpDataElem = bandElem.getElement("WarpData");
                if (warpDataElem != null) {
                    final MetadataElement[] GCPElems = warpDataElem.getElements();
                    formatter.format("%15s %15s %15s %15s %15s %20s\n", "GCP", "mst_x", "mst_y", "slv_x", "slv_y", "rms");

                    for (MetadataElement GCPElem : GCPElems) {
                        formatter.format("%15s", GCPElem.getName());
                        for (String attrib : GCPElem.getAttributeNames()) {
                            double value = GCPElem.getAttributeDouble(attrib);
                            formatter.format("%15.4f", value);
                        }
                        str.append('\n');
                    }

                    str.append('\n');
                    formatter.format("%-20s %-25.4f\n", "rmsStd", warpDataElem.getAttributeDouble("rmsStd", 0));
                    formatter.format("%-20s %-25.4f\n", "rmsMean", warpDataElem.getAttributeDouble("rmsMean", 0));
                    formatter.format("%-20s %-25.4f\n", "rowResidualStd", warpDataElem.getAttributeDouble("rowResidualStd", 0));
                    formatter.format("%-20s %-25.4f\n", "rowResidualMean", warpDataElem.getAttributeDouble("rowResidualMean", 0));
                    formatter.format("%-20s %-25.4f\n", "colResidualStd", warpDataElem.getAttributeDouble("colResidualStd", 0));
                    formatter.format("%-20s %-25.4f\n", "colResidualMean", warpDataElem.getAttributeDouble("colResidualMean", 0));

                    break;
                }
            }
        }
        return str.toString();
    }
}


