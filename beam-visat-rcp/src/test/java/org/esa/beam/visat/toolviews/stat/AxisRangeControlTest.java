package org.esa.beam.visat.toolviews.stat;

import org.junit.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        assertEquals(0.0, axisRangeControl.getBindingContext().getBinding("min").getPropertyValue());
        assertEquals(100.0, axisRangeControl.getBindingContext().getBinding("max").getPropertyValue());
        assertEquals(false, axisRangeControl.getBindingContext().getBinding("autoMinMax").getPropertyValue());
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
