/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.binning;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.snap.core.datamodel.Product;

import java.awt.Rectangle;
import java.util.Iterator;

/**
 * A "slice" of observations. A slice is a spatially contiguous area of observations.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 */
public class ObservationSlice implements Iterable<Observation> {

    private final MultiLevelImage[] sourceImages;
    private final MultiLevelImage maskImage;
    private final Product product;
    private final float[] superSamplingSteps;
    private final Rectangle sliceRect;
    private final BinningContext binningContext;

    public ObservationSlice(MultiLevelImage[] sourceImages, MultiLevelImage maskImage, Product product,
                            float[] superSamplingSteps, Rectangle sliceRect, BinningContext binningContext) {
        this.sourceImages = sourceImages;
        this.maskImage = maskImage;
        this.product = product;
        this.superSamplingSteps = superSamplingSteps;
        this.sliceRect = sliceRect;
        this.binningContext = binningContext;
    }

    @Override
    public Iterator<Observation> iterator() {
        return ObservationIterator.create(sourceImages, maskImage, product, superSamplingSteps, sliceRect, binningContext);
    }

}
