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
    private final InSARStatisticsTopComponent parent;

    public StatInSARInfo(final InSARStatisticsTopComponent parent) {
        this.parent = parent;
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

                final StringBuilder slaveNames = new StringBuilder(255);
                int i = 1;
                for (String slaveName : slaveProductNames) {
                    slaveNames.append("<b>Slave Product " + i + ": </b>");
                    slaveNames.append(slaveName.substring(0, slaveName.lastIndexOf('_')));
                    slaveNames.append(" [" + times[i].format() + ']');
                    slaveNames.append("<br>");
                    ++i;
                }

                String track = "";
                if (relOrbit != AbstractMetadata.NO_METADATA) {
                    track = "<b>Track: </b>" + relOrbit + "<br>";
                }

                String esdStats = getESDStats(absRoot);

                final StringBuilder content = new StringBuilder(255);
                content.append("<html>");
                content.append("<b>Product: </b>" + product.getProductRefString() + ' ' + product.getName() + "<br>");
                content.append("<b>Master Product: </b>" + mstName + " [" + mstTime + ']' + "<br>");
                content.append(slaveNames.toString() + "<br>");
                content.append("<b>Mode: </b>" + mode + "<br>");
                content.append("<b>Orbit: </b>" + orbitFile + "<br>");
                content.append(track);
                if(!esdStats.isEmpty()) {
                    content.append(esdStats);
                }
                content.append("</html>");

                textarea.setText(content.toString());
            }
        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to update product", e);
        }
    }

    private static String getESDStats(final MetadataElement absRoot) {
        String esdStat = "";
        final MetadataElement esdElem = absRoot.getElement(StatESDMeasure.ESD_MEASURE_ELEM);
        if (esdElem != null) {
            final MetadataElement overallShiftElem = esdElem.getElement("Overall_Range_Azimuth_Shift");
            if (overallShiftElem != null) {
                final MetadataElement burstElem = overallShiftElem.getElementAt(0);
                if(burstElem != null) {
                    double rangeShift = burstElem.getAttributeDouble("rangeShift", 0);
                    double azimuthShift = burstElem.getAttributeDouble("azimuthShift", 0);

                    esdStat = "<br><b>ESD Range Shift: </b>" + rangeShift + "<br>";
                    esdStat += "<b>ESD Azimuth Shift: </b>" + azimuthShift + "<br>";
                }
            }
        }
        return esdStat;
    }

    public void copyToClipboard() {
        SystemUtils.copyToClipboard(textarea.getText());
    }

    public void saveToFile() {
        saveToFile(textarea.getText());
    }
}


