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

package org.esa.snap.core.dataop.projection;

import org.geotools.metadata.iso.citation.Citations;
import org.geotools.parameter.ParameterGroup;
import org.geotools.referencing.NamedIdentifier;
import org.geotools.referencing.operation.MathTransformProvider;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.operation.MathTransform;

import java.util.ArrayList;
import java.util.List;

/**
 * This is not testing our implementation of Sinusoidal projection but the one of geotools.
 * Our implementation is not in use anymore and has been removed in SNAP 6.
 * <p>
 * Test data is taken from General Cartographic Transformation Package (GCTP).
 * It can be retrieved from: ftp://edcftp.cr.usgs.gov/pub/software/gctpc/
 */
public final class SinusoidalTest extends AbstractProjectionTest {


    @Override
    protected ReferenceIdentifier getProjectionIdentifier() {
        return new NamedIdentifier(Citations.OGC, "Sinusoidal");
    }

    @Override
    public MathTransform createMathTransform(MathTransformProvider provider) throws FactoryException {
        final ParameterGroup params = new ParameterGroup(provider.getParameters());
        params.parameter("semi_major").setValue(6370997.0);
        params.parameter("semi_minor").setValue(6370997.0);
        params.parameter("central_meridian").setValue(30.0);
        params.parameter("false_easting").setValue(0.0);
        params.parameter("false_northing").setValue(0.0);
        return createParameterizedTransform(params);
    }

    @Override
    protected List<ProjTestData> createTestData() {
        List<ProjTestData> dataList = new ArrayList<>(13);
        dataList.add(new ProjTestData(-180.0, -87.5, 727537.84417, -9729551.49991));
        dataList.add(new ProjTestData(-180.0, -59.5, 8465349.66961, -6616095.01994));
        dataList.add(new ProjTestData(-173.0, -45.5, 12236190.25202, -5059366.77995));
        dataList.add(new ProjTestData(-75.0, -35.0, -9563978.40140, -3891820.59996));
        dataList.add(new ProjTestData(-8.5, 45.5, -3000594.42486, 5059366.77995));
        dataList.add(new ProjTestData(5.5, -38.5, -2132039.38258, -4281002.65996));
        dataList.add(new ProjTestData(33.5, -28.0, 343627.36306, -3113456.47997));
        dataList.add(new ProjTestData(96.5, -21.0, 6903322.31757, -2335092.35998));
        dataList.add(new ProjTestData(103.5, -7.0, 8111904.27468, -778364.11999));
        dataList.add(new ProjTestData(163.0, -63.0, 6714028.40048, -7005277.07993));
        dataList.add(new ProjTestData(177.0, 87.5, 712987.08729, 9729551.49991));
        return dataList;
    }

}
