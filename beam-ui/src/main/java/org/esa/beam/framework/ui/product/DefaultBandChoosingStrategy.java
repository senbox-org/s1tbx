package org.esa.beam.framework.ui.product;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.GridBagUtils;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class DefaultBandChoosingStrategy implements BandChoosingStrategy {

    // @todo 3 nf/se - see ProductSubsetDialog for a similar declarations  (code smell!)
    private static final Font _SMALL_PLAIN_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font _SMALL_ITALIC_FONT = _SMALL_PLAIN_FONT.deriveFont(Font.ITALIC);

    private final Band[] _allBands;
    private Band[] _selectedBands;
    private final TiePointGrid[] _allTiePointGrids;
    private TiePointGrid[] _selectedTiePointGrids;
    private final int _allBandsLength;
    private boolean _multipleProducts;
    private int _numSelected;
    private JCheckBox[] _checkBoxes;
    private JCheckBox _selectAllCheckBox;
    private JCheckBox _selectNoneCheckBox;

    public DefaultBandChoosingStrategy(Band[] allBands, Band[] selectedBands, TiePointGrid[] allTiePointGrids,
                                       TiePointGrid[] selectedTiePointGrids, boolean multipleProducts) {
        this._allBands = allBands;
        this._selectedBands = selectedBands;
        this._allTiePointGrids = allTiePointGrids;
        this._selectedTiePointGrids = selectedTiePointGrids;
        if (_selectedBands == null) {
            _selectedBands = new Band[0];
        }
        if (_selectedTiePointGrids == null) {
            _selectedTiePointGrids = new TiePointGrid[0];
        }
        if (allBands != null) {
            _allBandsLength = allBands.length;
        } else {
            _allBandsLength = 0;
        }
        _multipleProducts = multipleProducts;
    }

    @Override
    public Band[] getSelectedBands() {
        return _selectedBands;
    }

    @Override
    public TiePointGrid[] getSelectedTiePointGrids() {
        return _selectedTiePointGrids;
    }

    @Override
    public JPanel createCheckersPane() {
        int length = 0;
        if (_allBands != null) {
            length += _allBandsLength;
        }
        if (_allTiePointGrids != null) {
            length += _allTiePointGrids.length;
        }
        _checkBoxes = new JCheckBox[length];
        final JPanel checkersPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("insets.left=4,anchor=WEST,fill=HORIZONTAL");
        final ActionListener checkListener = createActionListener();
        addBandCheckers(new StringBuffer(), checkersPane, gbc, checkListener);
        addTiePointCheckers(new StringBuffer(), checkersPane, gbc, checkListener);
        return checkersPane;
    }

    private void addBandCheckers(final StringBuffer description, final JPanel checkersPane,
                                 final GridBagConstraints gbc, final ActionListener checkListener) {
        if (_allBands != null) {
            for (int i = 0; i < _allBandsLength; i++) {
                Band band = _allBands[i];
                boolean checked = false;
                for (int j = 0; j < _selectedBands.length; j++) {
                    Band selectedBand = _selectedBands[j];
                    if (band == selectedBand) {
                        checked = true;
                        _numSelected++;
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
                check.setFont(_SMALL_PLAIN_FONT);
                check.addActionListener(checkListener);

                final JLabel label = new JLabel(description.toString());
                label.setFont(_SMALL_ITALIC_FONT);

                gbc.gridy++;
                GridBagUtils.addToPanel(checkersPane, check, gbc, "weightx=0,gridx=0");
                GridBagUtils.addToPanel(checkersPane, label, gbc, "weightx=1,gridx=1");
                _checkBoxes[i] = check;
            }
        }
    }

    private void addTiePointCheckers(final StringBuffer description, final JPanel checkersPane,
                                     final GridBagConstraints gbc, final ActionListener checkListener) {
        if (_allTiePointGrids != null) {
            for (int i = 0; i < _allTiePointGrids.length; i++) {
                TiePointGrid grid = _allTiePointGrids[i];
                boolean checked = false;
                for (int j = 0; j < _selectedTiePointGrids.length; j++) {
                    TiePointGrid selectedGrid = _selectedTiePointGrids[j];
                    if (grid == selectedGrid) {
                        checked = true;
                        _numSelected++;
                        break;
                    }
                }

                description.setLength(0);
                description.append(grid.getDescription() == null ? "" : grid.getDescription());

                final JCheckBox check = new JCheckBox(getRasterDisplayName(grid), checked);
                check.setFont(_SMALL_PLAIN_FONT);
                check.addActionListener(checkListener);

                final JLabel label = new JLabel(description.toString());
                label.setFont(_SMALL_ITALIC_FONT);

                gbc.gridy++;
                GridBagUtils.addToPanel(checkersPane, check, gbc, "weightx=0,gridx=0");
                GridBagUtils.addToPanel(checkersPane, label, gbc, "weightx=1,gridx=1");

                _checkBoxes[i + _allBandsLength] = check;
            }
        }
    }

    private String getRasterDisplayName(RasterDataNode rasterDataNode) {
        return _multipleProducts ? rasterDataNode.getDisplayName() : rasterDataNode.getName();
    }

    private ActionListener createActionListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final JCheckBox check = (JCheckBox) e.getSource();
                if (check.isSelected()) {
                    _numSelected++;
                } else {
                    _numSelected--;
                }
                updateCheckBoxStates();
            }
        };
    }

    public void updateCheckBoxStates() {
        _selectAllCheckBox.setSelected(_numSelected == _checkBoxes.length);
        _selectAllCheckBox.setEnabled(_numSelected < _checkBoxes.length);
        _selectAllCheckBox.updateUI();
        _selectNoneCheckBox.setSelected(_numSelected == 0);
        _selectNoneCheckBox.setEnabled(_numSelected > 0);
        _selectNoneCheckBox.updateUI();
    }

    @Override
    public void setCheckBoxes(JCheckBox selectAllCheckBox, JCheckBox selectNoneCheckBox) {
        this._selectAllCheckBox = selectAllCheckBox;
        this._selectNoneCheckBox = selectNoneCheckBox;
        updateCheckBoxStates();
    }


    @Override
    public void selectAll() {
        select(true);
    }

    @Override
    public void selectNone() {
        select(false);
    }

    @Override
    public boolean atLeastOneBandSelected() {
        checkSelectedBandsAndGrids();
        return _selectedBands.length > 0;
    }

    private void checkSelectedBandsAndGrids() {
        final List bands = new ArrayList();
        final List grids = new ArrayList();
        for (int i = 0; i < _checkBoxes.length; i++) {
            JCheckBox checkBox = _checkBoxes[i];
            if (checkBox.isSelected()) {
                if (_allBandsLength > i) {
                    bands.add(_allBands[i]);
                } else {
                    grids.add(_allTiePointGrids[i - _allBandsLength]);
                }
            }
        }
        _selectedBands = (Band[]) bands.toArray(new Band[bands.size()]);
        _selectedTiePointGrids = (TiePointGrid[]) grids.toArray(new TiePointGrid[grids.size()]);
    }

    private void select(boolean b) {
        for (JCheckBox checkBox : _checkBoxes) {
            if (b && !checkBox.isSelected()) {
                _numSelected++;
            }
            if (!b && checkBox.isSelected()) {
                _numSelected--;
            }
            checkBox.setSelected(b);
        }
        updateCheckBoxStates();
    }

}
