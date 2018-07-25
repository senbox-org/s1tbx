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
package org.esa.s1tbx.sentinel1.gpf.ui;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIUtils;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.gpf.ui.worldmap.NestWorldMapPane;
import org.esa.snap.graphbuilder.gpf.ui.worldmap.WorldMapUI;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;
import prefuse.util.ui.JRangeSlider;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

/**
 * User interface for S-1 TOPSAR Split
 */
public class TOPSARSplitOpUI extends BaseOperatorUI {

    private final JComboBox<String> subswathCombo = new JComboBox<>();
    private final JList<String> polList = new JList<>();

    private final JLabel burstLabel = new JLabel("");
    private final JRangeSlider burstRange = new JRangeSlider(0,0,0,0, JRangeSlider.HORIZONTAL);
    private final Map<String, Integer> swathBurstsMap = new HashMap<>();

    private final WorldMapUI worldMapUI = new WorldMapUI();
    private Sentinel1Utils su = null;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);

        subswathCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                Integer max = swathBurstsMap.get(subswathCombo.getSelectedItem());
                if (max != null) {
                    burstRange.setMaximum(max);
                }

                if (su != null) {
                    burstLabel.setText(burstRange.getLowValue() + " to " + burstRange.getHighValue() +
                            " (max number of bursts: " + burstRange.getMaximum() + ")");
                    worldMapUI.getModel().setAdditionalGeoBoundaries(getSelectedBoundaries());
                }
            }
        });

        burstRange.setMinimum(1);
        burstRange.setMaximum(9999);

        burstRange.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                burstLabel.setText(burstRange.getLowValue() + " to " + burstRange.getHighValue()+
                        " (max number of bursts: " + burstRange.getMaximum() + ")");

                if (su != null) {
                    worldMapUI.getModel().setAdditionalGeoBoundaries(getSelectedBoundaries());
                }
            }
        });

        final JComponent panel = createPanel();
        initParameters();
        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        if (sourceProducts != null && sourceProducts.length > 0) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]);

            final String acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
            subswathCombo.removeAllItems();
            if (acquisitionMode.equals("IW")) {
                subswathCombo.addItem("IW1");
                subswathCombo.addItem("IW2");
                subswathCombo.addItem("IW3");
            } else if (acquisitionMode.equals("EW")) {
                subswathCombo.addItem("EW1");
                subswathCombo.addItem("EW2");
                subswathCombo.addItem("EW3");
                subswathCombo.addItem("EW4");
                subswathCombo.addItem("EW5");
            }
            String subswath = (String) paramMap.get("subswath");
            if (subswath == null) {
                subswath = acquisitionMode + '1';
            }
            subswathCombo.setSelectedItem(subswath);

            OperatorUIUtils.initParamList(polList, Sentinel1Utils.getProductPolarizations(absRoot),
                    (String[]) paramMap.get("selectedPolarisations"));

            try {
                su = new Sentinel1Utils(sourceProducts[0]);
                for (int ss = 0; ss < subswathCombo.getItemCount(); ++ss) {
                    String swath = subswathCombo.getItemAt(ss);
                    swathBurstsMap.put(swath, su.getNumOfBursts(swath));
                }
            } catch (Exception e) {

            }

            burstRange.setMaximum(swathBurstsMap.get(subswath));

            worldMapUI.getModel().setAutoZoomEnabled(true);
            worldMapUI.getModel().setProducts(sourceProducts);
            worldMapUI.getModel().setSelectedProduct(sourceProducts[0]);
            worldMapUI.getWorlMapPane().zoomToProduct(sourceProducts[0]);
            worldMapUI.getWorlMapPane().revalidate();
        }

        burstRange.setLowValue((int) paramMap.get("firstBurstIndex"));
        burstRange.setHighValue((int) paramMap.get("lastBurstIndex"));

        if (burstRange.getHighValue() > burstRange.getMaximum()) {
            burstRange.setHighValue(burstRange.getMaximum());
        }
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        String subswathValue = (String) subswathCombo.getSelectedItem();
        if (subswathValue != null) {
            paramMap.put("subswath", subswathValue);
        }
        OperatorUIUtils.updateParamList(polList, paramMap, "selectedPolarisations");

        paramMap.put("firstBurstIndex", burstRange.getLowValue());
        paramMap.put("lastBurstIndex", burstRange.getHighValue());
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Subswath:", subswathCombo);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Polarisations:", polList);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Bursts:", burstLabel);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "", burstRange);

        final NestWorldMapPane worldPane = worldMapUI.getWorlMapPane();
        worldPane.setPreferredSize(new Dimension(500, 130));

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "", worldPane);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private GeoPos[][] getSelectedBoundaries() {
        final Sentinel1Utils.SubSwathInfo[] subswaths = su.getSubSwath();
        final String subswath = (String) subswathCombo.getSelectedItem();
        if (subswath == null)
            return null;

        for (Sentinel1Utils.SubSwathInfo swath : subswaths) {
            if (swath.subSwathName.contains(subswath)) {
                final int numBursts = burstRange.getHighValue() - burstRange.getLowValue() + 1;
                final GeoPos[][] geoBound = new GeoPos[numBursts][4];

                for (int i = 0; i < numBursts; ++i) {
                    geoBound[i] = getBox(burstRange.getLowValue() - 1 + i, swath);
                }
                return geoBound;
            }
        }
        return null;
    }

    private GeoPos[] getBox(final int i, final Sentinel1Utils.SubSwathInfo swath) {
        final int numPoints = swath.latitude[0].length - 1;

        final GeoPos[] geoBound = new GeoPos[5];
        geoBound[0] = new GeoPos(swath.latitude[i][0], swath.longitude[i][0]);
        geoBound[1] = new GeoPos(swath.latitude[i][numPoints], swath.longitude[i][numPoints]);
        geoBound[2] = new GeoPos(swath.latitude[i + 1][numPoints], swath.longitude[i + 1][numPoints]);
        geoBound[3] = new GeoPos(swath.latitude[i + 1][0], swath.longitude[i + 1][0]);
        geoBound[4] = new GeoPos(swath.latitude[i][0], swath.longitude[i][0]);
        return geoBound;
    }
}
