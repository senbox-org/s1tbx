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

package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class FieldDesc implements ElementDesc {
    private final Field field;
    private final FieldDoc fieldDoc;

    FieldDesc(Field field, FieldDoc fieldDoc) {
        this.field = field;
        this.fieldDoc = fieldDoc;
    }

    public String getName() {
        return field.getName();
    }

    public Class<?> getType() {
        return field.getType();
    }

    public Field getField() {
        return field;
    }

    public FieldDoc getFieldDoc() {
        return fieldDoc;
    }

    public String getLongDescription() {
        return fieldDoc != null ? fieldDoc.commentText() : "";
    }
}