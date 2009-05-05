package com.bc.ceres.binding.swing;

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.swing.internal.TextComponentAdapter;

import junit.framework.TestCase;

import javax.swing.*;
import javax.swing.text.BadLocationException;

import java.util.Arrays;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class BindingContextTest extends TestCase {

    private BindingContext bindingContextVB;
    private ValueContainer valueContainerVB;

    private ValueContainer valueContainerOB;
    private BindingContext bindingContextOB;
    private TestPojo pojo;

    @Override
    protected void setUp() throws Exception {
        valueContainerVB = ValueContainer.createValueBacked(TestPojo.class);
        valueContainerVB.getDescriptor("valueSetBoundIntValue").setValueSet(new ValueSet(TestPojo.intValueSet));
        bindingContextVB = new BindingContext(valueContainerVB);

        pojo = new TestPojo();
        valueContainerOB = ValueContainer.createObjectBacked(pojo);
        valueContainerOB.getDescriptor("valueSetBoundIntValue").setValueSet(new ValueSet(TestPojo.intValueSet));
        bindingContextOB = new BindingContext(valueContainerOB);
    }

    public void testBindSpinner() throws ValidationException {
        JSpinner spinner = new JSpinner();
        Binding binding = bindingContextVB.bind("intValue", spinner);
        assertNotNull(binding);
        assertSame(spinner, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("intValue", spinner.getName());

        spinner.setValue(3);
        assertEquals(3, valueContainerVB.getValue("intValue"));

        valueContainerVB.setValue("intValue", 76);
        assertEquals(76, spinner.getValue());

    }

    public void testBindComboBox() throws ValidationException {
        JComboBox combobox = new JComboBox(new Integer[]{1, 3, 7});
        Binding binding = bindingContextVB.bind("intValue", combobox);
        assertNotNull(binding);
        assertSame(combobox, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("intValue", combobox.getName());

        combobox.setSelectedItem(3);
        assertEquals(3, valueContainerVB.getValue("intValue"));

        valueContainerVB.setValue("intValue", 1);
        assertEquals(1, combobox.getSelectedItem());
    }

    public void testBindTextField() throws ValidationException {
        JTextField textField = new JTextField();
        Binding binding = bindingContextVB.bind("stringValue", textField);
        assertNotNull(binding);
        assertSame(textField, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("stringValue", textField.getName());

        textField.setText("Bibo");
        textField.postActionEvent();
        assertEquals("Bibo", valueContainerVB.getValue("stringValue"));

        valueContainerVB.setValue("stringValue", "Samson");
        assertEquals("Samson", textField.getText());
    }

    public void testBindTextField2() throws ValidationException {
        JTextField textField = new JTextField();
        Binding binding = bindingContextOB.bind("stringValue", textField);
        assertNotNull(binding);
        assertSame(textField, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("stringValue", textField.getName());

        textField.setText("Bibo");
        textField.postActionEvent();
        assertEquals("Bibo", valueContainerOB.getValue("stringValue"));

        valueContainerOB.setValue("stringValue", "Samson");
        assertEquals("Samson", pojo.stringValue);
        assertEquals("Samson", textField.getText());

        pojo.stringValue = "Oscar";
        assertSame("Oscar", valueContainerOB.getValue("stringValue"));
        assertNotSame("Oscar", textField.getText()); // value change not detected by binding
    }

    public void testBindFormattedTextFieldToString() throws ValidationException {
        JFormattedTextField textField = new JFormattedTextField();
        Binding binding = bindingContextVB.bind("stringValue", textField);
        assertNotNull(binding);
        assertSame(textField, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("stringValue", textField.getName());

        textField.setValue("Bibo");
        assertEquals("Bibo", valueContainerVB.getValue("stringValue"));

        valueContainerVB.setValue("stringValue", "Samson");
        assertEquals("Samson", textField.getValue());
    }

    public void testBindFormattedTextFieldToDouble() throws ValidationException {
        JFormattedTextField textField = new JFormattedTextField();
        Binding binding = bindingContextVB.bind("doubleValue", textField);
        assertNotNull(binding);
        assertSame(textField, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("doubleValue", textField.getName());

        textField.setValue(3.14);
        assertEquals(3.14, valueContainerVB.getValue("doubleValue"));

        valueContainerVB.setValue("doubleValue", 2.71);
        assertEquals(2.71, textField.getValue());
    }
    
    public void testBindTextArea() throws ValidationException, BadLocationException {
        JTextArea textArea = new JTextArea();
        TextComponentAdapter textComponentAdapter = new TextComponentAdapter(textArea);
        Binding binding = bindingContextVB.bind("stringValue", textComponentAdapter);
        assertNotNull(binding);
        assertSame(textArea, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("stringValue", textArea.getName());

        textArea.setText("Bibo");
        assertEquals("Bibo", valueContainerVB.getValue("stringValue"));

        valueContainerVB.setValue("stringValue", "Samson");
        assertEquals("Samson", textArea.getText());
    }

    public void testBindCheckBox() throws ValidationException {
        JCheckBox checkBox = new JCheckBox();
        Binding binding = bindingContextVB.bind("booleanValue", checkBox);
        assertNotNull(binding);
        assertSame(checkBox, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("booleanValue", checkBox.getName());

        checkBox.doClick();
        assertEquals(true, valueContainerVB.getValue("booleanValue"));

        valueContainerVB.setValue("booleanValue", false);
        assertEquals(false, checkBox.isSelected());
    }

    public void testBindRadioButton() throws ValidationException {
        JRadioButton radioButton = new JRadioButton();
        Binding binding = bindingContextVB.bind("booleanValue", radioButton);
        assertNotNull(binding);
        assertSame(radioButton, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("booleanValue", radioButton.getName());

        radioButton.doClick();
        assertEquals(true, valueContainerVB.getValue("booleanValue"));

        valueContainerVB.setValue("booleanValue", false);
        assertEquals(false, radioButton.isSelected());
    }

    public void testBindButtonGroup() throws ValidationException {
        JRadioButton radioButton1 = new JRadioButton();
        JRadioButton radioButton2 = new JRadioButton();
        JRadioButton radioButton3 = new JRadioButton();

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(radioButton1);
        buttonGroup.add(radioButton2);
        buttonGroup.add(radioButton3);

        ValueModel m = valueContainerVB.getModel("valueSetBoundIntValue");

        m.setValue(TestPojo.intValueSet[0]);

        Binding binding = bindingContextVB.bind("valueSetBoundIntValue", buttonGroup);
        assertNotNull(binding);
        assertSame(radioButton1, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(3, binding.getComponents().length);
        assertSame(radioButton1, binding.getComponents()[0]);
        assertSame(radioButton2, binding.getComponents()[1]);
        assertSame(radioButton3, binding.getComponents()[2]);

        assertEquals(true, radioButton1.isSelected());
        assertEquals(false, radioButton2.isSelected());
        assertEquals(false, radioButton3.isSelected());
        assertEquals(TestPojo.intValueSet[0], m.getValue());

        radioButton3.doClick();
        assertEquals(false, radioButton1.isSelected());
        assertEquals(false, radioButton2.isSelected());
        assertEquals(true, radioButton3.isSelected());
        assertEquals(TestPojo.intValueSet[2], m.getValue());

        radioButton2.doClick();
        assertEquals(false, radioButton1.isSelected());
        assertEquals(true, radioButton2.isSelected());
        assertEquals(false, radioButton3.isSelected());
        assertEquals(TestPojo.intValueSet[1], m.getValue());

        m.setValue(TestPojo.intValueSet[0]);
        assertEquals(true, radioButton1.isSelected());
        assertEquals(false, radioButton2.isSelected());
        assertEquals(false, radioButton3.isSelected());
        assertEquals(TestPojo.intValueSet[0], m.getValue());

        m.setValue(TestPojo.intValueSet[2]);
        assertEquals(false, radioButton1.isSelected());
        assertEquals(false, radioButton2.isSelected());
        assertEquals(true, radioButton3.isSelected());
        assertEquals(TestPojo.intValueSet[2], m.getValue());

        m.setValue(TestPojo.intValueSet[1]);
        assertEquals(false, radioButton1.isSelected());
        assertEquals(true, radioButton2.isSelected());
        assertEquals(false, radioButton3.isSelected());
        assertEquals(TestPojo.intValueSet[1], m.getValue());
    }

    public void testBindListSelection() throws ValidationException {
        JList list = new JList(new Integer[]{3, 4, 5, 6, 7});
        Binding binding = bindingContextVB.bind("listValue", list, true);
        assertNotNull(binding);
        assertSame(list, getPrimaryComponent(binding));
        assertNotNull(binding.getComponents());
        assertEquals(1, binding.getComponents().length);

        assertEquals("listValue", list.getName());

        list.setSelectedIndex(2);
        assertTrue(Arrays.equals(new int[]{5}, (int[]) valueContainerVB.getValue("listValue")));

        valueContainerVB.setValue("listValue", new int[]{6});
        assertEquals(6, list.getSelectedValue());
    }
    
    public void testSecondaryComponent() throws Exception {
        JTextField textField = new JTextField();
        Binding binding = bindingContextVB.bind("stringValue", textField);
        JLabel label = new JLabel("myLabel");
        binding.addComponent(label);
        
        JComponent[] components = binding.getComponents();
        assertNotNull(components);
        assertEquals(2, components.length);
        assertSame(getPrimaryComponent(binding), components[0]);
        assertSame(label, components[1]);
    }

    private JComponent getPrimaryComponent(Binding binding) {
        return binding.getComponents()[0];
    }

    private static class TestPojo {
        boolean booleanValue;
        int intValue;
        double doubleValue;
        String stringValue;
        int[] listValue;

        int valueSetBoundIntValue;
        static Integer[] intValueSet = new Integer[] {101, 102, 103};
    }
}
