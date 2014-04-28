/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.dialogs;

import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.*;

/**
 * Auto close warning messages after timer
 */
public class AutoCloseOptionPane {

    public AutoCloseOptionPane() {
    }

    public static void showWarningDialog(final String title, final String message) {

        final int timeout = 120;
        showMessageDialog(VisatApp.getApp().getMainFrame(), title, message, timeout,
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, null);
    }

    public static int showMessageDialog(final Component parentComponent, final String title,
                                        final Object message, final int timeout, final int optionType,
                                        final int messageType, final Object[] options, final Object initialValue) {
        final JOptionPane pane = new JOptionPane(message, messageType, optionType, null, options, initialValue);
        pane.setInitialValue(initialValue);

        final JDialog dialog = pane.createDialog(parentComponent, title);

        pane.selectInitialValue();
        new Thread() {
            public void run() {
                try {
                    for (int i = timeout; i >= 0; i--) {
                        Thread.sleep(1000);
                        if (dialog.isVisible() && i < 300) {
                            dialog.setTitle(title + "  (" + i + " seconds before auto continuing)");
                        }
                    }
                    if (dialog.isVisible()) {
                        dialog.setVisible(false);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }.start();
        dialog.setVisible(true);

        Object selectedValue = pane.getValue();
        if (selectedValue.equals("uninitializedValue")) {
            selectedValue = initialValue;
        }
        if (selectedValue == null)
            return JOptionPane.CLOSED_OPTION;
        if (options == null) {
            if (selectedValue instanceof Integer)
                return ((Integer) selectedValue).intValue();
            return JOptionPane.CLOSED_OPTION;
        }
        for (int counter = 0, maxCounter = options.length; counter < maxCounter; counter++) {
            if (options[counter].equals(selectedValue))
                return counter;
        }
        return JOptionPane.CLOSED_OPTION;
    }

}

