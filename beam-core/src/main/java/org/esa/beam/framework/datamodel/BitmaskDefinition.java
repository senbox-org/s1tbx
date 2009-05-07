/*
 * $Id: BitmaskDef.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataio.ProductSubsetDef;

import javax.media.jai.util.Range;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;

/**
 * Represents a bitmask definition.
 *
 * @author Norman Fomferra
 * @version $Revision: 2442 $ $Date: 2008-07-04 14:46:08 +0200 (Fr, 04 Jul 2008) $
 */
public abstract class BitmaskDefinition extends ProductNode {
    protected BitmaskDefinition(String name, String description) {
        super(name, description);
    }

    public Rectangle getSceneRasterBounds()  {
        return getProductSceneRasterBounds();
    }

    public final Rectangle getProductSceneRasterBounds()  {
        Product product = getProduct();
        if (product != null) {
            return new Rectangle(0, 0, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        }
        return null;
    }

    protected abstract RenderedImage createMaskImage();

    public static class BooleanExpression extends BitmaskDefinition {
        private String booleanExpression;

        public BooleanExpression(String name, String booleanExpression, String description) {
            super(name, description);
            this.booleanExpression = booleanExpression;
        }

        public String getBooleanExpression() {
            return booleanExpression;
        }

        public void setBooleanExpression(String booleanExpression) {
            this.booleanExpression = booleanExpression;
        }

        @Override
        protected RenderedImage createMaskImage() {
            return null;
        }

        @Override
        public long getRawStorageSize(ProductSubsetDef subsetDef) {
            return 0;
        }

        @Override
        public void acceptVisitor(ProductVisitor visitor) {
        }
    }

//    public static class ValueRange extends BitmaskDefinition {
//        Range  range;
//    }
//
//    public static class Not extends BitmaskDefinition {
//        BitmaskDefinition arg;
//    }
//
//    public abstract  static class Binary extends BitmaskDefinition {
//        BitmaskDefinition arg1;
//        BitmaskDefinition arg2;
//    }
//
//    public static class And extends Binary {
//    }
//
//    public static class Or extends Binary {
//    }
//
//    public static class Xor extends Binary {
//    }
}