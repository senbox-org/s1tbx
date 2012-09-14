/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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

import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.datamodel.MapProjectionHandler;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * User interface for RangeDopplerGeocodingOp
 */
public class GeolocationGridGeocodingOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();
    private final JComboBox imgResamplingMethod = new JComboBox(new String[] {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
                                                                      ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
                                                                      ResamplingFactory.CUBIC_CONVOLUTION_NAME });
    private final JButton crsButton = new JButton();
    private final MapProjectionHandler mapProjHandler = new MapProjectionHandler();

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);

        final JComponent panel = createPanel();
        initParameters();

        crsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mapProjHandler.promptForFeatureCrs(sourceProducts);
                crsButton.setText(mapProjHandler.getCRSName());
            }
        });

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        OperatorUIUtils.initBandList(bandList, getBandNames());
        imgResamplingMethod.setSelectedItem(paramMap.get("imgResamplingMethod"));
        final String mapProjection = (String)paramMap.get("mapProjection");
        mapProjHandler.initParameters(mapProjection, sourceProducts);
        crsButton.setText(mapProjHandler.getCRSName());
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateBandList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);
        paramMap.put("imgResamplingMethod", imgResamplingMethod.getSelectedItem());
        if(mapProjHandler.getCRS() != null) {
            paramMap.put("mapProjection", mapProjHandler.getCRS().toWKT());
        }
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(new JLabel("Source Bands:"), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        contentPane.add(new JScrollPane(bandList), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Image Resampling Method:", imgResamplingMethod);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Map Projection:", crsButton);
        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }
}