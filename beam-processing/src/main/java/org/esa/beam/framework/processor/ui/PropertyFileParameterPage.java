package org.esa.beam.framework.processor.ui;

import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestValidator;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.BeamFileFilter;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This parameter page creates an UI for editing a property file.
 * This page is intended to be used with the {@link MultiPageProcessorUI}.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Norman Fomferra
 */
public class PropertyFileParameterPage extends ParameterPage {

    /**
     * Name of the parameter which holds the path to the property file.
     */
    public static final String PROPERTY_FILE_PARAM_NAME = "property_file";

    /**
     * The default title of this page.
     */
    public static final String DEFAULT_PAGE_TITLE = "Processing Parameters";


    private final String _defaultPropertyText;
    private final File _defaultPropertyFile;

    private String _storedPropertyText;
    private JTextArea _textArea;
    private AbstractButton _saveButton;
    private AbstractButton _restoreDefaultsButton;

    /**
     * Creates a parameter page for editing a property file.
     * The constructor creates a {@link ParamGroup paramGroup} which contains a parameter with the name
     * {@link PropertyFileParameterPage#PROPERTY_FILE_PARAM_NAME} of type <code>java.io.File</code>.
     * This given file is used as the default parameter file.
     *
     * @param propertyFile the property file
     *
     * @see org.esa.beam.framework.processor.ProcessorConstants
     */
    public PropertyFileParameterPage(final File propertyFile) {
        this(createDefaultParamGroup(propertyFile));
    }

    /**
     * Creates a parameter page for editing a property file.
     * The {@link ParamGroup paramGroup} must contain a parameter with the name
     * {@link PropertyFileParameterPage#PROPERTY_FILE_PARAM_NAME} of type <code>java.io.File</code>.
     * The given file by this parameter is used as the default parameter file.
     *
     * @param paramGroup the paramGroup to create the UI from
     *
     * @see org.esa.beam.framework.processor.ProcessorConstants
     */
    public PropertyFileParameterPage(final ParamGroup paramGroup) {
        super(paramGroup);
        setTitle(DEFAULT_PAGE_TITLE);
        _defaultPropertyFile = getCurrentPropertyFile();
        _defaultPropertyText = getDefaultPropertyFileText(_defaultPropertyFile);
    }

    /**
     * Creates a parameter page for editing a property file.
     * The constructor creates a {@link ParamGroup paramGroup} which contains a parameter with the name
     * {@link PropertyFileParameterPage#PROPERTY_FILE_PARAM_NAME} of type <code>java.io.File</code>.
     *
     * @param propertyFile        the property file
     * @param defaultPropertyText the default properties. The user can always switch back to these properties.
     *
     * @see org.esa.beam.framework.processor.ProcessorConstants
     * @deprecated use {@link PropertyFileParameterPage#PropertyFileParameterPage(java.io.File)} )
     */
    public PropertyFileParameterPage(final File propertyFile, final String defaultPropertyText) {
        this(createDefaultParamGroup(propertyFile));
    }

    /**
     * Creates a parameter page for editing a property file.
     * The {@link ParamGroup paramGroup} must contain a parameter with the name
     * {@link PropertyFileParameterPage#PROPERTY_FILE_PARAM_NAME} of type <code>java.io.File</code>.
     *
     * @param paramGroup          the paramGroup to create the UI from
     * @param defaultPropertyText the default properties. The user can always switch back to these properties.
     *
     * @see org.esa.beam.framework.processor.ProcessorConstants
     * @deprecated use {@link PropertyFileParameterPage#PropertyFileParameterPage(ParamGroup)} )
     */
    public PropertyFileParameterPage(final ParamGroup paramGroup, final String defaultPropertyText) {
        this(paramGroup);
    }

    /**
     * Sets the parameter values by these given with the {@link Request request}.
     *
     * @param request the request to obtain the parameters
     *
     * @throws ProcessorException if an error occured
     */
    public void setUIFromRequest(final Request request) throws ProcessorException {
        setPropertyFileParameter(request);
    }

    /**
     * Fills the given {@link Request request} with parameters.
     *
     * @param request the request to fill
     *
     * @throws ProcessorException if an error occured
     */
    public void initRequestFromUI(final Request request) throws ProcessorException {
        request.addParameter(getParamGroup().getParameter(PROPERTY_FILE_PARAM_NAME));
    }

