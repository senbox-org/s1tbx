package org.esa.beam.framework.ui.product.spectrum;

import com.jidesoft.list.FilterableListModel;
import com.jidesoft.list.QuickListFilterField;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.ArrayUtils;
import org.esa.beam.util.StringUtils;

import javax.swing.AbstractButton;
import javax.swing.AbstractListModel;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SpectrumEditor extends ModalDialog {

    private JList availableBandList;
    private JList selectedBandList;
    private SpectrumListModel availableSpectraListModel;
    private SpectrumListModel selectedSpectraListModel;
    private JTextField patternField;
    private SpectrumInDisplay spectrum;
    private JTextField nameField;
    private JTextField descriptionField;
    private JRadioButton patternButton;
    private Map<String, Band> availableBandsMap;
    private String[] alreadyDefinedSpectrumNames;

    public SpectrumEditor(Window parent, String title, String helpID, Band[] availableSpectralBands,
                          String[] alreadyDefinedSpectrumNames) {
        this(parent, title, helpID, availableSpectralBands, null, alreadyDefinedSpectrumNames);
    }

    public SpectrumEditor(Window parent, String title, String helpID, Band[] availableSpectralBands,
                          SpectrumInDisplay spectrum, String[] alreadyDefinedSpectrumNames) {
        super(parent, title, ModalDialog.ID_OK_CANCEL, helpID);
        availableBandsMap = new HashMap<String, Band>();
        for (Band availableSpectralBand : availableSpectralBands) {
            availableBandsMap.put(createBandDisplayName(availableSpectralBand), availableSpectralBand);
        }
        this.spectrum = spectrum;
        this.alreadyDefinedSpectrumNames = alreadyDefinedSpectrumNames;
        initialiseSpectraListModels();
        initUI();
    }

    private void initialiseSpectraListModels() {
        availableSpectraListModel = new SpectrumListModel("<No spectral bands available>");
        selectedSpectraListModel = new SpectrumListModel("<No spectral bands selected>");
        final Set<Map.Entry<String, Band>> availableBands = availableBandsMap.entrySet();
        for (Map.Entry<String, Band> availableSpectralBandEntry : availableBands) {
            if (isInSpectrum(availableSpectralBandEntry.getValue())) {
                selectedSpectraListModel.addElement(availableSpectralBandEntry.getKey());
            } else {
                availableSpectraListModel.addElement(availableSpectralBandEntry.getKey());
            }
        }
    }

    private void updateSpectraListModels(String pattern) {
        availableSpectraListModel.removeAllElements();
        selectedSpectraListModel.removeAllElements();

        final Set<Map.Entry<String, Band>> availableBands = availableBandsMap.entrySet();
        for (Map.Entry<String, Band> availableSpectralBandEntry : availableBands) {
            if (StringUtils.isNotNullAndNotEmpty(pattern) && availableSpectralBandEntry.getValue().getName().contains(pattern)) {
                selectedSpectraListModel.addElement(availableSpectralBandEntry.getKey());
            } else {
                availableSpectraListModel.addElement(availableSpectralBandEntry.getKey());
            }
        }
        availableBandList.updateUI();
        selectedBandList.updateUI();
    }

    private boolean isInSpectrum(Band availableSpectralBand) {
        if (spectrum == null) {
            return false;
        } else if (StringUtils.isNotNullAndNotEmpty(spectrum.getNamePattern())) {
            return availableSpectralBand.getName().contains(spectrum.getNamePattern());
        } else {
            return ArrayUtils.isMemberOf(availableSpectralBand, spectrum.getSpectralBands());
        }
    }

    private String createBandDisplayName(Band band) {
        StringBuilder builder = new StringBuilder(band.getName());
        if (!Float.isNaN(band.getSpectralWavelength())) {
            builder.append(" (")
                    .append(band.getSpectralWavelength())
                    .append(" nm)");
        }
        return builder.toString();
    }

    @Override
    public int show() {
        availableSpectraListModel.removeElements(selectedSpectraListModel.getAllElements());
        return super.show();
    }

    private void initUI() {
        final JPanel content = GridBagUtils.createPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        nameField = new JTextField();
        final Dimension textFieldDimension = new Dimension(120, 20);
        nameField.setPreferredSize(textFieldDimension);
        if (spectrum != null) {
            nameField.setText(spectrum.getName());
        } else {
            nameField.setText(createDefaultSpectrumName());
        }
        descriptionField = new JTextField();
        descriptionField.setPreferredSize(textFieldDimension);
        if (spectrum != null) {
            descriptionField.setText(spectrum.getDescription());
        }
        patternField = new JTextField();
        patternField.setPreferredSize(textFieldDimension);
        if (spectrum != null) {
            patternField.setText(spectrum.getNamePattern());
        }
        patternField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSpectraListModels(patternField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSpectraListModels(patternField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // do nothing
            }
        });
        patternButton = new JRadioButton("Assign by name pattern");
        final JRadioButton manualButton = new JRadioButton("Assign spectral bands manually");
        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(patternButton);
        radioGroup.add(manualButton);
        if (spectrum == null || StringUtils.isNotNullAndNotEmpty(spectrum.getNamePattern())) {
            patternButton.setSelected(true);
        } else {
            manualButton.setSelected(true);
        }
        final JPanel bandAssignmentPanel = createBandAssignmentPanel();
        patternButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateUIEnablement(patternButton.isSelected(), bandAssignmentPanel);
            }
        });
        manualButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateUIEnablement(patternButton.isSelected(), bandAssignmentPanel);
            }
        });
        updateUIEnablement(patternButton.isSelected(), bandAssignmentPanel);
        GridBagUtils.addToPanel(content, new JLabel("Name:"), gbc, "gridx=0, gridy=0, anchor=NORTHWEST, fill=NONE, " +
                "weightx=0.0,gridwidth=1, weighty=0.0, insets=2");
        GridBagUtils.addToPanel(content, nameField, gbc, "gridx=1, fill=HORIZONTAL, weightx=1.0");
        GridBagUtils.addToPanel(content, new JLabel("Description:"), gbc, "gridx=0, gridy=1, fill=NONE, weightx=0.0");
        GridBagUtils.addToPanel(content, descriptionField, gbc, "gridx=1, fill=HORIZONTAL, weightx=1.0");
        GridBagUtils.addToPanel(content, patternButton, gbc, "gridx=0, gridy=2, weightx=1.0, gridwidth=2");
        GridBagUtils.addToPanel(content, patternField, gbc, "gridy=3");
        GridBagUtils.addToPanel(content, manualButton, gbc, "gridy=4");
        GridBagUtils.addToPanel(content, bandAssignmentPanel, gbc, "gridy=5, fill=BOTH, weighty=1.0");
        content.setMinimumSize(content.getPreferredSize());
        setContent(content);
        content.validate();
    }

    private String createDefaultSpectrumName() {
        int spectrumNumber = 1;
        StringBuilder builder = new StringBuilder("Spectrum ");
        builder.append(spectrumNumber++);
        while (ArrayUtils.isMemberOf(builder.toString(), alreadyDefinedSpectrumNames)) {
            builder.delete(9, builder.length());
            builder.append(spectrumNumber++);
        }
        return builder.toString();
    }


    private void updateUIEnablement(boolean patternMode, JPanel bandAssignmentPanel) {
        patternField.setEnabled(patternMode);
        final Component[] bandAssignmentPanelComponents = bandAssignmentPanel.getComponents();
        for (Component bandAssignmentPanelComponent : bandAssignmentPanelComponents) {
            bandAssignmentPanelComponent.setEnabled(!patternMode);
        }
        availableBandList.setEnabled(!patternMode);
        selectedBandList.setEnabled(!patternMode);
        patternField.updateUI();
        bandAssignmentPanel.updateUI();
    }

    private JPanel createBandAssignmentPanel() {
        final JPanel bandAssignmentPanel = GridBagUtils.createPanel();

        final Set<String> availableBands = availableBandsMap.keySet();
        List<String> availableSpectralBandNames = new ArrayList<String>(availableBands);
        availableSpectraListModel = new SpectrumListModel(availableSpectralBandNames, "<No spectral bands available>");
        QuickListFilterField filterField = new QuickListFilterField(availableSpectraListModel);
        filterField.setHintText("Type here to filter spectral bands");
        filterField.setWildcardEnabled(true);
        final FilterableListModel listModel = filterField.getDisplayListModel();
        availableBandList = new JList(listModel);
        filterField.setList(availableBandList);
        availableBandList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        final JScrollPane availableBandsScrollPane = new JScrollPane(availableBandList);
        availableBandsScrollPane.setPreferredSize(new Dimension(150, 150));
        availableBandsScrollPane.setBorder(new TitledBorder("Available spectral bands:"));

        final AbstractButton rightButton = createButton("icons/Right24.gif");
        rightButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Object[] selectedValues = availableBandList.getSelectedValues();
                if (!selectedValues[0].toString().equals(availableSpectraListModel.emptyMessage)) {
                    for (Object selectedValue : selectedValues) {
                        selectedSpectraListModel.addElement(selectedValue.toString());
                        availableSpectraListModel.removeElement(selectedValue.toString());
                        availableBandList.updateUI();
                        selectedBandList.updateUI();
                    }
                }
            }
        });
        final AbstractButton leftButton = createButton("icons/Left24.gif");
        leftButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Object[] selectedValues = selectedBandList.getSelectedValues();
                if (!selectedValues[0].toString().equals(selectedSpectraListModel.emptyMessage)) {
                    for (Object selectedValue : selectedValues) {
                        availableSpectraListModel.addElement(selectedValue.toString());
                        selectedSpectraListModel.removeElement(selectedValue.toString());
                        availableBandList.updateUI();
                        selectedBandList.updateUI();
                    }
                }
            }
        });
        final AbstractButton allRightButton = createButton("icons/PanelRight12.png");
        allRightButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final List<String> allElements = availableSpectraListModel.getAllElements();
                selectedSpectraListModel.addElements(allElements);
                availableSpectraListModel.removeAllElements();
                availableBandList.updateUI();
                selectedBandList.updateUI();
            }
        });
        final AbstractButton allLeftButton = createButton("icons/PanelLeft12.png");
        allLeftButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final List<String> allElements = selectedSpectraListModel.getAllElements();
                availableSpectraListModel.addElements(allElements);
                selectedSpectraListModel.removeAllElements();
                availableBandList.updateUI();
                selectedBandList.updateUI();
            }
        });

        selectedBandList = new JList(selectedSpectraListModel);
        final JScrollPane selectedBandScrollPane = new JScrollPane(selectedBandList);
        selectedBandScrollPane.setBorder(new TitledBorder("Selected spectral bands:"));
        availableBandList.setPreferredSize(new Dimension(100, 100));
        selectedBandList.setPreferredSize(new Dimension(100, 100));

        GridBagConstraints gbc = new GridBagConstraints();
        GridBagUtils.addToPanel(bandAssignmentPanel, filterField, gbc, "weightx=1.0, weighty=1.0, gridx=0, gridy=0, fill=HORIZONTAL");
        GridBagUtils.addToPanel(bandAssignmentPanel, availableBandsScrollPane, gbc, "gridy=1, fill=BOTH,gridheight=5");
        GridBagUtils.addToPanel(bandAssignmentPanel, rightButton, gbc, "gridx=1,gridy=1,weightx=0,weighty=0,fill=NONE,gridheight=1");
        GridBagUtils.addToPanel(bandAssignmentPanel, leftButton, gbc, "gridy=2");
        GridBagUtils.addToPanel(bandAssignmentPanel, allRightButton, gbc, "gridy=3");
        GridBagUtils.addToPanel(bandAssignmentPanel, allLeftButton, gbc, "gridy=4");
        GridBagUtils.addVerticalFiller(bandAssignmentPanel, gbc);
        GridBagUtils.addToPanel(bandAssignmentPanel, selectedBandScrollPane, gbc, "gridx=2,gridy=1,weightx=1.0, weighty=1.0,fill=BOTH,gridheight=5");
        return bandAssignmentPanel;
    }

    private AbstractButton createButton(String iconPath) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(iconPath), false);
    }

    public SpectrumInDisplay getSpectrum() {
        final List<String> selectedElements = selectedSpectraListModel.getAllElements();
        Band[] selectedBands = new Band[selectedElements.size()];
        for (int i = 0; i < selectedBands.length; i++) {
            selectedBands[i] = availableBandsMap.get(selectedElements.get(i));
        }
        if (patternButton.isSelected()) {
            return new SpectrumInDisplay(nameField.getText(), descriptionField.getText(), patternField.getText(), selectedBands);
        } else {
            return new SpectrumInDisplay(nameField.getText(), descriptionField.getText(), selectedBands);
        }
    }

    @Override
    protected boolean verifyUserInput() {
        if (StringUtils.isNullOrEmpty(nameField.getText())) {
            showInformationDialog("No name chosen.\nPlease assign a name to the spectrum.");
            return false;
        } else if (ArrayUtils.isMemberOf(nameField.getText(), alreadyDefinedSpectrumNames)) {
            showInformationDialog("Name already assigned.\nPlease assign another name to the spectrum.");
            return false;
        } else if (selectedSpectraListModel.getAllElements().size() == 0) {
            showInformationDialog("No bands selected.\nPlease select at least one band.");
            return false;
        }
        return true;
    }

    private class SpectrumListModel extends AbstractListModel {

        private final List<String> spectralBandNamesList;
        private String emptyMessage;

        SpectrumListModel(String emptyMessage) {
            this.spectralBandNamesList = new ArrayList<String>();
            this.emptyMessage = emptyMessage;
            insertEmptyMessageIfNecessary();
        }

        SpectrumListModel(List<String> spectralBandNames, String emptyMessage) {
            Collections.sort(spectralBandNames);
            this.spectralBandNamesList = new ArrayList<String>(spectralBandNames);
            this.emptyMessage = emptyMessage;
            insertEmptyMessageIfNecessary();
        }

        @Override
        public String getElementAt(int index) {
            return spectralBandNamesList.get(index);
        }

        @Override
        public int getSize() {
            return spectralBandNamesList.size();
        }

        public void addElement(String spectralBandName) {
            removeEmptyMessageIfNecessary();
            spectralBandNamesList.add(spectralBandName);
            Collections.sort(spectralBandNamesList);
        }

        public void addElements(List<String> spectralBandNames) {
            removeEmptyMessageIfNecessary();
            spectralBandNamesList.addAll(spectralBandNames);
            Collections.sort(spectralBandNamesList);
        }

        public List<String> getAllElements() {
            if (spectralBandNamesList.size() == 1 && spectralBandNamesList.get(0).equals(emptyMessage)) {
                return new ArrayList<String>();
            } else {
                return spectralBandNamesList;
            }
        }

        public void removeElement(String spectralBandName) {
            spectralBandNamesList.remove(spectralBandName);
            Collections.sort(spectralBandNamesList);
            insertEmptyMessageIfNecessary();
        }

        public void removeElements(List<String> spectralBandNames) {
            spectralBandNamesList.removeAll(spectralBandNames);
            insertEmptyMessageIfNecessary();
        }

        public void removeAllElements() {
            spectralBandNamesList.clear();
            insertEmptyMessageIfNecessary();
        }

        private void insertEmptyMessageIfNecessary() {
            if (spectralBandNamesList.isEmpty()) {
                spectralBandNamesList.add(emptyMessage);
            }
        }

        private void removeEmptyMessageIfNecessary() {
            if (spectralBandNamesList.size() == 1 && spectralBandNamesList.get(0).equals(emptyMessage)) {
                spectralBandNamesList.clear();
            }
        }

    }
}
