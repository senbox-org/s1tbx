package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValueAccessor;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ValueEditorsPane;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.Style;
import org.esa.beam.visat.toolviews.layermanager.LayerEditor;

import javax.swing.JComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * General Editor for layers using {@link ValueDescriptor ValueDescriptors}.
 *
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public abstract class AbstractValueDescriptorLayerEditor implements LayerEditor {

    private BindingContext bindingContext;

    private Layer layer;

    @Override
    public final JComponent createControl(Layer layer) {
        this.layer = layer;
        ValueContainer valueContainer = new ValueContainer();
        final ArrayList<ValueDescriptor> vds = new ArrayList<ValueDescriptor>(11);
        collectValueDescriptors(vds);

        Map<String, Object> valueData = new HashMap<String, Object>(vds.size());
        for (ValueDescriptor valueDescriptor : vds) {
            String propertyName = valueDescriptor.getName();
            Object value = layer.getStyle().getProperty(propertyName);
            if (value == null) {
                value = valueDescriptor.getDefaultValue();
            }
            valueData.put(propertyName, value);
            ValueAccessor accessor = new MapEntryAccessor(valueData, propertyName);
            ValueModel model = new ValueModel(valueDescriptor, accessor);
            valueContainer.addModel(model);
        }

        final ArrayList<PropertyChangeListener> listenerList = new ArrayList<PropertyChangeListener>(11);
        collectPropertyChangeListeners(listenerList);

        for (PropertyChangeListener listener : listenerList) {
            valueContainer.addPropertyChangeListener(listener);
        }
        bindingContext = new BindingContext(valueContainer);
        ValueEditorsPane parametersPane = new ValueEditorsPane(bindingContext);
        return parametersPane.createPanel();
    }

    @Override
    public void updateControl() {
        final ValueModel[] valueModels = bindingContext.getValueContainer().getModels();
        for (ValueModel valueModel : valueModels) {
            final ValueDescriptor valueDescriptor = valueModel.getDescriptor();
            String propertyName = valueDescriptor.getName();
            Binding binding = bindingContext.getBinding(propertyName);
            Style style = layer.getStyle();

            final Object value = style.getProperty(propertyName);
            final Object oldValue = binding.getPropertyValue();
            if (oldValue != value && (oldValue == null || !oldValue.equals(value))) {
                binding.setPropertyValue(value);
            }
        }
    }

    protected final BindingContext getBindingContext() {
        return bindingContext;
    }

    protected final Layer getLayer() {
        return layer;
    }

    protected void collectPropertyChangeListeners(List<PropertyChangeListener> listenerList) {
        final PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if (layer != null) {
                    layer.getStyle().setProperty(propertyName, evt.getNewValue());
                }
            }
        };
        listenerList.add(listener);
    }

    protected abstract void collectValueDescriptors(final List<ValueDescriptor> descriptorList);

}
