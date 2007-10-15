package com.bc.ceres.binding.swing;

import junit.framework.TestCase;

import java.util.HashMap;
import java.io.File;
import java.awt.*;

import com.bc.ceres.binding.Factory;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueSet;

import javax.swing.*;

public class PropertyPaneTest extends TestCase {
    public void testComponentsInPanel() {
        PropertyPane propertyPane = createParameterPane();
        JPanel panel = propertyPane.createPanel();
        Component[] components = panel.getComponents();
        assertEquals(7 + 7 +  1, components.length);  // #labels + #comps + 1 spacer
        HashMap<String,Component> editors = new HashMap<String, Component>(10);
        for (int i = 0; i < components.length - 1; i++) {
            Component component = components[i];
            if (i % 2 == 0) {
                // parameter label
                assertTrue("Label expected at " + i, component instanceof JLabel);
            } else if (i < components.length-1) {
                // parameter editor
                assertTrue("Name missing for component at " + i, component.getName() != null);
                editors.put(component.getName(), component);
            }
        }
        assertTrue(editors.get("threshold") instanceof JTextField);
        assertTrue(editors.get("iterationLimit") instanceof JTextField);
        assertTrue(editors.get("maxIterationCount") instanceof JTextField);
        assertTrue(editors.get("useLogFile") instanceof JCheckBox);
        assertTrue(editors.get("resamplingMethod") instanceof JComboBox);
        assertTrue(editors.get("productDescription") instanceof JTextField);
        assertTrue(editors.get("imageFile") instanceof JPanel);
    }

    private static PropertyPane createParameterPane() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("threshold", 0.5);
        map.put("iterationLimit", 0.1f);
        map.put("maxIterationCount", 10);
        map.put("useLogFile", true);
        map.put("resamplingMethod", "NN");
        map.put("productDescription", "All purpose");
        map.put("imageFile", new File(".").getAbsoluteFile());


        ValueContainer vc = Factory.createMapBackedValueContainer(map);
        vc.getValueDefinition("resamplingMethod").setValueSet(new ValueSet(new String[]{
              "NN", "CC", "BQ"
        }));

        SwingBindingContext sbc = new SwingBindingContext(vc, new SwingBindingContext.ErrorHandler() {
            public void handleError(JComponent component, Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        });
        return new PropertyPane(sbc);
    }

    public static void main(String[] args) {
        PropertyPane propertyPane = createParameterPane();
        JPanel panel = propertyPane.createPanel();
        JFrame frame = new JFrame("PropertyPaneTest");
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
    }
}
