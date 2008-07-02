package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.jidesoft.combobox.ColorComboBox;

import javax.swing.JComponent;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.esa.beam.framework.datamodel.ImageInfo;

/**
 * A binding for the JIDE {@link ColorComboBox}.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class ColorComboBoxBinding extends Binding {
    ColorComboBox colorComboBox;

    public ColorComboBoxBinding(BindingContext context, String propertyName, ColorComboBox colorComboBox) {
        super(context, propertyName);
        this.colorComboBox = colorComboBox;
        colorComboBox.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                adjustPropertyValue();
            }
        });
    }

    private void adjustPropertyValue() {
        final Color color = colorComboBox.getSelectedColor();
        setValue(color == null ? ImageInfo.NO_COLOR : color);
    }

    @Override
    protected void doAdjustComponents() {
        final Color color = (Color) getValue();
        colorComboBox.setSelectedColor(ImageInfo.NO_COLOR.equals(color) ? null : color);
    }

    @Override
    public JComponent getPrimaryComponent() {
        return colorComboBox;
    }
}
