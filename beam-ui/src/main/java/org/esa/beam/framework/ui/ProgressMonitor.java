/*
 * $Id: ProgressMonitor.java,v 1.1 2006/10/10 14:47:39 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.esa.beam.framework.datamodel.ProgressListener;

/**
 * The <code>ProgressMonitor</code> is an implementation of the <code>ProgressListener</code> interface which pops-up a
 * progress monitor dialog when the listener's <code>fireProcessStarted</code> method is called. Subsequential calls of
 * the <code>fireProcessInProgress</code> call-back then update the progress bar contained in the dialog. The dialog
 * also provides a cancel button so that the use can terminate a process at any time. The dialog is then automatically
 * closed. The dialog is also closed if the <code>fireProcessEnded</code> method of the listener is called.
 */
public class ProgressMonitor implements ProgressListener {

    private final Component _parent;
    private final String _title;
    private JDialog _dialog;
    private JLabel _messageLabel;
    private JProgressBar _progressBar;
    private JButton _cancelButton;
    private boolean _cancelRequested;
    private boolean _canceled;
    private TerminationHandler _terminationHandler;

    /**
     * Constructs a new progress monitor for the given parent frame and the given dialog title.
     *
     * @param parent the parent frame
     * @param title  the dialog title
     */
    public ProgressMonitor(JFrame parent, String title) {
        this(parent, title, null);
    }

    /**
     * Constructs a new progress monitor for the given parent dialog and the given dialog title.
     *
     * @param parent the parent dialog
     * @param title  the dialog title
     */
    public ProgressMonitor(JDialog parent, String title) {
        this(parent, title, null);
    }

    /**
     * Constructs a new progress monitor for the given parent dialog and the given dialog title.
     *
     * @param parent the parent dialog
     * @param title  the dialog title
     */
    private ProgressMonitor(Component parent, String title, String cancelMessage) {
        _parent = parent;
        _title = title;
        _terminationHandler = createTerminationHandler(
                cancelMessage == null ? "Really cancel '" + title + "'?" : cancelMessage);
    }

    public TerminationHandler getTerminationHandler() {
        return _terminationHandler;
    }

    public void setTerminationHandler(TerminationHandler terminationHandler) {
        _terminationHandler = terminationHandler;
    }

    /**
     * Initialise and show the UI.
     *
     * @param processDescription text which is displayed in top of the progress bar, as description for the runnig
     *                           process.
     * @param minProgressValue   the start progress value
     * @param maxProgressValue   the stop progress value
     *
     * @return <code>true</code>, if the progress monitoring was initialised and shown.
     */
    public boolean processStarted(String processDescription, int minProgressValue, int maxProgressValue) {

        if (!isUICreated()) {
            createUI();
        }

        setProcessDescription(processDescription);

        _canceled = false;
        _progressBar.setMinimum(minProgressValue);
        _progressBar.setMaximum(maxProgressValue);
        _progressBar.setStringPainted(true);

//_dialog.setModal(true);
        _dialog.pack();
        UIUtils.centerComponent(_dialog, _parent);
        show();

        return true;
    }

    /**
     * Sets the process description which is displayed at the progress monitor UI.
     *
     * @param processDescription the new process description displayed at the progress monitor UI.
     */
    public void setProcessDescription(String processDescription) {
        _messageLabel.setText(processDescription);
    }

    /**
     * Called while a process in in progress. A listener should return <code>true</code> if the process can be
     * continued, <code>false</code> if it should be terminated.
     *
     * @param currentProgressValue the current progress value
     *
     * @return <code>true</code> if the process should be continued, <code>false</code> otherwise
     */
    public boolean processInProgress(int currentProgressValue) {
        if (!isCanceled()) {
            if (isCancelRequested()) {
                if (getTerminationHandler() != null) {
                    if (getTerminationHandler().confirmTermination()) {
                        setCanceled(true);
                    } else {
                        _dialog.setCursor(Cursor.getDefaultCursor());
                        setCurrentProgressValue(currentProgressValue);
                    }
                    setCancelRequested(false);
                    _cancelButton.setEnabled(true);
                } else {
                    setCanceled(true);
                }
            } else {
                setCurrentProgressValue(currentProgressValue);
            }
        }
        return !isCanceled();
    }

