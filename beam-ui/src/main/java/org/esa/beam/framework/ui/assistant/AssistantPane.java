package org.esa.beam.framework.ui.assistant;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayDeque;
import java.util.Deque;

public class AssistantPane implements AssistantPageContext {

    private AssistantPage currentPage;
    private Deque<AssistantPage> pageStack;
    private JDialog dialog;
    private Action prevAction;
    private Action nextAction;
    private Action finishAction;
    private JLabel titleLabel;
    private JPanel pagePanel;
    private HelpAction helpAction;
    private AssistantPane.CancelAction cancelAction;

    public AssistantPane(Window parent, String title) {

        pageStack = new ArrayDeque<AssistantPage>();

        prevAction = new PrevAction();
        nextAction = new NextAction();
        finishAction = new FinishAction();
        cancelAction = new CancelAction();
        helpAction = new HelpAction();

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttonPanel.add(new JButton(prevAction));
        final JButton nextButton = new JButton(nextAction);
        buttonPanel.add(nextButton);
        buttonPanel.add(new JButton(finishAction));
        buttonPanel.add(new JButton(cancelAction));
        buttonPanel.add(new JButton(helpAction));

        titleLabel = new JLabel();
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14.0f));
        titleLabel.setHorizontalAlignment(JLabel.RIGHT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        titleLabel.setForeground(Color.WHITE);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(titlePanel.getBackground().darker());
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        pagePanel = new JPanel(new BorderLayout());
        pagePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        dialog = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().add(titlePanel, BorderLayout.NORTH);
        dialog.getContentPane().add(pagePanel, BorderLayout.CENTER);
        dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.getRootPane().setDefaultButton(nextButton);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelAction.cancel();
            }
        });
    }

    @Override
    public Window getWindow() {
        return dialog;
    }

    @Override
    public AssistantPage getCurrentPage() {
        return currentPage;
    }

    @Override
    public void setCurrentPage(AssistantPage startPage) {
        currentPage = startPage;
        pagePanel.removeAll();
        titleLabel.setText(currentPage.getPageTitle());
        pagePanel.add(currentPage.getPageComponent(this), BorderLayout.CENTER);
        updateState();
        dialog.invalidate();
        dialog.validate();
        dialog.repaint();
    }

    @Override
    public void updateState() {
        final AssistantPage page = getCurrentPage();
        final boolean pageValid = page.validatePage();
        prevAction.setEnabled(!pageStack.isEmpty());
        nextAction.setEnabled(pageValid && page.hasNextPage());
        finishAction.setEnabled(pageValid && page.canFinish());
        helpAction.setEnabled(pageValid && page.canHelp());
    }

    @Override
    public void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(dialog, message, getCurrentPage().getPageTitle(), JOptionPane.ERROR_MESSAGE);
    }

    public void show(AssistantPage startPage) {
        setCurrentPage(startPage);
        dialog.setSize(new Dimension(480, 320));
        dialog.setVisible(true);
    }

    private void close() {
        dialog.dispose();
        pageStack.clear();
        currentPage = null;
    }

    private class PrevAction extends AbstractAction {

        private PrevAction() {
            super("< Previous");
            putValue(ACTION_COMMAND_KEY, "Previous");
            putValue(MNEMONIC_KEY, (int) 'P');
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setCurrentPage(pageStack.pop());
        }
    }

    private class NextAction extends AbstractAction {

        private NextAction() {
            super("Next >");
            putValue(ACTION_COMMAND_KEY, "Next");
            putValue(MNEMONIC_KEY, (int) 'N');
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            AssistantPage nextPage = currentPage.getNextPage(AssistantPane.this);
            if (nextPage != null) {
                pageStack.push(currentPage);
                setCurrentPage(nextPage);
            }
        }

    }

    private class FinishAction extends AbstractAction {

        private FinishAction() {
            super("Finish");
            putValue(ACTION_COMMAND_KEY, "Finish");
            putValue(MNEMONIC_KEY, (int) 'F');

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (getCurrentPage().performFinish(AssistantPane.this)) {
                close();
            }
        }

    }

    private class CancelAction extends AbstractAction {

        private CancelAction() {
            super("Cancel");
            putValue(ACTION_COMMAND_KEY, "Cancel");
            putValue(MNEMONIC_KEY, (int) 'C');
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            cancel();
        }


        public void cancel() {
            getCurrentPage().performCancel();
            close();
        }

    }

    private class HelpAction extends AbstractAction {

        private HelpAction() {
            super("Help");
            putValue(ACTION_COMMAND_KEY, "Help");
            putValue(MNEMONIC_KEY, (int) 'H');
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            getCurrentPage().performHelp();
        }
    }
}
