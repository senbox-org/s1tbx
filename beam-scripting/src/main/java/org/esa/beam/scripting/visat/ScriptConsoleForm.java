package org.esa.beam.scripting.visat;

import com.bc.ceres.core.Assert;
import org.esa.beam.scripting.visat.actions.HelpAction;
import org.esa.beam.scripting.visat.actions.NewAction;
import org.esa.beam.scripting.visat.actions.OpenAction;
import org.esa.beam.scripting.visat.actions.RunAction;
import org.esa.beam.scripting.visat.actions.SaveAction;
import org.esa.beam.scripting.visat.actions.SaveAsAction;
import org.esa.beam.scripting.visat.actions.StopAction;
import org.esa.beam.visat.VisatApp;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.concurrent.CancellationException;

public class ScriptConsoleForm {

    // View
    private final Window window;
    private JTextArea inputTextArea;
    private JTextArea outputTextArea;
    private Action runAction;
    private Action stopAction;
    private JPanel contentPanel;

    private SwingWorker<Object, Object> worker;
    private ScriptManager scriptManager;

    public ScriptConsoleForm(Window window) {
        this.window = window;

        inputTextArea = new JTextArea(); // todo - replace by JIDE code editor component (nf)
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

        runAction = new RunAction(this);
        stopAction = new StopAction(this);

        final JToolBar toolBar = new JToolBar("Script Console");
        toolBar.setFloatable(false);
        toolBar.add(getToolButton(new NewAction(this)));
        toolBar.add(getToolButton(new OpenAction(this)));
        toolBar.add(getToolButton(new SaveAction(this)));
        toolBar.add(getToolButton(new SaveAsAction(this)));
        toolBar.addSeparator();
        toolBar.add(getToolButton(runAction));
        toolBar.add(getToolButton(stopAction));
        toolBar.addSeparator();
        toolBar.add(getToolButton(new HelpAction(this)));

        runAction.setEnabled(false);
        stopAction.setEnabled(false);
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

        ClassLoader cl = getClass().getClassLoader();
        ClassLoader cl0 = Thread.currentThread().getContextClassLoader() ;
        Thread.currentThread().setContextClassLoader(cl);
        scriptManager = new ScriptManager(new PrintWriter(new ScriptOutput(), true),
                                          cl,
                                          new ScriptEngineManager(cl));
        //Thread.currentThread().setContextClassLoader(cl0);
    }

    public Window getWindow() {
        return window;
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public void runScript() {
        
        if (scriptManager.getScriptEngine() == null) {
            showErrorMessage("No script language selected.");
            return;
        }

        final String text = inputTextArea.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        Assert.state(worker == null, "worker == null");

        outputTextArea.setText("");

        enableRun(false);

        worker = new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() throws Exception {
                return scriptManager.evalScriptCode(text);
            }

            @Override
            protected void done() {

                worker = null;
                enableRun(true);
                PrintWriter output = scriptManager.getOutput();
                try {
                    Object result = get();
                    if (result != null) {
                        output.println(result.toString());
                    }
                } catch (InterruptedException e) {
                    output.println("Script evaluation interrupted.");
                } catch (CancellationException e) {
                    output.println("Script evaluation canceled.");
                } catch (Throwable t) {
                    t.printStackTrace(new PrintWriter(output));
                    output.println();
                }
            }
        };

        worker.execute();
    }

    public void stopScript() {
        if (worker != null) {
            worker.cancel(true);
            worker = null;
        }
    }

    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(window,
                                      message, "Script Console - Error",
                                      JOptionPane.ERROR_MESSAGE);
    }

    public void newScript(ScriptEngine scriptEngine) {
        inputTextArea.setText("");
        outputTextArea.setText("");

        scriptManager.setScriptEngine(scriptEngine);

        inputTextArea.setEnabled(true);
        enableRun(true);
    }

    private void enableRun(boolean b) {
        runAction.setEnabled(b);
        stopAction.setEnabled(!b);
    }

    private JButton getToolButton(Action newAction) {
        final JButton button = new JButton(newAction);
        button.setText(null);
        return button;
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

        private void print0(final String s) {
            if (SwingUtilities.isEventDispatchThread()) {
                print1(s);
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        print1(s);
                    }
                });
            }
        }

        private void print1(String text) {
            try {
                final int offset = outputTextArea.getDocument().getEndPosition().getOffset();
                outputTextArea.getDocument().insertString(offset, text, null);
            } catch (BadLocationException e) {
                // ignore
            }
        }


    }
}