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

package org.esa.snap.core.gpf.doclet;

import com.sun.javadoc.FieldDoc;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;

public class ParameterDesc extends FieldDesc {
    private final Parameter annotation;

    ParameterDesc(Field field, FieldDoc fieldDoc, Parameter annotation) {
        super(field, fieldDoc);
        this.annotation = annotation;
    }

    @Override
    public String getName() {
        if (StringUtils.isNotNullAndNotEmpty(annotation.alias())) {
            return annotation.alias();
        }
        return super.getName();
    }

    @Override
    public String getShortDescription() {
        return annotation.description();
    }

    public String getDefaultValue() {
        return annotation.defaultValue();
    }

    public String getConstraints() {
        String format = annotation.format();
        boolean notNull = annotation.notNull();
        boolean notEmpty = annotation.notEmpty();
        String[] valueSet = annotation.valueSet();
        String interval = annotation.interval();
        String pattern = annotation.pattern();

        StringBuilder sb = new StringBuilder(64);
        if (notNull) {
            appendItem(sb, "not null");
        }
        if (notEmpty) {
            appendItem(sb, "non empty");
        }
        if (StringUtils.isNotNullAndNotEmpty(format)) {
            appendItem(sb, "format: " + format);
        }
        if (StringUtils.isNotNullAndNotEmpty(pattern)) {
            appendItem(sb, "pattern: " + pattern);
        }
        if (valueSet != null && valueSet.length > 0) {
            appendItem(sb, "value set: " + Arrays.toString(valueSet));
        }
        if (StringUtils.isNotNullAndNotEmpty(interval)) {
            appendItem(sb, "interval: " + interval);
        }
        return sb.toString();
    }

    private static void appendItem(StringBuilder sb, String str) {
        sb.append(sb.length() > 0 ? "; " : "");
        sb.append(str);
    }
}
