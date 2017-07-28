/*
 * Copyright (C) 2014 Catalysts GmbH (www.catalysts.cc)
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
 * This is not testing our implementation of Azimuthal_Equidistant projection but the one of geotools.
 * Our implementation is not in use anymore and has been removed in SNAP 6.
 *
 * Test data is taken from Proj.4: trac.osgeo.org/proj (version 4.4.6)
 */
public final class AzimuthalEquidistantTest extends AbstractProjectionTest {


    @Override
    protected ReferenceIdentifier getProjectionIdentifier() {
        return new NamedIdentifier(Citations.OGC, "Azimuthal_Equidistant");
    }

    @Override
    public MathTransform createMathTransform(MathTransformProvider provider) throws FactoryException {
        final ParameterGroup params = new ParameterGroup(provider.getParameters());
        params.parameter("semi_major").setValue(6370997.0);
        params.parameter("semi_minor").setValue(6370997.0);
        params.parameter("central_meridian").setValue(0.0);
        params.parameter("latitude_of_origin").setValue(0.0);
        params.parameter("false_easting").setValue(0.0);
        params.parameter("false_northing").setValue(0.0);
        return createParameterizedTransform(params);
    }

    @Override
    protected List<ProjTestData> createTestData() {
        List<ProjTestData> dataList = new ArrayList<>(13);

        dataList.add(new ProjTestData(0, 0, 0.00, 0.00));
        dataList.add(new ProjTestData(-180, 90, -0.00, 10007538.685621306));
        dataList.add(new ProjTestData(180, 90, 0.00, 10007538.685621306));
        dataList.add(new ProjTestData(180, -90, 0.00, -10007538.685621306));
        dataList.add(new ProjTestData(180.0, -87.5, 0.00, -10285525.87133301));
        dataList.add(new ProjTestData(180.0, -59.5, 0.00, -13398982.351304082));
        dataList.add(new ProjTestData(-173.0, -45.5, -1772868.478338835, -14803428.162628794));
        dataList.add(new ProjTestData(-75.0, -35.0, -7000586.400144643, -5074782.3858295875));
        dataList.add(new ProjTestData(-8.5, 45.5, -737084.3004029907, 5074524.19708281));
        dataList.add(new ProjTestData(5.5, -38.5, 516528.67517077894, -4286736.234169507));
        dataList.add(new ProjTestData(33.5, -28.0, 3410200.3550437223, -3285222.300525648));
        dataList.add(new ProjTestData(96.5, -21.0, 9964308.842777751, -3849686.1368150287));
        dataList.add(new ProjTestData(103.5, -7.0, 11406690.775134822, -1440362.8586200431));
        dataList.add(new ProjTestData(163.0, -63.0, 1896142.5998183272, -12728281.58890539));
        dataList.add(new ProjTestData(177.0, 87.5, 23501.876770090854, 10285117.806801353));

        return dataList;
    }

}
