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

package org.esa.snap.core.image;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.jexp.ParseException;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

public class ColoredMaskImageMultiLevelSource extends AbstractMultiLevelSource {

    private final Product product;
    private final Color color;
    private final String expression;
    private final boolean inverseMask;

    public static MultiLevelSource create(Product product, Color color, String expression,
                                          boolean inverseMask, AffineTransform i2mTransform) {
        Assert.notNull(product);
        Assert.notNull(color);
        Assert.notNull(expression);
        //todo [multisize_products] change when ref rasters might have different multilevelmodels
        MultiLevelModel model = null;
        try {
            final RasterDataNode[] refRasters = BandArithmetic.getRefRasters(expression, product);
            if (refRasters.length > 0) {
                model = refRasters[0].getMultiLevelModel();
            }
        } catch (ParseException e) {
            //model = null
        }
        if (model == null) {
            final int width = product.getSceneRasterWidth();
            final int height = product.getSceneRasterHeight();
            model = new DefaultMultiLevelModel(i2mTransform, width, height);
        }
        return new ColoredMaskImageMultiLevelSource(model, product, color, expression, inverseMask);
    }

    public ColoredMaskImageMultiLevelSource(MultiLevelModel model, Product product, Color color,
                                            String expression, boolean inverseMask) {
        super(model);
        this.product = product;
        this.color = color;
        this.expression = expression;
        this.inverseMask = inverseMask;
    }

    @Override
    public RenderedImage createImage(int level) {
        return ImageManager.getInstance().createColoredMaskImage(product, expression, color,
                                                                 inverseMask, level);
    }
}
