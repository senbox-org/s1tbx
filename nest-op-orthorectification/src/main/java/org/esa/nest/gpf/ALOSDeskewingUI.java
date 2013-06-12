/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * User interface for ALOS Deskewing
 */
public class ALOSDeskewingUI extends BaseOperatorUI {

    private final JList bandList = new JList();
    /*
    private final JCheckBox useMapreadyShiftOnlyCheckBox = new JCheckBox("Use Mapready Shift Only");
    private final JCheckBox useFAQShiftOnlyCheckBox = new JCheckBox("Use FAQ Shift Only");
    private final JCheckBox useBothCheckBox = new JCheckBox("Use Mapready + FAQ Shift");
    private final JCheckBox useHybridCheckBox = new JCheckBox("Use Mapready + Hybrid Shift");

    private boolean useMapreadyShiftOnly = false;
    private boolean useFAQShiftOnly = false;
    private boolean useBoth = false;
    private boolean useHybrid = false;
    */

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();
        /*
        useMapreadyShiftOnlyCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                useMapreadyShiftOnly = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        useFAQShiftOnlyCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                useFAQShiftOnly = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        useBothCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                useBoth = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        useHybridCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                useHybrid = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        */
        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        OperatorUIUtils.initParamList(bandList, getBandNames());
        /*
        useMapreadyShiftOnly = (Boolean)paramMap.get("useMapreadyShiftOnly");
        useMapreadyShiftOnlyCheckBox.setSelected(useMapreadyShiftOnly);

        useFAQShiftOnly = (Boolean)paramMap.get("useFAQShiftOnly");
        useFAQShiftOnlyCheckBox.setSelected(useFAQShiftOnly);

        useBoth = (Boolean)paramMap.get("useBoth");
        useBothCheckBox.setSelected(useBoth);

        useHybrid = (Boolean)paramMap.get("useHybrid");
        useHybridCheckBox.setSelected(useHybrid);
        */
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);
        /*
        paramMap.put("useMapreadyShiftOnly", useMapreadyShiftOnly);
        paramMap.put("useFAQShiftOnly", useFAQShiftOnly);
        paramMap.put("useBoth", useBoth);
        paramMap.put("useHybrid", useHybrid);
        */
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(new JLabel("Source Bands:"), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        contentPane.add(new JScrollPane(bandList), gbc);
        /*
        gbc.gridy++;
        contentPane.add(useMapreadyShiftOnlyCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(useFAQShiftOnlyCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(useBothCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(useHybridCheckBox, gbc);
        */
        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }
}