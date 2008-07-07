package com.bc.ceres.binding;

import com.bc.ceres.binding.accessors.ClassFieldAccessor;
import com.bc.ceres.binding.accessors.DefaultValueAccessor;
import com.bc.ceres.binding.accessors.MapEntryAccessor;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A container for {@link ValueModel}s.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class ValueContainer {

    private HashMap<String, ValueModel> valueModelMap = new HashMap<String, ValueModel>(10);
    private ArrayList<ValueModel> valueModelList = new ArrayList<ValueModel>(10);
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public ValueContainer() {
    }

    /**
     * Creates a value container for the given object.
     * The factory method will not modify the object, thus not setting any default values.
     *
     * @param object the backing object
     * @return the value container
     */
    public static ValueContainer createObjectBacked(Object object) {
        return createObjectBacked(object, new DefaultClassFieldDescriptorFactory());
    }

    /**
     * Creates a value container for the given object.
     * The factory method will not modify the object, thus not setting any default values.
     *
     * @param object                      the backing object
     * @param classFieldDescriptorFactory a factory used to create {@link ValueDescriptor}s of the fields of the object's type
     * @return the value container
     */
    public static ValueContainer createObjectBacked(Object object, ClassFieldDescriptorFactory classFieldDescriptorFactory) {
        return create(object.getClass(), new ObjectBackedValueAccessorFactory(object), classFieldDescriptorFactory, false);
    }

    /**
     * Creates a value container for a map backing the values.
     * The factory method will not modify the given map, thus not setting any default values.
     *
     * @param map the map which backs the values
     * @return the value container
     */
    public static ValueContainer createMapBacked(Map<String, Object> map) {
        ValueContainer vc = new ValueContainer();
        for (Entry<String, Object> entry : map.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            vc.addModel(new ValueModel(ValueDescriptor.createValueDescriptor(name, value.getClass()),
                                       new MapEntryAccessor(map, name)));
        }
        return vc;
    }

    /**
     * Creates a value container for the given template type and map backing the values.
     * The factory method will not modify the given map, thus not setting any default values.
     *
     * @param map          the map which backs the values
     * @param templateType the template type
     * @return the value container
     */
    public static ValueContainer createMapBacked(Map<String, Object> map, Class<?> templateType) {
        return createMapBacked(map, templateType, new DefaultClassFieldDescriptorFactory());
    }

    /**
     * Creates a value container for the given template type and map backing the values.
     * The factory method will not modify the given map, thus not setting any default values.
     *
     * @param map                         the map which backs the values
     * @param templateType                the template type
     * @param classFieldDescriptorFactory a factory used to create {@link ValueDescriptor}s of the fields of the template type
     * @return the value container
     */
    public static ValueContainer createMapBacked(Map<String, Object> map, Class<?> templateType, ClassFieldDescriptorFactory classFieldDescriptorFactory) {
        return create(templateType, new MapBackedValueAccessorFactory(map), classFieldDescriptorFactory, false);
    }

    /**
     * Creates a value container for the given template type.
     * The value model returned will have its values set to defaults (if specified).
     *
     * @param templateType the template type
     * @return the value container
     */
    public static ValueContainer createValueBacked(Class<?> templateType) {
        return createValueBacked(templateType, new DefaultClassFieldDescriptorFactory());
    }

    /**
     * Creates a value container for the given template type.
     * The value model returned will have its values set to defaults (if specified).
     *
     * @param templateType                the template type
     * @param classFieldDescriptorFactory a factory used to create {@link ValueDescriptor}s of the fields of the template type
     * @return the value container
     */
    public static ValueContainer createValueBacked(Class<?> templateType, ClassFieldDescriptorFactory classFieldDescriptorFactory) {
        return create(templateType, new ValueBackedValueAccessorFactory(), classFieldDescriptorFactory, true);
    }

    public ValueModel[] getModels() {
        return valueModelList.toArray(new ValueModel[valueModelList.size()]);
    }

    public ValueModel getModel(String name) {
        return valueModelMap.get(name);
    }

    public void addModel(ValueModel model) {
        if (valueModelMap.put(model.getDescriptor().getName(), model) != model) {
            final String alias = model.getDescriptor().getAlias();
            if (alias != null && !alias.isEmpty()) {
                valueModelMap.put(alias, model);
            }
            valueModelList.add(model);
            model.setContainer(this);
        }
    }

    public void addModels(ValueModel[] models) {
        for (ValueModel model : models) {
            addModel(model);
        }
    }

    public void removeModel(ValueModel model) {
        if (valueModelMap.remove(model.getDescriptor().getName()) != null) {
            final String alias = model.getDescriptor().getAlias();
            if (alias != null && !alias.isEmpty()) {
                valueModelMap.remove(alias);
            }
            valueModelList.remove(model);
            model.setContainer(null);
        }
    }

    public void removeModels(ValueModel[] models) {
        for (ValueModel model : models) {
            removeModel(model);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String name, PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(name, l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

    public void removePropertyChangeListener(String name, PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(name, l);
    }

    public Object getValue(String propertyName) {
        return getModel(propertyName).getValue();
    }

    public void setValue(String propertyName, Object value) throws ValidationException {
        getModel(propertyName).setValue(value);
    }

    public String getAsText(String propertyName) {
        return getModel(propertyName).getValueAsText();
    }

    public void setFromText(String propertyName, String text) throws ValidationException, ConversionException {
        getModel(propertyName).setValueFromText(text);
    }

    public ValueDescriptor getDescriptor(String propertyName) {
        return getModel(propertyName).getDescriptor();
    }

    public void setDefaultValues() throws ValidationException {
        for (final ValueModel model : getModels()) {
            final ValueDescriptor descriptor = model.getDescriptor();
            if (descriptor.getDefaultValue() != null) {
                model.setValue(descriptor.getDefaultValue());
            }
        }
    }

    PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    private static ValueContainer create(Class<?> type, ValueAccessorFactory valueAccessorFactory, ClassFieldDescriptorFactory classFieldDescriptorFactory, boolean setDefaultValues) {
        ValueContainer vc = new ValueContainer();
        collect(vc, type, valueAccessorFactory, classFieldDescriptorFactory);
        if (setDefaultValues) {
            try {
                vc.setDefaultValues();
            } catch (ValidationException e) {
                throw new IllegalStateException(e);
            }
        }
        return vc;
    }

    private static void collect(ValueContainer vc, Class<?> type, ValueAccessorFactory valueAccessorFactory, ClassFieldDescriptorFactory classFieldDescriptorFactory) {
        if (!type.equals(Object.class)) {
            collect(vc, type.getSuperclass(), valueAccessorFactory, classFieldDescriptorFactory);
            Field[] declaredFields = type.getDeclaredFields();
            for (Field field : declaredFields) {
                final ValueDescriptor valueDescriptor = ValueDescriptor.createValueDescriptor(field, classFieldDescriptorFactory);
                if (valueDescriptor != null) {
                    vc.addModel(new ValueModel(valueDescriptor, valueAccessorFactory.createValueAccessor(field)));
                }
            }
        }
    }

    private interface ValueAccessorFactory {
        ValueAccessor createValueAccessor(Field field);
    }

    private static class ObjectBackedValueAccessorFactory implements ValueAccessorFactory {
        private Object object;

        private ObjectBackedValueAccessorFactory(Object object) {
            this.object = object;
        }

        public ValueAccessor createValueAccessor(Field field) {
            return new ClassFieldAccessor(object, field);
        }
    }

    private static class MapBackedValueAccessorFactory implements ValueAccessorFactory {
        private Map<String, Object> map;

        private MapBackedValueAccessorFactory(Map<String, Object> map) {
            this.map = map;
        }

        public ValueAccessor createValueAccessor(Field field) {
            return new MapEntryAccessor(map, field.getName());
        }
    }

    private static class ValueBackedValueAccessorFactory implements ValueAccessorFactory {

        public ValueAccessor createValueAccessor(Field field) {
            return new DefaultValueAccessor();
        }
    }

    private static class DefaultClassFieldDescriptorFactory implements ClassFieldDescriptorFactory {
        public ValueDescriptor createValueDescriptor(Field field) {
            return new ValueDescriptor(field.getName(), field.getType());
        }
    }
}
