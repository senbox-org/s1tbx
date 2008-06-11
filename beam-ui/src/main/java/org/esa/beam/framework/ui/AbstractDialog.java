/*
 * $Id: ModalDialog.java,v 1.3 2007/04/18 13:01:13 norman Exp $
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

import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.util.Debug;
import org.esa.beam.util.logging.BeamLogManager;

import javax.help.BadIDException;
import javax.help.DefaultHelpBroker;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Window;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * The base class for {@link ModalDialog} and {@link ModelessDialog}.
 *
 * @author Norman Fomferra
 * @since BEAM 4.2
 */
public abstract class AbstractDialog {

    public static final int ID_RESET = 0x0010;
    public static final int ID_HELP = 0x0020;
    public static final int ID_OTHER = 0x8000;

    private final JDialog dialog;
    private final Window parent;
    private final int buttonMask;

    private int buttonId;
    private Component content;
    private boolean shown;

    // Java help support
    private String helpId;
    private HelpBroker helpBroker;
    private JButton helpButton;

    protected AbstractDialog(JDialog dialog, int buttonMask, Object[] otherButtons, String helpID) {
        this.parent = (Window) dialog.getParent();
        this.dialog = dialog;
        this.buttonMask = buttonMask;
        setButtonID(0);
        initUI(otherButtons);
        setHelpID(helpID);
    }

    /**
     * Gets the underlying Swing dialog passed to the constructor.
     * @return the underlying Swing dialog.
     */
    public JDialog getJDialog() {
        return dialog;
    }

    /**
     * Gets the owner of the dialog.
     * @return The owner of the dialog.
     */
    public Window getParent() {
        return parent;
    }

    /**
     * Gets the button mask passed to the constructor.
     * @return The button mask.
     */
    public int getButtonMask() {
        return buttonMask;
    }

    /**
     * Gets the identifier for the most recently pressed button.
     * @return The identifier for the most recently pressed button.
     */
    public int getButtonID() {
        return buttonId;
    }

    /**
     * Sets the identifier for the most recently pressed button.
     * @param buttonID The identifier for the most recently pressed button.
     */
    protected void setButtonID(final int buttonID) {
        buttonId = buttonID;
    }

    /**
     * Gets the help identifier for the dialog.
     * @return The help identifier.
     */
    public String getHelpID() {
        return helpId;
    }

    /**
     * Sets the help identifier for the dialog.
     * @param helpID The help identifier.
     */
    public void setHelpID(String helpID) {
        helpId = helpID;
        updateHelpID();
    }

    /**
     * Gets the dialog's content component.
     * @return The dialog's content component.
     */
    public Component getContent() {
        return content;
    }

    /**
     * Sets the dialog's content component.
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
     * Shows the dialog.
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
     * Hides the dialog. This method does nothing else than hiding the underlying Swing dialog.
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
     * @param errorMessage The message.
     */
    public void showErrorDialog(String errorMessage) {
        showMessageDialog(errorMessage, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows an information dialog on top of this dialog.
     * @param infoMessage The message.
     */
    public void showInformationDialog(String infoMessage) {
        showMessageDialog(infoMessage, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows a warning dialog on top of this dialog.
     * @param warningMessage The message.
     */
    public void showWarningDialog(String warningMessage) {
        showMessageDialog(warningMessage, JOptionPane.WARNING_MESSAGE);
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
     * Called if the reset button has been clicked.
     * The default implementation does nothing.
     * Clients should override this method to implement meaningful behaviour.
     */
    protected void onReset() {
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
     * @return {@code true} if and only if the validation was successful.
     */
    protected boolean verifyUserInput() {
        return true;
    }

    /**
     * Called by the constructor in order to initialise the user interface.
     * Override to add extra buttons to the given list of buttons.
     * @param buttons The container for the new extra buttons.
     */
    protected abstract void collectButtons(List<AbstractButton> buttons);

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
            buttons.add(button);
            helpButton = button;
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