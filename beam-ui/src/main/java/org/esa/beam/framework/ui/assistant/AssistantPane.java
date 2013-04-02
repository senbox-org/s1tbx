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

package org.esa.beam.framework.ui.assistant;

import org.esa.beam.framework.ui.UIUtils;

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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Shows a sequence of {@link AssistantPage assitant pages} with an dialog.
 */
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

    /**
     * Creates a new {@code AssistantPane}.
     *
     * @param parent The parent window.
     * @param title  The title of the dialog.
     */
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
    public void setCurrentPage(AssistantPage currentPage) {
        this.currentPage = currentPage;
        pagePanel.removeAll();
        titleLabel.setText(currentPage.getPageTitle());
        pagePanel.add(currentPage.getPageComponent(), BorderLayout.CENTER);
        updateState();
        dialog.invalidate();
        dialog.validate();
        dialog.repaint();
    }

    @Override
    public void updateState() {
        final AssistantPage page = getCurrentPage();
        if (page != null) {
            final boolean pageValid = page.validatePage();
            prevAction.setEnabled(!pageStack.isEmpty());
            nextAction.setEnabled(pageValid && page.hasNextPage());
            finishAction.setEnabled(pageValid && page.canFinish());
            helpAction.setEnabled(page.canHelp());
        }
    }

    @Override
    public void showErrorDialog(String message) {
        final String dialogTitle;
        final AssistantPage currentPage= getCurrentPage();
        if (currentPage != null) {
            dialogTitle = currentPage.getPageTitle();
        } else {
            dialogTitle = "Unexpected Error";
        }
        JOptionPane.showMessageDialog(dialog, message, dialogTitle, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Displays the dialog if this {@code AssistantPane} with
     * the given {@link AssistantPage page} as first page.
     *
     * @param firstPage The first page which is displayed in the dialog.
     */
    public void show(AssistantPage firstPage) {
        show(firstPage, null);
    }

    /**
     * Displays the dialog if this {@code AssistantPane} with
     * the given {@link AssistantPage page} as first page.
     *
     * @param firstPage The first page which is displayed in the dialog.
     * @param bounds    The screen bounds of the window, may be {@code null}.
     */
    public void show(AssistantPage firstPage, Rectangle bounds) {
        initPage(firstPage);
        setCurrentPage(firstPage);
        if (bounds == null) {
            dialog.setSize(480, 320);
            UIUtils.centerComponent(dialog, dialog.getParent());
        } else {
            dialog.setBounds(bounds);
        }
        dialog.setVisible(true);
    }

    private void initPage(AssistantPage currentPage) {
        currentPage.setContext(this);
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
            AssistantPage nextPage = currentPage.getNextPage();
            if (nextPage != null) {
                pageStack.push(currentPage);
                initPage(nextPage);
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
            if (getCurrentPage().performFinish()) {
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
