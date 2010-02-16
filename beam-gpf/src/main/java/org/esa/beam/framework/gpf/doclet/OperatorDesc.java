package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
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

    private final ArrayList<SourceProductDesc> sourceProducts;

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
        sourceProducts = new ArrayList<SourceProductDesc>();
        parameters = new ArrayList<ParameterDesc>();
        for (Field field : fields) {
            TargetProduct targetProduct = field.getAnnotation(TargetProduct.class);
            if (targetProduct != null) {
                targetProductDesc = new TargetProductDesc(field, fieldDocMap.get(field.getName()), targetProduct);
            }
            SourceProduct sourceProduct = field.getAnnotation(SourceProduct.class);
            if (sourceProduct != null) {
                sourceProducts.add(new SourceProductDesc(field, fieldDocMap.get(field.getName()), sourceProduct));
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

    public SourceProductDesc[] getSourceProducts() {
        return sourceProducts.toArray(new SourceProductDesc[sourceProducts.size()]);
    }

    public ParameterDesc[] getParameters() {
        return parameters.toArray(new ParameterDesc[parameters.size()]);
    }

}
