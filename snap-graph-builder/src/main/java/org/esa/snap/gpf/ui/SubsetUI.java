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
package org.esa.snap.gpf.ui;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.io.WKTReader;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.visat.VisatApp;
import org.esa.snap.gpf.ui.worldmap.WorldMapUI;
import org.esa.snap.gpf.ui.worldmap.NestWorldMapPane;
import org.esa.snap.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * User interface for Subset
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
    private final JPanel geoPanel = new JPanel(new BorderLayout());

    private final WorldMapUI worldMapUI = new WorldMapUI();
    private final JTextField geoText = new JTextField("");
    private final JButton geoUpdateButton = new JButton("Update");
    private Geometry geoRegion = null;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();

        initParameters();

        geoText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGeoRegion();
            }
        });
        geoUpdateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGeoRegion();
            }
        });

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        OperatorUIUtils.initParamList(bandList, getBandNames());

        final Rectangle region = (Rectangle)paramMap.get("region");
        if(region != null) {
            regionX.setText(String.valueOf(region.x));
            regionY.setText(String.valueOf(region.y));
            width.setText(String.valueOf(region.width));
            height.setText(String.valueOf(region.height));
        }
        if (sourceProducts != null && sourceProducts.length > 0) {
            if (region == null || region.width == 0)
                width.setText(String.valueOf(sourceProducts[0].getSceneRasterWidth()));
            if (region == null || region.height == 0)
                height.setText(String.valueOf(sourceProducts[0].getSceneRasterHeight()));

            worldMapUI.getModel().setAutoZoomEnabled(true);
            worldMapUI.getModel().setProducts(sourceProducts);
            worldMapUI.getModel().setSelectedProduct(sourceProducts[0]);
            worldMapUI.getWorlMapPane().zoomToProduct(sourceProducts[0]);
        }

        subSamplingX.setText(String.valueOf(paramMap.get("subSamplingX")));
        subSamplingY.setText(String.valueOf(paramMap.get("subSamplingY")));

        geoRegion = (Geometry) paramMap.get("geoRegion");
        if (geoRegion != null) {

            final Coordinate coord[] = geoRegion.getCoordinates();
            worldMapUI.setSelectionStart((float) coord[0].y, (float) coord[0].x);
            worldMapUI.setSelectionEnd((float) coord[2].y, (float) coord[2].x);

            getGeoRegion();

            geoCoordRadio.setSelected(true);
            pixelPanel.setVisible(false);
            geoPanel.setVisible(true);
        }
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(bandList, paramMap, "bandNames");

        int x=0, y=0, w=0, h=0;
        final String regionXStr = regionX.getText();
        if (regionXStr != null && !regionXStr.isEmpty())
            x = Integer.parseInt(regionXStr);
        final String regionYStr = regionY.getText();
        if (regionYStr != null && !regionYStr.isEmpty())
            y = Integer.parseInt(regionYStr);
        final String widthStr = width.getText();
        if (widthStr != null && !widthStr.isEmpty())
            w = Integer.parseInt(widthStr);
        final String heightStr = height.getText();
        if (heightStr != null && !heightStr.isEmpty())
            h = Integer.parseInt(heightStr);

        paramMap.put("region", new Rectangle(x,y,w,h));

        final String subSamplingXStr = subSamplingX.getText();
        if (subSamplingXStr != null && !subSamplingXStr.isEmpty())
            paramMap.put("subSamplingX", Integer.parseInt(subSamplingXStr));
        final String subSamplingYStr = subSamplingY.getText();
        if (subSamplingYStr != null && !subSamplingYStr.isEmpty())
            paramMap.put("subSamplingY", Integer.parseInt(subSamplingYStr));

        getGeoRegion();
        if (geoCoordRadio.isSelected() && geoRegion != null) {
            paramMap.put("geoRegion", geoRegion);
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
        pixgbc.gridwidth = 1;
        pixgbc.fill = GridBagConstraints.BOTH;
        addComponent(pixelPanel, pixgbc, "X:", regionX, 0);
        addComponent(pixelPanel, pixgbc, "Y:", regionY, 2);
        pixgbc.gridy++;
        addComponent(pixelPanel, pixgbc, "Width:", width, 0);
        addComponent(pixelPanel, pixgbc, "height:", height, 2);
        pixgbc.gridy++;
        addComponent(pixelPanel, pixgbc, "Sub-sampling X:", subSamplingX, 0);
        addComponent(pixelPanel, pixgbc, "Sub-sampling Y:", subSamplingY, 2);
        pixelPanel.add(new JPanel(), pixgbc);

        final NestWorldMapPane worldPane = worldMapUI.getWorlMapPane();
        worldPane.setPreferredSize(new Dimension(500, 130));

        final JPanel geoTextPanel = new JPanel(new BorderLayout());
        geoText.setColumns(45);
        geoTextPanel.add(geoText, BorderLayout.CENTER);
        geoTextPanel.add(geoUpdateButton, BorderLayout.EAST);

        geoPanel.add(worldPane, BorderLayout.CENTER);
        geoPanel.add(geoTextPanel, BorderLayout.SOUTH);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy++;
        contentPane.add(pixelPanel, gbc);
        geoPanel.setVisible(false);
        contentPane.add(geoPanel, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    public static JLabel addComponent(JPanel contentPane, GridBagConstraints gbc, String text, JComponent component, int pos) {
        gbc.gridx = pos;
        gbc.weightx = 0.5;
        final JLabel label = new JLabel(text);
        contentPane.add(label, gbc);
        gbc.gridx = pos+1;
        gbc.weightx = 2.0;
        contentPane.add(component, gbc);
        gbc.gridx = pos;
        gbc.weightx = 1.0;
        return label;
    }

    private class RadioListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().contains("pixelCoordRadio")) {
                pixelPanel.setVisible(true);
                geoPanel.setVisible(false);

                //reset geoRegion
                geoRegion = null;
                paramMap.put("geoRegion", geoRegion);
            } else {
                pixelPanel.setVisible(false);
                geoPanel.setVisible(true);
            }
        }
    }

    private void getGeoRegion() {
        geoRegion = null;
        geoText.setText("");
        if (geoCoordRadio.isSelected()) {
            final GeoPos[] selectionBox = worldMapUI.getSelectionBox();
            if (selectionBox != null) {
                final Coordinate[] coords = new Coordinate[selectionBox.length + 1];
                for (int i = 0; i < selectionBox.length; ++i) {
                    coords[i] = new Coordinate(selectionBox[i].getLon(), selectionBox[i].getLat());
                }
                coords[selectionBox.length] = new Coordinate(selectionBox[0].getLon(), selectionBox[0].getLat());

                final GeometryFactory geometryFactory = new GeometryFactory();
                final LinearRing linearRing = geometryFactory.createLinearRing(coords);

                geoRegion = geometryFactory.createPolygon(linearRing, null);
                geoText.setText(geoRegion.toText());
            }
        }
    }

    private void updateGeoRegion() {
        try {
            geoRegion = new WKTReader().read(geoText.getText());

            final Coordinate coord[] = geoRegion.getCoordinates();
            worldMapUI.setSelectionStart((float) coord[0].y, (float) coord[0].x);
            worldMapUI.setSelectionEnd((float) coord[2].y, (float) coord[2].x);
            worldMapUI.getWorlMapPane().revalidate();
            worldMapUI.getWorlMapPane().getLayerCanvas().updateUI();
        } catch (Exception e) {
            VisatApp.getApp().showErrorDialog(e.getMessage());
        }
    }
}