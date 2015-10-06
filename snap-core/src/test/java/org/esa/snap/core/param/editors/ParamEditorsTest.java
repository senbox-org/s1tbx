/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.param.editors;

import junit.framework.TestCase;
import org.esa.snap.core.param.AbstractParamEditor;
import org.esa.snap.core.param.ParamEditor;
import org.esa.snap.core.param.ParamProperties;
import org.esa.snap.core.param.Parameter;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Component;
import java.io.File;

/**
 * EditorComponentNames Tester.
 * Ensures that all the components given by any ParamEditor have the same name as the parameters lable text.
 *
 * @author <Sabine Embacher>
 * @version 1.0
 * @since <pre>02/25/2005</pre>
 */
public class ParamEditorsTest extends TestCase {

    private static final String LABEL_NAME = "This is the label";
    private static final String UNIT_NAME = "This is the unit";
    private static final String PARAM_NAME = "theParamName";

    public void testBooleanEditor() {
        final ParamProperties properties = new ParamProperties(Boolean.class);
        properties.setLabel(LABEL_NAME);
        properties.setPhysicalUnit(UNIT_NAME);
        properties.setDefaultValue(Boolean.TRUE);
        final Parameter parameter = new Parameter(PARAM_NAME, properties);
        ParamEditor editor = parameter.getEditor();
        assertTrue(editor instanceof BooleanEditor);
        final BooleanEditor booleanEditor = (BooleanEditor) editor;

        final JComponent editorComponent = booleanEditor.getEditorComponent();
        assertEquals(JCheckBox.class.getName(), editorComponent.getClass().getName());
        assertEquals("theParamName.Editor", editorComponent.getName());
        assertNull(booleanEditor.getLabelComponent());
        assertNull(booleanEditor.getPhysUnitLabelComponent());
    }

    public void testComboBoxEditor() {
        final ParamProperties properties = new ParamProperties(String[].class);
        properties.setLabel(LABEL_NAME);
        properties.setPhysicalUnit(UNIT_NAME);
        properties.setDefaultValue("ich");
        properties.setValueSet(new String[]{"ich", "du", "er", "sie", "es"});
        properties.setValueSetBound(true);
        properties.setEditorClass(ComboBoxEditor.class);
        final Parameter parameter = new Parameter(PARAM_NAME, properties);
        ParamEditor editor = parameter.getEditor();
        assertTrue(editor instanceof ComboBoxEditor);
        final ComboBoxEditor comboBoxEditor = (ComboBoxEditor) editor;

        final JComponent editorComponent = comboBoxEditor.getEditorComponent();
        assertEquals(JComboBox.class.getName(), editorComponent.getClass().getName());
        assertEquals("theParamName.Editor", editorComponent.getName());
        assertEquals("theParamName.Label", comboBoxEditor.getLabelComponent().getName());
        assertEquals(LABEL_NAME + ": ", comboBoxEditor.getLabelComponent().getText());
        assertNull(comboBoxEditor.getPhysUnitLabelComponent());
    }

    public void testLabelEditor() {
        final ParamProperties properties = new ParamProperties(String.class);
        properties.setLabel(LABEL_NAME);
        properties.setPhysicalUnit(UNIT_NAME);
        properties.setDefaultValue("was");
        properties.setEditorClass(LabelEditor.class);
        final Parameter parameter = new Parameter(PARAM_NAME, properties);
        ParamEditor editor = parameter.getEditor();
        assertTrue(editor instanceof LabelEditor);
        final LabelEditor labelEditor = (LabelEditor) editor;

        final JComponent editorComponent = labelEditor.getEditorComponent();
        assertEquals(JLabel.class.getName(), editorComponent.getClass().getName());
        assertEquals("theParamName.Editor", editorComponent.getName());
        assertNull(labelEditor.getLabelComponent());
        assertNull(labelEditor.getPhysUnitLabelComponent());
    }

    public void testListEditor() {
        final ParamProperties properties = new ParamProperties(String[].class);
        properties.setLabel(LABEL_NAME);
        properties.setPhysicalUnit(UNIT_NAME);
        properties.setDefaultValue("ich");
        properties.setValueSet(new String[]{"ich", "du", "er", "sie", "es"});
        final Parameter parameter = new Parameter(PARAM_NAME, properties);
        ParamEditor editor = parameter.getEditor();
        assertTrue(editor instanceof ListEditor);
        final ListEditor listEditor = (ListEditor) editor;

        final JComponent editorComponent = listEditor.getEditorComponent();
        assertEquals("theParamName.ScrollPane", editorComponent.getName());
        assertEquals(JScrollPane.class.getName(), editorComponent.getClass().getName());
        final Component listComp = ((JScrollPane) editorComponent).getViewport().getView();
        assertEquals("theParamName.Editor", listComp.getName());
        assertEquals(JList.class.getName(), listComp.getClass().getName());
        assertEquals("theParamName.Label", listEditor.getLabelComponent().getName());
        assertEquals(LABEL_NAME + ": ", listEditor.getLabelComponent().getText());
        assertNull(listEditor.getPhysUnitLabelComponent());
    }

    public void testTextFieldEditor() {
        final ParamProperties properties = new ParamProperties(String.class);
        properties.setLabel(LABEL_NAME);
        properties.setPhysicalUnit(UNIT_NAME);
        properties.setDefaultValue("ich");
        final Parameter parameter = new Parameter(PARAM_NAME, properties);
        ParamEditor editor = parameter.getEditor();
        assertTrue(editor instanceof TextFieldEditor);
        final TextFieldEditor textFieldEditor = (TextFieldEditor) editor;

        final JComponent editorComponent = textFieldEditor.getEditorComponent();
        assertEquals("theParamName.Editor", editorComponent.getName());
        assertEquals(JTextField.class.getName(), editorComponent.getClass().getName());
        assertEquals("theParamName.Label", textFieldEditor.getLabelComponent().getName());
        assertEquals(LABEL_NAME + ": ", textFieldEditor.getLabelComponent().getText());
        assertEquals("theParamName.Unit", textFieldEditor.getPhysUnitLabelComponent().getName());
        assertEquals(" " + UNIT_NAME, textFieldEditor.getPhysUnitLabelComponent().getText());
    }

