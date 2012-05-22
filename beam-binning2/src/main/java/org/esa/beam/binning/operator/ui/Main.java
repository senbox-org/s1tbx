/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator.ui;

import com.jidesoft.utils.Lm;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;

import javax.swing.UIManager;

/**
 * Test main class for UI.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class Main {

    public static void main(String[] args) {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable e) {
            // ok
        }

        final BinningDialog dialog = new BinningDialog(new DefaultAppContext("VISAT"), "Binning Op", "") {
            @Override
            protected void onClose() {
                System.exit(0);
            }
        };
        dialog.show();
    }
}
