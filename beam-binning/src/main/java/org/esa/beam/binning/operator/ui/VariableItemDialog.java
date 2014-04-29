package org.esa.beam.binning.operator.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.internal.TextComponentAdapter;
import com.bc.ceres.swing.binding.internal.TextFieldEditor;
import com.bc.jexp.ParseException;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.util.StringUtils;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class VariableItemDialog extends ModalDialog {

    private static final String PROPERTY_VARIABLE_NAME = "name";
    private static final String PROPERTY_EXPRESSION = "expr";

    private final VariableItem variableItem;
    private final boolean newVariable;
    private final Product contextProduct;
    private final BindingContext bindingContext;


    VariableItemDialog(final Window parent, VariableItem variableItem, boolean createNewVariable, Product contextProduct) {
        super(parent, "Intermediate Source Band", ID_OK_CANCEL, null);
        this.variableItem = variableItem;
        newVariable = createNewVariable;
        this.contextProduct = contextProduct;
        bindingContext = createBindingContext();
        makeUI();
    }

    @Override
    protected boolean verifyUserInput() {
        String expression = variableItem.variableConfig.getExpr() != null ? variableItem.variableConfig.getExpr().trim() : "";
        if (StringUtils.isNullOrEmpty(expression)) {
            JOptionPane.showMessageDialog(getParent(), "The source band could not be created. The expression is empty.");
            return false;
        }
        String variableName = variableItem.variableConfig.getName() != null ? variableItem.variableConfig.getName().trim() : "";
        if (StringUtils.isNullOrEmpty(variableName)) {
            JOptionPane.showMessageDialog(getParent(), "The source band could not be created. The name is empty.");
            return false;
        }
        if (newVariable && contextProduct.containsBand(variableName)) {
            String message = String.format("A source band or band with the name '%s' is already defined", variableName);
            JOptionPane.showMessageDialog(getParent(), message);
            return false;
        }
        try {
            BandArithmetic.getValidMaskExpression(expression, new Product[]{contextProduct}, 0, null);
        } catch (ParseException e) {
            String errorMessage = "The source band could not be created.\nThe expression could not be parsed:\n" + e.getMessage(); /*I18N*/
            JOptionPane.showMessageDialog(getParent(), errorMessage);
            return false;
        }
        return true;
    }

    @Override
    protected void onOK() {
        variableItem.variableConfig.setName(variableItem.variableConfig.getName().trim());
        variableItem.variableConfig.setExpr(variableItem.variableConfig.getExpr().trim());
        super.onOK();
    }

    VariableItem getVariableItem() {
        return variableItem;
    }

    private BindingContext createBindingContext() {
        final PropertyContainer container = PropertyContainer.createObjectBacked(variableItem.variableConfig, new ParameterDescriptorFactory());
        final BindingContext context = new BindingContext(container);

        PropertyDescriptor descriptor = container.getDescriptor(PROPERTY_VARIABLE_NAME);
        descriptor.setDescription("The name for the source band.");
        descriptor.setValidator(new VariableNameValidator());
        container.setDefaultValues();

        return context;
    }

    private void makeUI() {
        JComponent[] variableComponents = createComponents(PROPERTY_VARIABLE_NAME, TextFieldEditor.class);

        final TableLayout layout = new TableLayout(2);
        layout.setTablePadding(4, 3);
        layout.setCellWeightX(0, 1, 1.0);
        layout.setCellWeightX(1, 1, 1.0);
        layout.setCellWeightX(2, 0, 1.0);
        layout.setCellColspan(2, 0, 2);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        final JPanel panel = new JPanel(layout);

        panel.add(variableComponents[1]);
        panel.add(variableComponents[0]);

        JLabel expressionLabel = new JLabel("Variable expression:");
        JTextArea expressionArea = new JTextArea();
        expressionArea.setRows(3);
        TextComponentAdapter textComponentAdapter = new TextComponentAdapter(expressionArea);
        bindingContext.bind(PROPERTY_EXPRESSION, textComponentAdapter);
        panel.add(expressionLabel);
        panel.add(layout.createHorizontalSpacer());
        panel.add(expressionArea);

        JButton editExpressionButton = new JButton("Edit Expression...");
        editExpressionButton.setName("editExpressionButton");
        editExpressionButton.addActionListener(createEditExpressionButtonListener());
        panel.add(layout.createHorizontalSpacer());
        panel.add(editExpressionButton);

        setContent(panel);
    }

    private JComponent[] createComponents(String propertyName, Class<? extends PropertyEditor> editorClass) {
        PropertyDescriptor descriptor = bindingContext.getPropertySet().getDescriptor(propertyName);
        PropertyEditor editor = PropertyEditorRegistry.getInstance().getPropertyEditor(editorClass.getName());
        return editor.createComponents(descriptor, bindingContext);
    }

    private ActionListener createEditExpressionButtonListener() {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ProductExpressionPane expressionPane =
                        ProductExpressionPane.createGeneralExpressionPane(new Product[]{contextProduct},
                                                                          contextProduct,
                                                                          null);
                expressionPane.setCode(variableItem.variableConfig.getExpr());
                int status = expressionPane.showModalDialog(getJDialog(), "Expression Editor");
                if (status == ModalDialog.ID_OK) {
                    bindingContext.getBinding(PROPERTY_EXPRESSION).setPropertyValue(expressionPane.getCode());
                }
                expressionPane.dispose();
            }
        };
    }

    private class VariableNameValidator implements Validator {

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            final String name = (String) value;
            if (contextProduct.containsRasterDataNode(name)) {
                throw new ValidationException("The source band name must be unique.");
            }
        }
    }

}
