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
package org.esa.snap.core.gpf.internal;

import com.bc.ceres.binding.dom.DomElement;
import org.esa.snap.core.gpf.Operator;

import java.util.Set;

public class OperatorConfiguration {

    private final DomElement configuration;
    private final Set<Reference> referenceSet;

    public OperatorConfiguration(DomElement configuration,
                                 Set<Reference> references) {
        this.configuration = configuration;
        this.referenceSet = references;
    }

    public DomElement getConfiguration() {
        return configuration;
    }

    public Set<Reference> getReferenceSet() {
        return referenceSet;
    }

    public static interface Reference {
        public Object getValue();

        public String getParameterName();
    }

    public static class PropertyReference implements Reference {
        final String parameterName;
        final String propertyName;
        final Operator operator;

        public PropertyReference(String parameterName, String propertyName,
                                 Operator operator) {
            this.parameterName = parameterName;
            this.propertyName = propertyName;
            this.operator = operator;
        }

        @Override
        public Object getValue() {
            return operator.getTargetProperty(propertyName);
        }

        @Override
        public String getParameterName() {
            return parameterName;
        }
    }

    public static class ParameterReference implements Reference {

        private final String name;
        private final Object value;

        public ParameterReference(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getParameterName() {
            return name;
        }

        @Override
        public Object getValue() {
            return value;
        }

    }
}
