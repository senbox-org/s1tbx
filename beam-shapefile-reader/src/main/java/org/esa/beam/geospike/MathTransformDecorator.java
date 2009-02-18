/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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

import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.referencing.operation.transform.MathTransformProxy;
import org.geotools.referencing.wkt.Formatter;
import org.geotools.resources.Formattable;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform;

/**
 * todo - add API doc
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
class MathTransformDecorator extends MathTransformProxy {

    
    public MathTransformDecorator(MathTransform mathTransform) {
        super(mathTransform);
    }

    public ParameterDescriptorGroup getParameterDescriptors() {
        return new DefaultParameterDescriptorGroup(getClass().getSimpleName(), new GeneralParameterDescriptor[0]);
    }
}
