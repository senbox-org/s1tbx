package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.TiePointGrid;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

interface BandChoosingStrategy {

    Band[] getSelectedBands();

    TiePointGrid[] getSelectedTiePointGrids();

    JPanel createCheckersPane();

    void updateCheckBoxStates();

    void setCheckBoxes(JCheckBox selectAllCheckBox, JCheckBox selectNoneCheckBox);

    void selectAll();

    void selectNone();

    boolean atLeastOneBandSelected();

}
