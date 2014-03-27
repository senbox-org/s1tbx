package org.esa.beam.visat.actions.imgfilter;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import org.esa.beam.visat.actions.imgfilter.model.Filter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Norman
 */
public class FilterPropertiesForm extends JPanel implements PropertyChangeListener, Filter.Listener {
    public static final TrueCondition TRUE_CONDITION = new TrueCondition();
    private Filter filter;
    private JComboBox<Filter.Operation> operationComboBox;
    private JTextField nameField;
    private JTextField shorthandField;
    private JTextField tagsField;
    private JTextField kernelOffsetXField;
    private JTextField kernelOffsetYField;
    private JTextField kernelWidthField;
    private JTextField kernelHeightField;
    private JTextField kernelQuotientField;
    private BindingContext bindingContext;

    public FilterPropertiesForm(Filter filter) {
        super(new GridBagLayout());
        setBorder(new EmptyBorder(4, 4, 4, 4));
        createUI();
        setFilter(filter);
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {

        Filter filterOld = this.filter;
        if (filterOld != filter) {
            if (this.filter != null) {
                this.filter.removeListener(this);
            }
            if (bindingContext != null) {
                bindingContext.removePropertyChangeListener(this);
                bindingContext.unbind(bindingContext.getBinding("operation"));
                bindingContext.unbind(bindingContext.getBinding("name"));
                bindingContext.unbind(bindingContext.getBinding("shorthand"));
                bindingContext.unbind(bindingContext.getBinding("tags"));
                bindingContext.unbind(bindingContext.getBinding("kernelQuotient"));
                bindingContext.unbind(bindingContext.getBinding("kernelOffsetX"));
                bindingContext.unbind(bindingContext.getBinding("kernelOffsetY"));
                bindingContext.unbind(bindingContext.getBinding("kernelWidth"));
                bindingContext.unbind(bindingContext.getBinding("kernelHeight"));
                bindingContext = null;
            }

            this.filter = filter;
            if (this.filter != null) {
                this.filter.addListener(this);
                PropertyContainer propertyContainer = PropertyContainer.createObjectBacked(this.filter);
                propertyContainer.getDescriptor("tags").setConverter(new TagsConverter());
                propertyContainer.addPropertyChangeListener(this);
                bindingContext = new BindingContext(propertyContainer);
                bindingContext.addPropertyChangeListener(this);
                bindingContext.bind("operation", operationComboBox);
                bindingContext.bind("name", nameField);
                bindingContext.bind("shorthand", shorthandField);
                bindingContext.bind("tags", tagsField);
                bindingContext.bind("kernelQuotient", kernelQuotientField);
                bindingContext.bind("kernelOffsetX", kernelOffsetXField);
                bindingContext.bind("kernelOffsetY", kernelOffsetYField);
                bindingContext.bind("kernelWidth", kernelWidthField);
                bindingContext.bind("kernelHeight", kernelHeightField);

                Enablement.Condition condition = new Enablement.Condition() {
                    @Override
                    public boolean evaluate(BindingContext bindingContext) {
                        return bindingContext.getPropertySet().getValue("editable");
                    }
                };
                bindingContext.bindEnabledState("operation", true, condition);
                bindingContext.bindEnabledState("name", true, condition);
                bindingContext.bindEnabledState("shorthand", true, condition);
                bindingContext.bindEnabledState("tags", true, condition);
                bindingContext.bindEnabledState("kernelQuotient", true, condition);
                bindingContext.bindEnabledState("kernelOffsetX", true, condition);
                bindingContext.bindEnabledState("kernelOffsetY", true, condition);
                bindingContext.bindEnabledState("kernelWidth", false, TRUE_CONDITION);
                bindingContext.bindEnabledState("kernelHeight", false, TRUE_CONDITION);
                bindingContext.adjustComponents();
            } else {
                clearComponents();
            }

            firePropertyChange("filterModel", filterOld, this.filter);
        }
    }

    @Override
    public void filterModelChanged(Filter filter) {
        bindingContext.adjustComponents();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        this.filter.notifyChange();
    }

    private void clearComponents() {
        operationComboBox.setSelectedItem(null);
        nameField.setText(null);
        shorthandField.setText(null);
        tagsField.setText(null);
        kernelQuotientField.setText(null);
        kernelOffsetXField.setText(null);
        kernelOffsetYField.setText(null);
        kernelWidthField.setText(null);
        kernelHeightField.setText(null);
    }

    void createUI() {
        nameField = new JTextField(12);
        shorthandField = new JTextField(6);
        tagsField = new JTextField(16);
        operationComboBox = new JComboBox<>(Filter.Operation.values());
        kernelQuotientField = new JTextField(8);
        kernelOffsetXField = new JTextField(8);
        kernelOffsetYField = new JTextField(8);
        kernelWidthField = new JTextField(8);
        kernelHeightField = new JTextField(8);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 0, 2, 0);
        gbc.gridy = -1;

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Operation:"), gbc);
        gbc.gridx = 1;
        add(operationComboBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        add(nameField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Shorthand:"), gbc);
        gbc.gridx = 1;
        add(shorthandField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Tags:"), gbc);
        gbc.gridx = 1;
        add(tagsField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Kernel quotient:"), gbc);
        gbc.gridx = 1;
        add(kernelQuotientField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Kernel offset X:"), gbc);
        gbc.gridx = 1;
        add(kernelOffsetXField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Kernel offset Y:"), gbc);
        gbc.gridx = 1;
        add(kernelOffsetYField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Kernel width:"), gbc);
        gbc.gridx = 1;
        add(kernelWidthField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Kernel height:"), gbc);
        gbc.gridx = 1;
        add(kernelHeightField, gbc);

    }

    private static class TagsConverter implements Converter<Object> {
        @Override
        public Class<?> getValueType() {
            return Set.class;
        }

        @Override
        public Object parse(String text) throws ConversionException {
            return new HashSet<>(Arrays.asList(text.split(",")));
        }

        @Override
        public String format(Object value) {
            Set<String> set = (Set<String>) value;
            StringBuilder sb = new StringBuilder();
            for (String s : set) {
                if (sb.length() > 0) sb.append(",");
                sb.append(s);
            }
            return sb.toString();
        }
    }

    private static class TrueCondition extends Enablement.Condition {
        @Override
        public boolean evaluate(BindingContext bindingContext) {
            return true;
        }
    }
}
