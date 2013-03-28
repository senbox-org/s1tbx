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
package org.esa.beam.framework.processor.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.jidesoft.action.CommandBar;
import com.jidesoft.action.CommandMenuBar;
import com.jidesoft.swing.JideMenu;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.framework.processor.RequestLoader;
import org.esa.beam.framework.processor.RequestValidator;
import org.esa.beam.framework.processor.RequestWriter;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class serves as the main processor UI frame shared by all processors. It provides Request load and save
 * operations and the basic button set. This class mediates the communication between processor and processor UI.
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class ProcessorApp extends BasicApp {

    public static final String REQUEST_DIR_PREFERENCES_KEY = "processor.request.dir";
    public static final String INPUT_PRODUCT_DIR_PREFERENCES_KEY = "processor.input.product.dir";
    public static final String OUTPUT_PRODUCT_DIR_PREFERENCES_KEY = "processor.output.product.dir";

    private JFileChooser _fileDialog;
    private Processor _processor;
    private ProcessorUI _processorUI;
    private JButton _runButton;
    private JButton _helpButton;
    private RequestLoader _loader;
    private RequestWriter _writer;
    private Logger _logger;
    private boolean _standAlone;
    private RequestValidator _requestValidator;
    private List<RequestValidator> _requestValidatorList;

    private JMenuItem _openMenuItem;
    private JMenuItem _saveMenuItem;
    private JMenuItem _saveAsMenuItem;
    private JMenuItem _newMenuItem;
    private JMenuItem _helpMenuItem;
    private String _helpID;
    private ActionListener _exitHandler;

    /**
     * Constructs the object with given processor and logwriter
     *
     * @param processor the processor run by the ProcessorApp
     * @param loader    the request loader to be used (can be null)
     */
    public ProcessorApp(Processor processor, RequestLoader loader) {
        super(processor.getName(),
              processor.getSymbolicName(),
              processor.getVersion(),
              processor.getCopyrightInformation(),
              processor.getResourceBundleName(),
              null);

        _processor = processor;

        if (loader == null) {
            _loader = new RequestLoader();
            _loader.setElementFactory(_processor.getRequestElementFactory());
        } else {
            _loader = loader;
        }

        _logger = Logger.getLogger(ProcessorConstants.PACKAGE_LOGGER_NAME);

        addRequestValidator(new DefaultOutputValidator());

        _writer = new RequestWriter();
        _exitHandler = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isStandAlone()) {
                    shutDown();
                } else {
                    getMainFrame().setVisible(false);
                }
            }
        };
    }

    /**
     * Gets the associated processor.
     *
     * @return the processor
     * @since 4.1
     */
    public Processor getProcessor() {
        return _processor;
    }

    /**
     * Constructs the object with given processor
     *
     * @param processor the processor run by the ProcessorApp
     */
    public ProcessorApp(Processor processor) {
        this(processor, null);
    }

    /**
     * @return never <code>null</code>
     */
    public RequestValidator[] getRequestValidators() {
        if (_requestValidatorList != null) {
            return _requestValidatorList.toArray(new RequestValidator[_requestValidatorList.size()]);
        }
        return new RequestValidator[0];
    }

    public void addRequestValidator(RequestValidator requestValidator) {
        if (requestValidator == null) {
            return;
        }
        if (_requestValidatorList == null) {
            _requestValidatorList = new ArrayList<RequestValidator>(5);
        }
        if (!_requestValidatorList.contains(requestValidator)) {
            _requestValidatorList.add(requestValidator);
        }
    }

    public boolean removeRequestValidator(RequestValidator requestValidator) {
        if (_requestValidator == requestValidator) {
            _requestValidator = null;
        }
        return _requestValidatorList != null && _requestValidatorList.remove(requestValidator);
    }

    /**
     * Sets whether or not this processor application runs in stand-alone mode.
     * Must be called before {@link #startUp(com.bc.ceres.core.ProgressMonitor)} is called.
     *
     * @param standAlone whether or not this processor application runs in stand alone mode.
     */
    public void setStandAlone(boolean standAlone) {
        _standAlone = standAlone;
    }

    /**
     * Determines whether or not this processor application runs in stand-alone mode.
     *
     * @return <code>true</code>, if so.
     */
    public boolean isStandAlone() {
        return _standAlone;
    }

    /////////////////////////////////////////////////////////////////////////
    // Begin BasicApp Overrides

    /**
     * Runs the application.
     */
    @Override
    protected void initClient(ProgressMonitor pm) throws Exception {
        _processor.initProcessor();
    }

    /**
     * This method can be overridden in order to initialize a client user interface. It is called from the {@link
     * #startUp(com.bc.ceres.core.ProgressMonitor)} method before the {@link #applyPreferences()} is called and before an optional splash-screen closes
     * and the main frame becomes visible.
     *
     * @param pm A monitor to indicate progress.
     */
    @Override
    protected void initClientUI(ProgressMonitor pm) throws Exception {

        setCloseHandler(_exitHandler);

        _processor.setParentFrame(getMainFrame());

        ImageIcon imageIcon = UIUtils.loadImageIcon("icons/BeamIcon24.png");
        if (imageIcon != null) {
            getMainFrame().setIconImage(imageIcon.getImage());
        }

        getMainFrame().addComponentListener(createResizeListener());
        getMainFrame().pack();
    }

    /**
     * Creates the menu bar, attaches it to the main frame and adds the action listeners.
     */
    @Override
    protected CommandBar createMainMenuBar() {
        _newMenuItem = new JMenuItem("New Request");
        _newMenuItem.addActionListener(createNewRequestActionListener());
        _newMenuItem.setMnemonic('N');

        _openMenuItem = new JMenuItem("Open Request...");
        _openMenuItem.addActionListener(createLoadActionListener());
        _openMenuItem.setMnemonic('O');

        _saveMenuItem = new JMenuItem("Save Request");
        _saveMenuItem.addActionListener(createSaveActionListener());
        _saveMenuItem.setMnemonic('S');

        _saveAsMenuItem = new JMenuItem("Save Request As...");
        _saveAsMenuItem.addActionListener(createSaveAsActionListener());
        _saveAsMenuItem.setMnemonic('A');

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(_exitHandler);
        exitItem.setMnemonic('E');

        _helpMenuItem = new JMenuItem("Help");
        HelpSys.enableHelpOnButton(_helpMenuItem, "top");
        _helpMenuItem.setMnemonic('H');

        final JMenuItem aboutMenuItem = new JMenuItem("About...");
        aboutMenuItem.setMnemonic('A');
        aboutMenuItem.addActionListener(createAboutBoxActionListener());

        final JideMenu fileMenu = new JideMenu("File");
        fileMenu.setMnemonic('F');
        fileMenu.add(_newMenuItem);
        fileMenu.add(_openMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(_saveMenuItem);
        fileMenu.add(_saveAsMenuItem);
        if (isStandAlone()) {
            fileMenu.addSeparator();
            fileMenu.add(exitItem);
        }

        final JideMenu helpMenu = new JideMenu("Help");
        helpMenu.setMnemonic('H');
        helpMenu.add(_helpMenuItem);
        helpMenu.add(aboutMenuItem);

        CommandBar menuBar = new CommandMenuBar("Main Menu");
        menuBar.setFloatable(false);
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    /**
     * Creates the main pane for this application.
     */
    @Override
    protected JComponent createMainPane() {
        JComponent editorPane = createEditorPane();
        JComponent buttonPane = createButtonPane();

        JPanel mainPane = new JPanel(new BorderLayout());
        mainPane.add(editorPane, BorderLayout.CENTER);
        mainPane.add(buttonPane, BorderLayout.SOUTH);

        return mainPane;
    }

    /////////////////////////////////////////////////////////////////////////
    // End BasicApp Overrides
    /////////////////////////////////////////////////////////////////////////

    private ComponentAdapter createResizeListener() {
        return new ComponentAdapter() {
            final Dimension _minimumSize = getMainFrame().getPreferredSize();

            @Override
            public void componentResized(ComponentEvent e) {
                if (_minimumSize != null) {
                    final Dimension size = e.getComponent().getSize();
                    size.height = _minimumSize.height > size.height ? _minimumSize.height : size.height;
                    size.width = _minimumSize.width > size.width ? _minimumSize.width : size.width;
                    e.getComponent().setSize(size);
                }
            }
        };
    }

    /**
     * Called if the current processing request has been successfully completed. Shows a dialog stating the successful
     * processing.
     */
    public void processingCompleted() {
        if (isStandAlone()) {
            showInfoDialog("The request has successfully been processed.", null); /*I18N*/
        }
    }

    /**
     * Called if the current processing request has been aborted.
     */
    public void processingAborted() {
        _logger.warning("The processing has been aborted.");
        if (isStandAlone()) {
            showWarningDialog("The processing has been aborted."); /*I18N*/
        }
    }

    /**
     * Called if a processing error occurred.
     */
    public void processingFailed() {
        _logger.warning("The processing failed.");
        if (isStandAlone()) {
            showErrorDialog("Processing", /*I18N*/
                            "The processing failed."); /*I18N*/
        }
    }

    /**
     * Retrieves the requests currently used by the processor
     *
     * @return The requests processsed by the processor.
     * @throws org.esa.beam.framework.processor.ProcessorException
     *          If an error occurs.
     */
    public Vector getRequests() throws ProcessorException {
        return _processorUI.getRequests();
    }

    /**
     * Injects a list of requests to the processorUI.
     *
     * @param requests Sets the {@link org.esa.beam.framework.processor.Request requests} to be
     *                 processed by the processor
     * @throws org.esa.beam.framework.processor.ProcessorException
     *          If an error occurs.
     */
    public void setRequests(Vector requests) throws ProcessorException {
        _processorUI.setRequests(requests);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    private ActionListener createAboutBoxActionListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (_processorUI instanceof AbstractProcessorUI) {
                    ((AbstractProcessorUI) _processorUI).showAboutBox(_processor, _helpID);
                } else {
                    showInfoDialog("No about box available", null);
                }
            }
        };
    }

    // Creates the "Run", "Close" and "Help" button to the base frame
    private JComponent createButtonPane() {

        _runButton = new JButton("Run");
        _runButton.addActionListener(createRunActionListener());
        _runButton.setMnemonic('R');

        JButton closeButton = new JButton(isStandAlone() ? "Exit" : "Close");
        closeButton.addActionListener(_exitHandler);
        closeButton.setMnemonic(isStandAlone() ? 'E' : 'C');

        _helpButton = new JButton("Help");
        HelpSys.enableHelpOnButton(_helpButton, "top");
        _helpButton.setMnemonic('H');

        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        buttonPane.add(_runButton);
        buttonPane.add(closeButton);
        buttonPane.add(_helpButton);

        return buttonPane;
    }

    // Asks the processor to create it's user interface and add it to the frame window. When the processor has no user
    // interface, a default GUI is created, stating that the current processor has no GUI.
    private JComponent createEditorPane() {
        JComponent editorPane = null;
        String message = null;

        try {
            _processorUI = _processor.createUI();
            if (_processorUI != null) {
                _processorUI.setApp(this);
                editorPane = _processorUI.getGuiComponent();
                if (editorPane == null) {
                    throw new ProcessorException("Missing user interface component."); /*I18N*/
                }
                // set the initial requests.
                _processorUI.setRequests(_loader.getAllRequests());
            } else {
                message = "This processor has no user interface";  /*I18N*/
            }
        } catch (ProcessorException e) {
            message = "Failed to create processor user interface:\n" + e.getMessage();
            _logger.log(Level.SEVERE, message, e);
        }

        if (editorPane == null) {
            Guardian.assertNotNull("message", message);
            // must create a message string that processor does not support GUI
            editorPane = new JPanel();
            editorPane.add(new JLabel(message));
            _newMenuItem.setEnabled(false);
            _openMenuItem.setEnabled(false);
            _saveMenuItem.setEnabled(false);
            _saveAsMenuItem.setEnabled(false);
            _runButton.setEnabled(false);
        }

        editorPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        return editorPane;
    }

    /**
     * Callback invoked on run button clicks. Creates a SwingWorker thread and runs the processor on the request.
     */
    private void runProcessor() {

        final List requestList;
        try {
            requestList = _processorUI.getRequests();
        } catch (ProcessorException e) {
            showErrorDialog(getMainFrame().getTitle(),
                            SystemUtils.createHumanReadableExceptionMessage(e));
            return;
        }
        if (requestList == null || requestList.size() == 0) {
            showErrorDialog(getMainFrame().getTitle(),
                            "No processing requests defined.");
            return;
        }

        final RequestValidator[] requestValidators = getRequestValidators();
        for (RequestValidator requestValidator : requestValidators) {
            // If request validation enabled, perform all checks on Swing's event dispatching thread
            // and store the results in validationResults
            for (Object aRequestList : requestList) {
                final Request request = (Request) aRequestList;
                if (!requestValidator.validateRequest(_processor, request)) {
                    return;
                }
            }
        }

        SwingWorker worker = new ProgressMonitorSwingWorker(getMainFrame(), _processor.getUITitle()) {

            @Override
            protected Object doInBackground(ProgressMonitor pm) throws Exception {
                Object returnValue = null;
                try {
                    if (requestList.size() == 1) {
                        processSingleRequest(requestList, pm);
                    } else {
                        processMultipleRequests(requestList, pm);
                    }
                    if (_processor.isAborted()) {
                        processingAborted();
                    } else if (_processor.isFailed()) {
                        processingFailed();
                    } else {
                        processingCompleted();
                    }
                    _processor.setCurrentStatus(ProcessorConstants.STATUS_UNKNOWN);
                } catch (ProcessorException e) {
                    Debug.trace(e);
                    returnValue = e;
                } finally {
                    ProcessorUtils.removeLoggingHandler();
                }
                return returnValue;

            }

            @Override
            protected void done() {
                Object value = null;
                try {
                    value = get();
                } catch (Exception e) {
                    // ignore???
                }
                if (value instanceof ProcessorException) {
                    ProcessorException e = (ProcessorException) value;
                    showErrorDialog(getMainFrame().getTitle(),
                                    SystemUtils.createHumanReadableExceptionMessage(e));
                }
                enableUIAccess();
            }
        };

        disableUIAccess();
        worker.execute();
    }

    private void processMultipleRequests(final List requestList, com.bc.ceres.core.ProgressMonitor pm) throws
            ProcessorException {
        String message = _processor.getProgressMessage((Request) requestList.get(0));
        pm.beginTask(message, requestList.size());
        try {
            for (Object aRequestList : requestList) {
                if (_processor.isAborted()) {
                    break;
                }

                message = _processor.getProgressMessage((Request) aRequestList);
                pm.setSubTaskName(message);

                Request _currentRequest = (Request) aRequestList;
                _processor.processRequest(_currentRequest, SubProgressMonitor.create(pm, 1));
                if (pm.isCanceled()) {
                    break;
                }
            }
        } finally {
            pm.done();
        }
    }

    private void processSingleRequest(final List requestList, ProgressMonitor pm) throws ProcessorException {
        Request currentRequest = (Request) requestList.get(0);
        _processor.processRequest(currentRequest, pm);
    }

    /**
     * Enables access to the UI components
     */
    private void enableUIAccess() {
        _runButton.setEnabled(true);
        getMainFrame().setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Disables access to the UI components
     */
    private void disableUIAccess() {
        _runButton.setEnabled(false);
        getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    /**
     * Callback invoked on load request menu item
     */
    private void onLoadRequest() {
        JFileChooser fileChooser = getFileDialogSafe();
        int retVal;

        retVal = fileChooser.showOpenDialog(getMainFrame());
        if (fileChooser.getCurrentDirectory() != null) {
            setRequestDir(fileChooser.getCurrentDirectory());
        }
        if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                _loader.setAndParseRequestFile(file);
                if (_loader.getAllRequests().size() < 1) {
                    showErrorDialog("The selected XML file seems not to be a valid processing request file."); /*I18N*/
                } else {
                    _processorUI.setRequests(_loader.getAllRequests());
                }
            } catch (RequestElementFactoryException e) {
                Debug.trace(e);
                showErrorDialog(getMainFrame().getTitle(),
                                SystemUtils.createHumanReadableExceptionMessage(e));
            } catch (ProcessorException e) {
                Debug.trace(e);
                showErrorDialog(getMainFrame().getTitle(),
                                SystemUtils.createHumanReadableExceptionMessage(e));
            }
        }
    }

    /**
     * Callback invoked on save request menu item
     */
    private void onSaveRequest() {
        Vector<Request> requests = null;

        try {
            requests = _processorUI.getRequests();
        } catch (ProcessorException e) {
            showErrorDialog(getMainFrame().getTitle(),
                            SystemUtils.createHumanReadableExceptionMessage(e));
        }

        if (requests == null || requests.size() < 1) {
            return;
        }

        final File requestFile = requests.elementAt(0).getFile();
        if (requestFile == null) {
            // when the request has no file information - invoke save as dialog
            onSaveRequestAs();
        } else {
            try {
                Request[] allRequests = requests.toArray(new Request[requests.size()]);
                _writer.write(allRequests, requestFile);
            } catch (IOException e) {
                showErrorDialog(getMainFrame().getTitle(),
                                SystemUtils.createHumanReadableExceptionMessage(e));
            }
        }
    }

    /**
     * Callback invoked on save request as menu item
     */
    private void onSaveRequestAs() {
        final JFileChooser fileChooser = getFileDialogSafe();
        final int retVal = fileChooser.showSaveDialog(getMainFrame());

        if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            // check if directory exists - if not: create
            final File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // check if file has extension - if not: create
            file = FileUtils.ensureExtension(file, ".xml");

            if (!promptForOverwrite(file)) {
                return;
            }
            try {
                final Vector<Request> requests = _processorUI.getRequests();

                if (requests != null) {
                    final Request[] requestArray = requests.toArray(new Request[requests.size()]);
                    _writer.write(requestArray, file);
                    // set new file to request and reassign to UI
                    for (Request request : requestArray) {
                        if (request != null) {
                            request.setFile(file);
                        }
                    }
                }
                _processorUI.setRequests(requests);
            } catch (IOException e) {
                showErrorDialog(getMainFrame().getTitle(),
                                SystemUtils.createHumanReadableExceptionMessage(e));
            } catch (ProcessorException e) {
                showErrorDialog(getMainFrame().getTitle(),
                                SystemUtils.createHumanReadableExceptionMessage(e));
            }
        }
    }

    /**
     * Callback invoked on new request events
     */
    private void onNewRequest() {
        try {
            _processorUI.setDefaultRequests();
        } catch (ProcessorException e) {
            showErrorDialog(getMainFrame().getTitle(),
                            SystemUtils.createHumanReadableExceptionMessage(e));
        }
    }

    // Retrieves the file choosed object. If non is persent, an object is constructed
    private JFileChooser getFileDialogSafe() {
        if (_fileDialog == null) {
            _fileDialog = new BeamFileChooser();
            _fileDialog.setFileFilter(new BeamFileFilter("BEAM request files",
                                                         ".xml",
                                                         "BEAM request files (*.xml)"));
            _fileDialog.setCurrentDirectory(getRequestDir());
        }

        return _fileDialog;
    }

    private File getRequestDir() {
        return new File(getPreferences().getPropertyString(REQUEST_DIR_PREFERENCES_KEY,
                                                           SystemUtils.getApplicationHomeDir().getPath()));
    }

    private void setRequestDir(File dir) {
        getPreferences().setPropertyString(REQUEST_DIR_PREFERENCES_KEY, dir.getPath());
    }

    /**
     * Creates an action listener for the "Run" event
     *
     * @return the action listener
     */
    private ActionListener createRunActionListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                runProcessor();
            }
        };
    }

    /**
     * Creates an action listener for the "SaveAs" event
     *
     * @return the action listener
     */
    private ActionListener createSaveAsActionListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onSaveRequestAs();
            }
        };
    }

    /**
     * Creates an action listener for the "Open" event
     *
     * @return the action listener
     */
    private ActionListener createLoadActionListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (_processorUI != null) {
                    onLoadRequest();
                }
            }
        };
    }

    /**
     * Creates an action listener for the "Save" event
     *
     * @return the action listener
     */
    private ActionListener createSaveActionListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onSaveRequest();
            }
        };
    }

    /**
     * Creates an action listener for the "new Request" event
     *
     * @return the action listener
     */
    private ActionListener createNewRequestActionListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onNewRequest();
            }
        };
    }

    public void markIODirChanges(final ParamGroup paramGroup) {
        markParentDirChanges(paramGroup,
                             ProcessorConstants.INPUT_PRODUCT_PARAM_NAME,
                             INPUT_PRODUCT_DIR_PREFERENCES_KEY);
        markParentDirChanges(paramGroup,
                             ProcessorConstants.OUTPUT_PRODUCT_PARAM_NAME,
                             OUTPUT_PRODUCT_DIR_PREFERENCES_KEY);
    }

    public void markParentDirChanges(final ParamGroup paramGroup,
                                     final String paramName,
                                     final String preferencesKey) {
        final Parameter parameter = paramGroup.getParameter(paramName);
        if (parameter != null) {
            markParentDirChanges(parameter, preferencesKey);
        }
    }

    public void markParentDirChanges(final Parameter parameter, final String preferencesKey) {
        final String path = getPreferences().getPropertyString(preferencesKey);
        if (path != null) {
            parameter.getProperties().setPropertyValue(ParamProperties.LAST_DIR_KEY, new File(path));
        }
        parameter.getProperties().addPropertyChangeListener(new ParentDirMarker(preferencesKey));
    }

    public AbstractButton getHelpButton() {
        return _helpButton;
    }

    public void setHelpID(String helpID) {
        if (!HelpSys.isValidID(helpID)) {
            helpID = "top";
        }
        _helpID = helpID;
        HelpSys.enableHelpOnButton(_helpButton, helpID);
        HelpSys.enableHelpOnButton(_helpMenuItem, helpID);
        HelpSys.enableHelpKey(getMainFrame(), helpID);
    }


    private class ParentDirMarker implements PropertyChangeListener {

        private final String _preferencesKey;

        public ParentDirMarker(String preferencesKey) {
            _preferencesKey = preferencesKey;
        }

        /**
         * This method gets called when a bound property is changed.
         *
         * @param evt A PropertyChangeEvent object describing the event source and the property that has changed.
         */

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(ParamProperties.LAST_DIR_KEY) &&
                    evt.getNewValue() instanceof File) {
                File file = (File) evt.getNewValue();
                getPreferences().setPropertyString(_preferencesKey, file.getPath());
                Debug.trace(getAppName() + ": " + _preferencesKey + " = " + file);
            }
        }
    }

    private class DefaultOutputValidator implements RequestValidator {

        public boolean validateRequest(Processor processor, Request request) {
            for (int i = 0; i < request.getNumOutputProducts(); i++) {
                ProductRef product = request.getOutputProductAt(i);
                File outputFile = product.getFile();
                if (outputFile != null && outputFile.exists()) {
                    String message = "The specified output file\n\"{0}\"\n already exists.\n\n" +
                            "Do you want to overwrite the existing file?";
                    int answer = showQuestionDialog("Overwrite?",
                                                    MessageFormat.format(message, outputFile.getAbsolutePath()),
                                                    null);

                    if (answer != JOptionPane.YES_OPTION) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
