package org.esa.beam.visat.actions.masktools;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.undo.UndoContext;
import com.bc.ceres.swing.undo.support.DefaultUndoContext;
import com.thoughtworks.xstream.XStream;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.BandChooser;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
class MagicWandForm {
    public static final int TOLERANCE_SLIDER_RESOLUTION = 1000;
    public static final String PREFERENCES_KEY_LAST_DIR = "beam.magicWandTool.lastDir";

    private MagicWandInteractor interactor;

    private JSlider toleranceSlider;
    boolean adjustingSlider;

    private UndoContext undoContext;
    private AbstractButton redoButton;
    private AbstractButton undoButton;
    private BindingContext bindingContext;

    private File settingsFile;

    MagicWandForm(MagicWandInteractor interactor) {
        this.interactor = interactor;
    }

    public BindingContext getBindingContext() {
        return bindingContext;
    }

    public JPanel createPanel() {

        undoContext = new DefaultUndoContext(this);
        interactor.setUndoContext(undoContext);

        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(interactor.getModel()));
        bindingContext.addPropertyChangeListener("tolerance", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                adjustSlider();
                interactor.getModel().fireModelChanged(true);
            }
        });

        JLabel toleranceLabel = new JLabel("Tolerance:");
        toleranceLabel.setToolTipText("Sets the maximum Euclidian distance tolerated");

        JTextField toleranceField = new JTextField(10);
        bindingContext.bind("tolerance", toleranceField);
        toleranceField.setText(String.valueOf(interactor.getModel().getTolerance()));

        toleranceSlider = new JSlider(0, TOLERANCE_SLIDER_RESOLUTION);
        toleranceSlider.setSnapToTicks(false);
        toleranceSlider.setPaintTicks(false);
        toleranceSlider.setPaintLabels(false);
        toleranceSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!adjustingSlider) {
                    int sliderValue = toleranceSlider.getValue();
                    bindingContext.getPropertySet().setValue("tolerance", sliderValueToTolerance(sliderValue));
                }
            }
        });

        JTextField minToleranceField = new JTextField(4);
        JTextField maxToleranceField = new JTextField(4);
        bindingContext.bind("minTolerance", minToleranceField);
        bindingContext.bind("maxTolerance", maxToleranceField);
        final PropertyChangeListener minMaxToleranceListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                adjustSlider();
            }
        };
        bindingContext.addPropertyChangeListener("minTolerance", minMaxToleranceListener);
        bindingContext.addPropertyChangeListener("maxTolerance", minMaxToleranceListener);

        JPanel toleranceSliderPanel = new JPanel(new BorderLayout(2, 2));
        toleranceSliderPanel.add(minToleranceField, BorderLayout.WEST);
        toleranceSliderPanel.add(toleranceSlider, BorderLayout.CENTER);
        toleranceSliderPanel.add(maxToleranceField, BorderLayout.EAST);

        JCheckBox normalizeCheckBox = new JCheckBox("Normalize spectra by first band");
        normalizeCheckBox.setToolTipText("Normalizes collected spectra by dividing them by their first band value");
        bindingContext.bind("normalize", normalizeCheckBox);
        bindingContext.addPropertyChangeListener("normalize", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                interactor.getModel().fireModelChanged(true);
            }
        });

        JRadioButton baButton1 = new JRadioButton("Distance");
        JRadioButton baButton2 = new JRadioButton("Average");
        JRadioButton baButton3 = new JRadioButton("Limits");
        ButtonGroup baGroup = new ButtonGroup();
        baGroup.add(baButton1);
        baGroup.add(baButton2);
        baGroup.add(baButton3);
        bindingContext.bind("bandAccumulation", baGroup);
        bindingContext.addPropertyChangeListener("bandAccumulation", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                interactor.getModel().fireModelChanged(true);
            }
        });

        JRadioButton stButton1 = new JRadioButton("Integral");
        JRadioButton stButton2 = new JRadioButton("Identity");
        JRadioButton stButton3 = new JRadioButton("Derivative");
        ButtonGroup stGroup = new ButtonGroup();
        stGroup.add(stButton1);
        stGroup.add(stButton2);
        stGroup.add(stButton3);
        bindingContext.bind("spectrumTransform", stGroup);
        bindingContext.addPropertyChangeListener("spectrumTransform", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                interactor.getModel().fireModelChanged(true);
            }
        });

        final AbstractButton plusButton = createToggleButton("/org/esa/beam/resources/images/icons/Plus16.gif");
        plusButton.setToolTipText("Pick mode 'plus': newly picked spectra will be included in the mask.");
        final AbstractButton minusButton = createToggleButton("/org/esa/beam/resources/images/icons/Minus16.gif");
        minusButton.setToolTipText("Pick mode 'minus': newly picked spectra will be excluded from the mask.");
        plusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindingContext.getPropertySet().setValue("pickMode", plusButton.isSelected() ? MagicWandModel.PickMode.PLUS : MagicWandModel.PickMode.SINGLE);
            }
        });
        minusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindingContext.getPropertySet().setValue("pickMode", plusButton.isSelected() ? MagicWandModel.PickMode.MINUS : MagicWandModel.PickMode.SINGLE);
            }
        });

        bindingContext.addPropertyChangeListener("pickMode", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                plusButton.setSelected(interactor.getModel().getPickMode() == MagicWandModel.PickMode.PLUS);
                minusButton.setSelected(interactor.getModel().getPickMode() == MagicWandModel.PickMode.MINUS);
                interactor.getModel().fireModelChanged(false);
            }
        });

        final AbstractButton newButton = createButton("/com/bc/ceres/swing/actions/icons_16x16/document-new.png");
        newButton.setToolTipText("New settings");
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                interactor.getModel().clearSpectra();
            }
        });

        final AbstractButton openButton = createButton("/com/bc/ceres/swing/actions/icons_16x16/document-open.png");
        openButton.setToolTipText("Open settings");
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSettings((Component) e.getSource());
            }
        });

        final AbstractButton saveButton = createButton("/com/bc/ceres/swing/actions/icons_16x16/document-save.png");
        saveButton.setToolTipText("Save settings");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings((Component) e.getSource(), settingsFile);
            }
        });

        final AbstractButton saveAsButton = createButton("/com/bc/ceres/swing/actions/icons_16x16/document-save-as.png");
        saveAsButton.setToolTipText("Save settings as");
        saveAsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings((Component) e.getSource(), null);
            }
        });

        undoButton = createButton("/com/bc/ceres/swing/actions/icons_16x16/edit-undo.png");
        undoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoContext.canUndo()) {
                    undoContext.undo();
                }
            }
        });
        redoButton = createButton("/com/bc/ceres/swing/actions/icons_16x16/edit-redo.png");
        redoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoContext.canRedo()) {
                    undoContext.redo();
                }
            }
        });
        updateState();
        undoContext.addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                updateState();
            }
        });

        AbstractButton filterButton = createButton("icons/Filter24.gif");
        filterButton.setName("filterButton");
        filterButton.setToolTipText("Select bands to included."); /*I18N*/
        filterButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                showBandChooser();
            }
        });

        AbstractButton helpButton = createButton("icons/Help22.png");
        helpButton.setName("helpButton");
        helpButton.setToolTipText("Help."); /*I18N*/


        JPanel toolPanelN = new JPanel(new GridLayout(-1, 2));
        toolPanelN.add(newButton);
        toolPanelN.add(openButton);
        toolPanelN.add(saveButton);
        toolPanelN.add(saveAsButton);
        toolPanelN.add(filterButton);
        toolPanelN.add(new JLabel());
        toolPanelN.add(undoButton);
        toolPanelN.add(redoButton);
        toolPanelN.add(plusButton);
        toolPanelN.add(minusButton);

        JPanel toolPanelS = new JPanel(new GridLayout(-1, 2));
        toolPanelS.add(new JLabel());
        toolPanelS.add(helpButton);

        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setCellColspan(1, 0, tableLayout.getColumnCount());
        tableLayout.setCellColspan(6, 0, tableLayout.getColumnCount());
        /*
        tableLayout.setCellColspan(2, 0, tableLayout.getColumnCount());
        tableLayout.setCellColspan(3, 0, tableLayout.getColumnCount());
        tableLayout.setCellColspan(4, 0, tableLayout.getColumnCount());
        tableLayout.setCellColspan(5, 0, tableLayout.getColumnCount());
        */

        JPanel subPanel = new JPanel(tableLayout);
        subPanel.add(toleranceLabel, new TableLayout.Cell(0, 0));
        subPanel.add(toleranceField, new TableLayout.Cell(0, 1));
        subPanel.add(toleranceSliderPanel, new TableLayout.Cell(1, 0));

        subPanel.add(new JLabel("Spectrum transformation:"), new TableLayout.Cell(2, 0));
        subPanel.add(stButton1, new TableLayout.Cell(3, 0));
        subPanel.add(stButton2, new TableLayout.Cell(4, 0));
        subPanel.add(stButton3, new TableLayout.Cell(5, 0));

        subPanel.add(new JLabel("Band aggregation:"), new TableLayout.Cell(2, 1));
        subPanel.add(baButton1, new TableLayout.Cell(3, 1));
        subPanel.add(baButton2, new TableLayout.Cell(4, 1));
        subPanel.add(baButton3, new TableLayout.Cell(5, 1));

        subPanel.add(normalizeCheckBox, new TableLayout.Cell(6, 0));

        JPanel toolPanel = new JPanel(new BorderLayout(4, 4));
        toolPanel.add(toolPanelN, BorderLayout.NORTH);
        toolPanel.add(new JLabel(), BorderLayout.CENTER);
        toolPanel.add(toolPanelS, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(subPanel, BorderLayout.CENTER);
        panel.add(toolPanel, BorderLayout.EAST);

        adjustSlider();

        return panel;
    }

    private void openSettings(Component parent) {
        File settingsFile = getFile(parent, this.settingsFile, true);
        if (settingsFile == null) {
            return;
        }
        try {
            MagicWandModel model = (MagicWandModel) createXStream().fromXML(FileUtils.readText(settingsFile));
            interactor.updateModel(model);
            this.settingsFile = settingsFile;
        } catch (IOException e) {
            String msg = MessageFormat.format("Failed to open settings:\n{0}", e.getMessage());
            JOptionPane.showMessageDialog(parent, msg, "I/O Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveSettings(Component parent, File settingsFile) {
        if (settingsFile == null) {
            settingsFile = getFile(parent, this.settingsFile, false);
            if (settingsFile == null) {
                return;
            }
        }
        try {
            try (FileWriter writer = new FileWriter(settingsFile)) {
                writer.write(createXStream().toXML(interactor.getModel()));
            }
            this.settingsFile = settingsFile;
        } catch (IOException e) {
            String msg = MessageFormat.format("Failed to safe settings:\n{0}", e.getMessage());
            JOptionPane.showMessageDialog(parent, msg, "I/O Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private XStream createXStream() {
        XStream xStream = new XStream();
        xStream.setClassLoader(MagicWandModel.class.getClassLoader());
        xStream.alias("magicWandSettings", MagicWandModel.class);
        return xStream;
    }

    private static File getFile(Component parent, File file, boolean open) {
        String directoryPath = Preferences.userRoot().absolutePath();
        System.out.println("directoryPath = " + directoryPath);
        JFileChooser fileChooser = new JFileChooser(Preferences.userRoot().get(PREFERENCES_KEY_LAST_DIR, System.getProperty("user.home")));
        if (file != null) {
            fileChooser.setSelectedFile(file);
        } else {
            fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), "magic-wand-settings.xml"));
        }
        while (true) {
            int resp = open ? fileChooser.showOpenDialog(parent) : fileChooser.showSaveDialog(parent);
            File settingsFile = fileChooser.getSelectedFile();
            Preferences.userRoot().put(PREFERENCES_KEY_LAST_DIR, fileChooser.getCurrentDirectory().getPath());
            if (resp != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            if (open || !settingsFile.exists()) {
                return settingsFile;
            }
            String msg = MessageFormat.format("Settings file ''{0}'' already exists." +
                                              "\nOverwrite?", settingsFile.getName());
            int resp2 = JOptionPane.showConfirmDialog(parent, msg,
                                                      "File exists", JOptionPane.YES_NO_CANCEL_OPTION);
            if (resp2 == JOptionPane.YES_OPTION) {
                return settingsFile;
            }
            if (resp2 == JOptionPane.CANCEL_OPTION) {
                return null;
            }
        }
    }

    void updateState() {

        JComponent[] bandAccumulationComponents = bindingContext.getBinding("bandAccumulation").getComponentAdapter().getComponents();
        for (JComponent component : bandAccumulationComponents) {
            component.setEnabled(interactor.getModel().getPickMode() != MagicWandModel.PickMode.SINGLE);
        }

        JComponent[] spectrumTransformComponents = bindingContext.getBinding("spectrumTransform").getComponentAdapter().getComponents();
        for (JComponent component : spectrumTransformComponents) {
            component.setEnabled(interactor.getModel().getBandCount() != 1);
        }

        undoButton.setEnabled(undoContext.canUndo());
        redoButton.setEnabled(undoContext.canRedo());
    }

    private void adjustSlider() {
        adjustingSlider = true;
        double tolerance = interactor.getModel().getTolerance();
        toleranceSlider.setValue(toleranceToSliderValue(tolerance));
        adjustingSlider = false;
    }

    private int toleranceToSliderValue(double tolerance) {
        MagicWandModel model = interactor.getModel();
        double minTolerance = model.getMinTolerance();
        double maxTolerance = model.getMaxTolerance();
        return (int) Math.round(Math.abs(TOLERANCE_SLIDER_RESOLUTION * ((tolerance - minTolerance) / (maxTolerance - minTolerance))));
    }

    private double sliderValueToTolerance(int sliderValue) {
        MagicWandModel model = interactor.getModel();
        double minTolerance = model.getMinTolerance();
        double maxTolerance = model.getMaxTolerance();
        return minTolerance + sliderValue * (maxTolerance - minTolerance) / TOLERANCE_SLIDER_RESOLUTION;
    }

    private static AbstractButton createButton(String iconPath) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(iconPath), false);
    }

    private static AbstractButton createToggleButton(String iconPath) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(iconPath), true);
    }

    void showBandChooser() {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view == null) {
            VisatApp.getApp().showInfoDialog("Please select an image view first.", null);
            return;
        }
        Product product = view.getProduct();

        Band[] bands = product.getBands();
        if (bands.length == 0) {
            VisatApp.getApp().showInfoDialog("No bands in product.", null);
            return;
        }

        String[] oldBandNames = interactor.getModel().getBandNames();
        Set<String> oldBandNameSet = new HashSet<>(Arrays.asList(oldBandNames));
        Set<Band> oldBandSet = new HashSet<>();
        for (Band band : bands) {
            if (oldBandNameSet.contains(band.getName())) {
                oldBandSet.add(band);
            }
        }
        BandChooser bandChooser = new BandChooser(interactor.getOptionsWindow(),
                                                  "Available Bands and Tie Point Grids",
                                                  "",
                                                  bands,
                                                  oldBandSet.toArray(new Band[oldBandSet.size()]),
                                                  product.getAutoGrouping());

        if (bandChooser.show() == ModalDialog.ID_OK) {
            Band[] newBands = bandChooser.getSelectedBands();
            Set<String> newBandNameSet = new HashSet<>();
            for (Band newBand : newBands) {
                newBandNameSet.add(newBand.getName());
            }
            if (!oldBandNameSet.containsAll(newBandNameSet)
                || !newBandNameSet.containsAll(oldBandNameSet)) {
                interactor.getModel().setBandNames(newBandNameSet.toArray(new String[newBandNameSet.size()]));
            }
        }
    }
}
