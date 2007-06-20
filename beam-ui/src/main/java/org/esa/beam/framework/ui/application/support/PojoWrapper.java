package org.esa.beam.framework.ui.application.support;

import com.bc.ceres.core.Assert;
import org.esa.beam.util.ObjectUtils;

import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;


// todo move somewhere else

/**
 * Adds (Swing) property change support to objects.
 *
 * @author Norman Fomferra
 */
public class PojoWrapper implements PropertyChangeEmitter {

    private final Object pojo;
    private final boolean swing;
    private Map<String, Field> fieldMap;
    private PropertyChangeSupport propertyChangeSupport;

    /**
     * Constructs a new POJO wrapper.
     * Property change notifications are send on the caller thread.
     *
     * @param pojo The plain old Java object.
     */
    public PojoWrapper(Object pojo) {
        this(pojo, false);
    }

    /**
     * Constructs a new POJO wrapper.
     * Property change notifications are send depending on the {@code swing} parameter.
     *
     * @param pojo  The plain old Java object.
     * @param swing If true, property change notifications are send on the <i>Event Dispatch Thread</i>.
     */
    public PojoWrapper(Object pojo, boolean swing) {
        Assert.notNull(pojo, "pojo");
        this.pojo = pojo;
        this.swing = swing;
    }

    /**
     * @return The plain old Java object.
     */
    public Object getPojo() {
        return pojo;
    }

    /**
     * @return If true, property change notifications are send on the <i>Event Dispatch Thread</i>..
     */
    public boolean isSwing() {
        return swing;
    }

    /**
     * Gets the value of a named property.
     *
     * @param propertyName The property (field) name.
     * @return The value.
     */
    public Object getValue(String propertyName) {
        final Field field = getField(propertyName);
        return getFieldValue(field, propertyName);
    }

    /**
     * Sets the value of a named property.
     * If the value changes, a property change notification is send.
     *
     * @param propertyName The property (field) name.
     * @param value        The value.
     */
    public void setValue(String propertyName, Object value) {
        final Field field = getField(propertyName);
        Object oldValue = getFieldValue(field, propertyName);
        if (ObjectUtils.equalObjects(oldValue, value)) {
            return;
        }
        try {
            field.set(pojo, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(propertyName, e);
        }
        firePropertyChange(propertyName, oldValue, value);

    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        getPropertyChangeSupport().addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        getPropertyChangeSupport().addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        getPropertyChangeSupport().removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        getPropertyChangeSupport().removePropertyChangeListener(propertyName, listener);
    }

    private PropertyChangeSupport getPropertyChangeSupport() {
        if (propertyChangeSupport == null) {
            propertyChangeSupport = swing ? new SwingPropertyChangeSupport(pojo) : new PropertyChangeSupport(pojo);
        }
        return propertyChangeSupport;
    }

    private void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }


    private void collectFields(Class<? extends Object> aClass) {
        if (!aClass.equals(Object.class)) {
            final Field[] fields = aClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                fieldMap.put(field.getName(), field);
            }
            final Class<?> superclass = aClass.getSuperclass();
            if (superclass != null) {
                collectFields(superclass);
            }
        }
    }


    private Field getField(String propertyName) {
        ensureFieldMap();
        final Field field = fieldMap.get(propertyName);
        if (field == null) {
            throw new IllegalArgumentException(propertyName);
        }
        return field;
    }

    private Object getFieldValue(Field field, String propertyName) {
        try {
            return field.get(pojo);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(propertyName, e);
        }
    }

    private void ensureFieldMap() {
        if (this.fieldMap == null) {
            this.fieldMap = new HashMap<String, Field>(10);
            collectFields(this.pojo.getClass());
        }
    }
}
