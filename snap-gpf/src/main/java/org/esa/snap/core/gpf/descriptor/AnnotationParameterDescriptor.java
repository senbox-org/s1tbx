package org.esa.snap.core.gpf.descriptor;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.annotations.Parameter;

/**
 * A {@link ParameterDescriptor} implementation for the
 * {@link Parameter Parameter} annotation.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class AnnotationParameterDescriptor implements ParameterDescriptor {
    private final String name;
    private final Class<?> dataType;
    private final Parameter annotation;
    private final boolean isDeprecated;

    public AnnotationParameterDescriptor(String name, Class<?> dataType, boolean isDeprecated, Parameter annotation) {
        Assert.notNull(name, "name");
        Assert.notNull(dataType, "dataType");
        Assert.notNull(annotation, "annotation");
        this.annotation = annotation;
        this.name = name;
        this.dataType = dataType;
        this.isDeprecated = isDeprecated;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<?> getDataType() {
        return dataType;
    }

    public Parameter getAnnotation() {
        return annotation;
    }

    @Override
    public String getAlias() {
        return getNonEmptyStringOrNull(annotation.alias());
    }

    @Override
    public String getItemAlias() {
        return getNonEmptyStringOrNull(annotation.itemAlias());
    }

    @Override
    public String getDefaultValue() {
        return getNonEmptyStringOrNull(annotation.defaultValue());
    }

    @Override
    public String getLabel() {
        return getNonEmptyStringOrNull(annotation.label());
    }

    @Override
    public String getUnit() {
        return getNonEmptyStringOrNull(annotation.unit());
    }

    @Override
    public String getDescription() {
        return getNonEmptyStringOrNull(annotation.description());
    }

    @Override
    public String[] getValueSet() {
        return annotation.valueSet();
    }

    @Override
    public String getInterval() {
        return getNonEmptyStringOrNull(annotation.interval());
    }

    @Override
    public String getCondition() {
        return getNonEmptyStringOrNull(annotation.condition());
    }

    @Override
    public String getPattern() {
        return getNonEmptyStringOrNull(annotation.pattern());
    }

    @Override
    public String getFormat() {
        return getNonEmptyStringOrNull(annotation.format());
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
    public boolean isDeprecated() {
        return isDeprecated;
    }

    @Override
    public Class<? extends Validator> getValidatorClass() {
        return getDerivedClassOrNull(annotation.validator(), Validator.class);
    }

    @Override
    public Class<? extends Converter> getConverterClass() {
        return getDerivedClassOrNull(annotation.converter(), Converter.class);
    }

    @Override
    public Class<? extends DomConverter> getDomConverterClass() {
        return getDerivedClassOrNull(annotation.domConverter(), DomConverter.class);
    }

    @Override
    public Class<? extends RasterDataNode> getRasterDataNodeClass() {
        return getDerivedClassOrNull(annotation.rasterDataNodeType(), RasterDataNode.class);
    }

    @Override
    public boolean isStructure() {
        return DefaultParameterDescriptor.isStructure(getDataType());
    }

    @Override
    public ParameterDescriptor[] getStructureMemberDescriptors() {
        return DefaultParameterDescriptor.getDataMemberDescriptors(getDataType());
    }

    private static String getNonEmptyStringOrNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static <T> Class<? extends T> getDerivedClassOrNull(Class<? extends T> value, Class<T> abstractBaseType) {
        return !value.equals(abstractBaseType) && abstractBaseType.isAssignableFrom(value) ? value : null;
    }
}
