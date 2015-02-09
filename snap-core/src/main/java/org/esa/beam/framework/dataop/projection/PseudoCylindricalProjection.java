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

package org.esa.beam.framework.dataop.projection;

import org.geotools.referencing.operation.DefaultProjection;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CylindricalProjection;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationMethod;

import java.util.Map;


/**
 * Base class for cylindrical map projections.
 *
 */
public class PseudoCylindricalProjection extends DefaultProjection implements CylindricalProjection {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -969486613826553580L;

    /**
     * Constructs a new projection with the same values than the specified one, together with the
     * specified source and target CRS. While the source conversion can be an arbitrary one, it is
     * typically a {@linkplain org.geotools.referencing.operation.DefiningConversion defining conversion}.
     *
     * @param conversion The defining conversion.
     * @param sourceCRS The source CRS.
     * @param targetCRS The target CRS.
     * @param transform Transform from positions in the {@linkplain #getSourceCRS source CRS}
     *                  to positions in the {@linkplain #getTargetCRS target CRS}.
     */
    public PseudoCylindricalProjection(final Conversion                conversion,
                                        final CoordinateReferenceSystem sourceCRS,
                                        final CoordinateReferenceSystem targetCRS,
                                        final MathTransform             transform) {
        super(conversion, sourceCRS, targetCRS, transform);
    }

    /**
     * Constructs a projection from a set of properties. The properties given in argument
     * follow the same rules than for the {@link org.geotools.referencing.operation.AbstractCoordinateOperation} constructor.
     *
     * @param properties Set of properties. Should contains at least {@code "name"}.
     * @param sourceCRS The source CRS, or {@code null} if not available.
     * @param targetCRS The target CRS, or {@code null} if not available.
     * @param transform Transform from positions in the {@linkplain #getSourceCRS source coordinate
     *                  reference system} to positions in the {@linkplain #getTargetCRS target
     *                  coordinate reference system}.
     * @param method    The operation method.
     */
    public PseudoCylindricalProjection(final Map<String,?>             properties,
                                        final CoordinateReferenceSystem sourceCRS,
                                        final CoordinateReferenceSystem targetCRS,
                                        final MathTransform             transform,
                                        final OperationMethod           method) {
        super(properties, sourceCRS, targetCRS, transform, method);
    }
}