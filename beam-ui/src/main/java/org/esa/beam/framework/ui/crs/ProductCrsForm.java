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

package org.esa.beam.framework.ui.crs;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JComponent;
import javax.swing.JTextField;

public class ProductCrsForm extends CrsForm {

    private final Product product;

    public ProductCrsForm(AppContext appContext, Product product) {
        super(appContext);
        this.product = product;
    }

    @Override
    protected String getLabelText() {
        return "Use target CRS";
    }

    @Override
    public CoordinateReferenceSystem getCRS(GeoPos referencePos) throws FactoryException {
        return getMapCrs();
    }

    private CoordinateReferenceSystem getMapCrs() {
        return product.getGeoCoding().getMapCRS();
    }

    @Override
    protected JComponent createCrsComponent() {
        final JTextField field = new JTextField();
        field.setEditable(false);
        field.setText(getMapCrs().getName().getCode());
        return field;
    }

    @Override
    public void prepareShow() {
    }

    @Override
    public void prepareHide() {
    }
}
