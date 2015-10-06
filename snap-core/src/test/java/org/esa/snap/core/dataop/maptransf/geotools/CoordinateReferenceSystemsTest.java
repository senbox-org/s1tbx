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

package org.esa.snap.core.dataop.maptransf.geotools;

import junit.framework.TestCase;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.snap.core.dataop.maptransf.MapProjection;
import org.esa.snap.core.dataop.maptransf.MapProjectionRegistry;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Projection;
import org.opengis.util.GenericName;

import java.util.Set;

public class CoordinateReferenceSystemsTest extends TestCase {

    public void testCoordinateReferenceSystems() throws FactoryException {
        final MapProjection gp = MapProjectionRegistry.getProjection(new IdentityTransformDescriptor().getName());

        assertSame(DefaultGeographicCRS.WGS84,
                   CoordinateReferenceSystems.getCRS(gp, Datum.WGS_84));

        for (final MapProjection projection : MapProjectionRegistry.getProjections()) {
            if (!(projection.getMapTransform().getDescriptor() instanceof IdentityTransformDescriptor)) {
                assertTrue(CoordinateReferenceSystems.getCRS(projection, Datum.ITRF_97) instanceof ProjectedCRS);
                assertTrue(CoordinateReferenceSystems.getCRS(projection, Datum.WGS_72) instanceof ProjectedCRS);
                assertTrue(CoordinateReferenceSystems.getCRS(projection, Datum.WGS_84) instanceof ProjectedCRS);
            }
        }
    }

    public void testProjectionNamesAndAliases() {
        final DefaultMathTransformFactory mtf = new DefaultMathTransformFactory();
        Set<OperationMethod> methods = mtf.getAvailableMethods(Projection.class);
        for (OperationMethod method : methods) {
            //System.out.println("method.getName() = " + method.getName());
            for (final GenericName name : method.getAlias()) {
                if (name.toString().startsWith("EPSG")) {
                    //System.out.println("method.getAlias() = " + name);
                }
            }
        }
    }

}
