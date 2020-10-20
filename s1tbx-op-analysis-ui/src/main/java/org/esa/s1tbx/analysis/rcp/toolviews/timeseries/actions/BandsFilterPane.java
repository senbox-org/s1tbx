package org.esa.s1tbx.analysis.rcp.toolviews.timeseries.actions;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.color.ColorComboBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BandsFilterPane {

    private static final Font SMALL_PLAIN_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font SMALL_ITALIC_FONT = SMALL_PLAIN_FONT.deriveFont(Font.ITALIC);

    private Band[] allBands;
    private Band[] selectedBands;
    private final boolean multipleProducts;
    private int numSelected;
    private JCheckBox[] checkBoxes;
    private ColorComboBox[] colorComboBoxes;
    private JCheckBox selectAllCheckBox;
    private JCheckBox selectNoneCheckBox;

    private Map<String, Color> bandColorMap;

    public BandsFilterPane(Band[] allBands, Band[] selectedBands, boolean multipleProducts) {
        this.allBands = allBands;
        this.selectedBands = selectedBands;
        if (this.allBands == null) {
            this.allBands = new Band[0];
        }
        if (this.selectedBands == null) {
            this.selectedBands = new Band[0];
        }
        this.multipleProducts = multipleProducts;
    }

    public Band[] getSelectedBands() {
        checkSelectedBandsAndGrids();
        return selectedBands;
    }

    public void setBandColorMap(Map<String, Color> bandColorMap) {
        this.bandColorMap = bandColorMap;
    }

    public JPanel createCheckersPane() {
        int length = 0;
        if (allBands != null) {
            length += allBands.length;
        }
        checkBoxes = new JCheckBox[length];
        colorComboBoxes = new ColorComboBox[length];

        final JPanel checkersPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("insets.left=4,anchor=NORTHWEST,fill=HORIZONTAL");
        final ActionListener checkListener = createActionListener();
        addBandCheckers(new StringBuffer(), checkersPane, gbc, checkListener);
        GridBagUtils.addVerticalFiller(checkersPane, gbc);
        return checkersPane;
    }

    private void addBandCheckers(final StringBuffer description, final JPanel checkersPane,
                                 final GridBagConstraints gbc, final ActionListener checkListener) {
        for (int i = 0; i < allBands.length; i++) {
            Band band = allBands[i];
            boolean checked = false;
            for (Band selectedBand : selectedBands) {
                if (band == selectedBand) {
                    checked = true;
                    numSelected++;
                    break;
                }
            }

            description.setLength(0);
            description.append(band.getDescription() == null ? "" : band.getDescription());
            if (band.getSpectralWavelength() > 0.0) {
                description.append(" (");
                description.append(band.getSpectralWavelength());
                description.append(" nm)");
            }

            final JCheckBox check = new JCheckBox(getRasterDisplayName(band), checked);
            check.setFont(SMALL_PLAIN_FONT);
            check.addActionListener(checkListener);

            final JLabel label = new JLabel(description.toString());
            label.setFont(SMALL_ITALIC_FONT);

            gbc.gridy++;
            GridBagUtils.addToPanel(checkersPane, check, gbc, "weightx=0,gridx=0");
            GridBagUtils.addToPanel(checkersPane, label, gbc, "weightx=1,gridx=1");

            if(bandColorMap != null) {
                Color color = bandColorMap.get(band.getName());
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

    private String getRasterDisplayName(RasterDataNode rasterDataNode) {
        return multipleProducts ? rasterDataNode.getDisplayName() : rasterDataNode.getName();
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

    public boolean atLeastOneBandSelected() {
        checkSelectedBandsAndGrids();
        return selectedBands.length > 0;
    }

    public void selectRasterDataNodes(final String[] nodeNames) {
        for (int i = 0; i < allBands.length; i++) {
            Band band = allBands[i];
            for (String nodeName : nodeNames) {
                if (nodeName.equals(band.getName())) {
                    checkBoxes[i].setSelected(true);
                    numSelected++;
                    break;
                }
            }
        }
        updateCheckBoxStates();
    }

    private void checkSelectedBandsAndGrids() {
        final List<Band> bands = new ArrayList<>();
        for (int i = 0; i < checkBoxes.length; i++) {
            JCheckBox checkBox = checkBoxes[i];
            if (checkBox.isSelected()) {
                if (allBands.length > i) {
                    bands.add(allBands[i]);
                    if(colorComboBoxes[i] != null) {
                        bandColorMap.put(allBands[i].getName(), colorComboBoxes[i].getSelectedColor());
                    }
                }
            }
        }
        selectedBands = bands.toArray(new Band[0]);
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
