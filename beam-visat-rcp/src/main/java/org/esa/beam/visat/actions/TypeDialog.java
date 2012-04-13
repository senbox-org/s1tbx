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

package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.GeometryDescriptor;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.TrackPlacemarkDescriptor;
import org.esa.beam.framework.ui.ModalDialog;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
class TypeDialog extends ModalDialog {

    private PlacemarkDescriptor placemarkDescriptor;

    TypeDialog(Window parent, CoordinateReferenceSystem modelCrs) {
        super(parent, "Interpret data as...", ModalDialog.ID_OK, "csvTypeDialog");
        placemarkDescriptor = new TrackPlacemarkDescriptor(modelCrs);
        createUI();
    }

    private void createUI() {
        getJDialog().setPreferredSize(new Dimension(255, 170));

        JPanel panel = new JPanel();
        BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layout);

        JRadioButton trackButton = createButton("track data", placemarkDescriptor);
        JRadioButton shapeButton = createButton("shape (ignores attributes)", new GeometryDescriptor());
        JRadioButton pointButton = createButton("point data", new PinDescriptor());

        trackButton.setSelected(true);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(trackButton);
        buttonGroup.add(shapeButton);
        buttonGroup.add(pointButton);

        panel.add(trackButton);
        panel.add(shapeButton);
        panel.add(pointButton);

        setContent(panel);
    }

    private JRadioButton createButton(String text, final PlacemarkDescriptor pd) {
        JRadioButton button = new JRadioButton(text);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TypeDialog.this.placemarkDescriptor = pd;
            }
        });
        return button;
    }

    public String getFeatureTypeName() {
        return placemarkDescriptor.getBaseFeatureType().getName().getLocalPart();
    }
}
