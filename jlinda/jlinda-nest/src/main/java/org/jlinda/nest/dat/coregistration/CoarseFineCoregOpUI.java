package org.jlinda.nest.dat.coregistration;

import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.util.DialogUtils;
import org.jlinda.nest.gpf.coregistration.CoarseFineCoregOp;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class CoarseFineCoregOpUI extends BaseOperatorUI {

    private final JTextField numWindows = new JTextField("");

    // coarse coregistration
    private final JComboBox coarseRegistrationWindowWidth = new JComboBox(new String[] { "32","64","128","256","512","1024","2048" } );
    private final JComboBox coarseRegistrationWindowHeight = new JComboBox(new String[] { "32","64","128","256","512","1024","2048" } );
    private final JComboBox coarseRegistrationWindowAccAzimuth = new JComboBox(new String[] {"2", "4", "8", "16", "32", "64", "128", "256"} );
    private final JComboBox coarseRegistrationWindowAccRange = new JComboBox(new String[] {"2", "4", "8", "16", "32", "64", "128", "256"} );

    // fine coregistration

    private final JComboBox fineMethod = new JComboBox(new String[] {
            CoarseFineCoregOp.MAG_FFT, CoarseFineCoregOp.MAG_SPACE, CoarseFineCoregOp.MAG_OVERSAMPLE} );

    private final JComboBox fineRegistrationWindowWidth = new JComboBox(new String[]{"8", "16", "32", "64", "128"});
    private final JComboBox fineRegistrationWindowHeight = new JComboBox(new String[]{"8", "16", "32", "64", "128"});

    private final JComboBox fineRegistrationWindowAccAzimuth = new JComboBox(new String[]{"2", "4", "8", "16", "64"});
    private final JComboBox fineRegistrationWindowAccRange = new JComboBox(new String[]{"2", "4", "8", "16", "64"});

    private final JComboBox fineRegistrationOversampling = new JComboBox(new String[]{"2", "4", "8", "16", "64"});

    private final JTextField coherenceThreshold = new JTextField("");

    private boolean isComplex = true;
    private boolean applyFineRegistration = true;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        numWindows.setText(String.valueOf(paramMap.get("numWindows")));
        coarseRegistrationWindowWidth.setSelectedItem(paramMap.get("coarseRegistrationWindowWidth"));
        coarseRegistrationWindowHeight.setSelectedItem(paramMap.get("coarseRegistrationWindowHeight"));
        coarseRegistrationWindowAccAzimuth.setSelectedItem(paramMap.get("coarseRegistrationWindowAccAzimuth"));
        coarseRegistrationWindowAccRange.setSelectedItem(paramMap.get("coarseRegistrationWindowAccRange"));

        fineMethod.setSelectedItem(paramMap.get("fineMethod"));
        fineRegistrationWindowWidth.setSelectedItem(paramMap.get("fineRegistrationWindowWidth"));
        fineRegistrationWindowHeight.setSelectedItem(paramMap.get("fineRegistrationWindowHeight"));
        fineRegistrationWindowAccAzimuth.setSelectedItem(paramMap.get("fineRegistrationWindowAccAzimuth"));
        fineRegistrationWindowAccRange.setSelectedItem(paramMap.get("fineRegistrationWindowAccRange"));
        fineRegistrationOversampling.setSelectedItem(paramMap.get("fineRegistrationOversampling"));

        coherenceThreshold.setText(String.valueOf(paramMap.get("coherenceThreshold")));

    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("numWindows", Integer.parseInt(numWindows.getText()));
        paramMap.put("coarseRegistrationWindowWidth", coarseRegistrationWindowWidth.getSelectedItem());
        paramMap.put("coarseRegistrationWindowHeight", coarseRegistrationWindowHeight.getSelectedItem());
        paramMap.put("coarseRegistrationWindowAccAzimuth", coarseRegistrationWindowAccAzimuth.getSelectedItem());
        paramMap.put("coarseRegistrationWindowAccRange", coarseRegistrationWindowAccRange.getSelectedItem());

        paramMap.put("fineMethod", fineMethod.getSelectedItem());
        paramMap.put("fineRegistrationWindowWidth", fineRegistrationWindowWidth.getSelectedItem());
        paramMap.put("fineRegistrationWindowHeight", fineRegistrationWindowHeight.getSelectedItem());
        paramMap.put("fineRegistrationWindowAccAzimuth", fineRegistrationWindowAccAzimuth.getSelectedItem());
        paramMap.put("fineRegistrationWindowAccRange", fineRegistrationWindowAccRange.getSelectedItem());
        paramMap.put("fineRegistrationOversampling", fineRegistrationOversampling.getSelectedItem());

        paramMap.put("coherenceThreshold", Double.parseDouble(coherenceThreshold.getText()));

    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Number of Correlation Optimization Windows:", numWindows);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Coarse Window Width:", coarseRegistrationWindowWidth);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Coarse Window Height:", coarseRegistrationWindowHeight);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Coarse Accuracy in Azimuth:", coarseRegistrationWindowAccAzimuth);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Coarse Accuracy in Range:", coarseRegistrationWindowAccRange);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Fine Correlation Method:", fineMethod);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Fine Window Width:", fineRegistrationWindowWidth);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Fine Window Height:", fineRegistrationWindowHeight);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Fine Accuracy in Azimuth:", fineRegistrationWindowAccAzimuth);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Fine Accuracy in Range:", fineRegistrationWindowAccRange);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Fine Oversampling Factor:", fineRegistrationOversampling);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Coherence Threshold:", coherenceThreshold);
        gbc.gridy++;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }


}