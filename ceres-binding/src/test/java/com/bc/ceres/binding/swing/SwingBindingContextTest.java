package com.bc.ceres.binding.swing;

import com.bc.ceres.binding.*;
import junit.framework.TestCase;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JFormattedTextField;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class SwingBindingContextTest extends TestCase {

    private SwingBindingContext binding;
    private ValueContainer valueContainer;

    private ValueContainer valueContainer2;
    private SwingBindingContext binding2;
    private TestPojo pojo;

    @Override
    protected void setUp() throws Exception {
        ValueContainerFactory valueContainerFactory = new ValueContainerFactory();

        valueContainer = valueContainerFactory.createValueBackedValueContainer(TestPojo.class);
        binding = new SwingBindingContext(valueContainer);

        pojo = new TestPojo();
        valueContainer2 = valueContainerFactory.createObjectBackedValueContainer(pojo);
        binding2 = new SwingBindingContext(valueContainer2);
    }

    public void testBindSpinner() throws ValidationException {
        JSpinner spinner = new JSpinner();
        binding.bind(spinner, "intValue");

        assertEquals("intValue", spinner.getName());

        spinner.setValue(3);
        assertEquals(3, valueContainer.getValue("intValue"));

        valueContainer.setValue("intValue", 76);
        assertEquals(76, spinner.getValue());
    }

    public void testBindCombobox() throws ValidationException {
        JComboBox combobox = new JComboBox(new Integer[]{1, 3, 7});
        binding.bind(combobox, "intValue");

        assertEquals("intValue", combobox.getName());

        combobox.setSelectedItem(3);
        assertEquals(3, valueContainer.getValue("intValue"));

        valueContainer.setValue("intValue", 1);
        assertEquals(1, combobox.getSelectedItem());
    }

    public void testBindTextField() throws ValidationException {
        JTextField textField = new JTextField();
        binding.bind(textField, "stringValue");

        assertEquals("stringValue", textField.getName());

        textField.setText("Bibo");
        textField.postActionEvent();
        assertEquals("Bibo", valueContainer.getValue("stringValue"));

        valueContainer.setValue("stringValue", "Samson");
        assertEquals("Samson", textField.getText());
    }

    public void testBindTextField2() throws ValidationException {
        JTextField textField = new JTextField();
        binding2.bind(textField, "stringValue");

        assertEquals("stringValue", textField.getName());

        textField.setText("Bibo");
        textField.postActionEvent();
        assertEquals("Bibo", valueContainer2.getValue("stringValue"));

        valueContainer2.setValue("stringValue", "Samson");
        assertEquals("Samson", pojo.stringValue);
        assertEquals("Samson", textField.getText());

        pojo.stringValue = "Oscar";
        assertSame("Oscar", valueContainer2.getValue("stringValue"));
        assertNotSame("Oscar", textField.getText()); // value change not detected by binding
    }

    public void testBindFormattedTextFieldToString() throws ValidationException {
        JFormattedTextField textField = new JFormattedTextField();
        binding.bind(textField, "stringValue");

        assertEquals("stringValue", textField.getName());

        textField.setValue("Bibo");
        assertEquals("Bibo", valueContainer.getValue("stringValue"));

        valueContainer.setValue("stringValue", "Samson");
        assertEquals("Samson", textField.getValue());
    }

    public void testBindFormattedTextFieldToDouble() throws ValidationException {
        JFormattedTextField textField = new JFormattedTextField();
        binding.bind(textField, "doubleValue");

        assertEquals("doubleValue", textField.getName());

        textField.setValue(3.14);
        assertEquals(3.14, valueContainer.getValue("doubleValue"));

        valueContainer.setValue("doubleValue", 2.71);
        assertEquals(2.71, textField.getValue());
    }

    public void testBindCheckBox() throws ValidationException {
        JCheckBox checkBox = new JCheckBox();
        binding.bind(checkBox, "booleanValue");

        assertEquals("booleanValue", checkBox.getName());

        checkBox.doClick();
        assertEquals(true, valueContainer.getValue("booleanValue"));

        valueContainer.setValue("booleanValue", false);
        assertEquals(false, checkBox.isSelected());
    }

    public void testBindRadioButton() throws ValidationException {
        JRadioButton radioButton = new JRadioButton();
        binding.bind(radioButton, "booleanValue");

        assertEquals("booleanValue", radioButton.getName());

        radioButton.doClick();
        assertEquals(true, valueContainer.getValue("booleanValue"));

        valueContainer.setValue("booleanValue", false);
        assertEquals(false, radioButton.isSelected());
    }
    
    public void testBindRadioButtonWithGroup() throws ValidationException {
        JRadioButton radioButton1 = new JRadioButton();
        JRadioButton radioButton2 = new JRadioButton();
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(radioButton1);
        buttonGroup.add(radioButton2);
        binding.bind(radioButton1, "booleanValue");

        assertEquals("booleanValue", radioButton1.getName());

        radioButton1.doClick();
        assertEquals(true, valueContainer.getValue("booleanValue"));
        radioButton2.doClick();
        assertEquals(false, valueContainer.getValue("booleanValue"));

        valueContainer.setValue("booleanValue", false);
        assertEquals(false, radioButton1.isSelected());
    }

    public void testBindListSelection() throws ValidationException {
        JList list = new JList(new Integer[]{3, 4, 5, 6, 7});
        binding.bind(list, "listValue", true);

        assertEquals("listValue", list.getName());

        list.setSelectedIndex(2);
        assertTrue(Arrays.equals(new int[]{5}, (int[]) valueContainer.getValue("listValue")));

        valueContainer.setValue("listValue", new int[]{6});
        assertEquals(6, list.getSelectedValue());
    }

    private static class TestPojo {
        boolean booleanValue;
        int intValue;
        double doubleValue;
        String stringValue;
        int[] listValue;
    }
}
