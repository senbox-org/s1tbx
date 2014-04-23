package org.esa.beam.visat.toolviews.imageinfo;

import javax.swing.JCheckBox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class DiscreteCheckBox extends JCheckBox {

    private boolean shouldFireDiscreteEvent;

    DiscreteCheckBox(final ColorManipulationForm parentForm) {
        super("Discrete colors");

        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (shouldFireDiscreteEvent) {
                    parentForm.getImageInfo().getColorPaletteDef().setDiscrete(isSelected());
                    parentForm.applyChanges();
                }
            }
        });
    }

    void setDiscreteColorsMode(boolean discrete) {
        shouldFireDiscreteEvent = false;
        setSelected(discrete);
        shouldFireDiscreteEvent = true;
    }
}
