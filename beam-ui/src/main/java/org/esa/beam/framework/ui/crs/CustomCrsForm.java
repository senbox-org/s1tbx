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

package org.esa.beam.framework.ui.crs;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.crs.projdef.CustomCrsPanel;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.OperationMethod;

import javax.swing.JComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class CustomCrsForm extends CrsForm {

    public CustomCrsForm(AppContext appContext) {
        super(appContext);
    }

    @Override
    protected String getLabelText() {
        return "Custom CRS";
    }

    @Override
    boolean wrapAfterButton() {
        return true;
    }

    @Override
    public CoordinateReferenceSystem getCRS(GeoPos referencePos) throws FactoryException {
        return ((CustomCrsPanel)getCrsUI()).getCRS(referencePos);
    }

    @Override
    protected JComponent createCrsComponent() {
        final CustomCrsPanel panel = new CustomCrsPanel(getAppContext().getApplicationWindow());
        panel.addPropertyChangeListener("crs", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                fireCrsChanged();
            }
        });
        return panel;
    }

    @Override
    public void prepareShow() {
    }

    @Override
    public void prepareHide() {
    }

    public void setCustom(GeodeticDatum geodeticDatum, OperationMethod mapProjection, ParameterValueGroup parameterValues) {
        CustomCrsPanel customCrsPanel = (CustomCrsPanel) getCrsUI();
        customCrsPanel.setCustom(geodeticDatum, mapProjection, parameterValues);
    }
}
