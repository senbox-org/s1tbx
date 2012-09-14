package org.esa.nest.dat.views.polarview;

import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;

/**

 */
class ColourScaleDialog extends ModalDialog {

    private final JTextField min = new JTextField("");
    private final JTextField max = new JTextField("");

    private boolean ok = false;
    private final Axis colourAxis;

    public ColourScaleDialog(Axis colourAxis) {
        super(VisatApp.getApp().getMainFrame(), "Colour Scale", ModalDialog.ID_OK_CANCEL, null);

        this.colourAxis = colourAxis;
        final double[] range = colourAxis.getRange();
        min.setText(String.valueOf(range[0]));
        max.setText(String.valueOf(range[range.length-1]));

        setContent(createPanel());
    }

    @Override
    protected void onOK() {

        colourAxis.setDataRange(Double.parseDouble(min.getText()), Double.parseDouble(max.getText()));
        
        ok = true;
        hide();
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "min:", min);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "max:", max);
        gbc.gridy++;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }
}