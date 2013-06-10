package org.jlinda.nest.dat.snaphu;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUIUtils;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Prototype for UI for Snaphu data export:
 *  - it's based on CreateStackOpUI
 *  - Eventually I gave up on it since, the export graph it's cleaner and easier to explain in the help
 *  - Anyways this Snaphu link is a temp thing until the 'real' unwrapper is implemented in jLinda/NEST
 * */
public class SnaphuExportOpUI_Prototype extends BaseOperatorUI {

    private final JList ifgBandList = new JList();
    private final JList cohBandList = new JList();

    private final List<Integer> defaultIfgBandIndices = new ArrayList<Integer>(2);
    private final List<Integer> defaultCohBandIndices = new ArrayList<Integer>(2);

    private final JComboBox statCostMode = new JComboBox(new String[]{"TOPO", "DEFO", "SMOOTH", "NOSTATCOSTS"});
    private final JComboBox initMethod = new JComboBox(new String[]{"MST", "MCF"});

    private Product referenceProduct = null;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {
        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();

        initParameters();
        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {
        updateIfgCohSelection();
        statCostMode.setSelectedItem(paramMap.get("statCostMode"));
        initMethod.setSelectedItem(paramMap.get("initMethod"));
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        OperatorUIUtils.updateParamList(ifgBandList, paramMap, "masterBandNames");
        OperatorUIUtils.updateParamList(cohBandList, paramMap, "slaveBandNames");

        paramMap.put("statCostMode", statCostMode.getSelectedItem());
        paramMap.put("initMethod", initMethod.getSelectedItem());
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(new JLabel("InSAR Bands:"), gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        contentPane.add(new JScrollPane(ifgBandList), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;

        contentPane.add(new JLabel("Coherence Bands:"), gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        contentPane.add(new JScrollPane(cohBandList), gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        DialogUtils.addComponent(contentPane, gbc, "Statistical-cost mode:", statCostMode);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Initial method:", initMethod);
        gbc.gridy++;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private void updateIfgCohSelection() {

        final String bandNames[] = getBandNames();

        OperatorUIUtils.initBandList(ifgBandList, bandNames);
        OperatorUIUtils.initBandList(cohBandList, bandNames);

        OperatorUIUtils.setSelectedListIndices(ifgBandList, getSelectedIndices(bandNames,
                                                                (String[])paramMap.get("ifgBandNames"),
                                                                defaultIfgBandIndices));
        OperatorUIUtils.setSelectedListIndices(cohBandList, getSelectedIndices(bandNames,
                                                                (String[])paramMap.get("slaveBandNames"),
                                                                defaultCohBandIndices));
    }

    @Override
    protected String[] getBandNames() {
        if(sourceProducts == null) {
            return new String[] {};
        }
        if(referenceProduct == null && sourceProducts.length > 0) {
            referenceProduct = sourceProducts[0];
        }
        defaultIfgBandIndices.clear();
        defaultCohBandIndices.clear();
        final List<String> bandNames = new ArrayList<String>(5);
        boolean masterBandsSelected = false;
        for(Product prod : sourceProducts) {
            if(sourceProducts.length > 1) {

                final Band[] bands = prod.getBands();
                for(int i=0; i < bands.length; ++i) {
                    final Band band = bands[i];
                    bandNames.add(band.getName()+"::"+prod.getName());
                    final int index = bandNames.size()-1;

                    if(!(band instanceof VirtualBand)) {

                        if(prod == referenceProduct && !masterBandsSelected) {
                            defaultIfgBandIndices.add(index);
                            if(band.getUnit() != null && band.getUnit().equals(Unit.REAL)) {
                                if(i+1 < bands.length) {
                                    final Band qBand = bands[i+1];
                                    if(qBand.getUnit() != null && qBand.getUnit().equals(Unit.IMAGINARY)) {
                                        defaultIfgBandIndices.add(index+1);
                                        bandNames.add(qBand.getName()+"::"+prod.getName());
                                        ++i;
                                    }
                                }
                            }
                            masterBandsSelected = true;
                        } else {
                            defaultCohBandIndices.add(index);
                        }
                    }
                }
            } else {
                bandNames.addAll(Arrays.asList(prod.getBandNames()));
            }
        }

        return bandNames.toArray(new String[bandNames.size()]);
    }

    private static List<Integer> getSelectedIndices(final String[] allBandNames,
                                                         final String[] selBandNames,
                                                         final List<Integer> defaultIndices) {
        final List<Integer> bandIndices = new ArrayList<Integer>(2);
        if(selBandNames != null && selBandNames.length > 0) {
            int i=0;
            for(String bandName : allBandNames) {
                for(String selName : selBandNames) {
                    if(bandName.equals(selName)) {
                        bandIndices.add(i);
                    }
                }
                ++i;
            }
        }

        if(bandIndices.isEmpty())
            return defaultIndices;
        return bandIndices;
    }
}
