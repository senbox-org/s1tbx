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

package org.esa.beam.visat.toolviews.mask;

import com.jidesoft.utils.Lm;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.*;

import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.image.BufferedImage;

public class MaskFormTest extends TestCase {
    private Product product;
    private MaskManagerForm maskManagerForm;
    private MaskViewerForm maskViewerForm;

    static {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");
    }

    @Override
    public void setUp() {
        product = createTestProduct();

        maskManagerForm = new MaskManagerForm(null, null);
        maskManagerForm.reconfigureMaskTable(product, null);

        maskViewerForm = new MaskViewerForm(null);
        maskViewerForm.reconfigureMaskTable(product, null);
    }

    public void testMaskManagerForm() {
        assertSame(product, maskManagerForm.getProduct());
        assertNotNull(maskManagerForm.getHelpButton());
        assertEquals("helpButton", maskManagerForm.getHelpButton().getName());
        assertNotNull(maskManagerForm.createContentPanel());
        assertEquals(10, maskManagerForm.getRowCount());

        final TableModel tableModel = maskManagerForm.getMaskTable().getModel();

        assertEquals("M_1", tableModel.getValueAt(0, 0));

        product.getPinGroup(); // Force addition of "pin" group

        assertEquals(11, maskManagerForm.getRowCount());

        assertEquals("M_1", tableModel.getValueAt(0, 0));
        assertEquals("M_2", tableModel.getValueAt(1, 0));
        assertEquals("M_10", tableModel.getValueAt(9, 0));
        assertEquals("pins", tableModel.getValueAt(10, 0));
    }

    public void testMaskViewerForm() {
        assertSame(product, maskViewerForm.getProduct());
        assertNull(maskViewerForm.getHelpButton());
        assertNotNull(maskViewerForm.createContentPanel());
        assertEquals(10, maskViewerForm.getRowCount());
    }

    static Product createTestProduct() {
        Color[] colors = {
                Color.WHITE,
                Color.BLACK,
                Color.GREEN,
                Color.BLUE,
                Color.CYAN,
                Color.MAGENTA,
                Color.PINK,
                Color.YELLOW,
                Color.ORANGE,
                Color.RED,
        };
        Product product = new Product("P", "T", 256, 256);
        Band a = product.addBand("A", ProductData.TYPE_UINT8);
        Band b = product.addBand("B", ProductData.TYPE_UINT8);
        Band c = product.addBand("C", ProductData.TYPE_UINT8);
        a.setScalingFactor(1.0 / 255.0);
        b.setScalingFactor(1.0 / 255.0);
        c.setScalingFactor(1.0 / 255.0);
        a.setSourceImage(new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY));
        b.setSourceImage(new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY));
        c.setSourceImage(new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY));
        for (int i = 0; i < colors.length; i++) {
            String expression = "B > " + (i / (colors.length - 1.0));
            String name = "M_" + (product.getMaskGroup().getNodeCount() + 1);
            Mask mask = Mask.BandMathsType.create(name, expression, product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                                                  expression, colors[i], 1.0 - 1.0 / (1 + (i % 4)));
            product.getMaskGroup().add(mask);
        }

        for (int i = 0; i < product.getVectorDataGroup().getNodeCount(); i++) {
            VectorDataNode vectorDataNode = product.getVectorDataGroup().get(i);
            final Mask mask = product.addMask(vectorDataNode.getName(),
                                              Mask.VectorDataType.INSTANCE);
            mask.getImageConfig().setValue("color", colors[i % colors.length].brighter());
            mask.getImageConfig().setValue("transparency", 0.1);
            mask.getImageConfig().setValue("vectorData", vectorDataNode);
        }
        return product;
    }
}
