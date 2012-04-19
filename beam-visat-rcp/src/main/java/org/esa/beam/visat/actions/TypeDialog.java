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
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.PlacemarkDescriptorRegistry;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.FeatureUtils;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
class TypeDialog extends ModalDialog {

    private PlacemarkDescriptor placemarkDescriptor;

    TypeDialog(Window parent, SimpleFeatureType featureType) {
        super(parent, "Interpret data as...", ModalDialog.ID_OK, "csvTypeDialog");
        createUI(featureType);
    }

    private void createUI(SimpleFeatureType featureType) {
        getJDialog().setPreferredSize(new Dimension(255, 170));

        JPanel panel = new JPanel();
        BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layout);

        List<AbstractButton> buttons = new ArrayList<AbstractButton>();
        SortedSet<PlacemarkDescriptor> placemarkDescriptors = new TreeSet<PlacemarkDescriptor>(new Comparator<PlacemarkDescriptor>() {
            @Override
            public int compare(PlacemarkDescriptor o1, PlacemarkDescriptor o2) {
                return o1.getRoleLabel().compareTo(o2.getRoleLabel());
            }
        });
        placemarkDescriptors.addAll(PlacemarkDescriptorRegistry.getInstance().getValidPlacemarkDescriptors(featureType));
        placemarkDescriptors.add(PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(GeometryDescriptor.class));

        boolean first = true;
        for (PlacemarkDescriptor descriptor:placemarkDescriptors) {
            buttons.add(createButton(FeatureUtils.firstLetterUp(descriptor.getRoleLabel()), descriptor));
            if (first) {
                buttons.get(0).setSelected(true);
                placemarkDescriptor = descriptor;
                first = false;
            }
        }
        ButtonGroup buttonGroup = new ButtonGroup();
        for (AbstractButton button : buttons) {
            buttonGroup.add(button);
            panel.add(button);
        }

        setContent(panel);
    }

    private JRadioButton createButton(String text, final PlacemarkDescriptor pd) {
        JRadioButton button = new JRadioButton(text);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placemarkDescriptor = pd;
            }
        });
        return button;
    }

    public PlacemarkDescriptor getPlacemarkDescriptor() {
        return placemarkDescriptor;
    }
}
