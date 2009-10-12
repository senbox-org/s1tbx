package org.esa.beam.framework.ui;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ValueEditor;
import com.bc.ceres.binding.swing.ValueEditorRegistry;
import com.bc.ceres.swing.TableLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;


/**
 * A utility class used to create a {@link JPanel} containing default Swing components and their corresponding bindings for the
 * {@link ValueContainer} given by the {@link com.bc.ceres.binding.swing.BindingContext}.
 * <p/>
 * <p>If the {@code displayName} property of a {@link com.bc.ceres.binding.ValueDescriptor ValueDescriptor} is set, it will be used as label, otherwise
 * a label is derived from the {@code name} property.</p>
 */
public class ValueEditorsPane {

    private final BindingContext bindingContext;

    public ValueEditorsPane(ValueContainer container) {
        this(new BindingContext(container));
    }

    public ValueEditorsPane(BindingContext bindingContext) {
        this.bindingContext = bindingContext;
    }

    public BindingContext getBindingContext() {
        return bindingContext;
    }

    public JPanel createPanel() {
        ValueContainer valueContainer = bindingContext.getValueContainer();
        ValueModel[] models = valueContainer.getModels();
        TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTablePadding(3, 3);
        final JPanel panel = new JPanel(layout);

        int rowIndex = 0;
        final ValueEditorRegistry registry = ValueEditorRegistry.getInstance();
        for (ValueModel model : models) {
            ValueDescriptor descriptor = model.getDescriptor();
            ValueEditor valueEditor = registry.findValueEditor(descriptor);
            JComponent[] components = valueEditor.createComponents(descriptor, bindingContext);
            if (components.length == 2) {
                layout.setCellWeightX(rowIndex, 0, 0.0);
                panel.add(components[1]);
                layout.setCellWeightX(rowIndex, 1, 1.0);
                panel.add(components[0]);
            } else {
                layout.setCellColspan(rowIndex, 0, 2);
                layout.setCellWeightX(rowIndex, 0, 1.0);
                panel.add(components[0]);
            }
            rowIndex++;
        }
        layout.setCellColspan(rowIndex, 0, 2);
        layout.setCellWeightX(rowIndex, 0, 1.0);
        layout.setCellWeightY(rowIndex, 0, 1.0);
        panel.add(new JPanel());
        return panel;
    }
}
