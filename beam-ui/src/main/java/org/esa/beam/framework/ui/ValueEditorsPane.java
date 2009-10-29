package org.esa.beam.framework.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ValueEditor;
import com.bc.ceres.binding.swing.ValueEditorRegistry;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.util.StringUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * A utility class used to create a {@link JPanel} containing default Swing components and their corresponding bindings for the
 * {@link com.bc.ceres.binding.PropertyContainer} given by the {@link com.bc.ceres.binding.swing.BindingContext}.
 * <p/>
 * <p>If the {@code displayName} property of a {@link com.bc.ceres.binding.PropertyDescriptor ValueDescriptor} is set, it will be used as label, otherwise
 * a label is derived from the {@code name} property.</p>
 */
public class ValueEditorsPane {

    private final BindingContext bindingContext;

    public ValueEditorsPane(PropertyContainer container) {
        this(new BindingContext(container));
    }

    public ValueEditorsPane(BindingContext bindingContext) {
        this.bindingContext = bindingContext;
    }

    public BindingContext getBindingContext() {
        return bindingContext;
    }

    public JPanel createPanel() {
        PropertyContainer propertyContainer = bindingContext.getPropertyContainer();
        Property[] models = propertyContainer.getProperties();

        boolean displayUnitColumn = displayUnitColumn(models);
        TableLayout layout = new TableLayout(displayUnitColumn ? 3 : 2);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTablePadding(3, 3);
        final JPanel panel = new JPanel(layout);

        int rowIndex = 0;
        final ValueEditorRegistry registry = ValueEditorRegistry.getInstance();
        for (Property model : models) {
            PropertyDescriptor descriptor = model.getDescriptor();
            ValueEditor valueEditor = registry.findValueEditor(descriptor);
            JComponent[] components = valueEditor.createComponents(descriptor, bindingContext);
            if (components.length == 2) {
                layout.setCellWeightX(rowIndex, 0, 0.0);
                panel.add(components[1], new TableLayout.Cell(rowIndex, 0));
                layout.setCellWeightX(rowIndex, 1, 1.0);
                panel.add(components[0], new TableLayout.Cell(rowIndex, 1));
            } else {
                layout.setCellColspan(rowIndex, 0, 2);
                layout.setCellWeightX(rowIndex, 0, 1.0);
                panel.add(components[0], new TableLayout.Cell(rowIndex, 0));
            }
            if(displayUnitColumn) {
                final JLabel label = new JLabel("");
                if (descriptor.getUnit() != null) {
                    label.setText(descriptor.getUnit());
                }
                layout.setCellWeightX(rowIndex, 2, 0.0);
                panel.add(label, new TableLayout.Cell(rowIndex, 2));
            }
            rowIndex++;
        }
        layout.setCellColspan(rowIndex, 0, 2);
        layout.setCellWeightX(rowIndex, 0, 1.0);
        layout.setCellWeightY(rowIndex, 0, 1.0);
        panel.add(new JPanel());
        return panel;
    }

    private boolean displayUnitColumn(Property[] models) {
        boolean showUnitColumn = false;
        for (Property model : models) {
            PropertyDescriptor descriptor = model.getDescriptor();
            if (StringUtils.isNotNullAndNotEmpty(descriptor.getUnit())) {
                showUnitColumn = true;
                break;
            }
        }
        return showUnitColumn;
    }
}
