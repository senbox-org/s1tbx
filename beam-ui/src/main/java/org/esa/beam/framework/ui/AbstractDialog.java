/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.framework.ui;

import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.util.Debug;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.logging.BeamLogManager;

import javax.help.BadIDException;
import javax.help.DefaultHelpBroker;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The <code>AbstractDialog</code> is the base class for {@link ModalDialog} and {@link ModelessDialog},
 * two helper classes used to quickly construct modal and modeless dialogs. The dialogs created with this
 * class have a unique border and font and a standard button row for the typical buttons like "OK", "Cancel" etc.
 * <p/>
 * <p>Instances of a modal dialog are created with a parent component, a title, the actual dialog content component, and
 * a bit-combination of the standard buttons to be used.
 * <p/>
 * <p>A limited way of input validation is provided by the  <code>verifyUserInput</code> method which can be overridden
 * in order to return <code>false</code> if a user input is invalid. In this case the {@link #onOK()},
 * {@link #onYes()} and {@link #onNo()} methods are NOT called.
 *
 * @author Norman Fomferra
 * @since BEAM 4.2
 */
public abstract class AbstractDialog {

    public static final int ID_OK = 0x0001;
    public static final int ID_YES = 0x0002;
    public static final int ID_NO = 0x0004;
    public static final int ID_APPLY = 0x0008;
    public static final int ID_CLOSE = 0x0010;
    public static final int ID_CANCEL = 0x0020;
    public static final int ID_RESET = 0x0040;
    public static final int ID_HELP = 0x0080;
    public static final int ID_OTHER = 0xAAAAAAAA;

    private final JDialog dialog;
    private final Window parent;
    private final int buttonMask;

    private int buttonId;
    private Component content;
    private boolean shown;
    private Map<Integer, AbstractButton> buttonMap;

    // Java help support
    private String helpId;
    private HelpBroker helpBroker;

    protected AbstractDialog(JDialog dialog, int buttonMask, Object[] otherButtons, String helpID) {
        this.parent = (Window) dialog.getParent();
        this.dialog = dialog;
        this.buttonMask = buttonMask;
        this.buttonMap = new HashMap<Integer, AbstractButton>(5);
        setButtonID(0);
        initUI(otherButtons);
        setHelpID(helpID);
    }

    /**
     * Gets the underlying Swing dialog passed to the constructor.
     *
     * @return the underlying Swing dialog.
     */
    public JDialog getJDialog() {
        return dialog;
    }

    /**
     * Gets the owner of the dialog.
     *
     * @return The owner of the dialog.
     */
    public Window getParent() {
        return parent;
    }

    /**
     * @return The dialog's title.
     */
    public String getTitle() {
        return dialog.getTitle();
    }

    /**
     * @param title The dialog's title.
     */
    public void setTitle(String title) {
        dialog.setTitle(title);
    }

    /**
     * Gets the button mask passed to the constructor.
     *
     * @return The button mask.
     */
    public int getButtonMask() {
        return buttonMask;
    }

    /**
     * Gets the identifier for the most recently pressed button.
     *
     * @return The identifier for the most recently pressed button.
     */
    public int getButtonID() {
        return buttonId;
    }

    /**
     * Sets the identifier for the most recently pressed button.
     *
     * @param buttonID The identifier for the most recently pressed button.
     */
    protected void setButtonID(final int buttonID) {
        buttonId = buttonID;
    }

    /**
     * Gets the help identifier for the dialog.
     *
     * @return The help identifier.
     */
    public String getHelpID() {
        return helpId;
    }

    /**
     * Sets the help identifier for the dialog.
     *
     * @param helpID The help identifier.
     */
    public void setHelpID(String helpID) {
        helpId = helpID;
        updateHelpID();
    }


    /**
     * Gets the dialog's content component.
     *
     * @return The dialog's content component.
     */
    public Component getContent() {
        return content;
    }

    /**
     * Sets the dialog's content component.
     *
     * @param content The dialog's content component.
     */
    public void setContent(Component content) {
        if (this.content != null) {
            dialog.getContentPane().remove(this.content);
        }
        this.content = content;
        dialog.getContentPane().add(this.content, BorderLayout.CENTER);
        dialog.validate();
        updateHelpID();
    }

    /**
     * Sets the dialog's content.
     *
     * @param content The dialog's content.
     */
    public void setContent(Object content) {
        Component component;
        if (content instanceof Component) {
            component = (Component) content;
        } else {
            component = new JLabel(content.toString());
        }
        setContent(component);
    }

    /**
     * Gets the button for the given identifier.
     *
     * @param buttonID The button identifier.
     * @return The button, or {@code null}.
     */
    public AbstractButton getButton(int buttonID) {
        return buttonMap.get(buttonID);
    }

    /**
     * Shows the dialog. Overrides shall call {@code super.show()} at the end.
     *
     * @return the identifier of the last button pressed or zero if this is a modeless dialog.
     */
    public int show() {
        setButtonID(0);
        if (!shown) {
            dialog.pack();
            center();
        }
        dialog.setVisible(true);
        shown = true;
        return getButtonID();
    }

    /**
     * Hides the dialog. Overrides shall call {@code super.hide()} at the end. This method does nothing else than hiding the underlying Swing dialog.
     *
     * @see #getJDialog()
     */
    public void hide() {
        dialog.setVisible(false);
    }

    /**
     * This method is called, when the user clicks the close button of the dialog's top window bar.
     * It can also be called directly.
     * Override to implement the dialog's default close behaviour.
     */
    public abstract void close();

    /**
     * Centers the dialog within its parent window.
     */
    public void center() {
        UIUtils.centerComponent(dialog, parent);
    }

    /**
     * Shows an error dialog on top of this dialog.
     *
     * @param errorMessage The message.
     */
    public void showErrorDialog(String errorMessage) {
        showMessageDialog(errorMessage, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows an information dialog on top of this dialog.
     *
     * @param infoMessage The message.
     */
    public void showInformationDialog(String infoMessage) {
        showMessageDialog(infoMessage, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows a warning dialog on top of this dialog.
     *
     * @param warningMessage The message.
     */
    public void showWarningDialog(String warningMessage) {
        showMessageDialog(warningMessage, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Called if the "OK" button has been clicked.
     * The default implementation calls {@link #hide()}.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onOK() {
        hide();
    }

    /**
     * Called if the "Yes" button has been clicked.
     * The default implementation calls {@link #hide()}.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onYes() {
        hide();
    }

    /**
     * Called if the "No" button has been clicked.
     * The default implementation calls {@link #hide()}.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onNo() {
        hide();
    }

    /**
     * Called if the "Cancel" button has been clicked.
     * The default implementation calls {@link #hide()}.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onCancel() {
        hide();
    }

    /**
     * Called if the "Apply" button has been clicked.
     * The default implementation does nothing.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onApply() {
    }

    /**
     * Called if the "Close" button has been clicked.
     * The default implementation calls {@link #hide()}.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onClose() {
        hide();
    }

    /**
     * Called if the reset button has been clicked.
     * The default implementation does nothing.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onReset() {
    }

    /**
     * Called if the help button has been clicked.
     * Clients should override this method to implement a different behaviour.
     */
    protected void onHelp() {
        if (helpId == null) {
            showInformationDialog("Sorry, no help theme available."); /*I18N*/
        }
    }

    /**
     * Called if a non-standard button has been clicked.
     * The default implementation calls {@link #hide()}.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onOther() {
        hide();
    }

    /**
     * Called in order to perform input validation.
     *
     * @return {@code true} if and only if the validation was successful.
     */
    protected boolean verifyUserInput() {
        return true;
    }

    /**
     * Called by the constructor in order to initialise the user interface.
     * The default implementation does nothing.
     *
     * @param buttons The container into which new buttons shall be collected.
     */
    protected void collectButtons(List<AbstractButton> buttons) {
    }

    private void initUI(Object[] otherItems) {

        JPanel buttonRow = new JPanel();
        buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.X_AXIS));

        int insetSize = UIDefaults.INSET_SIZE;
        JPanel contentPane = new JPanel(new BorderLayout(0, insetSize + insetSize / 2));
        contentPane.setBorder(UIDefaults.DIALOG_BORDER);
        contentPane.add(buttonRow, BorderLayout.SOUTH);

        dialog.setResizable(true);
        dialog.setContentPane(contentPane);

        ArrayList<AbstractButton> buttons = new ArrayList<AbstractButton>();

        collectButtons(buttons);

        if (otherItems != null) {
            for (Object otherItem : otherItems) {
                if (otherItem instanceof String) {
                    String text = (String) otherItem;
                    JButton otherButton = new JButton(text);
                    otherButton.setName(getQualifiedPropertyName(text));
                    otherButton.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            setButtonID(ID_OTHER);
                            if (verifyUserInput()) {
                                onOther();
                            }
                        }
                    });
                    buttons.add(otherButton);
                } else if (otherItem instanceof AbstractButton) {
                    AbstractButton otherButton = (AbstractButton) otherItem;
                    otherButton.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            setButtonID(ID_OTHER);
                            if (verifyUserInput()) {
                                onOther();
                            }
                        }
                    });
                    buttons.add(otherButton);
                }
            }
        }

        if ((buttonMask & ID_OK) != 0) {
            JButton button = new JButton("OK");  /*I18N*/
            button.setMnemonic('O');
            button.setName(getQualifiedPropertyName("ok"));
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setButtonID(ID_OK);
                    if (verifyUserInput()) {
                        onOK();
                    }
                }
            });
            buttons.add(button);
            button.setDefaultCapable(true);
            getJDialog().getRootPane().setDefaultButton(button);
            registerButton(ID_OK, button);
        }
        if ((buttonMask & ID_YES) != 0) {
            JButton button = new JButton("Yes");  /*I18N*/
            button.setMnemonic('Y');
            button.setName(getQualifiedPropertyName("yes"));
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setButtonID(ID_YES);
                    if (verifyUserInput()) {
                        onYes();
                    }
                }
            });
            buttons.add(button);
            button.setDefaultCapable(true);
            getJDialog().getRootPane().setDefaultButton(button);
            registerButton(ID_YES, button);
        }
        if ((buttonMask & ID_NO) != 0) {
            JButton button = new JButton("No"); /*I18N*/
            button.setMnemonic('N');
            button.setName(getQualifiedPropertyName("no"));
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setButtonID(ID_NO);
                    if (verifyUserInput()) {
                        onNo();
                    }
                }
            });
            buttons.add(button);
            registerButton(ID_NO, button);
        }
        if ((buttonMask & ID_CANCEL) != 0) {
            JButton button = new JButton("Cancel");  /*I18N*/
            button.setMnemonic('C');
            button.setName(getQualifiedPropertyName("cancel"));
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    close();
                }
            });
            buttons.add(button);
            button.setVerifyInputWhenFocusTarget(false);
            registerButton(ID_CANCEL, button);
        }
        if ((buttonMask & ID_APPLY) != 0) {
            JButton button = new JButton("Apply");  /*I18N*/
            button.setMnemonic('A');
            button.setName(getQualifiedPropertyName("apply"));
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setButtonID(ID_APPLY);
                    if (verifyUserInput()) {
                        onApply();
                    }
                }
            });
            buttons.add(button);
            button.setDefaultCapable(true);
            getJDialog().getRootPane().setDefaultButton(button);
            registerButton(ID_APPLY, button);
        }
        if ((buttonMask & ID_CLOSE) != 0) {
            JButton button = new JButton("Close");  /*I18N*/
            button.setMnemonic('C');
            button.setName(getQualifiedPropertyName("close"));
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setButtonID(ID_CLOSE);
                    onClose();
                }
            });
            button.setToolTipText("Close dialog window");
            buttons.add(button);
            button.setVerifyInputWhenFocusTarget(false);
            registerButton(ID_CLOSE, button);
        }
        if ((buttonMask & ID_RESET) != 0) {
            JButton button = new JButton("Reset"); /*I18N*/
            button.setName(getQualifiedPropertyName("reset"));
            button.setMnemonic('R');
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    final int buttonID = ID_RESET;
                    setButtonID(buttonID);
                    onReset();
                }
            });
            buttons.add(button);
            registerButton(ID_RESET, button);
        }

        if ((buttonMask & ID_HELP) != 0) {
            JButton button = new JButton("Help"); /*I18N*/
            button.setName(getQualifiedPropertyName("help"));
            button.setMnemonic('H');
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setButtonID(ID_HELP);
                    onHelp();
                }
            });
            button.setToolTipText("Show help on this topic.");
            buttons.add(button);
            registerButton(ID_HELP, button);
        }

        buttonRow.add(Box.createHorizontalGlue());
        for (int i = 0; i < buttons.size(); i++) {
            if (i != 0) {
                buttonRow.add(Box.createRigidArea(new Dimension(4, 0)));
            }
            buttonRow.add(buttons.get(i));
        }

        dialog.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
    }

    protected String getQualifiedPropertyName(String name) {
        return getClass().getSimpleName() + "." + name;
    }

    protected void registerButton(int buttonID, AbstractButton button) {
        buttonMap.put(buttonID, button);
    }

    private void updateHelpID() {
        if (helpId == null) {
            return;
        }
        if (helpBroker == null) {
            initHelpBroker();
        }
        if (helpBroker == null) {
            return;
        }
        HelpSet helpSet = helpBroker.getHelpSet();
        try {
            helpBroker.setCurrentID(helpId);
        } catch (BadIDException e) {
            Logger systemLogger = BeamLogManager.getSystemLogger();
            if (systemLogger != null) {
                systemLogger.severe("ModalDialog: '" + helpId + "' is not a valid helpID");
            } else {
                Debug.trace(e);
            }
        }
        if (helpSet == null) {
            return;
        }
        if (getJDialog() != null) {
            helpBroker.enableHelpKey(getJDialog(), helpId, helpSet);
        }
        if (getJDialog().getContentPane() != null) {
            helpBroker.enableHelpKey(getJDialog().getContentPane(), helpId, helpSet);
        }
        AbstractButton helpButton = getButton(ID_HELP);
        if (helpButton != null) {
            helpBroker.enableHelpKey(helpButton, helpId, helpSet);
            helpBroker.enableHelpOnButton(helpButton, helpId, helpSet);
        }
    }

    private void initHelpBroker() {
        HelpSet helpSet = HelpSys.getHelpSet();
        if (helpSet != null) {
            helpBroker = helpSet.createHelpBroker();
            if (helpBroker instanceof DefaultHelpBroker) {
                DefaultHelpBroker defaultHelpBroker = (DefaultHelpBroker) helpBroker;
                defaultHelpBroker.setActivationWindow(getJDialog());
            }
        }
    }

    private void showMessageDialog(String message, int messageType) {
        JOptionPane.showMessageDialog(getJDialog(), message, getJDialog().getTitle(), messageType);
    }

}