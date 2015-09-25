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
package org.esa.s1tbx.insar.rcp.toolviews;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.gpf.StackUtils;
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
            if (!InSARStatisticsTopComponent.isValidProduct(product)) {
                textarea.setText(InSARStatisticsTopComponent.EmptyMsg);
            } else {
                final String[] slaveProductNames = StackUtils.getSlaveProductNames(product);
                //final ProductData.UTC[] times = StackUtils.getProductTimes(product);

                final StringBuilder slaveNames = new StringBuilder();
                int i = 1;
                for (String slaveName : slaveProductNames) {
                    slaveNames.append("<b>Slave Product " + i + ": </b>");
                    slaveNames.append(slaveName.substring(0, slaveName.lastIndexOf("_")));
                    slaveNames.append("<br>");
                }

                textarea.setText("<html>" +
                                "<b>Master Product: </b>" + product.getName() + "<br>" +
                                slaveNames.toString() +
                                "</html>"
                );
            }
        } catch (Exception e) {
            SnapApp.getDefault().handleError("Unable to update product", e);
        }
    }
}


