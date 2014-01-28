package org.esa.beam.framework.gpf.annotations;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.OperatorSpi;

/**
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class AnnotationParameterDescriptor implements OperatorSpi.ParameterDescriptor {
    private final String name;
    private final Class<?> dataType;
    private final Parameter annotation;

    public AnnotationParameterDescriptor(String name, Class<?> dataType, Parameter annotation) {
        Assert.notNull(name, "name");
        Assert.notNull(dataType, "dataType");
        Assert.notNull(annotation, "annotation");
        this.annotation = annotation;
        this.name = name;
        this.dataType = dataType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<?> getDataType() {
        return dataType;
    }

    @Override
    public String getAlias() {
        return annotation.alias();
    }


    @Override
    public String getItemAlias() {
        return annotation.itemAlias();
    }

    @Override
    public boolean areItemsInlined() {
        return annotation.itemsInlined();
    }

    @Override
    public String getDefaultValue() {
        return annotation.defaultValue();
    }

    @Override
    public String getLabel() {
        return annotation.label();
    }

    @Override
    public String getUnit() {
        return annotation.unit();
    }

    @Override
    public String getDescription() {
        return annotation.description();
    }

    @Override
    public String[] getValueSet() {
        return annotation.valueSet();
    }

    @Override
    public String getInterval() {
        return annotation.interval();
    }

    @Override
    public String getCondition() {
        return annotation.condition();
    }

    @Override
    public String getPattern() {
        return annotation.pattern();
    }

    @Override
    public String getFormat() {
        return annotation.format();
    }

    @Override
    public boolean isNotNull() {
        return annotation.notNull();
    }

    @Override
    public boolean isNotEmpty() {
        return annotation.notEmpty();
    }

    @Override
    public Class<? extends Validator> getValidator() {
        return annotation.validator();
    }

    @Override
    public Class<? extends Converter> getConverter() {
        return annotation.converter();
    }

    @Override
    public Class<? extends DomConverter> getDomConverter() {
        return annotation.domConverter();
    }

    @Override
    public Class<? extends RasterDataNode> getRasterDataNodeType() {
        return annotation.rasterDataNodeType();
    }
}
