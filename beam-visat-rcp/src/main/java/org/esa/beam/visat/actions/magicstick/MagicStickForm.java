package org.esa.beam.visat.actions.magicstick;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.undo.UndoContext;
import com.bc.ceres.swing.undo.support.DefaultUndoContext;
import com.thoughtworks.xstream.XStream;
import org.esa.beam.util.io.FileUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.prefs.Preferences;

/**
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
class MagicStickForm {
    public static final int TOLERANCE_SLIDER_RESOLUTION = 1000;
    public static final String PREFERENCES_KEY_LAST_DIR = "beam.magicStick.lastDir";

    private MagicStickInteractor interactor;

    // don't forget: private JCheckBox cumulativeModeCheckBox;
    private JTextField toleranceField;

    private JSlider toleranceSlider;
    boolean adjustingSlider;
    private JCheckBox normalizeCheckBox;
    private JTextField minToleranceField;
    private JTextField maxToleranceField;

    private UndoContext undoContext;
    private JButton redoButton;
    private JButton undoButton;
    private BindingContext bindingContext;

    private File settingsFile;

    MagicStickForm(MagicStickInteractor interactor) {
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
                interactor.updateMagicStickMask();
            }
        });

        JLabel toleranceLabel = new JLabel("Tolerance:");
        toleranceLabel.setToolTipText("Sets the maximum Euclidian distance tolerated (in units of the spectral bands)");

        toleranceField = new JTextField(10);
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

        minToleranceField = new JTextField(4);
        maxToleranceField = new JTextField(4);
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

        normalizeCheckBox = new JCheckBox("Normalize spectra");
        normalizeCheckBox.setToolTipText("Normalizes spectra by dividing them by their first values");
        bindingContext.bind("normalize", normalizeCheckBox);
        bindingContext.addPropertyChangeListener("normalize", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                interactor.updateMagicStickMask();
            }
        });

        JRadioButton methodButton1 = new JRadioButton("Distance");
        JRadioButton methodButton2 = new JRadioButton("Average");
        JRadioButton methodButton3 = new JRadioButton("Limits");
        ButtonGroup methodGroup = new ButtonGroup();
        methodGroup.add(methodButton1);
        methodGroup.add(methodButton2);
        methodGroup.add(methodButton3);
        bindingContext.bind("method", methodGroup);
        JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        methodPanel.add(methodButton1);
        methodPanel.add(methodButton2);
        methodPanel.add(methodButton3);
        bindingContext.addPropertyChangeListener("method", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                interactor.updateMagicStickMask();
            }
        });

        JRadioButton operatorButton1 = new JRadioButton("Integral");
        JRadioButton operatorButton2 = new JRadioButton("Identity");
        JRadioButton operatorButton3 = new JRadioButton("Derivative");
        ButtonGroup operatorGroup = new ButtonGroup();
        operatorGroup.add(operatorButton1);
        operatorGroup.add(operatorButton2);
        operatorGroup.add(operatorButton3);
        bindingContext.bind("operator", operatorGroup);
        JPanel operatorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        operatorPanel.add(operatorButton1);
        operatorPanel.add(operatorButton2);
        operatorPanel.add(operatorButton3);
        bindingContext.addPropertyChangeListener("operator", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                interactor.updateMagicStickMask();
            }
        });

        final JToggleButton plusButton = new JToggleButton(new ImageIcon(getClass().getResource("/org/esa/beam/resources/images/icons/Plus16.gif")));
        plusButton.setToolTipText("Switch to 'plus' mode: Selected spectra will be included in the mask.");
        final JToggleButton minusButton = new JToggleButton(new ImageIcon(getClass().getResource("/org/esa/beam/resources/images/icons/Minus16.gif")));
        minusButton.setToolTipText("Switch to 'minus' mode: Selected spectra will be excluded from the mask.");
        plusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindingContext.getPropertySet().setValue("mode", plusButton.isSelected() ? MagicStickModel.Mode.PLUS : MagicStickModel.Mode.SINGLE);
            }
        });
        minusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindingContext.getPropertySet().setValue("mode", plusButton.isSelected() ? MagicStickModel.Mode.MINUS : MagicStickModel.Mode.SINGLE);
            }
        });

        bindingContext.addPropertyChangeListener("mode", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                plusButton.setSelected(interactor.getModel().getMode() == MagicStickModel.Mode.PLUS);
                minusButton.setSelected(interactor.getModel().getMode() == MagicStickModel.Mode.MINUS);
            }
        });

        final JButton newButton = new JButton(new ImageIcon(getClass().getResource("/com/bc/ceres/swing/actions/icons_16x16/document-new.png")));
        newButton.setToolTipText("New settings");
        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                interactor.getModel().clearSpectra();
                interactor.updateMagicStickMask();
            }
        });

        final JButton openButton = new JButton(new ImageIcon(getClass().getResource("/com/bc/ceres/swing/actions/icons_16x16/document-open.png")));
        openButton.setToolTipText("Open settings");
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSettings((Component) e.getSource());
            }
        });

        final JButton saveButton = new JButton(new ImageIcon(getClass().getResource("/com/bc/ceres/swing/actions/icons_16x16/document-save.png")));
        saveButton.setToolTipText("Save settings");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings((Component) e.getSource(), settingsFile);
            }
        });

        final JButton saveAsButton = new JButton(new ImageIcon(getClass().getResource("/com/bc/ceres/swing/actions/icons_16x16/document-save-as.png")));
        saveAsButton.setToolTipText("Save settings as");
        saveAsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings((Component) e.getSource(), null);
            }
        });

        undoButton = new JButton(new ImageIcon(getClass().getResource("/com/bc/ceres/swing/actions/icons_16x16/edit-undo.png")));
        undoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoContext.canUndo()) {
                    undoContext.undo();
                }
            }
        });
        redoButton = new JButton(new ImageIcon(getClass().getResource("/com/bc/ceres/swing/actions/icons_16x16/edit-redo.png")));
        redoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoContext.canRedo()) {
                    undoContext.redo();
                }
            }
        });
        updateUndoRedoState();
        undoContext.addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                updateUndoRedoState();
            }
        });

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(newButton);
        toolBar.add(openButton);
        toolBar.add(saveButton);
        toolBar.add(saveAsButton);
        toolBar.add(new JLabel("   "));
        toolBar.add(undoButton);
        toolBar.add(redoButton);
        toolBar.add(new JLabel("   "));
        toolBar.add(plusButton);
        toolBar.add(minusButton);

        JPanel toolBarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        toolBarPanel.add(toolBar);

        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setCellColspan(1, 0, tableLayout.getColumnCount());
        tableLayout.setCellColspan(2, 0, tableLayout.getColumnCount());
        tableLayout.setCellColspan(3, 0, tableLayout.getColumnCount());
        tableLayout.setCellColspan(4, 0, tableLayout.getColumnCount());

        JPanel panel = new JPanel(tableLayout);
        panel.add(toleranceLabel, new TableLayout.Cell(0, 0));
        panel.add(toleranceField, new TableLayout.Cell(0, 1));
        panel.add(toleranceSliderPanel, new TableLayout.Cell(1, 0));
        panel.add(methodPanel, new TableLayout.Cell(2, 0));
        panel.add(operatorPanel, new TableLayout.Cell(3, 0));
        panel.add(normalizeCheckBox, new TableLayout.Cell(4, 0));
        panel.add(toolBarPanel, new TableLayout.Cell(5, 0));

        adjustSlider();

        return panel;
    }

    private void openSettings(Component parent) {
        File settingsFile = getFile(parent, this.settingsFile, true);
        if (settingsFile == null) {
            return;
        }
        try {
            MagicStickModel model = (MagicStickModel) createXStream().fromXML(FileUtils.readText(settingsFile));
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
            FileWriter writer = new FileWriter(settingsFile);
            try {
                writer.write(createXStream().toXML(interactor.getModel()));
            } finally {
                writer.close();
            }
            this.settingsFile = settingsFile;
        } catch (IOException e) {
            String msg = MessageFormat.format("Failed to safe settings:\n{0}", e.getMessage());
            JOptionPane.showMessageDialog(parent, msg, "I/O Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private XStream createXStream() {
        XStream xStream = new XStream();
        xStream.setClassLoader(MagicStickModel.class.getClassLoader());
        xStream.alias("magicStickSettings", MagicStickModel.class);
        return xStream;
    }

    private static File getFile(Component parent, File file, boolean open) {
        String directoryPath = Preferences.userRoot().absolutePath();
        System.out.println("directoryPath = " + directoryPath);
        JFileChooser fileChooser = new JFileChooser(Preferences.userRoot().get(PREFERENCES_KEY_LAST_DIR, System.getProperty("user.home")));
        if (file != null) {
            fileChooser.setSelectedFile(file);
        } else {
            fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), "magic-stick-settings.xml"));
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

    void updateUndoRedoState() {
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
        MagicStickModel model = interactor.getModel();
        double minTolerance = model.getMinTolerance();
        double maxTolerance = model.getMaxTolerance();
        return (int) Math.round(Math.abs(TOLERANCE_SLIDER_RESOLUTION * ((tolerance - minTolerance) / (maxTolerance - minTolerance))));
    }

    private double sliderValueToTolerance(int sliderValue) {
        MagicStickModel model = interactor.getModel();
        double minTolerance = model.getMinTolerance();
        double maxTolerance = model.getMaxTolerance();
        return minTolerance + sliderValue * (maxTolerance - minTolerance) / TOLERANCE_SLIDER_RESOLUTION;
    }

}
