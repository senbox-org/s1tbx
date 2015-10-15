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

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class OperatorDesc  implements ElementDesc {
    private final Class<? extends Operator> type;
    private final ClassDoc classDoc;

    private final OperatorDescriptor annotation;

    private final TargetProductDesc targetProduct;

    private final ArrayList<SourceProductDesc> sourceProductList;

    private SourceProductsDesc sourceProducts;

    private final ArrayList<ParameterDesc> parameters;

    OperatorDesc(Class<? extends Operator> type, ClassDoc classDoc, OperatorDescriptor annotation) {
        this.type = type;
        this.classDoc = classDoc;
        this.annotation = annotation;

        Field[] fields = type.getDeclaredFields();
        FieldDoc[] fieldDocs = classDoc.fields();
        HashMap<String, FieldDoc> fieldDocMap = new HashMap<>();
        for (FieldDoc fieldDoc : fieldDocs) {
            fieldDocMap.put(fieldDoc.name(), fieldDoc);
        }

        TargetProductDesc targetProductDesc = null;
        sourceProductList = new ArrayList<>();
        parameters = new ArrayList<>();
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
        return annotation.getAlias();
    }

    public String getShortDescription() {
        return annotation.getDescription();
    }

    public String getLongDescription() {
        return classDoc.commentText();
    }

    public String getVersion() {
        return annotation.getVersion();
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
