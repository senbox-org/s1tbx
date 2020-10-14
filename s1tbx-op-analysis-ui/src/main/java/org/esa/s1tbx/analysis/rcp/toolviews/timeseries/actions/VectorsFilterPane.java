package org.esa.s1tbx.analysis.rcp.toolviews.timeseries.actions;

import org.esa.s1tbx.analysis.rcp.toolviews.timeseries.graphs.VectorGraph;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.color.ColorComboBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VectorsFilterPane {

    private static final Font SMALL_PLAIN_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font SMALL_ITALIC_FONT = SMALL_PLAIN_FONT.deriveFont(Font.ITALIC);

    private String[] allVectors;
    private String[] selectedVectors;
    private final boolean multipleProducts;
    private int numSelected;
    private JCheckBox[] checkBoxes;
    private ColorComboBox[] colorComboBoxes;
    private JCheckBox selectAllCheckBox;
    private JCheckBox selectNoneCheckBox;

    private JToggleButton meanBtn;
    private JToggleButton stdDevBtn;

    private Map<String, Color> vectorColorMap;

    public VectorsFilterPane(final String[] allVectors, final String[] selectedVectors, final boolean multipleProducts) {
        this.allVectors = allVectors;
        this.selectedVectors = selectedVectors;
        if (this.allVectors == null) {
            this.allVectors = new String[0];
        }
        if (this.selectedVectors == null) {
            this.selectedVectors = new String[0];
        }
        this.multipleProducts = multipleProducts;
    }

    public String[] getSelectedVectors() {
        checkSelected();
        return selectedVectors;
    }

    public VectorGraph.TYPE getStatistic() {
        if(stdDevBtn.isSelected()) {
            return VectorGraph.TYPE.STD_DEV;
        }
        return VectorGraph.TYPE.AVERAGE;
    }

    public void setVectorColorMap(Map<String, Color> vectorColorMap) {
        this.vectorColorMap = vectorColorMap;
    }

    public JPanel createCheckersPane(VectorGraph.TYPE statistic) {
        int length = 0;
        if (allVectors != null) {
            length += allVectors.length;
        }
        checkBoxes = new JCheckBox[length];
        colorComboBoxes = new ColorComboBox[length];

        final JPanel checkersPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("insets.left=4,anchor=NORTHWEST,fill=HORIZONTAL");
        final ActionListener checkListener = createActionListener();
        addCheckers(new StringBuffer(), checkersPane, gbc, checkListener);
        GridBagUtils.addVerticalFiller(checkersPane, gbc);

        final JPanel content = new JPanel(new BorderLayout());
        content.add(checkersPane, BorderLayout.CENTER);

        final JPanel statsPanel = new JPanel();
        statsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Statistic"));
        content.add(statsPanel, BorderLayout.SOUTH);

        final ButtonGroup group = new ButtonGroup();
        meanBtn = new JRadioButton("Mean");
        stdDevBtn = new JRadioButton("Standard Deviation");
        statsPanel.add(meanBtn);
        statsPanel.add(stdDevBtn);
        group.add(meanBtn);
        group.add(stdDevBtn);

        meanBtn.setSelected(statistic.equals(VectorGraph.TYPE.AVERAGE));
        stdDevBtn.setSelected(statistic.equals(VectorGraph.TYPE.STD_DEV));

        return content;
    }

    private void addCheckers(final StringBuffer description, final JPanel checkersPane,
                                 final GridBagConstraints gbc, final ActionListener checkListener) {
        for (int i = 0; i < allVectors.length; i++) {
            String vector = allVectors[i];
            boolean checked = false;
            for (String selectedVector : selectedVectors) {
                if (vector.equals(selectedVector)) {
                    checked = true;
                    numSelected++;
                    break;
                }
            }

            description.setLength(0);

            final JCheckBox check = new JCheckBox(vector, checked);
            check.setFont(SMALL_PLAIN_FONT);
            check.addActionListener(checkListener);

            final JLabel label = new JLabel(description.toString());
            label.setFont(SMALL_ITALIC_FONT);

            gbc.gridy++;
            GridBagUtils.addToPanel(checkersPane, check, gbc, "weightx=0,gridx=0");
            GridBagUtils.addToPanel(checkersPane, label, gbc, "weightx=1,gridx=1");

            if(vectorColorMap != null) {
                Color color = vectorColorMap.get(vector);
                if (color != null) {
                    final ColorComboBox colorCombo = new ColorComboBox();
                    colorCombo.setSelectedColor(color);
                    GridBagUtils.addToPanel(checkersPane, colorCombo, gbc, "weightx=1,gridx=2");
                    colorComboBoxes[i] = colorCombo;
                }
            }

            checkBoxes[i] = check;
        }
    }

    private ActionListener createActionListener() {
        return e -> {
            final JCheckBox check = (JCheckBox) e.getSource();
            if (check.isSelected()) {
                numSelected++;
            } else {
                numSelected--;
            }
            updateCheckBoxStates();
        };
    }

    public void updateCheckBoxStates() {
        if (selectAllCheckBox != null) {
            selectAllCheckBox.setSelected(numSelected == checkBoxes.length);
            selectAllCheckBox.setEnabled(numSelected < checkBoxes.length);
            selectAllCheckBox.updateUI();
        }
        if (selectNoneCheckBox != null) {
            selectNoneCheckBox.setSelected(numSelected == 0);
            selectNoneCheckBox.setEnabled(numSelected > 0);
            selectNoneCheckBox.updateUI();
        }
    }

    public void setCheckBoxes(JCheckBox selectAllCheckBox, JCheckBox selectNoneCheckBox) {
        this.selectAllCheckBox = selectAllCheckBox;
        this.selectNoneCheckBox = selectNoneCheckBox;
        updateCheckBoxStates();
    }

    public void selectAll() {
        select(true);
    }

    public void selectNone() {
        select(false);
    }

    public boolean atLeastOneSelected() {
        checkSelected();
        return selectedVectors.length > 0;
    }

    public void selectRasterDataNodes(final String[] nodeNames) {
        for (int i = 0; i < allVectors.length; i++) {
            String vector = allVectors[i];
            for (String nodeName : nodeNames) {
                if (nodeName.equals(vector)) {
                    checkBoxes[i].setSelected(true);
                    numSelected++;
                    break;
                }
            }
        }
        updateCheckBoxStates();
    }

    private void checkSelected() {
        final List<String> selected = new ArrayList<>();
        for (int i = 0; i < checkBoxes.length; i++) {
            JCheckBox checkBox = checkBoxes[i];
            if (checkBox.isSelected()) {
                if (allVectors.length > i) {
                    selected.add(allVectors[i]);
                    if(colorComboBoxes[i] != null) {
                        vectorColorMap.put(allVectors[i], colorComboBoxes[i].getSelectedColor());
                    }
                }
            }
        }
        selectedVectors = selected.toArray(new String[0]);
    }

    private void select(boolean b) {
        for (JCheckBox checkBox : checkBoxes) {
            if (b && !checkBox.isSelected()) {
                numSelected++;
            }
            if (!b && checkBox.isSelected()) {
                numSelected--;
            }
            checkBox.setSelected(b);
        }
        updateCheckBoxStates();
    }

}
