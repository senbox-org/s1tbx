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
package org.csa.rstb.polarimetric.gpf.ui;

import org.csa.rstb.polarimetric.gpf.RVIOp;
import org.esa.snap.engine_utilities.gpf.FilterWindow;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

public class RVIOpUI extends BaseOperatorUI {

    private final JComboBox<String> windowSize = new JComboBox(new String[]{
            FilterWindow.SIZE_3x3, FilterWindow.SIZE_5x5, FilterWindow.SIZE_7x7, FilterWindow.SIZE_9x9, FilterWindow.SIZE_11x11,
            FilterWindow.SIZE_13x13, FilterWindow.SIZE_15x15, FilterWindow.SIZE_17x17});



    private static final JLabel windowSizeLabel = new JLabel("Window Size:");


    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();

        filter.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                updateFilterSelection();
            }
        });
        /* updateFilterSelection(); */

        initParameters();

        return panel;
    }

    @Override
    public void initParameters() {

        windowSize.setSelectedItem(paramMap.get("windowSize"));
        /* targetWindowSize.setSelectedItem(paramMap.get("targetWindowSizeStr")); */
        
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("windowSize", windowSize.getSelectedItem());
/*         paramMap.put("targetWindowSizeStr", targetWindowSize.getSelectedItem()); */
        
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, windowSizeLabel, windowSize);
		DialogUtils.enableComponents(windowSizeLabel, windowSize, false);

        

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

}
