package org.esa.beam.visat.toolviews.imageinfo;

import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ComponentAdapter;
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
class ColorComboBoxAdapter extends ComponentAdapter implements PropertyChangeListener {
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

    public void propertyChange(PropertyChangeEvent evt) {
        adjustPropertyValue();
    }
}
