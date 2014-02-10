package org.esa.pfa.db;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.queryparser.flexible.standard.config.NumericConfig;
import org.esa.pfa.fe.op.AttributeType;
import org.esa.pfa.fe.op.FeatureType;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by luis on 09/02/14.
 */
public class NumericConfiguration {

    private NumericConfig intNumericConfig = new NumericConfig(8, NumberFormat.getNumberInstance(Locale.ENGLISH), FieldType.NumericType.INT);
    private NumericConfig longNumericConfig = new NumericConfig(8, NumberFormat.getNumberInstance(Locale.ENGLISH), FieldType.NumericType.LONG);
    private NumericConfig floatNumericConfig = new NumericConfig(8, NumberFormat.getNumberInstance(Locale.ENGLISH), FieldType.NumericType.FLOAT);
    private NumericConfig doubleNumericConfig = new NumericConfig(8, NumberFormat.getNumberInstance(Locale.ENGLISH), FieldType.NumericType.FLOAT);

    private Map<Class<?>, NumericConfig> attributeNumericConfigMap;

    private final int precisionStep;

    public NumericConfiguration(int precisionStep) {
        this.precisionStep = precisionStep;
    }

    public Map<String, NumericConfig> getNumericConfigMap(DatasetDescriptor dsDescriptor) {
        initAttributeNumericConfig();
        Map<String, NumericConfig> numericConfigMap = new HashMap<>();
        numericConfigMap.put("id", longNumericConfig);
        numericConfigMap.put("px", intNumericConfig);
        numericConfigMap.put("py", intNumericConfig);
        numericConfigMap.put("rnd", doubleNumericConfig);
        numericConfigMap.put("lat", floatNumericConfig);
        numericConfigMap.put("lon", floatNumericConfig);
        numericConfigMap.put("time", longNumericConfig);
        addAttributeNumericConfigs(dsDescriptor, numericConfigMap);
        return numericConfigMap;
    }

    private void addAttributeNumericConfigs(DatasetDescriptor dsDescriptor, Map<String, NumericConfig> numericConfigMap) {
        FeatureType[] featureTypes = dsDescriptor.getFeatureTypes();
        for (FeatureType featureType : featureTypes) {
            if (featureType.hasAttributes()) {
                AttributeType[] attributeTypes = featureType.getAttributeTypes();
                for (AttributeType attributeType : attributeTypes) {
                    NumericConfig numericConfig = getAttributeNumericConfig(attributeType);
                    if (numericConfig != null) {
                        numericConfigMap.put(featureType.getName() + "." + attributeType.getName(), numericConfig);
                    }
                }
            } else {
                NumericConfig numericConfig = getAttributeNumericConfig(featureType);
                if (numericConfig != null) {
                    numericConfigMap.put(featureType.getName(), numericConfig);
                }
            }
        }
    }

    private NumericConfig getAttributeNumericConfig(AttributeType attributeType) {
        Class<?> valueType = attributeType.getValueType();
        return attributeNumericConfigMap.get(valueType);
    }


    void initAttributeNumericConfig() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);
        intNumericConfig = new NumericConfig(precisionStep, numberFormat, FieldType.NumericType.INT);
        longNumericConfig = new NumericConfig(precisionStep, numberFormat, FieldType.NumericType.LONG);
        floatNumericConfig = new NumericConfig(precisionStep, numberFormat, FieldType.NumericType.FLOAT);
        doubleNumericConfig = new NumericConfig(precisionStep, numberFormat, FieldType.NumericType.DOUBLE);

        attributeNumericConfigMap = new HashMap<>();
        attributeNumericConfigMap.put(Byte.TYPE, intNumericConfig);
        attributeNumericConfigMap.put(Byte.class, intNumericConfig);
        attributeNumericConfigMap.put(Short.TYPE, intNumericConfig);
        attributeNumericConfigMap.put(Short.class, intNumericConfig);
        attributeNumericConfigMap.put(Integer.TYPE, intNumericConfig);
        attributeNumericConfigMap.put(Integer.class, intNumericConfig);
        attributeNumericConfigMap.put(Long.TYPE, longNumericConfig);
        attributeNumericConfigMap.put(Long.class, longNumericConfig);
        attributeNumericConfigMap.put(Float.TYPE, floatNumericConfig);
        attributeNumericConfigMap.put(Float.class, floatNumericConfig);
        // Statistics of features are stored as Double, but Float is sufficient.
        attributeNumericConfigMap.put(Double.TYPE, floatNumericConfig);
        attributeNumericConfigMap.put(Double.class, floatNumericConfig);
    }
}
