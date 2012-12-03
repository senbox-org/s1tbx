package org.esa.nest.util;

import org.esa.beam.visat.VisatApp;

/**

 */
public class ErrorHandler {
    private static final boolean displayDlg = false;

    public static void reportError(final String msg) {
        if(VisatApp.getApp() != null && displayDlg) {
            VisatApp.getApp().showErrorDialog(msg);
        } else {
            System.out.println(msg);
        }
    }
}
