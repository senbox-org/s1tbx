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

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.annotations.SourceProduct;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class OperatorDesc  implements ElementDesc {
    private final Class<? extends Operator> type;
    private final ClassDoc classDoc;

    private final OperatorMetadata annotation;

    private final TargetProductDesc targetProduct;

    private final ArrayList<SourceProductDesc> sourceProductList;

    private SourceProductsDesc sourceProducts;

    private final ArrayList<ParameterDesc> parameters;

    OperatorDesc(Class<? extends Operator> type, ClassDoc classDoc, OperatorMetadata annotation) {
        this.type = type;
        this.classDoc = classDoc;
        this.annotation = annotation;

        Field[] fields = type.getDeclaredFields();
        FieldDoc[] fieldDocs = classDoc.fields();
        HashMap<String, FieldDoc> fieldDocMap = new HashMap<String, FieldDoc>();
        for (FieldDoc fieldDoc : fieldDocs) {
            fieldDocMap.put(fieldDoc.name(), fieldDoc);
        }

        TargetProductDesc targetProductDesc = null;
        sourceProductList = new ArrayList<SourceProductDesc>();
        parameters = new ArrayList<ParameterDesc>();
        for (Field field : fields) {
            TargetProduct targetProduct = field.getAnnotation(TargetProduct.class);
            if (targetProduct != null) {
                targetProductDesc = new TargetProductDesc(field, fieldDocMap.get(field.getName()), targetProduct);
            }
            SourceProduct sourceProduct = field.getAnnotation(SourceProduct.class);
            if (sourceProduct != null) {
                sourceProductList.add(new SourceProductDesc(field, fieldDocMap.get(field.getName()), sourceProduct));
            }
            SourceProducts sourceProductsAnnot = field.getAnnotation(SourceProducts.class);
            if (sourceProductsAnnot != null && sourceProducts == null) {
                sourceProducts = new SourceProductsDesc(field, fieldDocMap.get(field.getName()),
                                                             sourceProductsAnnot);
            }
            Parameter parameter = field.getAnnotation(Parameter.class);
            if (parameter != null) {
                parameters.add(new ParameterDesc(field, fieldDocMap.get(field.getName()), parameter));
            }
        }
        this.targetProduct = targetProductDesc;
    }

    public Class<? extends Operator> getType() {
        return type;
    }

    public ClassDoc getClassDoc() {
        return classDoc;
    }

    public String getName() {
        return annotation.alias();
    }

    public String getShortDescription() {
        return annotation.description();
    }

    public String getLongDescription() {
        return classDoc.commentText();
    }

    public String getVersion() {
        return annotation.version();
    }

    public TargetProductDesc getTargetProduct() {
        return targetProduct;
    }

    public SourceProductDesc[] getSourceProductList() {
        return sourceProductList.toArray(new SourceProductDesc[sourceProductList.size()]);
    }

    public SourceProductsDesc getSourceProducts() {
        return sourceProducts;
    }

    public ParameterDesc[] getParameters() {
        return parameters.toArray(new ParameterDesc[parameters.size()]);
    }

}
