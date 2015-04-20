package org.jlinda.nest.gpf;

import org.esa.snap.framework.ui.AppContext;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ui.BaseOperatorUI;
import org.esa.snap.gpf.ui.UIValidation;
import org.esa.snap.util.DialogUtils;
import org.jlinda.core.coregistration.LUT;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Map;

public class CrossResamplingOpUI extends BaseOperatorUI {

    private final JComboBox targetGeometry = new JComboBox(new String[]{"ERS", "Envisat ASAR"});
    private final JComboBox warpPolynomialOrder = new JComboBox(new String[] { "1","2","3" } );
    private final JComboBox interpolationMethod = new JComboBox(new String[] {
           LUT.CC4P, LUT.CC6P, LUT.TS6P, LUT.TS8P, LUT.TS16P} );

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        targetGeometry.setSelectedItem(paramMap.get("targetGeometry"));
        warpPolynomialOrder.setSelectedItem(paramMap.get("warpPolynomialOrder"));

        if (sourceProducts != null && sourceProducts.length > 0) {
            final boolean isComplex = OperatorUtils.isComplex(sourceProducts[0]);
        }

        interpolationMethod.setSelectedItem(paramMap.get("interpolationMethod"));
    }


    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        paramMap.put("targetGeometry", targetGeometry.getSelectedItem());
        paramMap.put("warpPolynomialOrder", Integer.parseInt((String) warpPolynomialOrder.getSelectedItem()));
        paramMap.put("interpolationMethod", interpolationMethod.getSelectedItem());
    }



    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Target geometry:", targetGeometry);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Warp Polynomial Order:", warpPolynomialOrder);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Interpolation Method:", interpolationMethod);
        gbc.gridy++;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

}
