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

import org.esa.beam.dataio.geometry.VectorDataNodeReader2;
import org.esa.beam.framework.ui.ModalDialog;

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

    private VectorDataNodeReader2.CsvType type = VectorDataNodeReader2.CsvType.TRACK;

    TypeDialog(Window parent) {
        super(parent, "Interpret data as...", ModalDialog.ID_OK, "csvTypeDialog");
        createUI();
    }

    private void createUI() {
        getJDialog().setPreferredSize(new Dimension(255, 170));

        JPanel panel = new JPanel();
        BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layout);

        JRadioButton trackButton = createButton("track data", VectorDataNodeReader2.CsvType.TRACK);
        JRadioButton shapeButton = createButton("shape (ignores attributes)", VectorDataNodeReader2.CsvType.SHAPE);
        JRadioButton pointButton = createButton("point data", VectorDataNodeReader2.CsvType.POINTS);

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

    private JRadioButton createButton(String text, final VectorDataNodeReader2.CsvType type) {
        JRadioButton button = new JRadioButton(text);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TypeDialog.this.type = type;
            }
        });
        return button;
    }


    VectorDataNodeReader2.CsvType getType() {
        return type;
    }
}
