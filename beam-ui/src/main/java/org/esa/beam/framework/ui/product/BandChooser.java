/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.framework.ui.product;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.Guardian;

/**
 * A dialog which lets the user select from a product's bands and tie-point grids.
 */
public class BandChooser extends ModalDialog {

    // @todo 3 nf/se - see ProductSubsetDialog for a similar declarations  (code smell!)
    private static final Font _SMALL_PLAIN_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font _SMALL_ITALIC_FONT = _SMALL_PLAIN_FONT.deriveFont(Font.ITALIC);

    private final Band[] _allBands;
    private Band[] _selectedBands;
    private final TiePointGrid[] _allTiePointGrids;
    private TiePointGrid[] _selectedGrids;

    private int _numSelected;

    private JCheckBox[] _checkBoxes;
    private JCheckBox _selectAllCheckBox;
    private JCheckBox _selectNoneCheckBox;
    private final int _allBandsLength;
    private final boolean _selectAtLeastOneBand;
    private boolean _multipleProducts;

    public BandChooser(Window parent, String title, String helpID,
                       Band[] allBands, Band[] selectedBands) {
        this(parent, title, helpID, true, allBands, selectedBands, null, null);
    }

    public BandChooser(Window parent, String title, String helpID, boolean selectAtLeastOneBand,
                       Band[] allBands, Band[] selectedBands,
                       TiePointGrid[] allTiePointGrids, TiePointGrid[] selectedTiePointGrids) {
        super(parent, title, ModalDialog.ID_OK_CANCEL, helpID);
        Guardian.assertNotNull("allBands", allBands);
        _allBands = allBands;
        _selectedBands = selectedBands;
        _allTiePointGrids = allTiePointGrids;
        _selectedGrids = selectedTiePointGrids;
        _selectAtLeastOneBand = selectAtLeastOneBand;
        if (_selectedBands == null) {
            _selectedBands = new Band[0];
        }
        if (_selectedGrids == null) {
            _selectedGrids = new TiePointGrid[0];
        }
        Set productSet = new HashSet();
        if (allBands != null) {
            _allBandsLength = _allBands.length;
            for (int i = 0; i < allBands.length; i++) {
                productSet.add(allBands[i].getProduct());
            }
        } else {
            _allBandsLength = 0;
        }
        if (allTiePointGrids != null) {
            for (int i = 0; i < allTiePointGrids.length; i++) {
                productSet.add(allTiePointGrids[i].getProduct());
            }
        }
        _multipleProducts = productSet.size() > 1;
        initUI();
    }

    @Override
    public int show() {
        updateUI();
        return super.show();
    }

    private void initUI() {
        JPanel checkersPane = createCheckersPane();

        _selectAllCheckBox = new JCheckBox("Select all"); /*I18N*/
        _selectAllCheckBox.setMnemonic('a');
        _selectAllCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                select(true);
            }
        });

        _selectNoneCheckBox = new JCheckBox("Select none"); /*I18N*/
        _selectNoneCheckBox.setMnemonic('n');
        _selectNoneCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                select(false);
            }
        });

        final JPanel checkPane = new JPanel(new BorderLayout());
        checkPane.add(_selectAllCheckBox, BorderLayout.WEST);
        checkPane.add(_selectNoneCheckBox, BorderLayout.CENTER);
        final JPanel content = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(checkersPane);
        final Dimension preferredSize = checkersPane.getPreferredSize();
        scrollPane.setPreferredSize(new Dimension(Math.min(preferredSize.width + 20, 400),
                                                  Math.min(preferredSize.height + 10, 300)));
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(checkPane, BorderLayout.SOUTH);
        setContent(content);
    }

    private JPanel createCheckersPane() {
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
        final StringBuffer description = new StringBuffer();
        final ActionListener checkListener = createActionListener();
        addBandCheckers(description, checkersPane, gbc, checkListener);
        addTiePointCheckers(description, checkersPane, gbc, checkListener);
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
                for (int j = 0; j < _selectedGrids.length; j++) {
                    TiePointGrid selectedGrid = _selectedGrids[j];
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
                updateUI();
            }
        };
    }

    private void select(boolean b) {
        for (int i = 0; i < _checkBoxes.length; i++) {
            JCheckBox checkBox = _checkBoxes[i];
            if (b && !checkBox.isSelected()) {
                _numSelected++;
            }
            if (!b && checkBox.isSelected()) {
                _numSelected--;
            }
            checkBox.setSelected(b);
        }
        updateUI();
    }

    private void updateUI() {
        _selectAllCheckBox.setSelected(_numSelected == _checkBoxes.length);
        _selectAllCheckBox.setEnabled(_numSelected < _checkBoxes.length);
        _selectAllCheckBox.updateUI();
        _selectNoneCheckBox.setSelected(_numSelected == 0);
        _selectNoneCheckBox.setEnabled(_numSelected > 0);
        _selectNoneCheckBox.updateUI();
    }

    @Override
    protected boolean verifyUserInput() {
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
        _selectedGrids = (TiePointGrid[]) grids.toArray(new TiePointGrid[grids.size()]);
        if (_selectAtLeastOneBand) {
            boolean result = _selectedBands.length > 0;
            if (!result) {
                showInformationDialog("No bands selected.\nPlease select at least one band.");
            }
            return result;
        }
        return true;
    }

    public Band[] getSelectedBands() {
        return _selectedBands;
    }

    public TiePointGrid[] getSelectedTiePointGrids() {
        return _selectedGrids;
    }
}
