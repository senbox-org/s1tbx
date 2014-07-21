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
package org.esa.beam.framework.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RGBImageProfile;
import org.esa.beam.framework.datamodel.RGBImageProfileManager;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.Debug;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RGBImageProfilePane extends JPanel {

    private static final boolean SHOW_ALPHA = false;

    private String[] COLOR_COMP_NAMES = new String[]{
            "Red", /*I18N*/
            "Green", /*I18N*/
            "Blue", /*I18N*/
            "Alpha", /*I18N*/
    };
    public static final Font EXPRESSION_FONT = new Font("Courier", Font.PLAIN, 12);

    private PropertyMap preferences;
    private Product product;
    private final Product[] openedProducts;
    private final int[] defaultBandIndices;
    private JComboBox<ProfileItem> profileBox;

    private JComboBox[] rgbaExprBoxes;
    private DefaultComboBoxModel<ProfileItem> profileModel;
    private AbstractAction saveAsAction;
    private AbstractAction deleteAction;
    private boolean settingRgbaExpressions;
    private File lastDir;
    protected JCheckBox storeInProductCheck;

    public RGBImageProfilePane(PropertyMap preferences) {
        this(preferences, null, null, null);
    }

    public RGBImageProfilePane(PropertyMap preferences, Product product,
                               final Product[] openedProducts, final int[] defaultBandIndices) {
        this.preferences = preferences;
        this.product = product;
        this.openedProducts = openedProducts;
        this.defaultBandIndices = defaultBandIndices;

        AbstractAction openAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                performOpen();
            }
        };
        openAction.putValue(Command.ACTION_KEY_LARGE_ICON, UIUtils.loadImageIcon("icons/Open24.gif"));
        openAction.putValue(Action.SHORT_DESCRIPTION, "Open an external RGB profile");

        saveAsAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                performSaveAs();
            }
        };
        saveAsAction.putValue(Command.ACTION_KEY_LARGE_ICON, UIUtils.loadImageIcon("icons/Save24.gif"));
        saveAsAction.putValue(Action.SHORT_DESCRIPTION, "Save the RGB profile");

        deleteAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                performDelete();
            }
        };
        deleteAction.putValue(Command.ACTION_KEY_LARGE_ICON,
                              UIUtils.loadImageIcon("icons/Remove24.gif"));   // todo - use the nicer "cross" icon
        deleteAction.putValue(Action.SHORT_DESCRIPTION, "Delete the selected RGB profile");

        JPanel p2 = new JPanel(new GridLayout(1, 3, 2, 2));
        p2.add(ToolButtonFactory.createButton(openAction, false));
        p2.add(ToolButtonFactory.createButton(saveAsAction, false));
        p2.add(ToolButtonFactory.createButton(deleteAction, false));

        profileModel = new DefaultComboBoxModel<>();
        profileBox = new JComboBox<>(profileModel);
        profileBox.addItemListener(new ProfileSelectionHandler());
        profileBox.setEditable(false);
        profileBox.setName("profileBox");
        setPrefferedWidth(profileBox, 200);

        storeInProductCheck = new JCheckBox();
        storeInProductCheck.setText("Store RGB channels as virtual bands in current product");
        storeInProductCheck.setSelected(false);
        storeInProductCheck.setVisible(this.product != null);
        storeInProductCheck.setName("storeInProductCheck");

        final String[] bandNames;
        if (this.product != null) {
            bandNames = this.product.getBandNames();
        } else {
            bandNames = new String[0];
        }
        rgbaExprBoxes = new JComboBox[4];
        for (int i = 0; i < rgbaExprBoxes.length; i++) {
            rgbaExprBoxes[i] = createRgbaBox(bandNames);
            rgbaExprBoxes[i].setName("rgbExprBox_" + i);
        }

        JPanel p1 = new JPanel(new BorderLayout(2, 2));
        p1.add(new JLabel("Profile: "), BorderLayout.NORTH);
        p1.add(profileBox, BorderLayout.CENTER);
        p1.add(p2, BorderLayout.EAST);

        JPanel p3 = new JPanel(new GridBagLayout());
        final GridBagConstraints c3 = new GridBagConstraints();
        c3.anchor = GridBagConstraints.WEST;
        c3.fill = GridBagConstraints.HORIZONTAL;
        c3.insets = new Insets(2, 2, 2, 2);
        final int n = SHOW_ALPHA ? 4 : 3;
        for (int i = 0; i < n; i++) {
            c3.gridy = i;
            addColorComponentRow(p3, c3, i);
        }

        setLayout(new BorderLayout(10, 10));
        add(p1, BorderLayout.NORTH);
        add(p3, BorderLayout.CENTER);
        add(storeInProductCheck, BorderLayout.SOUTH);

        final RGBImageProfile[] registeredProfiles = RGBImageProfileManager.getInstance().getAllProfiles();
        addProfiles(registeredProfiles);
        if (this.product != null) {
            final RGBImageProfile productProfile = RGBImageProfile.getCurrentProfile(this.product);
            if (productProfile.isValid()) {
                final RGBImageProfile similarProfile = findMatchingProfile(productProfile);
                if (similarProfile != null) {
                    selectProfile(similarProfile);
                } else {
                    addNewProfile(productProfile);
                    selectProfile(productProfile);
                }
            } else {
                List<RGBImageProfile> selectableProfiles = new ArrayList<>();
                for (int i = 0; i < profileModel.getSize(); i++) {
                    selectableProfiles.add(profileModel.getElementAt(i).getProfile());
                }
                RGBImageProfile[] selectableProfileArray = selectableProfiles.toArray(new RGBImageProfile[selectableProfiles.size()]);
                RGBImageProfile profile = findProfileForProductPattern(selectableProfileArray, product);
                if (profile != null) {
                    selectProfile(profile);
                }
            }
        }
        setRgbaExpressionsFromSelectedProfile();

        if(rgbaExprBoxes[0].getSelectedIndex() < 0) {
            // default
            if(defaultBandIndices != null && defaultBandIndices.length > 0) {
                for(int i=0; i < defaultBandIndices.length; ++i) {
                    rgbaExprBoxes[i].setSelectedIndex(defaultBandIndices[i]);
                }
            }
        }
    }

    public Product getProduct() {
        return product;
    }

    public void dispose() {
        preferences = null;
        product = null;
        profileModel.removeAllElements();
        profileModel = null;
        profileBox = null;
        saveAsAction = null;
        deleteAction = null;
        for (int i = 0; i < rgbaExprBoxes.length; i++) {
            rgbaExprBoxes[i] = null;
        }
        rgbaExprBoxes = null;
    }

    public boolean getStoreProfileInProduct() {
        return storeInProductCheck.isSelected();
    }

    /**
     * Gets the selected RGB-image profile if any.
     *
     * @return the selected profile, can be null
     *
     * @see #getRgbaExpressions()
     */
    public RGBImageProfile getSelectedProfile() {
        final ProfileItem profileItem = getSelectedProfileItem();
        return profileItem != null ? profileItem.getProfile() : null;
    }

    /**
     * Gets the selected RGB expressions as array of 3 strings.
     *
     * @return the selected RGB expressions, never null
     *
     * @see #getSelectedProfile()
     */
    public String[] getRgbExpressions() {
        return new String[]{
                getExpression(0),
                getExpression(1),
                getExpression(2),
        };
    }

    /**
     * Gets the selected RGBA expressions as array of 4 strings.
     *
     * @return the selected RGBA expressions, never null
     *
     * @see #getSelectedProfile()
     */
    public String[] getRgbaExpressions() {
        return new String[]{
                getExpression(0),
                getExpression(1),
                getExpression(2),
                getExpression(3),
        };
    }

    public void addProfiles(RGBImageProfile[] profiles) {
        for (RGBImageProfile profile : profiles) {
            addNewProfile(profile);
        }
        setRgbaExpressionsFromSelectedProfile();
    }

    public RGBImageProfile findMatchingProfile(RGBImageProfile profile) {
        // search in internal profiles first...
        RGBImageProfile matchingProfile = findMatchingProfile(profile, true);
        if (matchingProfile == null) {
            // ...then in non-internal profiles
            matchingProfile = findMatchingProfile(profile, false);
        }
        return matchingProfile;
    }

    public void selectProfile(RGBImageProfile profile) {
        profileModel.setSelectedItem(new ProfileItem(profile));
    }

    public boolean showDialog(Window parent, String title, String helpId) {
        ModalDialog modalDialog = new ModalDialog(parent, title, ModalDialog.ID_OK_CANCEL_HELP, helpId);
        modalDialog.setContent(this);
        final int status = modalDialog.show();
        modalDialog.getJDialog().dispose();
        return status == ModalDialog.ID_OK;
    }

    private String getExpression(int i) {
        return ((JTextField) rgbaExprBoxes[i].getEditor().getEditorComponent()).getText().trim();
    }

    private void setExpression(int i, String expression) {
        rgbaExprBoxes[i].setSelectedItem(expression);
    }

    private void performOpen() {
        final BeamFileChooser beamFileChooser = new BeamFileChooser(getProfilesDir());
        beamFileChooser.setFileFilter(
                new BeamFileFilter("RGB-PROFILE", RGBImageProfile.FILENAME_EXTENSION, "RGB-Image Profile Files"));
        final int status = beamFileChooser.showOpenDialog(this);
        if (beamFileChooser.getSelectedFile() == null) {
            return;
        }
        final File file = beamFileChooser.getSelectedFile();
        lastDir = file.getParentFile();
        if (status != BeamFileChooser.APPROVE_OPTION) {
            return;
        }

        final RGBImageProfile profile;
        try {
            profile = RGBImageProfile.loadProfile(file);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                                          "Failed to open RGB-profile '"
                                          + file.getName() + "':\n" + e.getMessage(),
                                          "Open RGB-Image Profile",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (profile == null) {
            JOptionPane.showMessageDialog(this,
                                          "Invalid RGB-Profile '" + file.getName() + "'.",
                                          "Open RGB-Image Profile",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        RGBImageProfileManager.getInstance().addProfile(profile);
        if (product != null && !profile.isApplicableTo(product)) {
            JOptionPane.showMessageDialog(this,
                                          "The selected RGB-Profile '" + profile.getName() + "'\n" +
                                          "is not applicable to the current product.",
                                          "Open RGB-Image Profile",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        addNewProfile(profile);
    }

    private void performSaveAs() {
        File file = promptForSaveFile();
        if (file == null) {
            return;
        }
        RGBImageProfile profile = new RGBImageProfile(FileUtils.getFilenameWithoutExtension(file),
                                                      getRgbaExpressions());
        try {
            profile.store(file);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                                          "Failed to save RGB-profile '" + file.getName() + "':\n"
                                          + e.getMessage(),
                                          "Open RGB-Image Profile",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        RGBImageProfileManager.getInstance().addProfile(profile);
        addNewProfile(profile);
    }

    private File promptForSaveFile() {
        final BeamFileChooser beamFileChooser = new BeamFileChooser(getProfilesDir());
        beamFileChooser.setFileFilter(new BeamFileFilter("RGB-PROFILE", ".rgb", "RGB-Image Profile Files"));

        File selectedFile;
        while (true) {
            final int status = beamFileChooser.showSaveDialog(this);
            if (beamFileChooser.getSelectedFile() == null) {
                selectedFile = null;
                break;
            }
            selectedFile = beamFileChooser.getSelectedFile();
            lastDir = selectedFile.getParentFile();
            if (status != BeamFileChooser.APPROVE_OPTION) {
                selectedFile = null;
                break;
            }
            if (selectedFile.exists()) {
                final int answer = JOptionPane.showConfirmDialog(RGBImageProfilePane.this,
                                                                 "The file '" + selectedFile.getName()
                                                                 + "' already exists.\n" +
                                                                 "So you really want to overwrite it?",
                                                                 "Safe RGB-Profile As",
                                                                 JOptionPane.YES_NO_CANCEL_OPTION);
                if (answer == JOptionPane.CANCEL_OPTION) {
                    selectedFile = null;
                    break;
                }
                if (answer == JOptionPane.YES_OPTION) {
                    break;
                }
            } else {
                break;
            }
        }
        return selectedFile;
    }

    private void performDelete() {
        final ProfileItem selectedProfileItem = getSelectedProfileItem();
        if (selectedProfileItem != null && !selectedProfileItem.getProfile().isInternal()) {
            profileModel.removeElement(selectedProfileItem);
        }
    }

    private File getProfilesDir() {
        if (lastDir != null) {
            return lastDir;
        } else {
            return RGBImageProfileManager.getProfilesDir();
        }
    }

    private void addNewProfile(RGBImageProfile profile) {
        if (product != null && !profile.isApplicableTo(product)) {
            return;
        }
        final ProfileItem profileItem = new ProfileItem(profile);
        final int index = profileModel.getIndexOf(profileItem);
        if (index == -1) {
            profileModel.addElement(profileItem);
        }
        profileModel.setSelectedItem(profileItem);
    }

    private void setRgbaExpressionsFromSelectedProfile() {
        settingRgbaExpressions = true;
        try {
            final ProfileItem profileItem = getSelectedProfileItem();
            if (profileItem != null) {
                final String[] rgbaExpressions = profileItem.getProfile().getRgbaExpressions();
                for (int i = 0; i < rgbaExprBoxes.length; i++) {
                    setExpression(i, rgbaExpressions[i]);
                }
            } else {
                for (int i = 0; i < rgbaExprBoxes.length; i++) {
                    setExpression(i, "");
                }
            }
        } finally {
            settingRgbaExpressions = false;
        }
        updateUIState();
    }

    private ProfileItem getSelectedProfileItem() {
        return (ProfileItem) profileBox.getSelectedItem();
    }

    private void addColorComponentRow(JPanel p3, final GridBagConstraints constraints, final int index) {
        final JButton editorButton = new JButton("...");
        editorButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                invokeExpressionEditor(index);
            }
        });
        final Dimension preferredSize = rgbaExprBoxes[index].getPreferredSize();
        editorButton.setPreferredSize(new Dimension(preferredSize.height, preferredSize.height));

        constraints.gridy = index;

        constraints.gridx = 0;
        constraints.weightx = 0;
        p3.add(new JLabel(getComponentName(index) + ": "), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        p3.add(rgbaExprBoxes[index], constraints);

        constraints.gridx = 2;
        constraints.weightx = 0;
        p3.add(editorButton, constraints);
    }

    protected String getComponentName(final int index) {
        return COLOR_COMP_NAMES[index];
    }

    private void invokeExpressionEditor(final int colorIndex) {
        final Window window = SwingUtilities.getWindowAncestor(this);
        final String title = "Edit " + getComponentName(colorIndex) + " Expression";
        if (product != null) {
            final ExpressionPane pane;
            final Product[] products = getCompatibleProducts(product, openedProducts);
            pane = ProductExpressionPane.createGeneralExpressionPane(products, product, preferences);
            pane.setCode(getExpression(colorIndex));
            int status = pane.showModalDialog(window, title);
            if (status == ModalDialog.ID_OK) {
                setExpression(colorIndex, pane.getCode());
            }
        } else {
            final JTextArea textArea = new JTextArea(8, 48);
            textArea.setFont(EXPRESSION_FONT);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setText(getExpression(colorIndex));
            final ModalDialog modalDialog = new ModalDialog(window, title, ModalDialog.ID_OK_CANCEL, "");
            final JPanel panel = new JPanel(new BorderLayout(2, 2));
            panel.add(new JLabel("Expression:"), BorderLayout.NORTH);
            panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
            modalDialog.setContent(panel);
            final int status = modalDialog.show();
            if (status == ModalDialog.ID_OK) {
                setExpression(colorIndex, textArea.getText());
            }
        }
    }

    private static Product[] getCompatibleProducts(final Product targetProduct, final Product[] productsList) {
        final List<Product> compatibleProducts = new ArrayList<Product>(1);
        compatibleProducts.add(targetProduct);
        final float geolocationEps = 180;
        Debug.trace("BandMathsDialog.geolocationEps = " + geolocationEps);
        Debug.trace("BandMathsDialog.getCompatibleProducts:");
        Debug.trace("  comparing: " + targetProduct.getName());
        if(productsList != null) {
            for (final Product product : productsList) {
                if (targetProduct != product) {
                    Debug.trace("  with:      " + product.getDisplayName());
                    final boolean isCompatibleProduct = targetProduct.isCompatibleProduct(product, geolocationEps);
                    Debug.trace("  result:    " + isCompatibleProduct);
                    if (isCompatibleProduct) {
                        compatibleProducts.add(product);
                    }
                }
            }
        }
        return compatibleProducts.toArray(new Product[compatibleProducts.size()]);
    }

    private JComboBox createRgbaBox(String[] suggestions) {
        final JComboBox<String> comboBox = new JComboBox<>(suggestions);
        setPrefferedWidth(comboBox, 320);
        comboBox.setEditable(true);
        final ComboBoxEditor editor = comboBox.getEditor();
        final JTextField textField = (JTextField) editor.getEditorComponent();
        textField.setFont(EXPRESSION_FONT);
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                onRgbaExpressionChanged();
            }

            public void removeUpdate(DocumentEvent e) {
                onRgbaExpressionChanged();
            }

            public void changedUpdate(DocumentEvent e) {
                onRgbaExpressionChanged();
            }
        });
        return comboBox;
    }

    private void onRgbaExpressionChanged() {
        if (settingRgbaExpressions) {
            return;
        }
        final ProfileItem profileItem = getSelectedProfileItem();
        if (profileItem != null) {
            if (isSelectedProfileModified()) {
                profileBox.revalidate();
                profileBox.repaint();
            }
        }
        updateUIState();
    }

    private boolean isSelectedProfileModified() {
        final ProfileItem profileItem = getSelectedProfileItem();
        final String[] profileRgbaExpressions = profileItem.getProfile().getRgbaExpressions();
        final String[] userRgbaExpressions = getRgbaExpressions();
        for (int i = 0; i < profileRgbaExpressions.length; i++) {
            final String userRgbaExpression = userRgbaExpressions[i];
            final String profileRgbaExpression = profileRgbaExpressions[i];
            if (!profileRgbaExpression.equals(userRgbaExpression)) {
                return true;
            }
        }
        return false;
    }

    private void updateUIState() {
        final ProfileItem profileItem = getSelectedProfileItem();
        if (profileItem != null) {
            saveAsAction.setEnabled(true);
            deleteAction.setEnabled(!profileItem.getProfile().isInternal());
        } else {
            saveAsAction.setEnabled(isAtLeastOneColorExpressionSet());
            deleteAction.setEnabled(false);
        }
    }

    private boolean isAtLeastOneColorExpressionSet() {
        final JComboBox[] rgbaExprBoxes = this.rgbaExprBoxes;
        for (int i = 0; i < 3; i++) {
            JComboBox rgbaExprBox = rgbaExprBoxes[i];
            final Object selectedItem = rgbaExprBox.getSelectedItem();
            if (selectedItem != null && !selectedItem.toString().trim().equals("")) {
                return true;
            }
        }
        return false;
    }

    private void setPrefferedWidth(final JComboBox comboBox, final int width) {
        final Dimension preferredSize = comboBox.getPreferredSize();
        comboBox.setPreferredSize(new Dimension(width, preferredSize.height));
    }

    public static RGBImageProfile findProfileForProductPattern(RGBImageProfile[] rgbImageProfiles, Product product) {
        if (rgbImageProfiles.length == 0) {
            return null;
        }

        String productType = product.getProductType();
        String productName = product.getName();
        String productDesc = product.getDescription();
        RGBImageProfile bestProfile = rgbImageProfiles[0];
        int bestMatchScore = 0;
        for (RGBImageProfile rgbImageProfile : rgbImageProfiles) {
            String[] pattern = rgbImageProfile.getPattern();
            if (pattern == null) {
                continue;
            }
            boolean productTypeMatches = matches(productType, pattern[0]);
            boolean productNameMatches = matches(productName, pattern[1]);
            boolean productDescMatches = matches(productDesc, pattern[2]);
            int currentMatchScore = (productTypeMatches ? 100 : 0) + (productNameMatches ? 10 : 0) + (productDescMatches ? 1 : 0);
            if (currentMatchScore > bestMatchScore) {
                bestProfile = rgbImageProfile;
                bestMatchScore = currentMatchScore;
            }
        }
        return bestProfile;
    }

    private static boolean matches(String s, String pattern) {
        return pattern != null &&
               s.matches(pattern.replace("*", ".*").replace("?", "."));
    }


    private class ProfileItem {

        private RGBImageProfile _profile;

        public ProfileItem(RGBImageProfile profile) {
            _profile = profile;
        }

        public RGBImageProfile getProfile() {
            return _profile;
        }

        @Override
        public int hashCode() {
            return getProfile().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof ProfileItem) {
                ProfileItem profileItem = (ProfileItem) obj;
                return getProfile().equals(profileItem.getProfile());
            }
            return false;
        }

        @Override
        public String toString() {
            String name = _profile.getName().replace('_', ' ');
            if (getSelectedProfileItem().equals(this) && isSelectedProfileModified()) {
                name += " (modified)";
            }
            return name;
        }
    }

    private class ProfileSelectionHandler implements ItemListener {

        public void itemStateChanged(ItemEvent e) {
            setRgbaExpressionsFromSelectedProfile();
        }
    }


    public RGBImageProfile findMatchingProfile(RGBImageProfile profile, boolean internal) {
        final int size = profileModel.getSize();
        for (int i = 0; i < size; i++) {
            final ProfileItem item = profileModel.getElementAt(i);
            final RGBImageProfile knownProfile = item.getProfile();
            if (knownProfile.isInternal() == internal
                    && Arrays.equals(profile.getRgbExpressions(), knownProfile.getRgbExpressions())) {
                return knownProfile;
            }
        }
        return null;
    }

}
