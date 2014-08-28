/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.gpf;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper functions for OperatorUIs
 */
public final class OperatorUIUtils {

    public final static String SOURCE_BAND_NAMES = "sourceBandNames";

    public static void initParamList(final JList paramList, final String[] availNames, final Object[] paramDefaults) {
        if (paramDefaults != null) {
            final String[] srcBandNames = new String[paramDefaults.length];
            final int[] indixes = new int[paramDefaults.length];
            for (int i = 0; i < srcBandNames.length; ++i) {
                srcBandNames[i] = (String) paramDefaults[i];
                indixes[i] = i;
            }
            paramList.setListData(srcBandNames);
            paramList.setSelectedIndices(indixes);
        }
        initParamList(paramList, availNames);
    }

    public static void initParamList(final JList paramList, final String[] availNames) {
        initParamList(paramList, availNames, null);
    }

    public static void initParamList(final JList paramList, final String[] availNames, final String[] defaultSelection) {
        final List selectedValues = paramList.getSelectedValuesList();

        paramList.removeAll();
        paramList.setListData(availNames);
        paramList.setFixedCellWidth(200);
        paramList.setMinimumSize(new Dimension(50, 4));

        final int size = paramList.getModel().getSize();
        final List<Integer> indices = new ArrayList<>(size);

        for (Object selectedValue : selectedValues) {
            final String selValue = (String) selectedValue;

            for (int j = 0; j < size; ++j) {
                final String val = (String) paramList.getModel().getElementAt(j);
                if (val.equals(selValue)) {
                    indices.add(j);
                    break;
                }
            }
        }
        if(selectedValues.isEmpty() && defaultSelection != null) {
            int j = 0;
            for(String name : availNames) {
                for(String defaultSel : defaultSelection) {
                    if (name.equals(defaultSel)) {
                        indices.add(j);
                    }
                }
                ++j;
            }
        }
        setSelectedListIndices(paramList, indices);
    }

    public static void setSelectedListIndices(final JList list, final List<Integer> indices) {
        final int[] selIndex = new int[indices.size()];
        for (int i = 0; i < indices.size(); ++i) {
            selIndex[i] = indices.get(i);
        }
        list.setSelectedIndices(selIndex);
    }

    public static void updateParamList(final JList paramList, final Map<String, Object> paramMap, final String paramName) {
        final List selectedValues = paramList.getSelectedValuesList();
        final String names[] = new String[selectedValues.size()];
        int i = 0;
        for (Object selectedValue : selectedValues) {
            names[i++] = (String) selectedValue;
        }
        paramMap.put(paramName, names);
    }

    public static double getNoDataValue(final File extFile) {
        try {
            final Product product = ProductIO.readProduct(extFile);
            if (product != null)
                return product.getBandAt(0).getNoDataValue();
        } catch (Exception e) {
            //
        }
        return 0;
    }
}