    private void setCurrentProgressValue(int currentProgressValue) {
        _progressBar.setValue(currentProgressValue);
    }

    /**
     * Stops the progress listenig and sets the canceled state <code>true</code>
     *
     * @param success
     */
    public void processEnded(boolean success) {
        hide();
    }

    public boolean isCancelRequested() {
        return _cancelRequested;
    }

    public void setCancelRequested(boolean cancelRequested) {
        _cancelRequested = cancelRequested;
    }

    /**
     * Gets the current canceled state
     *
     * @return <code>true</code> if canceld, otherwise <code>false</code>.
     */
    public boolean isCanceled() {
        return _canceled;
    }

    protected void setCanceled(boolean canceled) {
        boolean oldValue = _canceled;
        if (oldValue != canceled) {
            _canceled = canceled;
            if (_canceled) {
                hide();
            }
        }
    }

    private void show() {
        if (isUICreated()) {
            _dialog.setVisible(true);
        }
    }


    private void hide() {
        if (isUICreated()) {
            _dialog.setVisible(false);
        }
    }

    public void dispose() {
        if (isUICreated()) {
            _dialog.dispose();
        }
        init();
    }

    private boolean isUICreated() {
        return _dialog != null;
    }

    private void createUI() {

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(4, 4));
        contentPane.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

        _messageLabel = new JLabel();
        _cancelButton = new JButton(" Cancel ");
        _cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                _dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                _cancelButton.setEnabled(false);
                setCancelRequested(true);
            }
        });

        _progressBar = new JProgressBar();
        _progressBar.setPreferredSize(new Dimension(320, 24));
        _progressBar.setStringPainted(true);

        contentPane.add(BorderLayout.NORTH, _messageLabel);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(4, 4));
        panel.add(BorderLayout.NORTH, _progressBar);
        panel.add(BorderLayout.EAST, _cancelButton);

        contentPane.add(BorderLayout.SOUTH, panel);

        if (_parent instanceof JFrame) {
            _dialog = new JDialog((JFrame) _parent, _title, false); // @todo 1 nf/nf - check: set to modal?
        } else if (_parent instanceof JDialog) {
            _dialog = new JDialog((JDialog) _parent, _title, false); // @todo 1 nf/nf - check: set to modal?
        } else {
            _dialog = new JDialog((JFrame) null, _title, false);
        }
        _dialog.setContentPane(contentPane);
    }

    private void init() {
        _dialog = null;
        _messageLabel = null;
        _progressBar = null;
        _cancelButton = null;
        _canceled = false;
    }

    public TerminationHandler createTerminationHandler(String message) {
        return new DefaultTerminationHandler(message);
    }


    public interface TerminationHandler {

        boolean confirmTermination();
    }


    private class DefaultTerminationHandler implements TerminationHandler {

        private String _cancelMessage;

        /**
         * Constructs
         *
         * @param cancelMessage the message which displayed after the user press the cancel button.
         */
        public DefaultTerminationHandler(String cancelMessage) {
            _cancelMessage = cancelMessage;
        }

        /**
         * Sets the message which displayed in a dialog box after the user press the cancel button. If the user confirm
         * the upcoming dialog the prozess was realy canceled. If <code>message</code> was <code>null</code> no dialog
         * comes up after the cancel button was pressed, the prozess was canceled directly.
         */
        public boolean confirmTermination() {
            final int type = JOptionPane.YES_NO_OPTION;
            final int option = JOptionPane.showConfirmDialog(_dialog, _cancelMessage, "Cancel Process", type); /*I18N*/
            return option == JOptionPane.YES_OPTION;
        }
    }
}
