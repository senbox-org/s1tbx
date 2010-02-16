package org.esa.beam.scripting.visat;

import org.esa.beam.scripting.visat.actions.HelpAction;
import org.esa.beam.scripting.visat.actions.NewAction;
import org.esa.beam.scripting.visat.actions.OpenAction;
import org.esa.beam.scripting.visat.actions.RunAction;
import org.esa.beam.scripting.visat.actions.SaveAction;
import org.esa.beam.scripting.visat.actions.SaveAsAction;
import org.esa.beam.scripting.visat.actions.StopAction;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class ScriptConsoleForm {

    // View
    private final ScriptConsoleToolView toolView;
    private Map<String, Action> actionMap;
    private JTextArea inputTextArea;
    private JTextArea outputTextArea;
    private JPanel contentPanel;

    private ScriptManager scriptManager;
    private PrintWriter output;
    private File file;

    public ScriptConsoleForm(ScriptConsoleToolView toolView) {
        this.toolView = toolView;
        this.actionMap = new HashMap<String, Action>();

        registerAction(new NewAction(this));
        registerAction(new OpenAction(this));
        registerAction(new SaveAction(this));
        registerAction(new SaveAsAction(this));
        registerAction(new RunAction(this));
        registerAction(new StopAction(this));
        registerAction(new HelpAction(this));

        inputTextArea = new JTextArea(); // todo - replace by JIDE code editor component (nf)
        inputTextArea.setWrapStyleWord(false);
        inputTextArea.setTabSize(4);
        inputTextArea.setRows(10);
        inputTextArea.setColumns(80);
        inputTextArea.setFont(new Font("Courier", Font.PLAIN, 13));

        outputTextArea = new JTextArea();
        outputTextArea.setWrapStyleWord(false);
        outputTextArea.setTabSize(4);
        outputTextArea.setRows(3);
        outputTextArea.setColumns(80);
        outputTextArea.setEditable(false);
        outputTextArea.setBackground(Color.LIGHT_GRAY);
        outputTextArea.setFont(new Font("Courier", Font.PLAIN, 13));

        final JToolBar toolBar = new JToolBar("Script Console");
        toolBar.setFloatable(false);
        toolBar.add(getToolButton(NewAction.ID));
        toolBar.add(getToolButton(OpenAction.ID));
        toolBar.add(getToolButton(SaveAction.ID));
        toolBar.add(getToolButton(SaveAsAction.ID));
        toolBar.addSeparator();
        toolBar.add(getToolButton(RunAction.ID));
        toolBar.add(getToolButton(StopAction.ID));
        toolBar.addSeparator();
        toolBar.add(getToolButton(HelpAction.ID));

        getAction(NewAction.ID).setEnabled(true);
        getAction(OpenAction.ID).setEnabled(true);
        getAction(SaveAction.ID).setEnabled(false);
        getAction(SaveAsAction.ID).setEnabled(false);
        getAction(RunAction.ID).setEnabled(false);
        getAction(StopAction.ID).setEnabled(false);
        getAction(HelpAction.ID).setEnabled(true);
        inputTextArea.setEditable(false);
        inputTextArea.setEnabled(false);

        JScrollPane inputEditorScrollPane = new JScrollPane(inputTextArea);
        inputEditorScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        inputEditorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JScrollPane outputEditorScrollPane = new JScrollPane(outputTextArea);
        outputEditorScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        outputEditorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JSplitPane documentPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputEditorScrollPane, outputEditorScrollPane);
        documentPanel.setDividerLocation(0.7);
        documentPanel.setBorder(null);

        contentPanel = new JPanel(new BorderLayout(2, 2));
        contentPanel.setPreferredSize(new Dimension(800, 400));
        contentPanel.add(toolBar, BorderLayout.NORTH);
        contentPanel.add(documentPanel, BorderLayout.CENTER);

        output = new PrintWriter(new ScriptOutput(), true);
        scriptManager = new ScriptManager(getClass().getClassLoader(), output);
    }

    private void registerAction(Action action) {
        actionMap.put(action.getValue(Action.ACTION_COMMAND_KEY).toString(), action);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public void runScript() {

        if (scriptManager.getEngine() == null) {
            showErrorMessage("No script language selected.");
            return;
        }

        final String text = inputTextArea.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        outputTextArea.setText(null);

        enableRun(false);
        scriptManager.execute(text, new ExecutionObserver());
    }

    public void stopScript() {
        scriptManager.reset();
        getAction(StopAction.ID).setEnabled(false);
    }

    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(toolView.getContext().getPage().getWindow(),
                                      message, "Script Console - Error",
                                      JOptionPane.ERROR_MESSAGE);
    }

    public void newScript(ScriptEngineFactory scriptEngineFactory) {
        inputTextArea.setText(null);
        outputTextArea.setText(null);

        ScriptEngine factory = scriptManager.getEngineByFactory(scriptEngineFactory);
        scriptManager.setEngine(factory);
        setFile(null);

        enableRun(true);
    }

    private void enableRun(boolean b) {
        getAction(NewAction.ID).setEnabled(b);
        getAction(OpenAction.ID).setEnabled(b);
        getAction(SaveAction.ID).setEnabled(b);
        getAction(SaveAsAction.ID).setEnabled(b);

        getAction(RunAction.ID).setEnabled(b);
        getAction(StopAction.ID).setEnabled(!b);

        inputTextArea.setEnabled(b);
        inputTextArea.setEditable(b);
    }

    private JButton getToolButton(String actionId) {
        Action action = getAction(actionId);
        final JButton button = new JButton(action);
        button.setText(null);
        return button;
    }

    public Action getAction(String actionId) {
        return actionMap.get(actionId);
    }

    public void openScript(File file) {
        enableRun(false);
        // todo - use swing worker
        try {
            String fileName = file.getName();
            int i = fileName.lastIndexOf('.');
            if (i <= 0) {
                showErrorMessage(MessageFormat.format("Unknown script type ''{0}''.", fileName));
                return;
            }
            String ext = fileName.substring(i + 1);
            ScriptEngine scriptEngine = scriptManager.getEngineByExtension(ext);
            if (scriptEngine == null) {
                showErrorMessage(MessageFormat.format("Unknown script type ''{0}''.", fileName));
                return;
            }

            StringBuilder sb = new StringBuilder();
            try {
                LineNumberReader reader = new LineNumberReader(new FileReader(file));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                showErrorMessage(MessageFormat.format("I/O error:\n{0}", e.getMessage()));
                return;
            }

            inputTextArea.setText(sb.toString());
            scriptManager.setEngine(scriptEngine);
            setFile(file);
        } finally {
            enableRun(true);
        }
    }

    public File getFile() {
        return file;
    }

    private void setFile(File file) {
        this.file = file;
        updateTitle();
    }

    private void updateTitle() {

        ScriptEngine scriptEngine = scriptManager.getEngine();
        if (scriptEngine != null) {
            String languageName = scriptEngine.getFactory().getLanguageName();
            if (file != null) {
                toolView.setTitle(MessageFormat.format("{0} - [{1}] - [{2}]", toolView.getTitleBase(), languageName, file));
            } else {
                toolView.setTitle(MessageFormat.format("{0} - [{1}] - [unnamed]", toolView.getTitleBase(), languageName));
            }
        } else {
            toolView.setTitle(toolView.getTitleBase());
        }
    }

    public void saveScriptAs(File file) {
        enableRun(false);
        // todo - use swing worker
        try {
            try {
                FileWriter writer = new FileWriter(file);
                try {
                    writer.write(inputTextArea.getText());
                } finally {
                    writer.close();
                }
                setFile(file);
            } catch (IOException e) {
                showErrorMessage(MessageFormat.format("I/O error:\n{0}", e.getMessage()));
            }
        } finally {
            enableRun(true);
        }
    }

    public void saveScript() {
        saveScriptAs(getFile());
    }

    public class ScriptOutput extends Writer {

        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }

        @Override
        public void write(char characters[], int off, int len) {
            print0(new String(characters, off, len));
        }

        @Override
        public void write(String str) {
            print0(str);
        }

        /////////////////////////////////////////////////////////////////////
        // private

        private void print0(final String str) {
            if (SwingUtilities.isEventDispatchThread()) {
                print1(str);
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        print1(str);
                    }
                });
            }
        }

        private void print1(String text) {
            try {
                int offset = outputTextArea.getDocument().getEndPosition().getOffset();
                outputTextArea.getDocument().insertString(offset, text, null);
            } catch (BadLocationException e) {
                // ignore
            }
        }


    }

    private class ExecutionObserver implements ScriptManager.Observer {
        @Override
        public void onSuccess(Object value) {
            if (value != null) {
                output.println(String.valueOf(value));
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    enableRun(true);
                }
            });
        }

        @Override
        public void onFailure(Throwable throwable) {
            output.println("Error: " + throwable.getMessage());
            throwable.printStackTrace(output);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    enableRun(true);
                }
            });
        }
    }
}