package org.jlinda.nest.dat.coregistration;

import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.util.DialogUtils;
import org.jlinda.nest.gpf.coregistration.ResampleOp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

public class ResampleOpUI extends BaseOperatorUI {

    private final JComboBox cpmDegree = new JComboBox(new String[]{"1", "2", "3"});
    private final JComboBox cpmInterpKernel = new JComboBox(new String[] {ResampleOp.TRI,
            ResampleOp.CC4P, ResampleOp.CC6P, ResampleOp.TS6P, ResampleOp.TS8P, ResampleOp.TS16P} );
    private final JTextField cpmMaxIterations = new JTextField("");

    private final JComboBox cpmAlphaValue = new JComboBox(new String[]{"0.001", "0.05", "0.1"});

    final JCheckBox cpmDemRefinementCheckBox = new JCheckBox("Offset Refinement Based on DEM");
    boolean cpmDemRefinement;

    final JCheckBox openResidualsFileCheckBox = new JCheckBox("Show Residuals");
    boolean openResidualsFile;


    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        openResidualsFileCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                openResidualsFile = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        cpmDegree.setSelectedItem(paramMap.get("cpmDegree"));
        cpmMaxIterations.setText(String.valueOf(paramMap.get("cpmMaxIterations")));
        cpmInterpKernel.setSelectedItem(paramMap.get("cpmInterpKernel"));
        cpmAlphaValue.setSelectedItem(paramMap.get("cpmAlphaValue"));

//        if(sourceProducts != null && sourceProducts.length > 0) {
//            final boolean isComplex = OperatorUtils.isComplex(sourceProducts[0]);
//        }

    }


    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");

    }

    @Override
    public void updateParameters() {

        paramMap.put("cpmDegree", Integer.parseInt((String) cpmDegree.getSelectedItem()));
        paramMap.put("cpmInterpKernel", cpmInterpKernel.getSelectedItem());
        paramMap.put("cpmMaxIterations", Float.parseFloat(cpmMaxIterations.getText()));
        paramMap.put("cpmAlphaValue", Float.parseFloat((String) cpmAlphaValue.getSelectedItem()));

        paramMap.put("cpmDemRefinement", cpmDemRefinement);
        paramMap.put("openResidualsFile", openResidualsFile);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Coregistration Polynomial Degree:", cpmDegree);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Maximum number of iterations:", cpmMaxIterations);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Significance Level for Outlier Removal:", cpmAlphaValue);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Interpolation Method:", cpmInterpKernel);
        gbc.gridy++;
        contentPane.add(cpmDemRefinementCheckBox, gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(openResidualsFileCheckBox, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

}
