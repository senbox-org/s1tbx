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

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JDialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * The <code>ModalDialog</code> is a helper class to quickly construct modal dialogs. The dialogs created with this
 * class have a unique border and font and a standard button row for the typical buttons like "OK", "Cancel" etc.
 * <p/>
 * <p>Instances of a modal dialog are created with a parent component, a title, the actual dialog content component, and
 * a bit-combination of the standard buttons to be used.
 * <p/>
 * <p>The dialog can be used directly or the class is overridden in order to override the methods {@link #onOK()},
 * {@link #onCancel()} etc. which are called if a user presses the corresponding button.
 * <p/>
 * <p>A limited way of input validation is provided by the  <code>verifyUserInput</code> method which can be overridden
 * in order to return <code>false</code> if a user input is invalid. In this case the {@link #onOK()},
 * {@link #onYes()} and {@link #onNo()} methods are NOT called.
 *
 * @author Norman Fomferra
 * @since BEAM 1.0
 */
public class ModalDialog extends AbstractDialog {

    public static final int ID_OK = 0x0001;
    public static final int ID_YES = 0x0002;
    public static final int ID_NO = 0x0004;
    public static final int ID_CANCEL = 0x0008;

    public static final int ID_OK_CANCEL = ID_OK | ID_CANCEL;
    public static final int ID_OK_CANCEL_HELP = ID_OK_CANCEL | ID_HELP;
    public static final int ID_YES_NO = ID_YES | ID_NO;

    public ModalDialog(Window parent, String title, int buttonMask, String helpID) {
        this(parent, title, buttonMask, null, helpID);
    }

    public ModalDialog(Window parent, String title, Object content, int buttonMask, String helpID) {
        this(parent, title, content, buttonMask, null, helpID);
    }

    public ModalDialog(Window parent, String title, Object content, int buttonMask, Object[] otherButtons,
                       String helpID) {
        this(parent, title, buttonMask, otherButtons, helpID);
        setContent(content);
    }

    public ModalDialog(Window parent, String title, int buttonMask, Object[] otherButtons, String helpID) {
        super(new JDialog(parent, title, JDialog.DEFAULT_MODALITY_TYPE), buttonMask, otherButtons, helpID);
    }

    /**
     * This method is called, when the user clicks the "cancel" button or the "close" button of
     * the top bar of the dialog window. It can also be called directly.
     * The method sets the button identifier to {@link #ID_CANCEL} and calls {@link #onCancel()}.
     */
    @Override
    public void close() {
        setButtonID(ID_CANCEL);
        onCancel();
    }

    /**
     * @deprecated  Since BEAM 4.2 {@link #close()} is used instead.
     */
    @Deprecated
    protected final void cancelDialog() {
        close();
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

    @Override
    protected void collectButtons(List<AbstractButton> buttons) {
        int buttonMask = getButtonMask();
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
        }
    }

}
