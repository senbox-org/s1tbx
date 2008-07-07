package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.swing.BindingContext;
import junit.framework.TestCase;

import javax.swing.*;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;


public class ParametersPaneTest extends TestCase {
    public void testComponentsInPanel() throws ConversionException {
        ParametersPane parametersPane = createParametersPane();
        JPanel panel = parametersPane.createPanel();
        Component[] components = panel.getComponents();
        assertEquals(14, components.length);

        assertEquals(JCheckBox.class, components[0].getClass());
        assertEquals(JLabel.class, components[1].getClass());
        assertEquals(JTextField.class, components[2].getClass());
        assertEquals(JLabel.class, components[3].getClass());
        assertEquals(JTextField.class, components[4].getClass());
        assertEquals(JLabel.class, components[5].getClass());
        assertEquals(JTextField.class, components[6].getClass());
        assertEquals(JLabel.class, components[7].getClass());
        assertEquals(JComboBox.class, components[8].getClass());
        assertEquals(JLabel.class, components[9].getClass());
        assertEquals(JTextField.class, components[10].getClass());
        assertEquals(JLabel.class, components[11].getClass());
        assertEquals(JPanel.class, components[12].getClass());
        assertEquals(JPanel.class, components[13].getClass()); // Spacer!

        assertEquals("useLogFile", components[0].getName());
        assertEquals("Use log file", ((JCheckBox) components[0]).getText());
        assertEquals("Threshold:", ((JLabel) components[1]).getText());
        assertEquals("threshold", components[2].getName());
        assertEquals("Iteration limit:", ((JLabel) components[3]).getText());
        assertEquals("iterationLimit", components[4].getName());
        assertEquals("Max iteration count:", ((JLabel) components[5]).getText());
        assertEquals("maxIterationCount", components[6].getName());
        assertEquals("Resampling method:", ((JLabel) components[7]).getText());
        assertEquals("resamplingMethod", components[8].getName());
        assertEquals("Product description:", ((JLabel) components[9]).getText());
        assertEquals("productDescription", components[10].getName());
        assertEquals("Image file:", ((JLabel) components[11]).getText());
        assertEquals("imageFile", components[12].getName());
    }

    private static class V {
        boolean useLogFile = true;
        double threshold = 0.5;
        float iterationLimit = 0.1f;
        int maxIterationCount = 10;
        String resamplingMethod = "NN";
        String productDescription = "All purpose";
        File imageFile = new File(".").getAbsoluteFile();
    }

    private static ParametersPane createParametersPane() throws ConversionException {
        ValueContainer vc = ValueContainer.createObjectBacked(new V());

        vc.getDescriptor("threshold").setValueRange(ValueRange.parseValueRange("[0,1)")); // todo - not recognised (nf - 24.10.2007)
        vc.getDescriptor("resamplingMethod").setValueSet(
                new ValueSet(new String[]{"NN", "CC", "BQ"}));

        BindingContext sbc = new BindingContext(vc, new BindingContext.ErrorHandler() {
            @Override
            public void handleError(Exception e, JComponent component) {
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
