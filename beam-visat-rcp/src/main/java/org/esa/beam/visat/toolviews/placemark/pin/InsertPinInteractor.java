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

import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.visat.toolviews.placemark.InsertPlacemarkInteractor;

/**
 * A tool used to create (single click), select (single click on a pin) or edit (double click on a pin) the pins
 * displayed in product scene view.
 */
public class InsertPinInteractor extends InsertPlacemarkInteractor {

    public InsertPinInteractor() {
        super(PinDescriptor.getInstance());
    }
}