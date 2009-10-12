package org.esa.beam.framework.ui;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.BindingProblemListener;
import com.bc.ceres.binding.swing.BindingProblem;
import junit.framework.TestCase;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;


public class ValueEditorsPaneTest extends TestCase {

    public void testComponentsInPanel() throws ConversionException {
        ValueEditorsPane parametersPane = createPane(new BindingContext.SilentProblemHandler());
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

    public static void main(String[] args) throws ConversionException {
        final ModalDialog dialog = new ModalDialog(null, "ValueEditorsPaneTest", ModalDialog.ID_OK + ModalDialog.ID_CANCEL, null) {
            @Override
            protected void onOK() {
                hide();
            }

            @Override
            protected void onCancel() {
                hide();
            }
        };

        ValueEditorsPane propertyPane = createPane(new BindingProblemListener() {
            @Override
            public void problemReported(BindingProblem newProblem, BindingProblem oldProblem) {
                JOptionPane.showMessageDialog(dialog.getJDialog(), newProblem.getCause().getMessage());
            }

            @Override
            public void problemCleared(BindingProblem oldProblem) {
            }
        });
        JPanel panel = propertyPane.createPanel();
        dialog.setContent(panel);
        dialog.show();

        propertyPane.getBindingContext().getValueContainer().addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                System.out.println("ValueContainer." + evt.getPropertyName() + " = " + evt.getNewValue());
            }
        });
    }

    private static ValueEditorsPane createPane(BindingProblemListener bpl) throws ConversionException {
        ValueContainer vc = ValueContainer.createObjectBacked(new V());

        vc.getDescriptor("threshold").setValueRange(
                ValueRange.parseValueRange("[0,1)")); // todo - not recognised (nf - 24.10.2007)
        vc.getDescriptor("resamplingMethod").setValueSet(
                new ValueSet(new String[]{"NN", "CC", "BQ"}));

        BindingContext sbc = new BindingContext(vc, bpl);
        return new ValueEditorsPane(sbc);
    }

}
