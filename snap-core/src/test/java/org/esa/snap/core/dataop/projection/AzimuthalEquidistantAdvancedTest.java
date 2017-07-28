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

public final class AzimuthalEquidistantAdvancedTest extends AbstractProjectionTest {

    @Override
    protected ReferenceIdentifier getProjectionIdentifier() {
        return new NamedIdentifier(Citations.OGC, "Azimuthal_Equidistant");
    }


    @Override
    public MathTransform createMathTransform(MathTransformProvider provider) throws FactoryException {
        final ParameterGroup params = new ParameterGroup(provider.getParameters());
        params.parameter("semi_major").setValue(6370997.0);
        params.parameter("semi_minor").setValue(6370997.0);
        params.parameter("central_meridian").setValue(-35.2);
        params.parameter("latitude_of_origin").setValue(48.3);
        params.parameter("false_easting").setValue(0.0);
        params.parameter("false_northing").setValue(0.0);
        return createParameterizedTransform(params);
    }

    @Override
    protected List<ProjTestData> createTestData() {
        List<ProjTestData> dataList = new ArrayList<ProjTestData>(13);

        dataList.add(new ProjTestData(0, 0, 4358228.03, -4612866.6308653075));
        dataList.add(new ProjTestData(-173.0, -45.5, -16555530.351841368, -3051978.478605203));
        dataList.add(new ProjTestData(-75.0, -35.0, -5279729.168524442, -8573380.87697871));
        dataList.add(new ProjTestData(-8.5, 45.5, 2041188.65, 45056.607069453006));
        dataList.add(new ProjTestData(5.5, -38.5, 5348477.572219586, -8982775.385749478));
        dataList.add(new ProjTestData(33.5, -28.0, 9039137.527991086, -6062949.11973716));
        dataList.add(new ProjTestData(96.5, -21.0, 14061438.429027915, 4544937.777912626));
        dataList.add(new ProjTestData(103.5, -7.0, 11332231.415172944, 8228595.226849682));
        dataList.add(new ProjTestData(163.0, -63.0, -8368730.214692355, -15977388.159654297));
        dataList.add(new ProjTestData(177.0, 87.5, -163581.15074707987, 4871147.2753199255));

        return dataList;
    }
}
