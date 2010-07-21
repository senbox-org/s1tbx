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
package org.esa.beam.visat.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.Guardian;

public class ProductChooser extends ModalDialog {

    private static final Font _SMALL_PLAIN_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font _SMALL_ITALIC_FONT = _SMALL_PLAIN_FONT.deriveFont(Font.ITALIC);

    private final Product[] _allProducts;
    private Product[] _selectedProducts;
    private String _roiBandName;

    private int _numSelected;

    private JCheckBox[] _checkBoxes;
    private JCheckBox _selectAllCheckBox;
    private JCheckBox _selectNoneCheckBox;
    private final boolean _selectAtLeastOneProduct;
    private boolean _multipleProducts;
    private JRadioButton _transferToAllBandsRB;

    public ProductChooser(Window parent, String title, String helpID,
                          Product[] allProducts, Product[] selectedProducts,
                          String roiBandName) {
        super(parent, title, ModalDialog.ID_OK_CANCEL, helpID);
        Guardian.assertNotNull("allProducts", allProducts);
        Guardian.assertNotNullOrEmpty("roiBandName", roiBandName);
        _allProducts = allProducts;
        _selectedProducts = selectedProducts;
        _selectAtLeastOneProduct = true;
        if (_selectedProducts == null) {
            _selectedProducts = new Product[0];
        }
        _multipleProducts = allProducts.length > 1;
        _roiBandName = roiBandName;
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

        JRadioButton transferToBandRB = new JRadioButton("Transfer ROI to '" + _roiBandName + "' only"); /*I18N*/
        _transferToAllBandsRB = new JRadioButton("Transfer ROI to all bands"); /*I18N*/
        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(transferToBandRB);
        buttonGroup.add(_transferToAllBandsRB);
        transferToBandRB.setSelected(true);
        final JPanel bandNamePanel = new JPanel(new BorderLayout());
        bandNamePanel.add(transferToBandRB, BorderLayout.CENTER);
        bandNamePanel.add(_transferToAllBandsRB, BorderLayout.SOUTH);
        bandNamePanel.setBorder(BorderFactory.createTitledBorder("Transfer mode")); /*I18N*/


        final JPanel checkPane = new JPanel(new BorderLayout());
        checkPane.add(_selectAllCheckBox, BorderLayout.WEST);
        checkPane.add(_selectNoneCheckBox, BorderLayout.CENTER);
        JScrollPane scrollPane = new JScrollPane(checkersPane);
        final Dimension preferredSize = checkersPane.getPreferredSize();
        scrollPane.setPreferredSize(new Dimension(Math.min(preferredSize.width + 20, 400),
                                                  Math.min(preferredSize.height + 40, 300)));
        final JLabel label = new JLabel("Target product(s):"); /*I18N*/

        final JPanel content = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0;
        content.add(label, gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        gbc.weightx = 1;
        content.add(scrollPane, gbc);
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.weightx = 0;
        content.add(checkPane, gbc);
        gbc.gridy++;
        gbc.insets.top = 20;
        content.add(bandNamePanel, gbc);
        setContent(content);
    }

    private JPanel createCheckersPane() {
        _checkBoxes = new JCheckBox[_allProducts.length];
        final JPanel checkersPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("insets.left=4,anchor=WEST,fill=HORIZONTAL");
        final StringBuffer description = new StringBuffer();
        addProductCheckers(description, checkersPane, gbc);
        return checkersPane;
    }

    private void addProductCheckers(final StringBuffer description, final JPanel checkersPane,
                                    final GridBagConstraints gbc) {
        final ActionListener checkListener = createActionListener();
        for (int i = 0; i < _allProducts.length; i++) {
            Product product = _allProducts[i];
            boolean checked = false;
            for (int j = 0; j < _selectedProducts.length; j++) {
                Product selectedProduct = _selectedProducts[j];
                if (product == selectedProduct) {
                    checked = true;
                    _numSelected++;
                    break;
                }
            }

            description.setLength(0);
            description.append(product.getDescription() == null ? "" : product.getDescription());

            final JCheckBox check = new JCheckBox(getDisplayName(product), checked);
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

    private String getDisplayName(Product rasterDataNode) {
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
        final List products = new ArrayList();
        for (int i = 0; i < _checkBoxes.length; i++) {
            JCheckBox checkBox = _checkBoxes[i];
            if (checkBox.isSelected()) {
                products.add(_allProducts[i]);
            }
        }
        _selectedProducts = (Product[]) products.toArray(new Product[products.size()]);
        if (_selectAtLeastOneProduct) {
            boolean result = _selectedProducts.length > 0;
            if (!result) {
                showInformationDialog("No products selected.\n" +
                                      "Please select at least one product."); /*I18N*/
            }
            return result;
        }
        return true;
    }

    public Product[] getSelectedProducts() {
        return _selectedProducts;
    }

    public boolean isTransferToAllBands() {
        return _transferToAllBandsRB.isSelected();
    }
}
