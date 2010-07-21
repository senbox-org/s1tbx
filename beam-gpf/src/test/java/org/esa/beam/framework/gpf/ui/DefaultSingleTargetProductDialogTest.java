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

package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import javax.swing.JFrame;
import javax.swing.UIManager;


public class DefaultSingleTargetProductDialogTest extends TestCase {
    private static final TestOp.Spi SPI = new TestOp.Spi();

    @Override
    protected void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(SPI);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(SPI);
    }

    public void testNothing() {
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(SPI);

        DefaultAppContext app = new DefaultAppContext("Killer App");
        app.getApplicationWindow().setSize(200, 200);

        final DefaultSingleTargetProductDialog dialog = (DefaultSingleTargetProductDialog) DefaultSingleTargetProductDialog.createDefaultDialog(
                TestOp.Spi.class.getName(), app);
        dialog.setTargetProductNameSuffix("_test");
        dialog.getJDialog().setTitle("TestOp GUI");
        dialog.show();
    }

    public static class TestOp extends Operator {
        @SourceProduct
        Product masterProduct;
        @SourceProduct
        Product slaveProduct;
        @TargetProduct
        Product target;
        @Parameter(defaultValue = "true")
        boolean copyTiePointGrids;
        @Parameter(defaultValue = "false")
        Boolean copyMetadata;
        @Parameter(interval = "[-1,+1]", defaultValue = "-0.1")
        double threshold;
        @Parameter(valueSet = {"ME-203", "ME-208", "ME-002"}, defaultValue = "ME-208")
        String method;

        @Override
        public void initialize() throws OperatorException {
            Product product = new Product("N", "T", 16, 16);
            product.addBand("B1", ProductData.TYPE_FLOAT32);
            product.addBand("B2", ProductData.TYPE_FLOAT32);
            product.setPreferredTileSize(4, 4);
            //System.out.println("product = " + product);
            target = product;
        }

        @Override
        public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(TestOp.class);
            }
        }
    }
}














































