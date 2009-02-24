/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.geospike;

import org.esa.beam.framework.datamodel.Band;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

/**
 * Utility class for defining a {@link org.geotools.coverage.Category} with
 * a sample to geophysical transform for scaled {@link Band}s.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
class SampleToGeophysicalTransform extends BandScaling1D {
    private volatile MathTransform1D inverseTransform;

    SampleToGeophysicalTransform(Band band) {
        super(band);
    }

    @Override
    public final double transform(double value) throws TransformException {
        return getBand().scale(value);
    }

    @Override
    public final double derivative(double value) throws TransformException {
        // todo - derivative for log-scaled bands
        return getBand().getScalingFactor();
    }

    @Override
    public final MathTransform1D inverse() throws NoninvertibleTransformException {
        if (inverseTransform == null) {
            synchronized (this) {
                if (inverseTransform == null) {
                    inverseTransform = createInverseTransform();
                }
            }
        }
        return inverseTransform;
    }

    private MathTransform1D createInverseTransform() {
        return new BandScaling1D(getBand()) {
            @Override
            public double transform(double value) throws TransformException {
                return getBand().scaleInverse(value);
            }

            @Override
            public double derivative(double value) throws TransformException {
                // todo - derivative for log-scaled bands
                return 1.0 / getBand().getScalingFactor();
            }

            @Override
            public BandScaling1D inverse() throws NoninvertibleTransformException {
                return SampleToGeophysicalTransform.this;
            }
        };
    }
}
