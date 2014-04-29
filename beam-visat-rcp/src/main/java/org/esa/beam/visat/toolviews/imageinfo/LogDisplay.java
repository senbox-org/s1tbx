package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.datamodel.ColorPaletteDef;

import javax.swing.AbstractButton;
import javax.swing.JOptionPane;
import java.awt.Component;

class LogDisplay {

    static AbstractButton createButton() {
        final AbstractButton logDisplayButton = ImageInfoEditorSupport.createToggleButton("icons/LogDisplay24.png");
        logDisplayButton.setName("logDisplayButton");
        logDisplayButton.setToolTipText("Switch to logarithmic display"); /*I18N*/
        return logDisplayButton;
    }

    static void showNotApplicableInfo(Component parent) {
        JOptionPane.showMessageDialog(parent, "Log display is not applicable!\nThe color palette must contain only positive slider values.");
    }

    static boolean checkApplicability(ColorPaletteDef cpd) {
        final ColorPaletteDef.Point[] points = cpd.getPoints();
        for (ColorPaletteDef.Point point : points) {
            if (point.getSample() <= 0.0) {
                return false;
            }
        }
        return true;
    }
}
