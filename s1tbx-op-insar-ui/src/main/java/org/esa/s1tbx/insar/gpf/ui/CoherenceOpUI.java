/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s1tbx.insar.gpf.ui;

import org.esa.s1tbx.insar.gpf.CoherenceOp;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * User interface for CreateCoherenceImageOp
 */
public class CoherenceOpUI extends BaseOperatorUI {

    private final JCheckBox squarePixelCheckBox = new JCheckBox("Square Pixel");
    private final JCheckBox independentWindowSizeCheckBox = new JCheckBox("Independent Window Sizes");
    private final JCheckBox subtractFlatEarthPhaseCheckBox = new JCheckBox(
            "Subtract flat-earth phase in coherence phase");

    private final JTextField cohWinAz = new JTextField("");
    private final JTextField cohWinRg = new JTextField("");

    private final JComboBox<Integer> srpPolynomialDegreeStr = new JComboBox(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8});
    private final JComboBox<Integer> srpNumberPointsStr = new JComboBox(new Integer[]{301, 401, 501, 601, 701, 801, 901, 1001});
    private final JComboBox<Integer> orbitDegreeStr = new JComboBox(new Integer[]{1, 2, 3, 4, 5});

    private static final JLabel cohWinAzLabel = new JLabel("Coherence Azimuth Window Size");
    private static final JLabel cohWinRgLabel = new JLabel("Coherence Range Window Size");
    private static final JLabel srpPolynomialDegreeStrLabel = new JLabel("Degree of \"Flat Earth\" polynomial");
    private static final JLabel srpNumberPointsStrLabel = new JLabel("Number of \"Flat Earth\" estimation points");
    private static final JLabel orbitDegreeStrLabel = new JLabel("Orbit interpolation degree");

    private Boolean squarePixel = true;
    private Boolean subtractFlatEarthPhase = false;
    private final CoherenceOp.DerivedParams param = new CoherenceOp.DerivedParams();

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);

        final JComponent panel = new JScrollPane(createPanel());
        initParameters();

        subtractFlatEarthPhaseCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {

                subtractFlatEarthPhase = (e.getStateChange() == ItemEvent.SELECTED);
                if (subtractFlatEarthPhase) {
                    srpPolynomialDegreeStr.setEnabled(true);
                    srpNumberPointsStr.setEnabled(true);
                    orbitDegreeStr.setEnabled(true);
                } else {
                    srpPolynomialDegreeStr.setEnabled(false);
                    srpNumberPointsStr.setEnabled(false);
                    orbitDegreeStr.setEnabled(false);
                }
            }
        });

        squarePixelCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                squarePixel = (e.getStateChange() == ItemEvent.SELECTED);
                independentWindowSizeCheckBox.setSelected(!squarePixel);
                if (squarePixel) {
                    cohWinAz.setText("1");
                    cohWinAz.setEditable(false);
                }
                setCohWinAz();
                setCohWinRg();
            }
        });

        independentWindowSizeCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                squarePixel = (e.getStateChange() != ItemEvent.SELECTED);
                squarePixelCheckBox.setSelected(squarePixel);
                if (!squarePixel) {
                    cohWinAz.setEditable(true);
                }
                setCohWinAz();
                setCohWinRg();
            }
        });

        return panel;
    }

    @Override
    public void initParameters() {

        cohWinAz.setText(String.valueOf(paramMap.get("cohWinAz")));
        cohWinRg.setText(String.valueOf(paramMap.get("cohWinRg")));

        Boolean paramVal;
        paramVal = (Boolean) paramMap.get("subtractFlatEarthPhase");
        if (paramVal != null) {
            subtractFlatEarthPhase = paramVal;
            subtractFlatEarthPhaseCheckBox.setSelected(subtractFlatEarthPhase);
        }

        srpPolynomialDegreeStr.setSelectedItem(paramMap.get("srpPolynomialDegree"));
        srpNumberPointsStr.setSelectedItem(paramMap.get("srpNumberPoints"));
        orbitDegreeStr.setSelectedItem(paramMap.get("orbitDegree"));

        squarePixel = (Boolean) paramMap.get("squarePixel");
        if (squarePixel != null) {
            squarePixelCheckBox.setSelected(squarePixel);
            independentWindowSizeCheckBox.setSelected(!squarePixel);
            if (squarePixel) {
                cohWinAz.setText("1");
                cohWinAz.setEditable(false);
            } else {
                cohWinAz.setEditable(true);
            }
        }

        setCohWinAz();
        setCohWinRg();

        if(sourceProducts != null && sourceProducts.length > 0) {
            boolean isComplex = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]).
                    getAttributeString(AbstractMetadata.SAMPLE_TYPE).contains("COMPLEX");

            enableControls(isComplex);
        }
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("cohWinAz", Integer.parseInt(cohWinAz.getText()));
        paramMap.put("cohWinRg", Integer.parseInt(cohWinRg.getText()));

        paramMap.put("subtractFlatEarthPhase", subtractFlatEarthPhase);

        if (subtractFlatEarthPhase) {
			paramMap.put("srpPolynomialDegree", srpPolynomialDegreeStr.getSelectedItem());
            paramMap.put("srpNumberPoints", srpNumberPointsStr.getSelectedItem());
			paramMap.put("orbitDegree", orbitDegreeStr.getSelectedItem());
		}
    }

    JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridx = 0;
        contentPane.add(squarePixelCheckBox, gbc);

        gbc.gridx = 1;
        contentPane.add(independentWindowSizeCheckBox, gbc);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, cohWinRgLabel, cohWinRg);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, cohWinAzLabel, cohWinAz);
        cohWinAz.setEditable(false);
        cohWinRg.setDocument(new CohWinRgDocument());

        gbc.gridy++;
        contentPane.add(subtractFlatEarthPhaseCheckBox, gbc);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, srpPolynomialDegreeStrLabel, srpPolynomialDegreeStr);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, srpNumberPointsStrLabel, srpNumberPointsStr);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, orbitDegreeStrLabel, orbitDegreeStr);

        srpPolynomialDegreeStr.setEnabled(false);
        srpNumberPointsStr.setEnabled(false);
        orbitDegreeStr.setEnabled(false);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private void enableControls(final boolean flag) {
        subtractFlatEarthPhaseCheckBox.setEnabled(flag);
    }


    private synchronized void setCohWinAz() {
        if (sourceProducts != null && sourceProducts.length > 0) {
            try {
                if (squarePixelCheckBox.isSelected()) {
                    param.cohWinRg = Integer.parseInt(cohWinRg.getText());
                    CoherenceOp.getDerivedParameters(sourceProducts[0], param);
                    cohWinAz.setText(String.valueOf(param.cohWinAz));
                }
            } catch (Exception e) {
            }
        }
    }

    private void setCohWinRg() {
        if (sourceProducts != null && sourceProducts.length > 0) {
            if (squarePixelCheckBox.isSelected()) {
                cohWinRg.setText(String.valueOf(param.cohWinRg));
            }
        }
    }

    @SuppressWarnings("serial")
    private class CohWinRgDocument extends PlainDocument {

        @Override
        public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            super.replace(offset, length, text, attrs);

            setCohWinAz();
        }
    }
}
