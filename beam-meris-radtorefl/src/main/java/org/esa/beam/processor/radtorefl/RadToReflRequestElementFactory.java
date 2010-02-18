package org.esa.beam.processor.radtorefl;

import org.esa.beam.framework.param.ParamException;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.util.SystemUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class RadToReflRequestElementFactory implements RequestElementFactory {

    private DefaultRequestElementFactory _defaultFactory;
    private Map _paramPropertiesMap;

    /**
     * Singleton interface - creates the one and only instance of this factory.
     *
     * @return areference to the factory
     */
    public static RadToReflRequestElementFactory getInstance() {
        return Holder.instance;
    }

    public ProductRef createInputProductRef(File url, String fileFormat, String typeId) throws RequestElementFactoryException {
        return _defaultFactory.createInputProductRef(url, fileFormat, typeId);
    }

    public ProductRef createOutputProductRef(File url, String fileFormat, String typeId) throws RequestElementFactoryException {
        return _defaultFactory.createOutputProductRef(url, fileFormat, typeId);
    }

    public Parameter createDefaultInputProductParameter() {
        return _defaultFactory.createDefaultInputProductParameter();
    }

    public Parameter createDefaultOutputProductParameter() {
        final File defaultValue = new File(SystemUtils.getUserHomeDir(),
                                           RadToReflConstants.DEFAULT_OUTPUT_PRODUCT_FILE_NAME);
        final Parameter parameter = _defaultFactory.createDefaultOutputProductParameter();
        parameter.setValue(defaultValue, null);
        return parameter;
    }

    public Parameter createOutputFormatParameter() {
        return _defaultFactory.createOutputFormatParameter();
    }


    public Parameter createDefaultLogPatternParameter(String prefix) {
        return _defaultFactory.createDefaultLogPatternParameter(prefix);
    }

    public Parameter createLogToOutputParameter(String value) throws ParamValidateException {
        return _defaultFactory.createLogToOutputParameter(value);
    }

    public Parameter createParameter(final String name, final String value) throws RequestElementFactoryException {
        final Parameter parameter = createParameter(name);
        if (parameter == null) {
            throw new RequestElementFactoryException("Unknown processing parameter '" + name + "'");
        }
        try {
            parameter.setValueAsText(value);
        } catch (ParamException e) {
            throw new RequestElementFactoryException(e.getMessage(), e);
        }
        return parameter;
    }

    public Parameter createParameter(final String name) {
        final ParamProperties paramProperties = (ParamProperties) _paramPropertiesMap.get(name);
        if (paramProperties != null) {
            return new Parameter(name, paramProperties);
        }
        return null;
    }

    /**
     * Constructs the object.
     */
    private RadToReflRequestElementFactory() {
        _defaultFactory = DefaultRequestElementFactory.getInstance();
        _paramPropertiesMap = new HashMap();

        addBandsParameter();
        addCopyBandsParameter();
        _paramPropertiesMap.put(RadToReflConstants.OUTPUT_FORMAT_PARAM_NAME,
                                _defaultFactory.createOutputFormatParameter().getProperties());
    }

    private void addBandsParameter() {
        final ParamProperties properties = _defaultFactory.createStringArrayParamProperties();
        properties.setDefaultValue(new String[0]);
        properties.setValueSet(RadToReflConstants.INPUT_BANDS_PARAM_DEFAULT);
        properties.setLabel(RadToReflConstants.INPUT_BANDS_PARAM_LABEL);

        _paramPropertiesMap.put(RadToReflConstants.INPUT_BANDS_PARAM_NAME, properties);
    }

    private void addCopyBandsParameter() {
        final ParamProperties properties = _defaultFactory.createBooleanParamProperties();
        properties.setDefaultValue(RadToReflConstants.COPY_INPUT_BANDS_PARAM_DEFAULT);
        properties.setLabel(RadToReflConstants.COPY_INPUT_BANDS_PARAM_LABEL);

        _paramPropertiesMap.put(RadToReflConstants.COPY_INPUT_BANDS_PARAM_NAME, properties);
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final RadToReflRequestElementFactory instance = new RadToReflRequestElementFactory();
    }
}
