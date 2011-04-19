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

package org.esa.beam.visat.dialogs;

import com.jidesoft.combobox.DateComboBox;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.param.AbstractParamEditor;
import org.esa.beam.framework.param.Parameter;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

public class DateEditor extends AbstractParamEditor {

    private DateComboBox dateComboBox;

    /**
     * Creates the object with a given parameter.
     *
     * @param parameter the <code>Parameter</code> to be edited.
     */
    public DateEditor(Parameter parameter) {
        super(parameter, false);
    }

    @Override
    public JComponent getEditorComponent() {
        return dateComboBox;
    }

    @Override
    protected void initUI() {
        setDefaultLabelComponent(true);

        dateComboBox = new DateComboBox();
        dateComboBox.setTimeDisplayed(true);
        dateComboBox.setFormat(ProductData.UTC.createDateFormat(ProductData.UTC.DATE_FORMAT_PATTERN + ".SSS"));
        dateComboBox.setTimeFormat("HH:mm:ss.SSS");
        dateComboBox.setShowNoneButton(true);
        dateComboBox.setShowTodayButton(false);
        dateComboBox.setShowOKButton(true);
        dateComboBox.setEditable(true);

        nameEditorComponent(dateComboBox);

        if (getParameter().getProperties().getDescription() != null) {
            dateComboBox.setToolTipText(getParameter().getProperties().getDescription());
        }

        dateComboBox.setEnabled(!getParameter().getProperties().isReadOnly());
        dateComboBox.setEditable(!getParameter().getProperties().isValueSetBound());

        dateComboBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                updateParameter();
            }
        });


    }

    @Override
    public void updateUI() {
        super.updateUI();
        dateComboBox.setDate((Date) getParameter().getValue());
    }

    private void updateParameter() {
        getParameter().setValue(dateComboBox.getDate(), null);
    }
}
