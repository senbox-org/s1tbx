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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.*;
import java.util.Map;

/**
 * Default OperatorUI for operators using @parameter
 */
public class DefaultUI extends BaseOperatorUI {

    @Override
    public JComponent CreateOpTab(final String operatorName,
                                  final Map<String, Object> parameterMap, final AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final BindingContext context = new BindingContext(valueContainer);

        initParameters();

        final PropertyPane parametersPane = new PropertyPane(context);
        return new JScrollPane(parametersPane.createPanel());
    }

    @Override
    public void initParameters() {
        updateSourceBands();
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

    }

    private void updateSourceBands() {
        if (valueContainer == null) return;

        final Property[] properties = valueContainer.getProperties();
        for (Property p : properties) {
            final PropertyDescriptor descriptor = p.getDescriptor();
            final String itemAlias = descriptor.getItemAlias();

            if (sourceProducts != null && itemAlias != null && itemAlias.equals("band")) {
                final String[] bandNames = getBandNames();
                if (bandNames.length > 0) {
                    final ValueSet valueSet = new ValueSet(bandNames);
                    descriptor.setValueSet(valueSet);

                    try {
                        if (descriptor.getType().isArray()) {
                            if (p.getValue() == null)
                                p.setValue(bandNames);//new String[] {bandNames[0]});
                        } else {
                            p.setValue(bandNames[0]);
                        }
                    } catch (ValidationException e) {
                        System.out.println(e.toString());
                    }
                }
            }
        }
    }
}
