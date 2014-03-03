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
package org.esa.pfa.ui.toolviews.cbir;

import org.esa.pfa.ordering.ProductOrder;
import org.esa.pfa.ordering.ProductOrderBasket;
import org.esa.pfa.ordering.ProductOrderService;

import javax.swing.*;
import java.io.File;
import java.util.Locale;

/**
 * Product Ordering Toolview
 */
public class CBIROrderingToolViewTest {

    public static final String[] PRODUCT_NAMES = new String[]{
            "MER_RR__1PRACR20100923_052600_000020872093_00134_44777_0000.N1",
            "MER_RR__1PRACR20100929_021701_000026312093_00218_44861_0000.N1",
            "MER_RR__1PRACR20101107_122518_000026243096_00153_45426_0000.N1",
            "MER_RR__1PRACR20101119_050535_000026233096_00321_45594_0000.N1",
            "MER_RR__1PRACR20101122_231829_000026233096_00375_45648_0000.N1",
            "MER_RR__1PRACR20101126_054941_000026223096_00422_45695_0000.N1",
            "MER_RR__1PRACR20110406_104112_000026193101_00152_47580_0000.N1",
            "MER_RR__1PRACR20110802_083231_000026283105_00122_49274_0000.N1",
    };

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
        }

        Locale.setDefault(Locale.ENGLISH);

        CBIROrderingToolView toolView = new CBIROrderingToolView();

        toolView.setLocalProductDir(new File("C:\\Users\\Norman\\BC\\EOData\\PFA\\fex-in"));
        ProductOrderBasket productOrderBasket = new ProductOrderBasket();
        toolView.setProductOrderBasket(productOrderBasket);

        final ProductOrderService productOrderService = new ProductOrderService(productOrderBasket);

        JComponent control = toolView.createControl();

        final JFrame frame = new JFrame("CBIROrderingToolView Test");
        frame.setContentPane(control);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
            }
        });

        for (String productName : PRODUCT_NAMES) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
            }
            productOrderService.submit(new ProductOrder(productName));
        }
    }

}
