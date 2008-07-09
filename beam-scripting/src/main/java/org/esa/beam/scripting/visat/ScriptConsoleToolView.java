package org.esa.beam.scripting.visat;

import com.bc.ceres.core.Assert;
import com.jidesoft.status.StatusBar;
import com.jidesoft.swing.JideScrollPane;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

// todo - IMPORTANT NOTE: This code represents a feasibility study. It lacks junit level tests and
// requires an extreme refactoring.

// todo - find out how to:
// (1) ... greatfully cancel a running script
// (2) ... remove bindings (references) in JavaScript to products, views, etc. in order to avoid memory leaks
// (3) ... debug a script
// (4) ... trace & undo changes to BEAM made by a script

/**
 * A tool window for the scripting console.
 */
public class ScriptConsoleToolView extends AbstractToolView {

    public static final String ID = ScriptConsoleToolView.class.getName();

    private static final String DEFAULT_SCRIPT_LANGUAGE_NAME = "JavaScript";

    private JTextArea inputTextArea;
    private JTextArea outputTextArea;
    private ScriptEngineManager scriptEngineManager;
    private ScriptEngine scriptEngine;
    private StringBuffer out = new StringBuffer();
    private Action runAction;
    private Action stopAction;
    private Action clearAction;
    private SwingWorker<Object, Object> worker;
    private StatusBar statusBar;

    public ScriptConsoleToolView() {
    }

    @Override
    public JComponent createControl() {
        String errorMessage = null;

        if (scriptEngine == null) {
            try {
                initScriptEngine();
            } catch (Exception e) {
                scriptEngine = null;
                errorMessage = "Failed to initialise scripting engine.\n" + getStackTraceText(e);
            }
        }

        if (scriptEngine != null) {
            try {
                loadInitScript();
            } catch (Exception e) {
                errorMessage = "Failed to load initialisation script.\n" + getStackTraceText(e);
            }
        }

        boolean enabled = scriptEngine != null;


        inputTextArea = new JTextArea(); // todo - replace by JIDE code editor component
        inputTextArea.setWrapStyleWord(false);
        inputTextArea.setTabSize(4);
        inputTextArea.setRows(10);
        inputTextArea.setColumns(80);
        inputTextArea.setFont(new Font("Courier", Font.PLAIN, 13));
        inputTextArea.setEditable(true);
        inputTextArea.setEnabled(enabled);

        outputTextArea = new JTextArea();
        outputTextArea.setWrapStyleWord(false);
        outputTextArea.setTabSize(4);
        outputTextArea.setRows(3);
        outputTextArea.setColumns(80);
        outputTextArea.setFont(new Font("Courier", Font.PLAIN, 13));
        outputTextArea.setEditable(false);
        outputTextArea.setBackground(Color.LIGHT_GRAY);
        outputTextArea.setEnabled(enabled);

        statusBar = new StatusBar();
        statusBar.setEnabled(enabled);

        final JToolBar toolBar = new JToolBar("scripting");

        runAction = new RunAction();
        runAction.setEnabled(enabled);
        toolBar.add(ToolButtonFactory.createButton(runAction, false));

        stopAction = new StopAction();
        stopAction.setEnabled(enabled);
        toolBar.add(ToolButtonFactory.createButton(stopAction, false));

        clearAction = new ClearAction();
        clearAction.setEnabled(enabled);
        toolBar.add(ToolButtonFactory.createButton(clearAction, false));

        JComboBox comboBox = createLanguageSwitcher();
        comboBox.setEnabled(enabled);
        toolBar.add(comboBox);

        final JMenuBar menuBar = createMenuBar();
        menuBar.setEnabled(enabled);

        final JScrollPane inputEditorScrollPane = new JideScrollPane(inputTextArea); // <JIDE>
        inputEditorScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        inputEditorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputEditorScrollPane.setBorder(null);
        inputEditorScrollPane.setViewportBorder(null);
        inputEditorScrollPane.setEnabled(enabled);

        final JScrollPane outputEditorScrollPane = new JideScrollPane(outputTextArea); // <JIDE>
        outputEditorScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        outputEditorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outputEditorScrollPane.setBorder(null);
        outputEditorScrollPane.setViewportBorder(null);
        outputEditorScrollPane.setEnabled(enabled);

        final JSplitPane documentPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputEditorScrollPane, outputEditorScrollPane);
        documentPanel.setDividerLocation(0.7);
        documentPanel.setEnabled(enabled);

