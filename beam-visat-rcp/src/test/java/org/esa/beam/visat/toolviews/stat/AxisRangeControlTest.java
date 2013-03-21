package org.esa.beam.visat.toolviews.stat;

import com.jidesoft.swing.TitledSeparator;
import org.junit.Test;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import java.awt.Component;
import java.awt.GridLayout;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class AxisRangeControlTest {

    @Test
    public void testBindingContextInPlace() throws Exception {
        final AxisRangeControl axisRangeControl = new AxisRangeControl("X-Axis");
        axisRangeControl.getPanel();
        assertNotNull(axisRangeControl.getBindingContext());
        assertNotNull(axisRangeControl.getBindingContext().getBinding("autoMinMax"));
        assertNotNull(axisRangeControl.getBindingContext().getBinding("min"));
        assertNotNull(axisRangeControl.getBindingContext().getBinding("max"));
    }

    @Test
    public void testInitialValues() throws Exception {
        final AxisRangeControl axisRangeControl = new AxisRangeControl("");
        axisRangeControl.getPanel();
        assertEquals(true, axisRangeControl.getBindingContext().getBinding("autoMinMax").getPropertyValue());
        assertEquals((Double) 0.0, axisRangeControl.getMin());
        assertEquals((Double) 100.0, axisRangeControl.getMax());
    }

    @Test
    public void testMinMaxSetterAndGetter() {
        final AxisRangeControl axisRangeControl = new AxisRangeControl("");
        axisRangeControl.adjustComponents(3.4, 13.8, 2);
        assertEquals((Double) 3.4, axisRangeControl.getMin());
        assertEquals((Double) 13.8, axisRangeControl.getMax());
    }

    @Test
    public void testVerySmallValues() {
        final AxisRangeControl axisRangeControl = new AxisRangeControl("");

        int numDecimalPlaces = 2;
        axisRangeControl.adjustComponents(-5e-9, 5e-9, numDecimalPlaces);

        assertEquals(0.0, axisRangeControl.getMin(), 1e-7);
        assertEquals(Math.pow(10, -numDecimalPlaces), axisRangeControl.getMax(), 1e-7);
    }

    @Test
    public void testSetTitle() {
        final String axisName = "Titel";
        final AxisRangeControl control = new AxisRangeControl(axisName);
        final JPanel rangeControlPanel = control.getPanel();

        final Component component = rangeControlPanel.getComponent(0);
        assertTrue(component instanceof TitledSeparator);
        final TitledSeparator titledSeparator = (TitledSeparator) component;
        final JLabel titleLabel = (JLabel) titledSeparator.getLabelComponent();
        assertEquals(axisName, titleLabel.getText());

        control.setTitleSuffix("radiance_3");

        assertEquals(axisName + " (radiance_3)", titleLabel.getText());

        control.setTitleSuffix("");

        assertEquals(axisName, titleLabel.getText());

        control.setTitleSuffix("radiance_5");

        assertEquals(axisName + " (radiance_5)", titleLabel.getText());

        control.setTitleSuffix(null);

        assertEquals(axisName, titleLabel.getText());
    }

    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, IllegalAccessException, InstantiationException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        final JPanel axesPanel = new JPanel(new GridLayout(-1, 1));
        axesPanel.add(new AxisRangeControl("X-Axis").getPanel());
        axesPanel.add(new AxisRangeControl("Y-Axis").getPanel());

        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(axesPanel);
        frame.pack();
        frame.setVisible(true);
    }

}
