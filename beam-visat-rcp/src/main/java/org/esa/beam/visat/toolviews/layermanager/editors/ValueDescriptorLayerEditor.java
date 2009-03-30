package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValueAccessor;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.accessors.MapEntryAccessor;
import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.Style;

import org.esa.beam.visat.toolviews.layermanager.LayerEditor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

/**
 * General Editor for layers using valueDescriptors.
 *
 * @author marco Zühlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class ValueDescriptorLayerEditor {

    private final ValueDescriptor[] valueDescriptors;
    private BindingContext bindingContext;
    private Layer layer;

    public ValueDescriptorLayerEditor(ValueDescriptor[] valueDescriptors) {
        this.valueDescriptors = valueDescriptors;
    }
    
    public BindingContext getBindingContext() {
        return bindingContext;
    }

    public JComponent createControl() {
        ValueContainer valueContainer = new ValueContainer();
        Map<String, Object> valueData = new HashMap<String, Object>(valueDescriptors.length);
        for (ValueDescriptor valueDescriptor : valueDescriptors) {
            String propertyName = valueDescriptor.getName();
            valueData.put(propertyName, valueDescriptor.getDefaultValue());
            ValueAccessor accessor = new MapEntryAccessor(valueData, propertyName);
            ValueModel model = new ValueModel(valueDescriptor, accessor);
            valueContainer.addModel(model);
        }
        valueContainer.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if (layer != null) {
                    layer.getStyle().setProperty(propertyName, evt.getNewValue());
                }
                
            }}
        );
        bindingContext = new BindingContext(valueContainer);
        ParametersPane parametersPane = new ParametersPane(bindingContext);
        return parametersPane.createPanel();
    }

    public void updateControl(Layer selectedLayer) {
        if (layer == selectedLayer) {
            return;
        }
        layer = selectedLayer;
        for (ValueDescriptor valueDescriptor : valueDescriptors) {
            String propertyName = valueDescriptor.getName();
            Binding binding = bindingContext.getBinding(propertyName);
            Style style = layer.getStyle();
            binding.setPropertyValue(style.getProperty(propertyName));
        }
    }
}
