/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.smos.visat;

import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class SnapshotSelector {
    private final JSpinner spinner;
    private final JSlider slider;
    private final JTextField sliderTextField;

    private SnapshotSelectorModel model;

    SnapshotSelector() {
        spinner = new JSpinner();
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(8);
        
        slider = new JSlider();
        sliderTextField = new JTextField(10);
        sliderTextField.setEditable(false);

        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateSliderTextField();
            }
        });
    }

    SnapshotSelector(SnapshotSelectorModel model) {
        this();
        setModel(model);
    }

    SnapshotSelectorModel getModel() {
        return model;
    }

    void setModel(SnapshotSelectorModel model) {
        if (this.model != model) {
            this.model = model;
            spinner.setModel(model.getSpinnerModel());
            slider.setModel(model.getSliderModel());
            updateSliderTextField();
        }
    }

    JSpinner getSpinner() {
        return spinner;
    }

    JSlider getSlider() {
        return slider;
    }

    JTextField getSliderTextField() {
        return sliderTextField;
    }

    private void updateSliderTextField() {
        if (model != null) {
            sliderTextField.setText(model.getSliderInfo());
        } else {
            sliderTextField.setText("");
        }
    }
}
