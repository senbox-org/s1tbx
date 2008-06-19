package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.swing.BindingContext;

import javax.swing.*;
import java.awt.Component;
import java.util.HashMap;
import java.io.File;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import junit.framework.TestCase;


public class ParametersPaneTest extends TestCase {
    public void testComponentsInPanel() throws ConversionException {
        ParametersPane parametersPane = createParametersPane();
        JPanel panel = parametersPane.createPanel();
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

    private static ParametersPane createParametersPane() throws ConversionException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("threshold", 0.5);
        map.put("iterationLimit", 0.1f);
        map.put("maxIterationCount", 10);
        map.put("useLogFile", true);
        map.put("resamplingMethod", "NN");
        map.put("productDescription", "All purpose");
        map.put("imageFile", new File(".").getAbsoluteFile());


        ValueContainer vc = ValueContainerFactory.createMapBackedValueContainer(map);
        vc.getValueDescriptor("threshold").setValueRange(ValueRange.parseValueRange("[0,1)")); // todo - not recognised (nf - 24.10.2007)
        vc.getValueDescriptor("resamplingMethod").setValueSet(
                new ValueSet(new String[]{"NN", "CC", "BQ"}));

        BindingContext sbc = new BindingContext(vc, new BindingContext.ErrorHandler() {
            public void handleError(JComponent component, Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        });
        return new ParametersPane(sbc);
    }

    public static void main(String[] args) throws ConversionException {
        ParametersPane propertyPane = createParametersPane();
        JPanel panel = propertyPane.createPanel();
        JFrame frame = new JFrame("PropertyPaneTest");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);

        propertyPane.getBindingContext().getValueContainer().addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                System.out.println("ValueContainer." + evt.getPropertyName() + " = " + evt.getNewValue());
            }
        });
    }
}
