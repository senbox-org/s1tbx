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
