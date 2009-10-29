package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.swing.BindingContext;
import org.esa.beam.framework.ui.ValueEditorsPane;

import javax.swing.JPanel;

/**
 * A utility class used to create a {@link JPanel} containg default Swing components and their corresponding bindings for the
 * {@link com.bc.ceres.binding.PropertyContainer} given by the {@link BindingContext}.
 * <p/>
 * <p>If the {@code displayName} property of a {@link com.bc.ceres.binding.PropertyDescriptor ValueDescriptor} is set, it will be used as label, otherwise
 * a label is derived from the {@code name} property.</p>
 *
 * @deprecated Use {@link org.esa.beam.framework.ui.ValueEditorsPane} instead.
 */
@Deprecated
public class ParametersPane extends ValueEditorsPane {

    public ParametersPane(BindingContext bindingContext) {
        super(bindingContext);
    }

    public static String createDisplayName(String name) {
        return PropertyDescriptor.createDisplayName(name);
    }
}
