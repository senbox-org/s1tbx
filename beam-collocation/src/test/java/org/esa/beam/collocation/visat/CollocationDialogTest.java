package org.esa.beam.collocation.visat;

import org.esa.beam.collocation.CollocateOpTest;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;

import javax.swing.JDialog;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class CollocationDialogTest {
    public static void main(String[] args) throws IllegalAccessException, UnsupportedLookAndFeelException, InstantiationException, ClassNotFoundException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        final Product inputProduct1 = CollocateOpTest.createTestProduct1();
        final Product inputProduct2 = CollocateOpTest.createTestProduct2();
        final DefaultAppContext context = new DefaultAppContext("dev0");
        context.getProductManager().addProduct(inputProduct1);
        context.getProductManager().addProduct(inputProduct2);
        context.setSelectedProduct(inputProduct1);
        final CollocationDialog dialog = new CollocationDialog(context);
        dialog.getJDialog().setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.show();
    }
}
