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

import org.esa.snap.core.util.SystemUtils;
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
 * This is not testing our implementation of Mollweide projection but the one of geotools.
 * Our implementation is not in use anymore and has been removed in SNAP 6.
 *
 * Test data is taken from General Cartographic Transformation Package (GCTP).
 * It can be retrieved from: ftp://edcftp.cr.usgs.gov/pub/software/gctpc/
 */
public final class MollweideTest extends AbstractProjectionTest {

    @Override
    public void setUp() {
        super.setUp();
        SystemUtils.initGeoTools();
    }

    @Override
    protected ReferenceIdentifier getProjectionIdentifier() {
        return new NamedIdentifier(Citations.OGC, "Mollweide");
    }

    @Override
    public MathTransform createMathTransform(MathTransformProvider provider) throws FactoryException {
        final ParameterGroup params = new ParameterGroup(provider.getParameters());
        params.parameter("semi_major").setValue(6378206.400);
        params.parameter("semi_minor").setValue(6378206.400);
        params.parameter("central_meridian").setValue(-100.000);
        params.parameter("false_easting").setValue(0.0);
        params.parameter("false_northing").setValue(0.0);
        return createParameterizedTransform(params);
    }


    @Override
    protected List<ProjTestData> createTestData() {
        List<ProjTestData> dataList = new ArrayList<>(13);
        dataList.add(new ProjTestData(-180.0, -90, 0.0, -9020145.99445, -100.0, -90.0));
        dataList.add(new ProjTestData(-180.0, -75.0, -3392851.08440, -8172751.64401, 180, -75));
        dataList.add(new ProjTestData(-165.0, 0.0, -6514549.88492, 0.00000));
        dataList.add(new ProjTestData(-130.0, -70.0, -1524588.86019, -7774554.20128));
        dataList.add(new ProjTestData(-30.0, 50.0, 5324490.23820, 5873535.86499));
        dataList.add(new ProjTestData(0.0, 0.0, 10022384.43834, 0.00000));
        dataList.add(new ProjTestData(20.0, -45.0, 9692535.26606, -5340303.01832));
        dataList.add(new ProjTestData(70.0, 20.0, 16395601.15334, 2453612.57967));
        dataList.add(new ProjTestData(90.0, -90.0, 0.0, -9020145.99445, -100.00000, -90.00000));
        dataList.add(new ProjTestData(155.0, -55.0, -7431377.50698, -6386649.45996));
        dataList.add(new ProjTestData(180.0, 65.0, -4659298.68057, 7340810.54044));
        dataList.add(new ProjTestData(180.0, 90.0, 0.0, 9020145.99445, -100.00000, 90.00000));
        return dataList;
    }

}
