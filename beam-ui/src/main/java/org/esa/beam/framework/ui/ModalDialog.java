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
import java.awt.Window;

/**
 * A helper class used to implement standard modal dialogs.
 * <p/>
 * <p>The dialog can be used directly or the class is used as base class in order to override the methods {@link #onOK()},
 * {@link #onCancel()} etc. which are called if a user presses the corresponding button.
 * <p/>
 *
 * @author Norman Fomferra
 * @since BEAM 1.0
 */
public class ModalDialog extends AbstractDialog {

    public static final int ID_OK_CANCEL = ID_OK | ID_CANCEL;
    public static final int ID_OK_CANCEL_HELP = ID_OK_CANCEL | ID_HELP;
    public static final int ID_OK_APPLY_CANCEL = ID_OK | ID_APPLY | ID_CANCEL;
    public static final int ID_OK_APPLY_CANCEL_HELP = ID_OK_APPLY_CANCEL | ID_HELP;
    public static final int ID_YES_NO = ID_YES | ID_NO;
    public static final int ID_YES_NO_HELP = ID_YES_NO | ID_HELP;

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

}
