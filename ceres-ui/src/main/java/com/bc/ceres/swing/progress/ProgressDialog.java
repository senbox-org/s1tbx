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
import com.jidesoft.swing.JideButton;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;

/**
 * A utility class very similar to {@link javax.swing.ProgressMonitor} but with the following extensions:
 * <ul>
 * <li>It is cancelable.</li>
 * <li>It has a {@link Dialog.ModalityType dialog modality type}.</li>
 * </ul>
 */
public class ProgressDialog {

    public static final Color SELECTED_BORDER_COLOR = new Color(8, 36, 107);
    private static final Color SELECTED_BACKGROUND_COLOR = new Color(130, 146, 185);
    private static final Color ROLLOVER_BACKGROUND_COLOR = new Color(181, 190, 214);
    private static final int BUTTON_MIN_SIZE = 16;

    private Component parentComponent;
    private String title;
    private JComponent messageComponent;
    private JComponent extensibleMessageComponent;
    int notExtendedDialogHeight;
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
    private boolean placeExtensibleMessageAboveProgressBar;
    private boolean placeMessageAboveProgressBar;

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
        setMessageComponent(messageComponent, true);
    }

    public void setMessageComponent(JComponent messageComponent, boolean placeMessageAboveProgressBar) {
        this.messageComponent = messageComponent;
        this.placeMessageAboveProgressBar = placeMessageAboveProgressBar;
    }

    public JComponent getExtensibleMessageComponent() {
        return extensibleMessageComponent;
    }

    public void setExtensibleMessageComponent(JComponent extensibleMessageComponent,
                                              boolean placeExtensibleMessageAboveProgressBar) {
        this.extensibleMessageComponent = extensibleMessageComponent;
        this.placeExtensibleMessageAboveProgressBar = placeExtensibleMessageAboveProgressBar;
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
     * If the specified value is &gt;= the maximum, the progress
     * monitor is closed.
     *
     * @param progress an int specifying the current value, between the
     *                 maximum and minimum specified for this component
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
     * when the value set by {@link #setProgress} is &gt;= {@link #getMaximum maximum} , but it may be called
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
     * @see #setMinimum
     */
    public int getMinimum() {
        return minimum;
    }

    /**
     * Specifies the minimum value.
     *
     * @param minimum an int specifying the minimum value
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
     * @see #setMaximum
     */
    public int getMaximum() {
        return maximum;
    }

    /**
     * Specifies the maximum value.
     *
     * @param maximum an int specifying the maximum value
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
        progressPanel.add(progressBar, BorderLayout.SOUTH);

        JPanel extensibleMessagePanel = new JPanel(new BorderLayout(4, 4));
        if (extensibleMessageComponent != null) {
            extensibleMessageComponent.setVisible(false);
            final ImageIcon[] icons = new ImageIcon[]{
                    new ImageIcon(getClass().getResource("icons/PanelUp12.png")),
                    new ImageIcon(getClass().getResource("icons/PanelDown12.png"))};
            final ImageIcon[] rolloverIcons = new ImageIcon[]{
                    createRolloverIcon(icons[0]),
                    createRolloverIcon(icons[1]),
            };
            final JLabel extendLabel = new JLabel();
            final String moreText = "More";
            final String lessText = "Less";
            final JideButton extendButton = new JideButton(icons[1]);
            configure(extendButton);
            extendLabel.setText(moreText);
            extendButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (extensibleMessageComponent.isVisible()) {
                        extensibleMessageComponent.setVisible(false);
                        extendButton.setIcon(icons[1]);
                        extendButton.setRolloverIcon(rolloverIcons[1]);
                        extendLabel.setText(moreText);
                        Dimension size = dialog.getSize();
                        dialog.setSize(size.width, notExtendedDialogHeight);
                        dialog.pack();
                    } else {
                        extensibleMessageComponent.setVisible(true);
                        extendButton.setIcon(icons[0]);
                        extendButton.setRolloverIcon(rolloverIcons[0]);
                        extendLabel.setText(lessText);
                        Dimension size = dialog.getSize();
                        dialog.setSize(size.width,
                                       Math.max(notExtendedDialogHeight + 200, size.height));
                    }
                }
            });
            JPanel extendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            extendPanel.add(extendButton);
            extendPanel.add(extendLabel);
            extensibleMessagePanel.add(extendPanel, BorderLayout.NORTH);
            extensibleMessagePanel.add(extensibleMessageComponent, BorderLayout.CENTER);
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (cancelable) {
            this.cancelButton = new JButton(UIManager.getString("OptionPane.cancelButtonText"));
            this.cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cancel();
                }
            });
            buttonPanel.add(cancelButton);
        }

        GridBagLayout gbl = new GridBagLayout();
        JPanel contentPanel = new JPanel(gbl);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(4, 4, 4, 4);
        if (messageComponent != null) {
            gbc.gridy = 3;
            if (placeMessageAboveProgressBar) {
                gbc.gridy = 0;
            }
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbl.addLayoutComponent(messagePanel, gbc);
            contentPanel.add(messagePanel);
        }
        if (extensibleMessageComponent != null) {
            gbc.gridy = 4;
            if (placeExtensibleMessageAboveProgressBar) {
                gbc.gridy = 1;
            }
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbl.addLayoutComponent(extensibleMessagePanel, gbc);
            contentPanel.add(extensibleMessagePanel);
        }
        gbc.gridy = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbl.addLayoutComponent(progressBar, gbc);
        contentPanel.add(progressBar);
        if (cancelable) {
            gbc.gridy = 5;
            gbl.addLayoutComponent(buttonPanel, gbc);
            contentPanel.add(buttonPanel);
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

        notExtendedDialogHeight = dialog.getHeight();

        dialog.setVisible(true);
    }

    private void configure(AbstractButton button) {
        Icon icon = button.getIcon();
        final int space = 3;
        Dimension prefSize = new Dimension(Math.max(icon.getIconWidth(), BUTTON_MIN_SIZE) + space,
                Math.max(icon.getIconHeight(), BUTTON_MIN_SIZE) + space);
        Dimension minSize = new Dimension(Math.max(icon.getIconWidth(), BUTTON_MIN_SIZE),
                Math.max(icon.getIconHeight(), BUTTON_MIN_SIZE));
        Dimension maxSize = new Dimension(Math.max(icon.getIconWidth(), BUTTON_MIN_SIZE) + space,
                Math.max(icon.getIconHeight(), BUTTON_MIN_SIZE) + space);
        button.setPreferredSize(prefSize);
        button.setMaximumSize(maxSize);
        button.setMinimumSize(minSize);

    }

    public static ImageIcon createRolloverIcon(ImageIcon imageIcon) {
        return new ImageIcon(createRolloverImage(imageIcon.getImage()));
    }

    private static Image createRolloverImage(Image image) {
        return Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(image.getSource(),
                new BrightBlueFilter()));
    }

    private static class BrightBlueFilter extends RGBImageFilter {

        public BrightBlueFilter() {
            canFilterIndexColorModel = true;
        }

        @Override
        public int filterRGB(int x, int y, int rgb) {
            int a = (rgb & 0xff000000) >> 24;
            int r = (rgb & 0x00ff0000) >> 16;
            int g = (rgb & 0x0000ff00) >> 8;
            int b = rgb & 0x000000ff;
            int i = (r + g + b) / 3;
            r = g = i;
            b = 255;
            return a << 24 | r << 16 | g << 8 | b;
        }
    }


}