        JPanel consolePanel = new JPanel(new BorderLayout(2, 2));
        consolePanel.setPreferredSize(new Dimension(800, 300));
        consolePanel.add(toolBar, BorderLayout.NORTH);
        consolePanel.add(documentPanel, BorderLayout.CENTER);
        consolePanel.setEnabled(enabled);

        JPanel windowPanel = new JPanel(new BorderLayout(2, 2));
        windowPanel.setPreferredSize(new Dimension(800, 300));
        windowPanel.add(menuBar, BorderLayout.NORTH);
        windowPanel.add(consolePanel, BorderLayout.CENTER);
        windowPanel.add(statusBar, BorderLayout.SOUTH);
        windowPanel.setEnabled(enabled);

        printEngineDetails(errorMessage);
        return windowPanel;
    }

    private String getStackTraceText(Exception e) {
        final StringWriter writer = new StringWriter();
        final PrintWriter s = new PrintWriter(writer);
        e.printStackTrace(s);
        s.close();
        final String s1 = writer.toString();
        return s1;
    }

    private JComboBox createLanguageSwitcher() {
        List<ScriptEngineFactory> engineFactories = scriptEngineManager.getEngineFactories();
        List<String> languageNames = new ArrayList<String>(engineFactories.size());
        for (ScriptEngineFactory scriptEngineFactory : engineFactories) {
            if (scriptEngineFactory.getNames().contains(DEFAULT_SCRIPT_LANGUAGE_NAME)) {
                languageNames.add(DEFAULT_SCRIPT_LANGUAGE_NAME);
            } else {
                languageNames.add(scriptEngineFactory.getLanguageName());
            }
        }
        final JComboBox comboBox = new JComboBox(languageNames.toArray());
        comboBox.setMaximumSize(comboBox.getPreferredSize());
        comboBox.setSelectedItem(DEFAULT_SCRIPT_LANGUAGE_NAME);
        comboBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) comboBox.getSelectedItem();
                scriptEngine = scriptEngineManager.getEngineByName(selectedItem);
                printEngineDetails(null);
            }
        });
        return comboBox;
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());
        menuBar.add(createHelpMenu());
        menuBar.setFocusable(true);
        return menuBar;
    }

    private JMenu createFileMenu() {
        final JMenu menu = new JMenu("File");
        menu.setMnemonic('F');
        return menu;
    }

    private JMenu createEditMenu() {
        final JMenu menu = new JMenu("Edit");
        menu.setMnemonic('E');
        return menu;
    }

    private JMenu createHelpMenu() {
        final JMenu menu = new JMenu("Help");
        menu.setMnemonic('H');
        menu.add(createJsMenu());
        return menu;
    }

    private JMenu createJsMenu() {
        final JMenu jsMenu = new JMenu("JavaScript");
        jsMenu.setMnemonic('J');
        final String[][] entries = new String[][]{
                {"BEAM JavaScript (BEAM Wiki)", "http://www.brockmann-consult.de/beam-wiki/display/BEAM/BEAM+JavaScript"},
                {"JavaScript Introduction (Mozilla)", "http://developer.mozilla.org/en/docs/JavaScript"},
                {"JavaScript Syntax (Wikipedia)", "http://en.wikipedia.org/wiki/JavaScript_syntax"},
        };


        for (final String[] entry : entries) {
            final String text = entry[0];
            final String target = entry[1];
            final JMenuItem menuItem = new JMenuItem(text);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        Desktop.getDesktop().browse(new URI(target));
                    } catch (IOException e1) {
                        showErrorDialog(e1.getMessage());
                    } catch (URISyntaxException e1) {
                        showErrorDialog(e1.getMessage());
                    }
                }
            });
            jsMenu.add(menuItem);
        }
        return jsMenu;
    }

    private void printEngineDetails(String errorMessage) {
        if (scriptEngine != null) {
            out.append("Script engine name: ");
            out.append(scriptEngine.getFactory().getEngineName());
            out.append("\n");
            out.append("Script engine version: ");
            out.append(scriptEngine.getFactory().getEngineVersion());
            out.append("\n");
            out.append("Script language name: ");
            out.append(scriptEngine.getFactory().getLanguageName());
            out.append("\n");
            out.append("Script language version: ");
            out.append(scriptEngine.getFactory().getLanguageVersion());
            out.append("\n");
        }
        if (errorMessage != null) {
            out.append("\n");
            out.append(errorMessage);
            out.append("\n");
        }
        outputTextArea.setText(out.toString());
        out.setLength(0);
    }

    private void initScriptEngine() throws ScriptException, IOException {
        final ClassLoader oldClassLoader = getContextClassLoader();
        try {
            setContextClassLoader(ScriptConsoleToolView.class.getClassLoader());
            // create a script engine manager
            scriptEngineManager = new ScriptEngineManager(ScriptConsoleToolView.class.getClassLoader());
            // create a JavaScript engine
            scriptEngine = scriptEngineManager.getEngineByName(DEFAULT_SCRIPT_LANGUAGE_NAME);
            if (scriptEngine == null) {
                throw new ScriptException("No engine for scripting language '" + DEFAULT_SCRIPT_LANGUAGE_NAME + "' found.");
            }

            final ScriptWriter writer = new ScriptWriter(); // fixme - don't get any output
            scriptEngine.getContext().setWriter(writer);
            scriptEngine.getContext().setErrorWriter(writer);
        } finally {
            setContextClassLoader(oldClassLoader);
        }
    }

    private void loadInitScript() throws ScriptException, IOException {
        final InputStreamReader streamReader = new InputStreamReader(getClass().getResourceAsStream("visat.js"));
        try {
            scriptEngine.eval(streamReader);
        } finally {
            streamReader.close();
        }
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    private void setContextClassLoader(ClassLoader loader) {
        Thread.currentThread().setContextClassLoader(loader);
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, getTitle(), JOptionPane.ERROR_MESSAGE);
    }

    private class RunAction extends AbstractAction {

        public RunAction() {
            putValue(AbstractAction.NAME, "Run");
            putValue(AbstractAction.ACTION_COMMAND_KEY, "scriptConsole.run");
            final ImageIcon icon = UIUtils.loadImageIcon("/org/esa/beam/scripting/visat/icons/media-playback-start-16.png", ScriptConsoleToolView.class);
            putValue(AbstractAction.SMALL_ICON, icon);
            putValue(AbstractAction.LARGE_ICON_KEY, icon);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            final String text = inputTextArea.getText().trim();
            if (text.isEmpty()) {
                return;
            }

            runAction.setEnabled(false);
            stopAction.setEnabled(true);

            Assert.state(worker == null, "worker == null");

            worker = new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground() throws Exception {
                    final ClassLoader oldClassLoader = getContextClassLoader();
                    try {
                        setContextClassLoader(ScriptConsoleToolView.class.getClassLoader());
                        // evaluate script code from String
                        return scriptEngine.eval(text);
                    } finally {
                        setContextClassLoader(oldClassLoader);
                    }
                }

                @Override
                protected void done() {

                    worker = null;
                    runAction.setEnabled(true);
                    stopAction.setEnabled(false);

                    try {
                        Object result = get();
                        out.append("Result: " + result + "\n");
                    } catch (InterruptedException e) {
                        out.append("Script evaluation interrupted.\n");
                    } catch (CancellationException e) {
                        out.append("Script evaluation canceled.\n");
                    } catch (Throwable e) {
                        out.append("Error: " + e.getCause().getMessage() + "\n");
                    }
                    outputTextArea.setText(out.toString());
                    out.setLength(0);
                }
            };

            worker.execute();
        }
    }

    private class StopAction extends AbstractAction {

        public StopAction() {
            putValue(AbstractAction.NAME, "Stop");
            putValue(AbstractAction.ACTION_COMMAND_KEY, "scriptConsole.stop");
            final ImageIcon icon = UIUtils.loadImageIcon("/org/esa/beam/scripting/visat/icons/process-stop-16.png", ScriptConsoleToolView.class);
            putValue(AbstractAction.SMALL_ICON, icon);
            putValue(AbstractAction.LARGE_ICON_KEY, icon);
        }

        public void actionPerformed(ActionEvent e) {
            if (worker != null) {
                worker.cancel(true);
                worker = null;
            }
        }
    }

    private class ClearAction extends AbstractAction {
        public ClearAction() {
            putValue(AbstractAction.NAME, "Clear");
            putValue(AbstractAction.ACTION_COMMAND_KEY, "scriptConsole.clear");
            final ImageIcon icon = UIUtils.loadImageIcon("/org/esa/beam/scripting/visat/icons/edit-clear-16.png", ScriptConsoleToolView.class);
            putValue(AbstractAction.SMALL_ICON, icon);
            putValue(AbstractAction.LARGE_ICON_KEY, icon);
        }

        public void actionPerformed(ActionEvent e) {
            inputTextArea.setText("");
            outputTextArea.setText("");
            out.setLength(0);
        }
    }

    private class ScriptWriter extends Writer {

        @Override
        public void close() throws IOException {
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void write(char cbuf[], int off, int len) throws IOException {
            out.append(cbuf, off, len);
        }
    }
}
 
