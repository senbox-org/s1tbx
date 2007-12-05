package org.esa.beam.scripting.visat;

import com.jidesoft.swing.JideScrollPane;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.visat.VisatApp;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.Writer;

/**
 * The tool window which displays the tree of open products.
 */
public class ScriptConsoleToolView extends AbstractToolView {

    public static final String ID = ScriptConsoleToolView.class.getName();

    private static final String SCRIPT_LANGUAGE_NAME = "JavaScript";

    private VisatApp visatApp;
    private JTextArea inputTextArea;
    private JTextArea outputTextArea;
    private ScriptEngineManager scriptEngineManager;
    private ScriptEngine scriptEngine;
    private StringBuffer out = new StringBuffer();

    public ScriptConsoleToolView() {
        this.visatApp = VisatApp.getApp();
    }

    @Override
    public JComponent createControl() {
        if (scriptEngineManager == null) {
            final ClassLoader oldClassLoader = getContextClassLoader();
            try {
                setContextClassLoader(ScriptConsoleToolView.class.getClassLoader());
                // create a script engine manager
                scriptEngineManager = new ScriptEngineManager(ScriptConsoleToolView.class.getClassLoader());
                // create a JavaScript engine
                scriptEngine = scriptEngineManager.getEngineByName(SCRIPT_LANGUAGE_NAME);

                final ScriptWriter writer = new ScriptWriter(); // fixme - don't get any output
                scriptEngine.getContext().setWriter(writer);
                scriptEngine.getContext().setErrorWriter(writer);
            } finally {
                setContextClassLoader(oldClassLoader);
            }
        }

        inputTextArea = new JTextArea(); // todo - replace by JIDE code editor component
        inputTextArea.setWrapStyleWord(false);
        inputTextArea.setTabSize(4);
        inputTextArea.setRows(10);
        inputTextArea.setColumns(80);
        inputTextArea.setFont(new Font("Courier", Font.PLAIN, 13));
        inputTextArea.setEditable(true);

        outputTextArea = new JTextArea();
        outputTextArea.setWrapStyleWord(false);
        outputTextArea.setTabSize(4);
        outputTextArea.setRows(3);
        outputTextArea.setColumns(80);
        outputTextArea.setFont(new Font("Courier", Font.PLAIN, 13));
        outputTextArea.setEditable(false);
        outputTextArea.setBackground(Color.LIGHT_GRAY);

        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));

        final Action runAction = new RunAction();
        buttonPanel.add(ToolButtonFactory.createButton(runAction, false));

        final Action terminateAction = new TerminateAction();
        buttonPanel.add(ToolButtonFactory.createButton(terminateAction, false));

        final Action clearAction = new ClearAction();
        buttonPanel.add(ToolButtonFactory.createButton(clearAction, false));

        final JScrollPane inputEditorScrollPane = new JideScrollPane(inputTextArea); // <JIDE>
        inputEditorScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        inputEditorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputEditorScrollPane.setBorder(null);
        inputEditorScrollPane.setViewportBorder(null);

        final JScrollPane outputEditorScrollPane = new JideScrollPane(outputTextArea); // <JIDE>
        outputEditorScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        outputEditorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outputEditorScrollPane.setBorder(null);
        outputEditorScrollPane.setViewportBorder(null);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputEditorScrollPane, outputEditorScrollPane);
        splitPane.setDividerLocation(0.7);

        JPanel consolePanel = new JPanel(new BorderLayout(2, 2));
        consolePanel.setPreferredSize(new Dimension(800, 300));
        consolePanel.add(buttonPanel, BorderLayout.NORTH);
        consolePanel.add(splitPane, BorderLayout.CENTER);

        return consolePanel;
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    private void setContextClassLoader(ClassLoader loader) {
        Thread.currentThread().setContextClassLoader(loader);
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

            final ClassLoader oldClassLoader = getContextClassLoader();
            try {
                setContextClassLoader(ScriptConsoleToolView.class.getClassLoader());
                // evaluate JavaScript code from String
                final Object o = scriptEngine.eval(text);
                out.append("Result: " + o + "\n");
            } catch (ScriptException e) {
                out.setLength(0);
                out.append("Error: " + e.getMessage() + "\n");
            } finally {
                setContextClassLoader(oldClassLoader);
            }
            outputTextArea.setText(out.toString());
            out.setLength(0);
        }
    }

    private class TerminateAction extends AbstractAction {

        public TerminateAction() {
            putValue(AbstractAction.NAME, "Terminate");
            putValue(AbstractAction.ACTION_COMMAND_KEY, "scriptConsole.terminate");
            final ImageIcon icon = UIUtils.loadImageIcon("/org/esa/beam/scripting/visat/icons/process-stop-16.png", ScriptConsoleToolView.class);
            putValue(AbstractAction.SMALL_ICON, icon);
            putValue(AbstractAction.LARGE_ICON_KEY, icon);
        }

        public void actionPerformed(ActionEvent e) {
            // todo
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
 
