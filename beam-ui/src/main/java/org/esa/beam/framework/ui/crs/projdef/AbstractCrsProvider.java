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

package org.esa.beam.framework.ui.crs.projdef;

import org.esa.beam.framework.datamodel.GeoPos;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.GeodeticDatum;

public abstract class AbstractCrsProvider {

    private final String name;
    private final boolean hasParameters;
    private final boolean isDatumChangable;
    private final GeodeticDatum defaultDatum;

    AbstractCrsProvider(String name, boolean hasParameters,
                boolean datumChangable, GeodeticDatum defaultDatum) {
        this.name = name;
        this.hasParameters = hasParameters;
        isDatumChangable = datumChangable;
        this.defaultDatum = defaultDatum;
    }

    public String getName() {
        return name;
    }

    boolean hasParameters() {
        return hasParameters;
    }

    boolean isDatumChangable() {
        return isDatumChangable;
    }

    GeodeticDatum getDefaultDatum() {
        return defaultDatum;
    }

    abstract ParameterValueGroup getParameter();

    abstract CoordinateReferenceSystem getCRS(final GeoPos referencePos, ParameterValueGroup parameter,
                                              GeodeticDatum datum) throws FactoryException;
}
