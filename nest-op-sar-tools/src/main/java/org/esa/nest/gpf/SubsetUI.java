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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.dat.toolviews.productlibrary.WorldMapUI;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * User interface for Multilook
 */
public class SubsetUI extends BaseOperatorUI {

    private final JList bandList = new JList();

    private final JTextField regionX = new JTextField("");
    private final JTextField regionY = new JTextField("");
    private final JTextField width = new JTextField("");
    private final JTextField height = new JTextField("");
    private final JTextField subSamplingX = new JTextField("");
    private final JTextField subSamplingY = new JTextField("");

    private final JRadioButton pixelCoordRadio = new JRadioButton("Pixel Coordinates");
    private final JRadioButton geoCoordRadio = new JRadioButton("Geographic Coordinates");
    private final JPanel pixelPanel = new JPanel(new GridBagLayout());
    private final JPanel geoPanel = new JPanel();

    private final WorldMapUI worldMapUI = new WorldMapUI();
    private boolean pixelCoords = true;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        OperatorUIUtils.initParamList(bandList, getBandNames());

        regionX.setText(String.valueOf(paramMap.get("regionX")));
        regionY.setText(String.valueOf(paramMap.get("regionY")));

        Integer widthVal = (Integer)paramMap.get("width");
        Integer heightVal = (Integer)paramMap.get("height");
        if(sourceProducts != null && sourceProducts.length > 0) {
            if(widthVal == null || widthVal == 0)
                widthVal = sourceProducts[0].getSceneRasterWidth();
            if(heightVal == null || heightVal == 0)
                heightVal = sourceProducts[0].getSceneRasterHeight();

            worldMapUI.getModel().setAutoZoomEnabled(true);
            worldMapUI.getModel().setProducts(sourceProducts);
            worldMapUI.getModel().setSelectedProduct(sourceProducts[0]);
            worldMapUI.getWorlMapPane().zoomToProduct(sourceProducts[0]);
        }
        width.setText(String.valueOf(widthVal));
        height.setText(String.valueOf(heightVal));
        subSamplingX.setText(String.valueOf(paramMap.get("subSamplingX")));
        subSamplingY.setText(String.valueOf(paramMap.get("subSamplingY")));

    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        final String regionXStr = regionX.getText();
        if(regionXStr != null && !regionXStr.isEmpty())
            paramMap.put("regionX", Integer.parseInt(regionXStr));
        final String regionYStr = regionY.getText();
        if(regionYStr != null && !regionYStr.isEmpty())
            paramMap.put("regionY", Integer.parseInt(regionYStr));
        final String widthStr = width.getText();
        if(widthStr != null && !widthStr.isEmpty())
            paramMap.put("width", Integer.parseInt(widthStr));
        final String heightStr = height.getText();
        if(heightStr != null && !heightStr.isEmpty())
            paramMap.put("height", Integer.parseInt(heightStr));
        final String subSamplingXStr = subSamplingX.getText();
        if(subSamplingXStr != null && !subSamplingXStr.isEmpty())
            paramMap.put("subSamplingX", Integer.parseInt(subSamplingXStr));
        final String subSamplingYStr = subSamplingY.getText();
        if(subSamplingYStr != null && !subSamplingYStr.isEmpty())
            paramMap.put("subSamplingY", Integer.parseInt(subSamplingYStr));

        if(!pixelCoords) {
            final GeoPos[] selectionBox = worldMapUI.getSelectionBox();
            if(selectionBox != null) {
                final Coordinate[] coords = new Coordinate[selectionBox.length+1];
                for(int i=0; i<selectionBox.length; ++i) {
                    coords[i] = new Coordinate(selectionBox[i].getLon(), selectionBox[i].getLat());
                }
                coords[selectionBox.length] = new Coordinate(selectionBox[0].getLon(), selectionBox[0].getLat());

                final GeometryFactory geometryFactory = new GeometryFactory();
                final LinearRing linearRing = geometryFactory.createLinearRing(coords);

                final Geometry geoRegion = geometryFactory.createPolygon(linearRing, null);
                paramMap.put("geoRegion", geoRegion);
            }
        }
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(new JLabel("Source Bands:"), gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        contentPane.add(new JScrollPane(bandList), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(pixelCoordRadio, gbc);
        gbc.gridx = 1;
        contentPane.add(geoCoordRadio, gbc);

        pixelCoordRadio.setSelected(true);
        pixelCoordRadio.setActionCommand("pixelCoordRadio");
        geoCoordRadio.setActionCommand("geoCoordRadio");
        ButtonGroup group = new ButtonGroup();
    	group.add(pixelCoordRadio);
	    group.add(geoCoordRadio);
        RadioListener myListener = new RadioListener();
        pixelCoordRadio.addActionListener(myListener);
        geoCoordRadio.addActionListener(myListener);

        final GridBagConstraints pixgbc = DialogUtils.createGridBagConstraints();
        pixgbc.fill = GridBagConstraints.BOTH;
        DialogUtils.addComponent(pixelPanel, pixgbc, "X:", regionX);
        pixgbc.gridy++;
        DialogUtils.addComponent(pixelPanel, pixgbc, "Y:", regionY);
        pixgbc.gridy++;
        DialogUtils.addComponent(pixelPanel, pixgbc, "Width:", width);
        pixgbc.gridy++;
        DialogUtils.addComponent(pixelPanel, pixgbc, "height:", height);
        pixgbc.gridy++;
        DialogUtils.addComponent(pixelPanel, pixgbc, "Sub-sampling X:", subSamplingX);
        pixgbc.gridy++;
        DialogUtils.addComponent(pixelPanel, pixgbc, "Sub-sampling Y:", subSamplingY);
        DialogUtils.fillPanel(pixelPanel, pixgbc);

        geoPanel.add(worldMapUI.getWorlMapPane(), BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy++;
        contentPane.add(pixelPanel, gbc);
        geoPanel.setVisible(false);
        contentPane.add(geoPanel, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private class RadioListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().contains("pixelCoordRadio")) {
                pixelPanel.setVisible(true);
                geoPanel.setVisible(false);
                pixelCoords = true;
            } else {
                pixelPanel.setVisible(false);
                geoPanel.setVisible(true);
                pixelCoords = false;
            }
        }
    }

}