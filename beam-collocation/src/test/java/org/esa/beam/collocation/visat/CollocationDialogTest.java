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

package org.esa.beam.collocation.visat;

import org.esa.beam.collocation.CollocateOpTest;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.junit.Test;

import javax.swing.JDialog;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import static junit.framework.Assert.*;

import java.awt.HeadlessException;

public class CollocationDialogTest {
    @Test
    public void testCollocationDialogExtensively() {
        try {
            final CollocationDialog dialog = createDialog();
            assertEquals("Collocation", dialog.getTitle());
            assertEquals("collocation", dialog.getHelpID());
        } catch (HeadlessException e) {
            warnHeadless();
        }
    }

    private void warnHeadless() {
        System.out.println("A " + CollocationDialogTest.class + " test has not been performed: HeadlessException");
    }

    public static void main(String[] args) throws IllegalAccessException, UnsupportedLookAndFeelException, InstantiationException, ClassNotFoundException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        final CollocationDialog dialog = createDialog();
        dialog.getJDialog().setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.show();
    }

    private static CollocationDialog createDialog() {
        final Product inputProduct1 = CollocateOpTest.createTestProduct1();
        final Product inputProduct2 = CollocateOpTest.createTestProduct2();
        final DefaultAppContext context = new DefaultAppContext("dev0");
        context.getProductManager().addProduct(inputProduct1);
        context.getProductManager().addProduct(inputProduct2);
        context.setSelectedProduct(inputProduct1);
        return new CollocationDialog(context);
    }
}
