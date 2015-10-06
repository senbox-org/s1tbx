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
package org.esa.snap.core.param;

import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ObjectUtils;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.StringUtils;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The <code>ParamProperties</code> class is used to store parameter attributes such as parameter type and description
 * or validation information such as minimum and maximum values. An instance of this class which implements this
 * interface can contain any number of attributes. The interpretation of particular attributes is handed over to
 * specialized parameter editors and validators.
 * <p> <i>Important note:</i> Attribute keys must NOT contain the period (.) character.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$  $Date$
 * @see Parameter
 * @see ParamEditor
 * @see ParamValidator
 */
public class ParamProperties {

    // Important note: Attribute names must NOT contain the period (.) character.

    public final static String VALUETYPE_KEY = "valueType";
    public final static String DEFAULTVALUE_KEY = "defaultValue";
    public final static String NUMCOLS_KEY = "numCols";
    public final static String NUMROWS_KEY = "numRows";
    public final static String WORD_WRAP_KEY = "wordWrap";
    public final static String MINVALUE_KEY = "minValue";
    public final static String MAXVALUE_KEY = "maxValue";
    public final static String INCREMENT_KEY = "increment";
    public final static String VALUESET_KEY = "valueSet";
    public final static String VALUESETBOUND_KEY = "valueSetBound";
    public final static String VALUESETDELIM_KEY = "valueSetDelim";
    public final static String NULLVALUEALLOWED_KEY = "nullValueAllowed";
    public final static String EMPTYVALUESNOTALLOWED_KEY = "emptyValueAllowed";
    public final static String IDENTIFIERSONLY_KEY = "namesOnly";
    public final static String CASESENSITIVE_KEY = "caseSensitive";
    public final static String READONLY_KEY = "readOnly";
    public final static String HIDDEN_KEY = "hidden";
    public final static String LABEL_KEY = "label"; /*I18N*/
    public final static String DESCRIPTION_KEY = "description"; /*I18N*/
    public final static String PHYSICALUNIT_KEY = "physUnit"; /*I18N*/
    public final static String VALIDATORCLASS_KEY = "validatorClass";
    public final static String EDITORCLASS_KEY = "editorClass";
    public final static String LISTMODEL_KEY = "listModel";
    public final static String FILE_SELECTION_MODE_KEY = "fsm";
    public static final String LAST_DIR_KEY = "lastDir";
    public final static String CHOOSABLE_FILE_FILTERS_KEY = "choosableFileFilters";
    public final static String CURRENT_FILE_FILTER_KEY = "currentFileFilter";
    public final static String COMP_PRODUCTS_FOR_BAND_ARITHMETHIK_KEY = "compatibleProductsForBandArithmethik";
    public final static String SEL_PRODUCT_FOR_BAND_ARITHMETHIK_KEY = "selectedProductForBandArithmethik";
    public final static String SELECT_ALL_ON_FOCUS_KEY = "selectAllOnFocus";

    /**
     * File selection mode JFileChooser.FILES_ONLY
     */
    public final static int FSM_FILES_ONLY = JFileChooser.FILES_ONLY;
    /**
     * File selection mode JFileChooser.DIRECTORIES_ONLY
     */
    public final static int FSM_DIRECTORIES_ONLY = JFileChooser.DIRECTORIES_ONLY;
    /**
     * File selection mode JFileChooser.FILES_AND_DIRECTORIES
     */
    public final static int FSM_FILES_AND_DIRECTORIES = JFileChooser.FILES_AND_DIRECTORIES;

    private Map<String, Object> _propertyMap;
    private PropertyChangeSupport _propertyChangeSupport;

    public ParamProperties() {
    }

    public ParamProperties(Class valueType) {
        this(valueType, null);
    }

    public ParamProperties(Class valueType,
                           Object defaultValue) {
        this(valueType, defaultValue, null);
    }

    public ParamProperties(Class valueType,
                           Object defaultValue,
                           String[] valueSet) {
        this(valueType, defaultValue, valueSet, false);
    }

    public ParamProperties(Class valueType,
                           Object defaultValue,
                           String[] valueSet,
                           boolean valueSetBound) {
        setValueType(valueType);
        setDefaultValue(defaultValue);
        setValueSet(valueSet);
        setValueSetBound(valueSetBound);
    }

    public ParamProperties(Class valueType,
                           Number defaultValue,
                           Number minValue,
                           Number maxValue) {
        this(valueType, defaultValue, minValue, maxValue, null);
    }

