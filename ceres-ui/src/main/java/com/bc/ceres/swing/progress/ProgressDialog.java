package com.bc.ceres.swing.progress;

import com.bc.ceres.core.Assert;
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

    private long startTime;
    private int millisToDecideToPopup = 500;
    private int millisToPopup = 2000;
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
        this.startTime = System.currentTimeMillis();
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
            progressBar.setValue(progress);
            return;
        }

        maybeShowDialog(progress);
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
     * Returns true if the user hit the 'Cancel' button in the progress dialog.
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Specifies the amount of time to wait before deciding whether or
     * not to popup a progress monitor.
     *
     * @param millisToDecideToPopup an int specifying the time to wait,
     *                              in milliseconds
     *
     * @see #getMillisToDecideToPopup
     */
    public void setMillisToDecideToPopup(int millisToDecideToPopup) {
        this.millisToDecideToPopup = millisToDecideToPopup;
    }

    /**
     * Returns the amount of time this object waits before deciding whether
     * or not to popup a progress monitor.
     *
     * @see #setMillisToDecideToPopup
     */
    public int getMillisToDecideToPopup() {
        return millisToDecideToPopup;
    }

    /**
     * Specifies the amount of time it will take for the popup to appear.
     * (If the predicted time remaining is less than this time, the popup
     * won't be displayed.)
     *
     * @param millisToPopup an int specifying the time in milliseconds
     *
     * @see #getMillisToPopup
     */
    public void setMillisToPopup(int millisToPopup) {
        this.millisToPopup = millisToPopup;
    }

    /**
     * Returns the amount of time it will take for the popup to appear.
     *
     * @see #setMillisToPopup
     */
    public int getMillisToPopup() {
        return millisToPopup;
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
    private void maybeShowDialog(int progress) {
        long T = System.currentTimeMillis();
        long dT = (int) (T - startTime);
        if (dT >= millisToDecideToPopup) {
            int predictedCompletionTime;
            if (progress > minimum) {
                predictedCompletionTime = (int) (dT *
                                                 (maximum - minimum) /
                                                                     (progress - minimum));
            } else {
                predictedCompletionTime = millisToPopup;
            }
            if (predictedCompletionTime >= millisToPopup) {
                showDialog(progress);
            }
        }
    }

    private void showDialog(int progress) {
        Assert.state(progressBar == null, "progressBar == null");

        Window parentWindow = null;
        if (parentComponent != null) {
            parentWindow = SwingUtilities.getWindowAncestor(parentComponent);
        }

        progressBar = new JProgressBar();
        progressBar.setMinimum(minimum);
        progressBar.setMaximum(maximum);
        progressBar.setValue(progress);
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
