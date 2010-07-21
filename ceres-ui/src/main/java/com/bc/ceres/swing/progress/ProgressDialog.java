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

package com.bc.ceres.swing.progress;

import com.bc.ceres.swing.SwingHelper;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * A utility class very similar to {@link javax.swing.ProgressMonitor} but with the following extensions:
 * <ul>
 * <li>It is cancelable.</li>
 * <li>It has a {@link Dialog.ModalityType dialog modality type}.</li>
 * </ul>
 */
public class ProgressDialog {

    private Component parentComponent;
    private String title;
    private JComponent messageComponent;
    private String note;
    private int minimum;
    private int maximum;
    private boolean cancelable;
    private Dialog.ModalityType modalityType;

    private JDialog dialog;
    private JLabel noteLabel;
    private JProgressBar progressBar;
    public JButton cancelButton;

    private boolean canceled = false;

    /**
     * Constructs a graphic object that shows progress, typically by filling
     * in a rectangular bar as the process nears completion.
     *
     * @param parentComponent the parent component for the dialog box
     */
    public ProgressDialog(Component parentComponent) {
        this.parentComponent = parentComponent;
        this.minimum = 0;
        this.maximum = 100;
        this.title = UIManager.getString("ProgressMonitor.progressText");
        this.note = "";
        this.cancelable = true;
        this.modalityType = Dialog.ModalityType.MODELESS;
    }

    public Component getParentComponent() {
        return parentComponent;
    }

    public JComponent getMessageComponent() {
        return messageComponent;
    }

    public void setMessageComponent(JComponent messageComponent) {
        this.messageComponent = messageComponent;
    }

    public Dialog.ModalityType getModalityType() {
        return modalityType;
    }

    public void setModalityType(Dialog.ModalityType modalityType) {
        this.modalityType = modalityType;
    }

    public boolean isCancelable() {
        return cancelable;
    }

    public void setCancelable(boolean cancelable) {
        if (cancelButton != null) {
            cancelButton.setEnabled(cancelable && !canceled);
        }
        this.cancelable = cancelable;
    }

    /**
     * Indicate the progress of the operation being monitored.
     * If the specified value is >= the maximum, the progress
     * monitor is closed.
     *
     * @param progress an int specifying the current value, between the
     *                 maximum and minimum specified for this component
     *
     * @see #setMinimum
     * @see #setMaximum
     * @see #close
     */
    public void setProgress(int progress) {
        if (progress >= maximum) {
            close();
            return;
        }
        if (progressBar != null) {
            if (progressBar.isIndeterminate()) {
                progressBar.setIndeterminate(false);
            }
            progressBar.setValue(progress);
        }
    }


    /**
     * Sets the canceled state if this dialog is cancelable.
     * Note that this method will not automatically close the dialog.
     */
    public void cancel() {
        if (isCancelable()) {
            canceled = true;
            setCancelable(false);
        }
    }

    /**
     * Indicate that the operation is complete.  This happens automatically
     * when the value set by {@link #setProgress} is >= {@link #getMaximum maximum} , but it may be called
     * earlier if the operation ends early.
     */
    public void close() {
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
            dialog = null;
            progressBar = null;
            noteLabel = null;
            cancelButton = null;
        }
    }

    /**
     * Returns the minimum value -- the lower end of the progress value.
     *
     * @return an int representing the minimum value
     *
     * @see #setMinimum
     */
    public int getMinimum() {
        return minimum;
    }

    /**
     * Specifies the minimum value.
     *
     * @param minimum an int specifying the minimum value
     *
     * @see #getMinimum
     */
    public void setMinimum(int minimum) {
        if (progressBar != null) {
            progressBar.setMinimum(minimum);
        }
        this.minimum = minimum;
    }

    /**
     * Returns the maximum value -- the higher end of the progress value.
     *
     * @return an int representing the maximum value
     *
     * @see #setMaximum
     */
    public int getMaximum() {
        return maximum;
    }

    /**
     * Specifies the maximum value.
     *
     * @param maximum an int specifying the maximum value
     *
     * @see #getMaximum
     */
    public void setMaximum(int maximum) {
        if (progressBar != null) {
            progressBar.setMaximum(maximum);
        }
        this.maximum = maximum;
    }

    /**
     * @return {@code true} if the user hit the 'Cancel' button in the progress dialog.
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Specifies the message that is displayed.
     *
     * @param title a String specifying the message to display
     *
     * @see #getTitle
     */
    public void setTitle(String title) {
        if (dialog != null) {
            dialog.setTitle(title);
        }
        this.title = title;
    }

    /**
     * Specifies the message that is displayed.
     *
     * @return a String specifying the message to display
     *
     * @see #setTitle
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Specifies the additional note that is displayed along with the
     * progress message. Used, for example, to show which file the
     * is currently being copied during a multiple-file copy.
     *
     * @param note a String specifying the note to display
     *
     * @see #getNote
     */
    public void setNote(String note) {
        if (noteLabel != null) {
            noteLabel.setText(note);
        }
        this.note = note;
    }

    /**
     * Specifies the additional note that is displayed along with the
     * progress message.
     *
     * @return a String specifying the note to display
     *
     * @see #setNote
     */
    public String getNote() {
        return this.note;
    }

    /////////////////////////////////////////////////////////////////////////////////////

    public void show() {
        if (dialog != null) {
            dialog.setVisible(true);
            return;
        }

        Window parentWindow = null;
        if (parentComponent != null) {
            parentWindow = SwingUtilities.getWindowAncestor(parentComponent);
        }

        progressBar = new JProgressBar();
        progressBar.setMinimum(minimum);
        progressBar.setMaximum(maximum);
        progressBar.setIndeterminate(true);

        Dimension preferredSize = progressBar.getPreferredSize();
        preferredSize.width = Math.max(300, preferredSize.width);
        progressBar.setPreferredSize(preferredSize);

        noteLabel = new JLabel(note);

        JPanel messagePanel = new JPanel(new BorderLayout(4, 4));
        if (messageComponent != null) {
            messagePanel.add(messageComponent, BorderLayout.CENTER);
        }
        messagePanel.add(noteLabel, BorderLayout.SOUTH);


        JPanel progressPanel = new JPanel(new BorderLayout(4, 4));
        progressPanel.add(messagePanel, BorderLayout.CENTER);
        progressPanel.add(progressBar, BorderLayout.SOUTH);

        JPanel contentPanel = new JPanel(new BorderLayout(4, 4));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        contentPanel.add(progressPanel, BorderLayout.CENTER);

        if (cancelable) {
            this.cancelButton = new JButton(UIManager.getString("OptionPane.cancelButtonText"));
            this.cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cancel();
                }
            });
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(cancelButton);
            contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        }

        dialog = new JDialog(parentWindow, title, modalityType);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });

        dialog.setContentPane(contentPanel);

        dialog.pack();
        SwingHelper.centerComponent(dialog, parentWindow);

        dialog.setVisible(true);
    }


}
