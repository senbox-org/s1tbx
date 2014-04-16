package org.esa.beam.visat.actions.imgfilter;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import org.esa.beam.visat.actions.imgfilter.model.Filter;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A tabular editor form for a filter's properties.
 *
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
        // super(new GridBagLayout());
        super(new TableLayout());
        setBorder(new EmptyBorder(4, 4, 4, 4));
        createUI();
        setFilter(filter);
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {

        if (this.filter != filter) {
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

            Filter oldFilter = this.filter;
            this.filter = filter;

            if (this.filter != null) {
                PropertyContainer propertyContainer = PropertyContainer.createObjectBacked(this.filter);
                propertyContainer.getDescriptor("tags").setConverter(new TagsConverter());

                propertyContainer.getDescriptor("operation").setDescription("<html>The image filter operation.<br/>CONVOLVE uses a real-valued kernel matrix.<br/>Other operations have Boolean matrices.");
                propertyContainer.getDescriptor("tags").setDescription("<html>Tags are used categorise and group filters.<br/>Use a comma to separate multiple tags.");
                propertyContainer.getDescriptor("name").setDescription("The filter's display name");
                propertyContainer.getDescriptor("shorthand").setDescription("A shorthand for the name, used as default band name suffix");
                propertyContainer.getDescriptor("kernelQuotient").setDescription("<html>Inverse scaling factor, will be used<br/>to pre-multiply the kernel matrix before convolution");
                propertyContainer.getDescriptor("kernelWidth").setDescription("<html>Width of the kernel matrix<br/>(editing not supported here, use the graphical editor)");
                propertyContainer.getDescriptor("kernelHeight").setDescription("<html>Height of the kernel matrix<br/>(editing not supported here, use the graphical editor)");
                propertyContainer.getDescriptor("kernelOffsetX").setDescription("<html>Offset in X of the kernel matrix' 'key element'<br/>(editing not yet supported, will always be kernel center)");
                propertyContainer.getDescriptor("kernelOffsetY").setDescription("<html>Offset in Y of the kernel matrix' 'key element'<br/>(editing not yet supported, will always be kernel center)");

                bindingContext = new BindingContext(propertyContainer);
                bindingContext.bind("operation", operationComboBox);
                bindingContext.bind("name", nameField);
                bindingContext.bind("shorthand", shorthandField);
                bindingContext.bind("tags", tagsField);
                bindingContext.bind("kernelQuotient", kernelQuotientField);
                bindingContext.bind("kernelOffsetX", kernelOffsetXField);
                bindingContext.bind("kernelOffsetY", kernelOffsetYField);
                bindingContext.bind("kernelWidth", kernelWidthField);
                bindingContext.bind("kernelHeight", kernelHeightField);

                Enablement.Condition editableCondition = new Enablement.Condition() {
                    @Override
                    public boolean evaluate(BindingContext bindingContext) {
                        return bindingContext.getPropertySet().getValue("editable");
                    }
                };
                Enablement.Condition editableConvolutionCondition = new Enablement.Condition() {
                    @Override
                    public boolean evaluate(BindingContext bindingContext) {
                        PropertySet propertySet = bindingContext.getPropertySet();
                        return Boolean.TRUE.equals(propertySet.getValue("editable")) && Filter.Operation.CONVOLVE.equals(propertySet.getValue("operation"));
                    }
                };
                bindingContext.bindEnabledState("operation", true, editableCondition);
                bindingContext.bindEnabledState("name", true, editableCondition);
                bindingContext.bindEnabledState("shorthand", true, editableCondition);
                bindingContext.bindEnabledState("tags", true, editableCondition);
                bindingContext.bindEnabledState("kernelQuotient", true, editableConvolutionCondition);
                // width and height are disabled here, because user shall use intended spinners
                bindingContext.bindEnabledState("kernelWidth", false, TRUE_CONDITION);
                bindingContext.bindEnabledState("kernelHeight", false, TRUE_CONDITION);
                // offsetX and offsetY are disabled here, because com.bc.ceres.jai.opimage.GeneralFilterOpImage does not support it so far
                bindingContext.bindEnabledState("kernelOffsetX", false, TRUE_CONDITION);
                bindingContext.bindEnabledState("kernelOffsetY", false, TRUE_CONDITION);
                bindingContext.adjustComponents();

                bindingContext.addPropertyChangeListener(this);

                this.filter.addListener(this);
            } else {
                clearComponents();
            }

            firePropertyChange("filter", oldFilter, this.filter);
        }
    }

    @Override
    public void filterChanged(Filter filter, String propertyName) {
        if (this.filter == filter) {
            bindingContext.adjustComponents();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (this.filter != null) {
            this.filter.fireChange(evt.getPropertyName());
        }
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
        operationComboBox = new JComboBox<>(Filter.Operation.values());
        nameField = new JTextField(12);
        shorthandField = new JTextField(6);
        tagsField = new JTextField(16);
        kernelQuotientField = new JTextField(8);
        kernelOffsetXField = new JTextField(8);
        kernelOffsetYField = new JTextField(8);
        kernelWidthField = new JTextField(8);
        kernelHeightField = new JTextField(8);

        TableLayout layout = (TableLayout) getLayout();
        layout.setColumnCount(2);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableWeightX(0.5);
        layout.setTablePadding(2, 2);

        int row = 0;
        add(new JLabel("Operation:"), TableLayout.cell(row, 0));
        add(operationComboBox, TableLayout.cell(row, 1));
        row++;
        add(new JLabel("Name:"), TableLayout.cell(row, 0));
        add(nameField, TableLayout.cell(row, 1));
        row++;
        add(new JLabel("Shorthand:"), TableLayout.cell(row, 0));
        add(shorthandField, TableLayout.cell(row, 1));
        row++;
        add(new JLabel("Tags:"), TableLayout.cell(row, 0));
        add(tagsField, TableLayout.cell(row, 1));
        row++;
        add(new JLabel("Kernel quotient:"), TableLayout.cell(row, 0));
        add(kernelQuotientField, TableLayout.cell(row, 1));
        row++;
        add(new JLabel("Kernel offset X:"), TableLayout.cell(row, 0));
        add(kernelOffsetXField, TableLayout.cell(row, 1));
        row++;
        add(new JLabel("Kernel offset Y:"), TableLayout.cell(row, 0));
        add(kernelOffsetYField, TableLayout.cell(row, 1));
        row++;
        add(new JLabel("Kernel width:"), TableLayout.cell(row, 0));
        add(kernelWidthField, TableLayout.cell(row, 1));
        row++;
        add(new JLabel("Kernel height:"), TableLayout.cell(row, 0));
        add(kernelHeightField, TableLayout.cell(row, 1));

        
        /*

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
        */
    }

    private static class TagsConverter implements Converter<Object> {

        public static final HashSet<String> EMPTY_TAGS = new HashSet<>();

        @Override
        public Class<?> getValueType() {
            return HashSet.class;
        }

        @Override
        public HashSet<String> parse(String text) throws ConversionException {
            if (text == null || text.isEmpty()) {
                return EMPTY_TAGS;
            }
            String[] tagArray = text.split(",");
            HashSet<String> tags = new LinkedHashSet<>();
            for (String rawTag : tagArray) {
                String tag = rawTag.trim();
                if (!tag.isEmpty()) {
                    tags.add(tag);
                }
            }
            return tags;
        }

        @Override
        public String format(Object value) {
            if (value instanceof Set) {
                Set<String> set = (Set<String>) value;
                if (!set.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (String s : set) {
                        if (sb.length() > 0) {
                            sb.append(",");
                        }
                        sb.append(s);
                    }
                    return sb.toString();
                }
            }
            return null;
        }
    }

    private static class TrueCondition extends Enablement.Condition {
        @Override
        public boolean evaluate(BindingContext bindingContext) {
            return true;
        }
    }
}
