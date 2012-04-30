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
import org.esa.beam.framework.datamodel.PointDescriptor;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.VisatApp;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 * @author Norman Fomferra
 */
class TypeDialog extends ModalDialog {
    private InterpretationMethod interpretationMethod;
    private PlacemarkDescriptor placemarkDescriptor;

    TypeDialog(Window parent, SimpleFeatureType featureType) {
        super(parent, "Point Data Interpretation", ModalDialog.ID_OK_CANCEL_HELP, "importCSV");
        createUI(featureType);
    }

    private void createUI(SimpleFeatureType featureType) {
        getJDialog().setPreferredSize(new Dimension(400, 250));

        JPanel panel = new JPanel();
        BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layout);

        panel.add(new JLabel("<html>" + VisatApp.getApp().getApplicationName() + " can interpret the imported point data in various ways.<br>" +
                                     "Please select:<br><br></html>"));

        List<AbstractButton> buttons = new ArrayList<AbstractButton>();

        placemarkDescriptor = PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(GeometryDescriptor.class);
        interpretationMethod = InterpretationMethod.LEAVE_UNCHANGED;

        buttons.add(createButton("<html>Leave imported data <b>unchanged</b></html>.", true, InterpretationMethod.LEAVE_UNCHANGED, placemarkDescriptor));
        buttons.add(createButton("<html>Interpret each point as vertex of a single <b>line or polygon</b><br>" +
                                         "(This will remove all attributes from points)</html>", false, InterpretationMethod.CONVERT_TO_SHAPE,
                                 PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(PointDescriptor.class)));

        SortedSet<PlacemarkDescriptor> placemarkDescriptors = new TreeSet<PlacemarkDescriptor>(new Comparator<PlacemarkDescriptor>() {
            @Override
            public int compare(PlacemarkDescriptor o1, PlacemarkDescriptor o2) {
                return o1.getRoleLabel().compareTo(o2.getRoleLabel());
            }
        });

        placemarkDescriptors.addAll(PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptors(featureType));
        placemarkDescriptors.remove(PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(GeometryDescriptor.class));
        placemarkDescriptors.remove(PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(PointDescriptor.class));

        for (PlacemarkDescriptor descriptor : placemarkDescriptors) {
            buttons.add(createButton("<html>Interpret each point as <b>" + descriptor.getRoleLabel() + "</br></html>",
                                     false,
                                     InterpretationMethod.APPLY_DESCRIPTOR,
                                     descriptor));
        }
        ButtonGroup buttonGroup = new ButtonGroup();
        for (AbstractButton button : buttons) {
            buttonGroup.add(button);
            panel.add(button);
        }

        setContent(panel);
    }

    private JRadioButton createButton(String text, boolean selected, final InterpretationMethod im, final PlacemarkDescriptor pd) {
        JRadioButton button = new JRadioButton(text, selected);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                interpretationMethod = im;
                placemarkDescriptor = pd;
            }
        });
        return button;
    }

    public InterpretationMethod getInterpretationMethod() {
        return interpretationMethod;
    }

    public PlacemarkDescriptor getPlacemarkDescriptor() {
        return placemarkDescriptor;
    }

    public enum InterpretationMethod {
        LEAVE_UNCHANGED,
        CONVERT_TO_SHAPE,
        APPLY_DESCRIPTOR
    }

    @Override
    protected void onOK() {
        super.onOK();
        getParent().setVisible(true);    // todo: Visat main window disappears otherwise, find better solution
    }

    @Override
    protected void onCancel() {
        super.onCancel();
        getParent().setVisible(true);
    }

    @Override
    protected void onHelp() {
        // todo
        super.onHelp();
    }
}
