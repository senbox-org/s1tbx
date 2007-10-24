package com.bc.ceres.binding;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;

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

    PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    // todo - remove?
    public Object getValue(String propertyName) {
        return getModel(propertyName).getValue();
    }

    // todo - remove?
    public void setValue(String propertyName, Object value) throws ValidationException {
        getModel(propertyName).setValue(value);
    }

    // todo - remove?
    public String getAsText(String propertyName) {
        return getModel(propertyName).getValueAsText();
    }

    // todo - remove?
    public void setFromText(String propertyName, String text) throws ValidationException, ConversionException {
        getModel(propertyName).setValueFromText(text);
    }

    // todo - remove?
    public ValueDescriptor getValueDescriptor(String propertyName) {
        return getModel(propertyName).getDescriptor();
    }
}
