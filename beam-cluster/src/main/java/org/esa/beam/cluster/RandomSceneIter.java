/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.cluster;

import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.util.Random;
import java.awt.*;

import javax.media.jai.ROI;

import com.bc.ceres.core.ProgressMonitor;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class RandomSceneIter {
    private final Operator operator;
    private final Random random;
    private final RasterDataNode[] rdn;
    private final ROI roi;

    public RandomSceneIter(Operator operator, RasterDataNode[] rdn, ROI roi, int seed) {
        this.operator = operator;
        this.rdn = rdn;
        this.roi = roi;
        random = new Random(seed);
    }

    public double[] getNextValue() {
        final Product product = operator.getSourceProduct("source");

        final int rasterWidth = product.getSceneRasterWidth();
        final int rasterHeight = product.getSceneRasterHeight();
        final double[] value = new double[rdn.length];

        boolean valid = false;
        while (!valid) {
            final int x = random.nextInt(rasterWidth);
            final int y = random.nextInt(rasterHeight);
            if (roi.contains(x, y)) {
                final Rectangle rectangle = new Rectangle(x, y, 1, 1);
                for (int i = 0; i < rdn.length; i++) {
                    final Tile sourceTile = operator.getSourceTile(rdn[i], rectangle, ProgressMonitor.NULL);
                    value[i] = sourceTile.getSampleDouble(x, y);
                }
                valid = true;
            }
        }
        return value;
    }
}
