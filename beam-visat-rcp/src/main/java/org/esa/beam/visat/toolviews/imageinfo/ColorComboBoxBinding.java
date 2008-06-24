package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.jidesoft.combobox.ColorComboBox;

import javax.swing.JComponent;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A binding for the JIDE {@link ColorComboBox}.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class ColorComboBoxBinding extends Binding {
    ColorComboBox colorComboBox;

    public ColorComboBoxBinding(BindingContext context, ColorComboBox colorComboBox, String propertyName) {
        super(context, propertyName);
        this.colorComboBox = colorComboBox;
        colorComboBox.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                adjustPropertyValue();
            }
        });
    }

    private void adjustPropertyValue() {
        setValue(colorComboBox.getSelectedColor());
    }

    @Override
    protected void doAdjustComponents() {
        colorComboBox.setSelectedColor((Color) getValue());
    }

    @Override
    public JComponent getPrimaryComponent() {
        return colorComboBox;
    }
}
