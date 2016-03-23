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

package org.esa.snap.core.util.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.converters.ClassConverter;

public class JavaTypeConverter extends ClassConverter {

    public JavaTypeConverter() {
        super();
        addPackageQualifier("com.vividsolutions.jts.geom.");
    }

    @Override
    public Class<Class> getValueType() {
        return Class.class;
    }

    @Override
    public Class parse(String text) throws ConversionException {
        if(text.isEmpty()) {
            throw new ConversionException(text);

        }
        return super.parse(text);
    }

    @Override
    public String format(Class javaType) {
        return super.format(javaType);
    }
}
