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
package org.esa.beam.visat.toolviews.placemark.pin;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.visat.toolviews.placemark.PlacemarkManagerToolView;
import org.esa.beam.visat.toolviews.placemark.TableModelFactory;

/**
 * A dialog used to manage the list of pins associated with a selected product.
 */
public class PinManagerToolView extends PlacemarkManagerToolView {

    public static final String ID = PinManagerToolView.class.getName();

    public PinManagerToolView() {
        super(PinDescriptor.INSTANCE, new TableModelFactory() {
            @Override
            public PinTableModel createTableModel(PlacemarkDescriptor placemarkDescriptor, Product product,
                                                  Band[] selectedBands, TiePointGrid[] selectedGrids) {
                return new PinTableModel(placemarkDescriptor, product, selectedBands, selectedGrids);
            }
        });
    }
}