    public ParamProperties(Class valueType,
                           Number defaultValue,
                           Number minValue,
                           Number maxValue,
                           Number increment) {
        setValueType(valueType);
        setDefaultValue(defaultValue);
        setMinValue(minValue);
        setMaxValue(maxValue);
        setIncrement(increment);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (_propertyChangeSupport == null) {
            _propertyChangeSupport = new PropertyChangeSupport(this);
        }
        _propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (_propertyChangeSupport == null) {
            return;
        }
        _propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void setValueType(Class valueType) {
        setPropertyValue(VALUETYPE_KEY, valueType);
    }

    public Class getValueType() {
        return convertClassValue(VALUETYPE_KEY);
    }

    public void setValidatorClass(Class validatorClass) {
        setPropertyValue(VALIDATORCLASS_KEY, validatorClass);
    }

    public Class getValidatorClass() {
        return convertClassValue(VALIDATORCLASS_KEY);
    }

    public void setEditorClass(Class editorClass) {
        setPropertyValue(EDITORCLASS_KEY, editorClass);
    }

    public Class getEditorClass() {
        return convertClassValue(EDITORCLASS_KEY);
    }

    public void setDefaultValue(Object defaultValue) {
        setPropertyValue(DEFAULTVALUE_KEY, defaultValue);
    }

    public Object getDefaultValue() {
        return getPropertyValue(DEFAULTVALUE_KEY);
    }

    public void setNumCols(int numCols) {
        setPropertyValue(NUMCOLS_KEY, numCols);
    }

    public int getNumCols() {
        Number n = convertNumberValue(NUMCOLS_KEY);
        return n != null ? n.intValue() : 0;
    }

    public void setNumRows(int numRows) {
        setPropertyValue(NUMROWS_KEY, numRows);
    }

    public int getNumRows() {
        Number n = convertNumberValue(NUMROWS_KEY);
        return n != null ? n.intValue() : 0;
    }

    public void setMinValue(Number minValue) {
        setPropertyValue(MINVALUE_KEY, minValue);
    }

    public Number getMinValue() {
        return convertNumberValue(MINVALUE_KEY);
    }

    public void setMaxValue(Number maxValue) {
        setPropertyValue(MAXVALUE_KEY, maxValue);
    }

    public Number getMaxValue() {
        return convertNumberValue(MAXVALUE_KEY);
    }

    public void setIncrement(Number increment) {
        setPropertyValue(INCREMENT_KEY, increment);
    }

    public Number getIncrement() {
        return convertNumberValue(INCREMENT_KEY);
    }

    public void setValueSet(String[] valueSet) {
        setPropertyValue(VALUESET_KEY, valueSet);
    }

    public String[] getValueSet() {
        return convertStringArrayValue(VALUESET_KEY);
    }

    public void setValueSetDelim(char delim) {
        this.setPropertyValue(VALUESETDELIM_KEY, new Character(delim));
    }

    public char getValueSetDelim() {
        char delim = ',';
        Object delimValue = getPropertyValue(VALUESETDELIM_KEY);
        if (delimValue != null) {
            String delimStr = delimValue.toString();
            if (delimStr.length() > 0) {
                delim = delimStr.charAt(0);
            }
        }
        return delim;
    }

    public void setValueSetBound(boolean valueSetBound) {
        setPropertyValue(VALUESETBOUND_KEY, valueSetBound);
    }

    public boolean isValueSetBound() {
        return getPropertyValue(VALUESETBOUND_KEY, false);
    }

    public void setNullValueAllowed(boolean nullAllowed) {
        setPropertyValue(NULLVALUEALLOWED_KEY, nullAllowed);
    }

    public boolean isNullValueAllowed() {
        return getPropertyValue(NULLVALUEALLOWED_KEY, false);
    }

    public void setEmptyValuesNotAllowed(boolean emptyAllowed) {
        setPropertyValue(EMPTYVALUESNOTALLOWED_KEY, emptyAllowed);
    }

    public boolean isEmptyValuesNotAllowed() {
        return getPropertyValue(EMPTYVALUESNOTALLOWED_KEY, false);
    }

    public void setIdentifiersOnly(boolean identifiersOnly) {
        setPropertyValue(IDENTIFIERSONLY_KEY, identifiersOnly);
    }

    public boolean isIdentifiersOnly() {
        return getPropertyValue(IDENTIFIERSONLY_KEY, false);
    }

    public void setCaseSensitive(boolean caseSensitive) {
        setPropertyValue(CASESENSITIVE_KEY, caseSensitive);
    }

    public boolean isCaseSensitive() {
        return getPropertyValue(CASESENSITIVE_KEY, true);
    }

    public void setReadOnly(boolean readOnly) {
        setPropertyValue(READONLY_KEY, readOnly);
    }

    public boolean isReadOnly() {
        return getPropertyValue(READONLY_KEY, false);
    }

    public void setHidden(boolean hidden) {
        setPropertyValue(HIDDEN_KEY, hidden);
    }

    public boolean isHidden() {
        return getPropertyValue(HIDDEN_KEY, false);
    }

    public void setLabel(String label) {
        setPropertyValue(LABEL_KEY, label);
    }

    public String getLabel() {
        return getPropertyValue(LABEL_KEY, "");
    }

    public void setDescription(String description) {
        setPropertyValue(DESCRIPTION_KEY, description);
    }

    public String getDescription() {
        return convertStringValue(DESCRIPTION_KEY);
    }

    public void setPhysicalUnit(String unit) {
        setPropertyValue(PHYSICALUNIT_KEY, unit);
    }

    public String getPhysicalUnit() {
        return convertStringValue(PHYSICALUNIT_KEY);
    }

    public void setFileSelectionMode(int fsm) {
        setPropertyValue(FILE_SELECTION_MODE_KEY, fsm);
    }

    public int getFileSelectionMode() {
        return getPropertyValue(FILE_SELECTION_MODE_KEY, FSM_FILES_AND_DIRECTORIES);
    }

    /**
     * Sets the <code>current FileFilter</code> used in the <code>FileChooser</code> displayed when the button from
     * <code>FileEditor</code> was klicked.
     *
     * @param filter a javax.swing.filechooser.FileFilter
     */
    public void setCurrentFileFilter(FileFilter filter) {
        setPropertyValue(CURRENT_FILE_FILTER_KEY, filter);
    }

    /**
     * Gets the <code>current FileFilter</code> which was set in the <code>FileChooser</code> displayed when the button
     * from <code>FileEditor</code> was klicked.
     */
    public FileFilter getCurrentFileFilter() {
        return (FileFilter) getPropertyValue(CURRENT_FILE_FILTER_KEY);
    }

    /**
     * Sets an array of <code>choosable FileFilter</code> used in the <code>FileChooser</code> displayed when the button
     * from <code>FileEditor</code> was klicked. If no <code>FileFilterCurrent</code> was set, the first
     * <code>FileFilter</code> in this array is the <code>current FileFilter</code>
     *
     * @param filters a javax.swing.filechooser.FileFilter[]
     */
    public void setChoosableFileFilters(FileFilter[] filters) {
        setPropertyValue(CHOOSABLE_FILE_FILTERS_KEY, filters);
    }

    /**
     * Gets an array of <code>choosable FileFilter</code> which was set in the <code>FileChooser</code> displayed when
     * the button from <code>FileEditor</code> was klicked.
     */
    public FileFilter[] getChoosableFileFilters() {
        return (FileFilter[]) getPropertyValue(CHOOSABLE_FILE_FILTERS_KEY);
    }

    public void setPropertyValue(String key, boolean value) {
        setPropertyValue(key, Boolean.valueOf(value));
    }

    public void setPropertyValue(String key, int value) {
        setPropertyValue(key, new Integer(value));
    }

    public void setPropertyValue(String key, long value) {
        setPropertyValue(key, new Long(value));
    }

    public void setPropertyValue(String key, float value) {
        setPropertyValue(key, new Float(value));
    }

    public void setPropertyValue(String key, double value) {
        setPropertyValue(key, new Double(value));
    }

    public void setPropertyValue(String key, Object value) {
        Object oldValue = null;
        if (_propertyChangeSupport != null) {
            oldValue = getPropertyValue(key);
        }
        if (_propertyMap == null) {
            _propertyMap = createPropertyMap(null);
        }
        _propertyMap.put(key, value);
        if (_propertyChangeSupport != null && !ObjectUtils.equalObjects(oldValue, value)) {
            _propertyChangeSupport.firePropertyChange(key, oldValue, value);
        }
    }

    /**
     * Sets the properties to the values found in the given <code>Properties</code> instance.
     * <p>This utility method searches for all keys in the given <code>Properties</code> instance whose keys start with
     * <code>paramName + "."</code>. The rest of the key is expected to be a valid <code>ParamProperties</code>
     * attribute key and the property value a corresponding textual representation of the attribute's value.
     * <p> The method can be used to automatically configure parameters from Java property files.
     *
     * @param paramName the parameter name
     */
    public void setPropertyValues(String paramName, PropertyMap propertyMap) {
        String namePrefix = paramName + ".";
        propertyMap.getPropertyKeys().stream().filter(key -> key.startsWith(namePrefix)).forEach(key -> {
            String paramPropName = key.substring(namePrefix.length());
            String paramPropValue = propertyMap.getPropertyString(key);
            setPropertyValue(paramPropName, paramPropValue);
        });
    }

    /**
     * Creates a subset of the properties in this map, containing only properties whose name start with the
     * given <code>namePrefix</code>.
     *
     * @param namePrefix the name prefix
     * @return the map subset
     */
    public Map<String, Object> getProperties(final String namePrefix) {
        final HashMap<String, Object> properties = new HashMap<String, Object>();
        final Set<Map.Entry<String, Object>> entrySet = _propertyMap.entrySet();
        for (Map.Entry<String, Object> entry : entrySet) {
            final String key = entry.getKey();
            if (key.startsWith(namePrefix)) {
                properties.put(key, entry.getValue());
            }
        }
        return properties;
    }

    /**
     * Returns the value of the attribute with the given name. If an attribute with given name could not be found the
     * method returns <code>null</code>.
     *
     * @param key the attribute key, must not be <code>null</code>
     * @return the attribute value
     */
    public Object getPropertyValue(String key) {
        Guardian.assertNotNull("key", key);
        return _propertyMap != null ? _propertyMap.get(key) : null;
    }

    /**
     * Returns <code>true</code> if an attribute with given name was found.
     *
     * @param key the attribute key, must not be <code>null</code>
     */
    public boolean containsProperty(String key) {
        return getPropertyValue(key) != null;
    }

    /**
     * Returns the <code>boolean</code> value of the attribute with the given name.
     *
     * @param key          the attribute key, must not be <code>null</code>
     * @param defaultValue the default value which is returned if an attribute with the given name was not found
     * @return the attribute value
     */
    public boolean getPropertyValue(String key, boolean defaultValue) {
        Boolean boolValue = convertBooleanValue(key);
        return boolValue != null ? boolValue : defaultValue;
    }

    /**
     * Returns the <code>int</code> value of the attribute with the given name.
     *
     * @param key          the attribute key, must not be <code>null</code>
     * @param defaultValue the default value which is returned if an attribute with the given name was not found
     * @return the attribute value
     */
    public int getPropertyValue(String key, int defaultValue) {
        Number numberValue = convertNumberValue(key);
        return numberValue != null ? numberValue.intValue() : defaultValue;
    }

    /**
     * Returns the <code>double</code> value of the attribute with the given name.
     *
     * @param key          the attribute key, must not be <code>null</code>
     * @param defaultValue the default value which is returned if an attribute with the given name was not found
     * @return the attribute value
     */
    public double getPropertyValue(String key, double defaultValue) {
        Number numberValue = convertNumberValue(key);
        return numberValue != null ? numberValue.doubleValue() : defaultValue;
    }

    /**
     * Returns the <code>Class</code> value of the attribute with the given name.
     *
     * @param key          the attribute key, must not be <code>null</code>
     * @param defaultValue the default value which is returned if an attribute with the given name was not found
     * @return the attribute value
     */
    public Class getPropertyValue(String key, Class defaultValue) {
        Class classValue = convertClassValue(key);
        return classValue != null ? classValue : defaultValue;
    }

    /**
     * Returns the <code>String</code> value of the attribute with the given name.
     *
     * @param key          the attribute key, must not be <code>null</code>
     * @param defaultValue the default value which is returned if an attribute with the given name was not found
     * @return the attribute value
     */
    public String getPropertyValue(String key, String defaultValue) {
        String stringValue = convertStringValue(key);
        return stringValue != null ? stringValue : defaultValue;
    }

    /**
     * Returns the <code>Object</code> value of the attribute with the given name.
     *
     * @param key          the attribute key, must not be <code>null</code>
     * @param defaultValue the default value which is returned if an attribute with the given name was not found
     * @return the attribute value
     */
    public Object getPropertyValue(String key, Object defaultValue) {
        Object objectValue = getPropertyValue(key);
        return objectValue != null ? objectValue : defaultValue;
    }

    /**
     * Creates an appropriate validator for this parameter info.
     *
     * @return a validator, never <code>null</code>
     */
    public ParamValidator createValidator() {

        ParamValidator validator = null;

        Class validatorClass = getValidatorClass();
        if (validatorClass != null) {
            try {
                validator = (ParamValidator) validatorClass.newInstance();
            } catch (Exception e) {
                // @todo 1 nf/nf - throw exception ??? I think so!
                Debug.trace(e);
            }
        }

        if (validator == null) {
            validator = ParamValidatorRegistry.getValidator(getValueType());
        }

        Debug.assertTrue(validator != null);

        return validator;
    }

    /**
     * Creates and returns a copy of this object.
     */
    public ParamProperties createCopy() {
        ParamProperties copy = new ParamProperties();
        copy._propertyMap = createPropertyMap(_propertyMap);
        copy._propertyChangeSupport = _propertyChangeSupport;
        return copy;
    }

    /**
     * Creates a <code>Map</code> to be used to store the attributes. This method can be overridden in order to return a
     * specialized <code>Map</code>. The default implementation returns a new <code>HashMap()</code> instance.
     *
     * @param map the map whose mappings are to be initially placed in the new map, can be <code>null</code>.
     */
    protected Map<String, Object> createPropertyMap(Map<String, Object> map) {
        if (map != null) {
            return new HashMap<String, Object>(map);
        }
        return new HashMap<String, Object>();
    }

    /**
     * Loads the class with the specified name.
     * <p>This method can be overridden in order to implement a specialized mechanism to load parameter validator and
     * editor classes. The default implementation simply returns  <code>getClass().getClassLoader().loadClass(className)</code>.
     *
     * @param className the fully qualified name of the class
     * @return the resulting Class object
     * @throws ClassNotFoundException if the class was not found
     */
    protected Class loadClass(String className) throws ClassNotFoundException {
        return getClass().getClassLoader().loadClass(className);
    }

    private Class convertClassValue(String key) {
        Object value = getPropertyValue(key);
        Class classValue = null;
        if (value != null) {
            if (value instanceof Class) {
                classValue = (Class) value;
            } else if (value instanceof String) {
                try {
                    classValue = loadClass((String) value);
                    setPropertyValue(key, classValue);
                } catch (ClassNotFoundException e) {
                    // @todo 1 nf/nf - check: throw exception here in order to signal that attribute is not a class?
                    Debug.trace(key + " = " + value + ": class could not be loaded");
                    Debug.trace(e);
                }
            }
        }
        return classValue;
    }

    private Boolean convertBooleanValue(String key) {
        Object value = getPropertyValue(key);
        Boolean booleanValue = null;
        if (value != null) {
            if (value instanceof Boolean) {
                booleanValue = (Boolean) value;
            } else if (value instanceof String) {
                booleanValue = Boolean.valueOf(value.toString());
                setPropertyValue(key, booleanValue);
            }
        }
        return booleanValue;
    }

    private String convertStringValue(String key) {
        Object value = getPropertyValue(key);
        String stringValue = null;
        if (value != null) {
            if (value instanceof String) {
                stringValue = (String) value;
            } else {
                stringValue = value.toString();
                setPropertyValue(key, stringValue);
            }
        }
        return stringValue;
    }

    private Number convertNumberValue(String key) {
        Object value = getPropertyValue(key);
        Number numberValue = null;
        if (value != null) {
            if (value instanceof Number) {
                numberValue = (Number) value;
            } else if (value instanceof String) {
                try {
                    numberValue = Integer.valueOf(value.toString());
                    setPropertyValue(key, numberValue);
                } catch (Exception e1) {
                    try {
                        numberValue = Double.valueOf(value.toString());
                        setPropertyValue(key, numberValue);
                    } catch (Exception e2) {
                        // @todo 1 nf/nf - check: throw exception here in order to signal that attribute is not a number?
                        Debug.trace(key + " = " + value + ": numeric value expected");
                        Debug.trace(e2);
                    }
                }
            }
        }
        return numberValue;
    }

    private String[] convertStringArrayValue(String key) {
        Object value = getPropertyValue(key);
        String[] arrayValue = null;
        if (value != null) {
            if (value instanceof String[]) {
                arrayValue = (String[]) value;
            } else if (value instanceof String) {
                char separator = getValueSetDelim();
                arrayValue = StringUtils.split(value.toString(), new char[]{separator}, true);
                setPropertyValue(key, arrayValue);
            }
        }
        return arrayValue;
    }

}