    public void testColorEditor() {
        final ParamProperties properties = new ParamProperties(Color.class);
        properties.setLabel(LABEL_NAME);
        properties.setPhysicalUnit(UNIT_NAME);
        properties.setDefaultValue(Color.red);
        final Parameter parameter = new Parameter(PARAM_NAME, properties);
        ParamEditor editor = parameter.getEditor();
        assertTrue(editor instanceof ColorEditor);
        final ColorEditor colorEditor = (ColorEditor) editor;

        final JComponent editorComponent = colorEditor.getEditorComponent();
        assertEquals("theParamName.XEditor", editorComponent.getName());
        assertEquals(JPanel.class.getName(), editorComponent.getClass().getSuperclass().getName());
        final Component[] components = editorComponent.getComponents();
        assertEquals(2, components.length);
        assertEquals("theParamName.ColorDisplay", components[0].getName());
        assertEquals("theParamName.ButtonPanel", components[1].getName());
        assertEquals(ColorEditor.ColorDisplay.class.getName(),
                     components[0].getClass().getName());
        assertEquals(JPanel.class.getName(), components[1].getClass().getName());
        final JPanel buttonPanel = ((JPanel) components[1]);
        assertEquals(1, buttonPanel.getComponentCount());
        assertEquals(JButton.class.getName(), buttonPanel.getComponents()[0].getClass().getName());
        assertEquals("theParamName.Button", ((JPanel) components[1]).getComponents()[0].getName());
        assertEquals("theParamName.Label", colorEditor.getLabelComponent().getName());
        assertEquals(LABEL_NAME + ": ", colorEditor.getLabelComponent().getText());
        assertEquals("theParamName.Unit", colorEditor.getPhysUnitLabelComponent().getName());
        assertEquals(" " + UNIT_NAME, colorEditor.getPhysUnitLabelComponent().getText());
    }

    public void testFileEditor() {
        final ParamProperties properties = new ParamProperties(File.class);
        properties.setLabel(LABEL_NAME);
        properties.setPhysicalUnit(UNIT_NAME);
        properties.setDefaultValue(new File("sabines_home"));
        final Parameter parameter = new Parameter(PARAM_NAME, properties);
        ParamEditor editor = parameter.getEditor();
        assertTrue(editor instanceof FileEditor);
        final FileEditor fileEditor = (FileEditor) editor;

        final JComponent editorComponent = fileEditor.getEditorComponent();
        assertEquals("theParamName.XEditor", editorComponent.getName());
        assertEquals(JPanel.class.getName(), editorComponent.getClass().getSuperclass().getName());
        final Component[] components = editorComponent.getComponents();
        assertEquals(2, components.length);
        assertEquals("theParamName.Editor", components[0].getName());
        assertEquals("theParamName.ButtonPanel", components[1].getName());
        assertEquals(JTextField.class.getName(), components[0].getClass().getName());
        assertEquals(JPanel.class.getName(), components[1].getClass().getName());
        final JPanel buttonPanel = ((JPanel) components[1]);
        assertEquals(1, buttonPanel.getComponentCount());
        assertEquals(JButton.class.getName(), buttonPanel.getComponents()[0].getClass().getName());
        assertEquals("theParamName.Label", fileEditor.getLabelComponent().getName());
        assertEquals(LABEL_NAME + ": ", fileEditor.getLabelComponent().getText());
        assertEquals("theParamName.Unit", fileEditor.getPhysUnitLabelComponent().getName());
        assertEquals(" " + UNIT_NAME, fileEditor.getPhysUnitLabelComponent().getText());
    }

    public void testCorrectFormatingOfLabelComponentText() {
        AbstractParamEditor testEditor = createTestEditor("label", "unit");
        assertEquals("label: ", testEditor.getLabelComponent().getText());

        testEditor = createTestEditor("label:", "unit");
        assertEquals("label: ", testEditor.getLabelComponent().getText());

        testEditor = createTestEditor("label: ", "unit");
        assertEquals("label: ", testEditor.getLabelComponent().getText());

        testEditor = createTestEditor("label:   ", "unit");
        assertEquals("label: ", testEditor.getLabelComponent().getText());
    }

    public void testCorrectFormatingOfPhysUnitLabelText() {
        AbstractParamEditor testEditor = createTestEditor("label: ", "unit");
        assertEquals(" unit", testEditor.getPhysUnitLabelComponent().getText());

        testEditor = createTestEditor("label: ", "  unit");
        assertEquals("  unit", testEditor.getPhysUnitLabelComponent().getText());

        testEditor = createTestEditor("label: ", "unit  ");
        assertEquals(" unit  ", testEditor.getPhysUnitLabelComponent().getText());

    }

////////////////////////////////////////////////////////////////////////////////////////////////////
// Test-Helper

    private AbstractParamEditor createTestEditor(final String labelName, final String unit) {
        final ParamProperties properties = new ParamProperties(String.class);
        properties.setLabel(labelName);
        properties.setPhysicalUnit(unit);
        final Parameter parameter = new Parameter(PARAM_NAME, properties);
        return new AbstractParamEditor(parameter, true) {
            public JComponent getEditorComponent() {
                return new JLabel();
            }

            @Override
            public JLabel getLabelComponent() {
                return super.getLabelComponent();
            }
        };
    }

}
