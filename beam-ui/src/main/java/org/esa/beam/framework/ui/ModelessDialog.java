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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.logging.Logger;

/**
 * A modeless dialog providing an "Apply" and "Close" button.
 * 
 * @author Norman Fomferra
 * @version $Revision: 1.3 $  $Date: 2007/04/18 13:01:13 $
 */
public class ModelessDialog extends AbstractDialog {

    public static final int ID_APPLY = 0x0001;
    public static final int ID_CLOSE = 0x0008;

    public static final int ID_APPLY_CLOSE = ID_APPLY | ID_CLOSE;
    public static final int ID_APPLY_CLOSE_HELP = ID_APPLY_CLOSE | ID_HELP;

    public ModelessDialog(Window parent, String title, int buttonMask, String helpID) {
        this(parent, title, buttonMask, null, helpID);
    }

    public ModelessDialog(Window parent, String title, Object content, int buttonMask, String helpID) {
        this(parent, title, content, buttonMask, null, helpID);
    }

    public ModelessDialog(Window parent, String title, Object content, int buttonMask, Object[] otherButtons,
                       String helpID) {
        this(parent, title, buttonMask, otherButtons, helpID);
        setContent(content);
    }

    public ModelessDialog(Window parent, String title, int buttonMask, Object[] otherButtons, String helpID) {
        super(new JDialog(parent, title, Dialog.ModalityType.MODELESS), buttonMask, otherButtons, helpID);
    }

    /**
     * This method is called, when the user clicks the "close" button of the bottom button row
     * or the "close" button of the top bar of the dialog window. It can also be called directly.
     * The method sets the button identifier to {@link #ID_CLOSE} and calls {@link #onClose()}.
     */
    @Override
    public void close() {
        setButtonID(ID_CLOSE);
        onClose();
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


    @Override
    protected void collectButtons(java.util.List<AbstractButton> buttons) {
        int buttonMask = getButtonMask();
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
            buttons.add(button);
            button.setVerifyInputWhenFocusTarget(false);
        }
    }

}