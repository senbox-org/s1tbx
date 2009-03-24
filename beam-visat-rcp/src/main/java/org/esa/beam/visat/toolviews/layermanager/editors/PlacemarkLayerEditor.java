package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.glayer.Layer;
import com.jidesoft.combobox.ColorComboBox;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.glayer.PlacemarkLayer;
import org.esa.beam.visat.toolviews.layermanager.LayerEditor;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;

/**
 * Editor for placemark layers.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class PlacemarkLayerEditor implements LayerEditor {
    private JCheckBox textEnabled;
    private ColorComboBox textForegroundColor;
    private ColorComboBox textBackgroundColor;

    public PlacemarkLayerEditor() {
    }

    @Override
    public JComponent createControl() {

        textEnabled = new JCheckBox();
        textForegroundColor = new ColorComboBox();
        textBackgroundColor = new ColorComboBox();

        JPanel control = GridBagUtils.createPanel();
        //control.setBorder(UIUtils.createGroupBorder("Graticule Overlay")); /*I18N*/

        GridBagConstraints gbc = GridBagUtils.createConstraints("fill=HORIZONTAL,anchor=WEST");
        gbc.gridy = 0;

        gbc.insets.top = 10;

        // param = getConfigParam("pin.text.enabled");
        gbc.weightx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        control.add(textEnabled, gbc);
        gbc.gridwidth = 1;
        gbc.gridy++;

        gbc.insets.top = 10;

        // param = getConfigParam("pin.text.fg.color");
        gbc.weightx = 0;
        control.add(new JLabel("Text foreground colour:"), gbc);
        gbc.weightx = 1;
        control.add(textForegroundColor, gbc);
        gbc.gridy++;

        // param = getConfigParam("pin.text.bg.color");
        gbc.weightx = 0;
        control.add(new JLabel("Text background colour:"), gbc);
        gbc.weightx = 1;
        control.add(textBackgroundColor, gbc);
        gbc.gridy++;

        return control;
    }

    @Override
    public void updateControl(Layer selectedLayer) {
        final PlacemarkLayer layer = (PlacemarkLayer) selectedLayer;
        textEnabled.setSelected(layer.isTextEnabled());
        textForegroundColor.setSelectedColor(layer.getTextFgColor());
        textBackgroundColor.setSelectedColor(layer.getTextBgColor());
    }
}
