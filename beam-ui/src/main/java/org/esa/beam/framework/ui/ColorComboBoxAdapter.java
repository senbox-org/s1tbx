package org.esa.beam.framework.ui;

import com.bc.ceres.swing.binding.ComponentAdapter;
import com.jidesoft.combobox.ColorComboBox;

import org.esa.beam.framework.datamodel.ImageInfo;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;

/**
 * A binding for the JIDE {@link ColorComboBox}.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ColorComboBoxAdapter extends ComponentAdapter implements PropertyChangeListener {
    private ColorComboBox colorComboBox;

    public ColorComboBoxAdapter(ColorComboBox colorComboBox) {
        this.colorComboBox = colorComboBox;
    }

    @Override
    public JComponent[] getComponents() {
        return new JComponent[] {colorComboBox};
    }

    @Override
    public void bindComponents() {
        colorComboBox.addPropertyChangeListener(this);
    }

    @Override
    public void unbindComponents() {
        colorComboBox.removePropertyChangeListener(this);
    }

    @Override
    public void adjustComponents() {
        final Color color = (Color) getBinding().getPropertyValue();
        colorComboBox.setSelectedColor(ImageInfo.NO_COLOR.equals(color) ? null : color);
    }

    private void adjustPropertyValue() {
        final Color color = colorComboBox.getSelectedColor();
        getBinding().setPropertyValue(color == null ? ImageInfo.NO_COLOR : color);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        adjustPropertyValue();
    }
}
