package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyAccessor;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.PropertyPane;
import org.esa.beam.visat.toolviews.layermanager.LayerEditor;

import javax.swing.JComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

/**
 * General Editor for layers using {@link com.bc.ceres.binding.PropertyDescriptor ValueDescriptors}.
 *
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public abstract class AbstractBindingLayerEditor implements LayerEditor {

    private BindingContext bindingContext;

    private Layer layer;

    @Override
    public JComponent createControl(AppContext appContext, Layer layer) {
        this.layer = layer;

        // TODO - replace this code block with the following line (rq-20090528)
        // bindingContext = new BindingContext(layer.getConfiguration());
        bindingContext = new BindingContext();
        PropertySet propertySet = bindingContext.getPropertySet();
        propertySet.addPropertyChangeListener(new UpdateStylePropertyChangeListener());
        initializeBinding(appContext, bindingContext);
        // ODOT

        PropertyPane parametersPane = new PropertyPane(bindingContext);
        return parametersPane.createPanel();
    }

    protected final void addValueDescriptor(PropertyDescriptor propertyDescriptor) {
        Map<String, Object> valueData = new HashMap<String, Object>();
        String propertyName = propertyDescriptor.getName();
        Object value = getLayer().getConfiguration().getValue(propertyName);
        if (value == null) {
            value = propertyDescriptor.getDefaultValue();
        }
        valueData.put(propertyName, value);
        PropertyAccessor accessor = new MapEntryAccessor(valueData, propertyName);
        Property model = new Property(propertyDescriptor, accessor);
        bindingContext.getPropertySet().addProperty(model);
    }

    @Override
    public void updateControl() {
        final Property[] properties = bindingContext.getPropertySet().getProperties();
        for (Property property : properties) {
            final PropertyDescriptor propertyDescriptor = property.getDescriptor();
            String propertyName = propertyDescriptor.getName();
            Binding binding = bindingContext.getBinding(propertyName);
            PropertySet configuration = layer.getConfiguration();

            if (configuration.getProperty(propertyName) != null) {
                final Object value = configuration.getProperty(propertyName).getValue();
                final Object oldValue = binding.getPropertyValue();
                if (oldValue != value && (oldValue == null || !oldValue.equals(value))) {
                    binding.setPropertyValue(value);
                }
            }
        }
    }

    protected final BindingContext getBindingContext() {
        return bindingContext;
    }

    protected final Layer getLayer() {
        return layer;
    }

    protected abstract void initializeBinding(AppContext appContext, final BindingContext bindingContext);

    private class UpdateStylePropertyChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            if (layer != null) {
                try {
                    final Property property = layer.getConfiguration().getProperty(propertyName);
                    if (property != null) {
                        property.setValue(evt.getNewValue());
                    }
                } catch (ValidationException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
        }
    }
}
