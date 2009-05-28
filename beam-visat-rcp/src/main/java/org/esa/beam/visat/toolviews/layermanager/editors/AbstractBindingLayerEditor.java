package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueAccessor;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ValueEditorsPane;
import org.esa.beam.visat.toolviews.layermanager.LayerEditor;

import javax.swing.JComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

/**
 * General Editor for layers using {@link ValueDescriptor ValueDescriptors}.
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
        ValueContainer valueContainer = bindingContext.getValueContainer();
        valueContainer.addPropertyChangeListener(new UpdateStylePropertyChangeListener());
        initializeBinding(appContext, bindingContext);
        // ODOT

        ValueEditorsPane parametersPane = new ValueEditorsPane(bindingContext);
        return parametersPane.createPanel();
    }

    protected final void addValueDescriptor(ValueDescriptor valueDescriptor) {
        Map<String, Object> valueData = new HashMap<String, Object>();
        String propertyName = valueDescriptor.getName();
        Object value = getLayer().getConfiguration().getValue(propertyName);
        if (value == null) {
            value = valueDescriptor.getDefaultValue();
        }
        valueData.put(propertyName, value);
        ValueAccessor accessor = new MapEntryAccessor(valueData, propertyName);
        ValueModel model = new ValueModel(valueDescriptor, accessor);
        bindingContext.getValueContainer().addModel(model);
    }

    @Override
    public void updateControl() {
        final ValueModel[] valueModels = bindingContext.getValueContainer().getModels();
        for (ValueModel valueModel : valueModels) {
            final ValueDescriptor valueDescriptor = valueModel.getDescriptor();
            String propertyName = valueDescriptor.getName();
            Binding binding = bindingContext.getBinding(propertyName);
            ValueContainer configuration = layer.getConfiguration();

            if (configuration.getModel(propertyName) != null) {
                final Object value = configuration.getModel(propertyName).getValue();
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
                    final ValueModel valueModel = layer.getConfiguration().getModel(propertyName);
                    if (valueModel != null) {
                        valueModel.setValue(evt.getNewValue());
                    }
                } catch (ValidationException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
        }
    }
}
