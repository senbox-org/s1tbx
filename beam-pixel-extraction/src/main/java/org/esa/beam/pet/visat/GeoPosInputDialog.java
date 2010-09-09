/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.pet.visat;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.ui.ModalDialog;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

class GeoPosInputDialog extends ModalDialog {

    private float lat;
    private float lon;

    private JTextField lonField;
    private JTextField latField;

    GeoPosInputDialog(Window parent, String title, int buttonMask, String helpID) {
        super(parent, title, buttonMask, helpID);
        TableLayout layout = new TableLayout(2);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(0.0);
        layout.setTableWeightY(0.0);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        JPanel dialogPanel = new JPanel(layout);

        dialogPanel.add(new JLabel("Latitude"));
        latField = new JTextField("00.0000");
        latField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                latField.selectAll();
            }
        });
        latField.selectAll();
        dialogPanel.add(latField);

        dialogPanel.add(new JLabel("Longitude"));
        lonField = new JTextField("00.0000");
        lonField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                lonField.selectAll();
            }
        });
        dialogPanel.add(lonField);

        this.setContent(dialogPanel);
    }

    public float getLat() {
        return lat;
    }

    public float getLon() {
        return lon;
    }

    @Override
    protected boolean verifyUserInput() {
        try {
            float lon = Float.parseFloat(lonField.getText());
            if (lon > 180 || lon < -180) {
                JOptionPane.showMessageDialog(this.getContent(),
                                              "Value of longitude must be between -180.0 and 180.0.");
                return false;
            }
        } catch (NumberFormatException ignored) {
            JOptionPane.showMessageDialog(this.getContent(), "Illegal number format for latitude.");
            return false;
        }
        try {
            float lat = Float.parseFloat(latField.getText());
            if (lat > 90 || lat < -90) {
                JOptionPane.showMessageDialog(this.getContent(), "Value of latitude must be between -90.0 and 90.0.");
                return false;
            }
        } catch (NumberFormatException ignored) {
            JOptionPane.showMessageDialog(this.getContent(), "Illegal number format for latitude.");
            return false;
        }
        return true;
    }

    @Override
    protected void onOK() {
        lon = Float.parseFloat(lonField.getText());
        lat = Float.parseFloat(latField.getText());
        super.onOK();
    }
}
