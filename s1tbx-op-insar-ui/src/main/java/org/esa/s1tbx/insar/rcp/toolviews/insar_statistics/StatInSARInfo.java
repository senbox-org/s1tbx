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
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.rcp.SnapApp;

import javax.swing.*;
import java.awt.*;

/**
 * Basic Stack Information
 */
public class StatInSARInfo implements InSARStatistic {

    private JTextPane textarea;

    public StatInSARInfo() {

    }

    public String getName() {
        return "Stack Information";
    }

    public Component createPanel() {
        textarea = new JTextPane();
        textarea.setContentType("text/html");
        return new JScrollPane(textarea);
    }

    public void update(final Product product) {
        try {
            if (!InSARStatistic.isValidProduct(product)) {
                textarea.setText(InSARStatisticsTopComponent.EmptyMsg);
            } else {
                final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
                final String mstName = absRoot.getAttributeString(AbstractMetadata.PRODUCT);
                final String mode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
                final String orbitFile = absRoot.getAttributeString(AbstractMetadata.orbit_state_vector_file);
                final int relOrbit = absRoot.getAttributeInt(AbstractMetadata.REL_ORBIT);

                final String[] slaveProductNames = StackUtils.getSlaveProductNames(product);
                final ProductData.UTC[] times = StackUtils.getProductTimes(product);
                final String mstTime = times[0].format();

                final StringBuilder slaveNames = new StringBuilder();
                int i = 1;
                for (String slaveName : slaveProductNames) {
                    slaveNames.append("<b>Slave Product " + i + ": </b>");
                    slaveNames.append(slaveName.substring(0, slaveName.lastIndexOf("_")));
                    slaveNames.append(" [" + times[i].format() + "]");
                    slaveNames.append("<br>");
                    ++i;
                }

                String track = "";
                if(relOrbit != AbstractMetadata.NO_METADATA) {
                    track = "<b>Track: </b>" + relOrbit + "<br";
                }

                textarea.setText("<html>" +
                                         "<b>Product: </b>" + product.getProductRefString() +" "+ product.getName() + "<br" +
                                         "<b>Master Product: </b>" + mstName + " [" + mstTime + "]" + "<br>" +
                                         slaveNames.toString() + "<br>" +
                                         "<b>Mode: </b>" + mode + "<br" +
                                         "<b>Orbit: </b>" + orbitFile + "<br" +
                                         track +
                                         "</html>"
                );
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
}


