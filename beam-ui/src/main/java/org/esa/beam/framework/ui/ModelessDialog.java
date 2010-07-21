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
package org.esa.beam.framework.ui;

import javax.swing.JDialog;
import java.awt.Dialog;
import java.awt.Window;

/**
 * <p>A helper class used to implement standard modeless dialogs.</p>
 * <p>The dialog can be used directly (which doesn't make much sense) or the class is used as base class in
 * order to override the methods {@link #onApply()}, {@link #onClose()} etc. which are called if a user
 * presses the corresponding button.<p/>
 *
 * @author Norman Fomferra
 * @since BEAM 4.2
 */
public class ModelessDialog extends AbstractDialog {

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
}