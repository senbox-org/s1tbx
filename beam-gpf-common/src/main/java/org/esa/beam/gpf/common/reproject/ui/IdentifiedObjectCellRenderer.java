package org.esa.beam.gpf.common.reproject.ui;

import org.opengis.referencing.IdentifiedObject;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Component;

/**
 * The code part of the name is used to render the {@link IdentifiedObject}.
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class IdentifiedObjectCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        final Component component = super.getListCellRendererComponent(list, value, index,
                                                                       isSelected, cellHasFocus);
        JLabel label = (JLabel) component;
        if (value != null) {
            IdentifiedObject identifiedObject = (IdentifiedObject) value;
            label.setText(identifiedObject.getName().getCode());
        }

        return label;
    }
}
