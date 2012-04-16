/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.visat.toolviews.placemark;

import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PlacemarkManagerButtons extends JPanel {

    private AbstractButton newButton;
    private AbstractButton copyButton;
    private AbstractButton editButton;
    private AbstractButton removeButton;
    private AbstractButton importButton;
    private AbstractButton exportButton;
    private AbstractButton filterButton;
    private AbstractButton exportTableButton;
    private AbstractButton zoomToPlacemarkButton;

    public PlacemarkManagerButtons(final PlacemarkManagerToolView view) {
        super(new GridBagLayout());

        newButton = createButton("icons/New24.gif");
        newButton.setName("newButton");
        final String placemarkLabel = view.getPlacemarkDescriptor().getRoleLabel();
        newButton.setToolTipText("Create and add new " + placemarkLabel + "."); /*I18N*/
        newButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                view.newPin();
            }
        });

        copyButton = createButton("icons/Copy24.gif");
        copyButton.setName("copyButton");
        copyButton.setToolTipText("Copy an existing " + placemarkLabel + "."); /*I18N*/
        copyButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                view.copyActivePlacemark();
            }
        });

        editButton = createButton("icons/Edit24.gif");
        editButton.setName("editButton");
        editButton.setToolTipText("Edit selected " + placemarkLabel + "."); /*I18N*/
        editButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                view.editActivePin();
            }
        });

        removeButton = createButton("icons/Remove24.gif");
        removeButton.setName("removeButton");
        removeButton.setToolTipText("Remove selected " + placemarkLabel + "."); /*I18N*/
        removeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                view.removeSelectedPins();
            }
        });

        importButton = createButton("icons/Import24.gif");
        importButton.setName("importButton");
        importButton.setToolTipText("Import all " + placemarkLabel + "s from XML or text file."); /*I18N*/
        importButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                view.importPlacemarks(true);
                view.updateUIState();
            }
        });

        exportButton = createButton("icons/Export24.gif");
        exportButton.setName("exportButton");
        exportButton.setToolTipText("Export selected " + placemarkLabel + "s to XML file."); /*I18N*/
        exportButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                view.exportPlacemarks();
                view.updateUIState();
            }
        });

        filterButton = createButton("icons/Filter24.gif");
        filterButton.setName("filterButton");
        filterButton.setToolTipText("Filter pixel data to be displayed in table."); /*I18N*/
        filterButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                view.applyFilteredGrids();
                view.updateUIState();
            }
        });

        exportTableButton = createButton("icons/ExportTable.gif");
        exportTableButton.setName("exportTableButton");
        exportTableButton.setToolTipText("Export selected data to flat text file."); /*I18N*/
        exportTableButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                view.exportPlacemarkDataTable();
                view.updateUIState();
            }
        });

        zoomToPlacemarkButton = createButton("icons/ZoomTo24.gif");
        zoomToPlacemarkButton.setName("zoomToButton");
        zoomToPlacemarkButton.setToolTipText("Zoom to selected " + placemarkLabel + "."); /*I18N*/
        zoomToPlacemarkButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                view.zoomToActivePin();
            }
        });

        final AbstractButton helpButton = createButton("icons/Help22.png");
        helpButton.setName("helpButton");
        final String helpId = view.getDescriptor().getHelpId();
        if (helpId != null) {
            HelpSys.enableHelpOnButton(helpButton, helpId);
        }

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.5;
        gbc.gridy++;
        add(newButton, gbc);
        add(copyButton, gbc);
        gbc.gridy++;
        add(editButton, gbc);
        add(removeButton, gbc);
        gbc.gridy++;
        add(importButton, gbc);
        add(exportButton, gbc);
        gbc.gridy++;
        add(filterButton, gbc);
        add(exportTableButton, gbc);
        gbc.gridy++;
        add(zoomToPlacemarkButton, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.gridwidth = 1;
        add(helpButton, gbc);
    }

    void updateUIState(final boolean productSelected, int numPins, final int numSelectedPins) {

        boolean hasSelectedPins = numSelectedPins > 0;
        boolean hasActivePin = numSelectedPins == 1;

        newButton.setEnabled(productSelected);
        copyButton.setEnabled(hasActivePin);
        editButton.setEnabled(hasActivePin);
        removeButton.setEnabled(hasSelectedPins);
        zoomToPlacemarkButton.setEnabled(hasActivePin);
        importButton.setEnabled(productSelected);
        exportButton.setEnabled(numPins > 0);
        exportTableButton.setEnabled(hasSelectedPins);
        filterButton.setEnabled(productSelected);
    }

    private static AbstractButton createButton(String path) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), false);
    }
}
