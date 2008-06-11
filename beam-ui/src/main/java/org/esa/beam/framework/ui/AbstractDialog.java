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
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * @author Norman Fomferra
 * @version $Revision: 1.3 $  $Date: 2007/04/18 13:01:13 $
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
        createUI(otherButtons);
        setHelpID(helpID);
    }

    public int getButtonMask() {
        return buttonMask;
    }

    protected void setButtonID(final int buttonID) {
        buttonId = buttonID;
    }

    protected void createUI(Object[] otherItems) {

        JPanel buttonRow = new JPanel();
        buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.X_AXIS));

        int insetSize = UIDefaults.INSET_SIZE;
        JPanel contentPane = new JPanel(new BorderLayout(0, insetSize + insetSize / 2));
        contentPane.setBorder(UIDefaults.DIALOG_BORDER);
        contentPane.add(buttonRow, BorderLayout.SOUTH);

        dialog.setResizable(true);
        dialog.setContentPane(contentPane);

        ArrayList<AbstractButton> buttons = new ArrayList<AbstractButton>();

        addStandardButtons(buttons);

        if (otherItems != null) {
            for (Object otherItem : otherItems) {
                if (otherItem instanceof String) {
                    JButton otherButton = new JButton((String) otherItem);
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
            button.setMnemonic('H');
            button.setIcon(UIUtils.loadImageIcon("/org/esa/beam/resources/images/icons/Help16.gif"));
            button.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setButtonID(ID_HELP);
                    onHelp();
                }
            });
            buttons.add(button);
            helpButton = button;
        }

        for (int i = 0; i < buttons.size(); i++) {
            if (i != 0) {
                buttonRow.add(Box.createRigidArea(new Dimension(6, 0)));
            }
            buttonRow.add(buttons.get(i));
        }

        dialog.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                closeDialog();
            }
        });
    }


    protected abstract void addStandardButtons(List<AbstractButton> buttons);

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

    public String getHelpID() {
        return helpId;
    }

    public void setHelpID(String helpID) {
        helpId = helpID;
        updateHelpID();
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

    public JDialog getJDialog() {
        return dialog;
    }

    public Window getParent() {
        return parent;
    }

    /**
     * Shows the dialog.
     * @return the identifier of the last button pressed or -1.
     */
    public int show() {
        setButtonID(-1);
        if (!shown) {
            dialog.pack();
            center();
        }
        dialog.setVisible(true);
        shown = true;
        return getButtonID();
    }


    /**
     * Hides the dialog.
     */
    public void hide() {
        dialog.setVisible(false);
    }

    public void center() {
        UIUtils.centerComponent(dialog, parent);
    }

    public int getButtonID() {
        return buttonId;
    }

    public void setContent(Object content) {
        Component comp;
        if (content instanceof Component) {
            comp = (Component) content;
        } else {
            comp = new JLabel(content.toString());
        }
        if (this.content != null) {
            dialog.getContentPane().remove(this.content);
        }
        this.content = comp;
        dialog.getContentPane().add(this.content, BorderLayout.CENTER);
        dialog.validate();
        updateHelpID();
    }

    public void showErrorDialog(String errorMessage) {
        showMessageDialog(errorMessage, JOptionPane.ERROR_MESSAGE);
    }

    public void showInformationDialog(String infoMessage) {
        showMessageDialog(infoMessage, JOptionPane.INFORMATION_MESSAGE);
    }

    public void showWarningDialog(String warningMessage) {
        showMessageDialog(warningMessage, JOptionPane.WARNING_MESSAGE);
    }

    protected abstract void closeDialog();

    protected void onHelp() {
        if (helpId == null) {
            showInformationDialog("Sorry, no help theme available."); /*I18N*/
        }
    }

    protected void onReset() {
    }

    protected void onOther() {
        hide();
    }

    protected boolean verifyUserInput() {
        return true;
    }

    private void showMessageDialog(String message, int messageType) {
        JOptionPane.showMessageDialog(getJDialog(), message, getJDialog().getTitle(), messageType);
    }
}