    /**
     * Sets the processor app for the UI
     */
    public void setApp(final ProcessorApp app) {
        super.setApp(app);
        final ParamGroup paramGroup = getParamGroup();
        if (paramGroup != null) {
            app.markParentDirChanges(paramGroup.getParameter(PROPERTY_FILE_PARAM_NAME), "property_file_dir");
        }
        app.addRequestValidator(new RequestValidator() {
            public boolean validateRequest(final Processor processor, final Request request) {
                if (!_storedPropertyText.equals(_textArea.getText())) {
                    // here is parameter OK, not to confuse the user
                    app.showInfoDialog("Parameter file is modified.\n" +
                                       "Unable to start processing.\n" +
                                       "\n" +
                                       "Please save the parameter file first.", null);
                    return false;
                }
                return true;
            }
        });
    }

    /**
     * It creates the UI by using the {@link ParamGroup parameter group} of this page.
     * <p/>
     * <p>It's only called once by the {@link MultiPageProcessorUI} during lifetime of an
     * instance of this class.</p>
     *
     * @return the UI component displayed as page of the {@link MultiPageProcessorUI}.
     */
    public JComponent createUI() {
        _textArea = new JTextArea();
        final JScrollPane textPane = new JScrollPane(_textArea);
        textPane.setPreferredSize(new Dimension(600, 250));
        final JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints(null);
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // property file chooser
        final Parameter param = getParamGroup().getParameter(PROPERTY_FILE_PARAM_NAME);
        param.addParamChangeListener(createPropertyFileChooserListener());
        gbc.gridy++;
        gbc.gridwidth = 3;
        panel.add(param.getEditor().getLabelComponent(), gbc);

        _saveButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Save16.gif"), false);
        _saveButton.setEnabled(false);
        _saveButton.setToolTipText("Saves the edited parameters");
        _saveButton.addActionListener(createSaveButtonListener());

        _restoreDefaultsButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Undo16.gif"), false);
        _restoreDefaultsButton.setEnabled(false);
        _restoreDefaultsButton.setToolTipText("Restores the processor default parameters");
        _restoreDefaultsButton.addActionListener(createRestoreDefaultsButtonListener());

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(_saveButton, gbc);
        panel.add(_restoreDefaultsButton, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(param.getEditor().getComponent(), gbc);

        // property file editor
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets.top = 5;
        panel.add(textPane, gbc);

        _textArea.addKeyListener(createPropertyFileEditorKeyListener());

        // init text area
        loadPropertyFile();
        return panel;

    }

    private static ParamGroup createDefaultParamGroup(final File propertyFile) {
        final int fileSelectionMode = ParamProperties.FSM_FILES_ONLY;
        final DefaultRequestElementFactory factory = DefaultRequestElementFactory.getInstance();
        final ParamProperties paramProps = factory.createFileParamProperties(fileSelectionMode, propertyFile);
        // here is parameter OK, not to confuse the user
        paramProps.setLabel("Processing parameters file");       /*I18N*/
        final BeamFileFilter prpFileFilter = new BeamFileFilter("PROPERTIES", ".properties", "Property Files");
        final BeamFileFilter txtFileFilter = new BeamFileFilter("TXT", ".txt", "Text Files");
        paramProps.setChoosableFileFilters(new BeamFileFilter[]{
                txtFileFilter,
                prpFileFilter
        });
        paramProps.setCurrentFileFilter(txtFileFilter);
        final Parameter parameter = new Parameter(PROPERTY_FILE_PARAM_NAME, paramProps);
        parameter.setDefaultValue();
        final ParamGroup paramGroup = new ParamGroup();
        paramGroup.addParameter(parameter);
        return paramGroup;
    }

    private ParamChangeListener createPropertyFileChooserListener() {
        return new ParamChangeListener() {
            public void parameterValueChanged(final ParamChangeEvent event) {
                loadPropertyFile();
            }
        };
    }

    private void loadPropertyFile() {
        final boolean successLoading = readPropertyFile();
        if (successLoading) {
            _saveButton.setEnabled(false);
            _restoreDefaultsButton.setEnabled(!getCurrentPropertyFile().equals(_defaultPropertyFile));
        }
    }

    private boolean readPropertyFile() {
        final File propertyFile = getCurrentPropertyFile();
        if (propertyFile == null || !propertyFile.isFile()) {
            return false;
        }
        try {
            final FileReader in = new FileReader(propertyFile);
            try {
                _textArea.read(in, getCurrentPropertyFile());
                _storedPropertyText = _textArea.getText();
            } finally {
                in.close();
            }
            return true;
        } catch (IOException e) {
            Debug.trace(e);
            final ProcessorApp app = getApp();
            if (app != null) {
                app.showWarningDialog("I/O Error",
                                      "Unable to read property file '" + propertyFile.getName() + "'." +
                                      "\n\n" +
                                      e.getMessage());
            }
            return false;
        }
    }

    private File getCurrentPropertyFile() {
        return (File) getParamGroup().getParameter(PROPERTY_FILE_PARAM_NAME).getValue();
    }

    private KeyListener createPropertyFileEditorKeyListener() {
        return new KeyAdapter() {
            public void keyTyped(final KeyEvent e) {
                _saveButton.setEnabled(true);
                _restoreDefaultsButton.setEnabled(!_textArea.getText().equals(_defaultPropertyText));
            }
        };
    }

    private ActionListener createSaveButtonListener() {
        return new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                savePropertyFile();
            }
        };
    }

    private ActionListener createRestoreDefaultsButtonListener() {
        return new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                final int answer = JOptionPane.showConfirmDialog((Component) event.getSource(),
                                                                 "Do you really want to reload the default parameters file?\n" +
                                                                 "All current settings will be lost.");
                if (answer == JOptionPane.YES_OPTION) {
                    setPropertyFileParameter(_defaultPropertyFile);
                    _restoreDefaultsButton.setEnabled(false);
                }
            }
        };
    }

    private void savePropertyFile() {
        final File propertyFile = getDestinationFile();
        if (propertyFile == null) {
            return;
        }
        final PrintWriter pw = createPrintWriter(propertyFile);
        if (pw != null) {
            try {
                writePropertyFile(pw);
            } finally {
                pw.close();
            }
            getParamGroup().getParameter(PROPERTY_FILE_PARAM_NAME).setValue(propertyFile, null);
            _saveButton.setEnabled(false);
            _storedPropertyText = _textArea.getText();
        }
    }

    private File getDestinationFile() {
        File propertyFile = getCurrentPropertyFile();
        final ProcessorApp app = getApp();
        if (app != null) {
            while (propertyFile.equals(_defaultPropertyFile) || !app.promptForOverwrite(propertyFile)) {
                if (propertyFile.equals(_defaultPropertyFile)) {
                    final String msgText = "It is not allowed to overwrite the default parameters file.\n" +
                                           "Please choose a different file name.";
                    if (getApp() != null) {
                        getApp().showWarningDialog(getTitle(), msgText);
                    } else {
                        JOptionPane.showMessageDialog(null, msgText, getTitle(), JOptionPane.WARNING_MESSAGE);
                    }
                }
                final JFileChooser chooser = new JFileChooser(propertyFile.getParentFile());
                chooser.setSelectedFile(propertyFile);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(app.getMainFrame())) {
                    propertyFile = chooser.getSelectedFile();
                } else {
                    return null;
                }
            }
        }
        return propertyFile;
    }

    private PrintWriter createPrintWriter(final File file) {
        try {
            return new PrintWriter(new FileWriter(file));
        } catch (IOException e) {
            Debug.trace(e);
            final ProcessorApp app = getApp();
            if (app != null) {
                app.showWarningDialog("I/O Error", "Unable to write '" + file.getName() + "'." +
                                                   "\n\n" +
                                                   e.getMessage());
            }
            return null;
        }
    }

    private void writePropertyFile(final PrintWriter out) {
        for (int lineNumber = 0; lineNumber < _textArea.getLineCount(); lineNumber++) {
            try {
                final int start = _textArea.getLineStartOffset(lineNumber);
                final int len = _textArea.getLineEndOffset(lineNumber) - start;
                final String line = _textArea.getText(start, len);
                final String lineWithoutCRorLF = line.replaceAll("[\\n\\r]", "");
                out.println(lineWithoutCRorLF);
            } catch (BadLocationException e) {
                Debug.trace(e);
                // should never come here
            }
        }
    }

    private String getDefaultPropertyFileText(File propertyFile) {
        final StringBuffer sb = new StringBuffer();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(propertyFile));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(System.getProperty("line.separator", "\n"));
            }
        } catch (IOException e) {
            Debug.trace(e);
            final String msgText = "Not able to load default parameters from file: \n"
                                   + e.getMessage();
            if (getApp() != null) {
                getApp().showErrorDialog(getTitle(), msgText);
            } else {
                JOptionPane.showMessageDialog(null, msgText, getTitle(), JOptionPane.WARNING_MESSAGE);
            }
            sb.setLength(0);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Debug.trace(e);
                    // ignore
                }
            }
        }
        return sb.toString();
    }

    private void setPropertyFileParameter(final Request request) {
        final Parameter parameter = request.getParameter(PROPERTY_FILE_PARAM_NAME);
        final File file;
        if (parameter != null) {
            file = new File(parameter.getValueAsText());
            setPropertyFileParameter(file);
        }
    }

    private void setPropertyFileParameter(final File propertyFile) {
        if (propertyFile != null) {
            getParamGroup().getParameter(PROPERTY_FILE_PARAM_NAME).setValue(propertyFile, null);
        }
    }

}
