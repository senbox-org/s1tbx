/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.image.ImageManager;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;

public class OperatorImage extends SourcelessOpImage {

    private final OperatorContext operatorContext;
    private Band targetBand;

    public OperatorImage(Band targetBand, OperatorContext operatorContext) {
        this(targetBand, operatorContext, ImageManager.createSingleBandedImageLayout(targetBand));
    }

    private OperatorImage(Band targetBand, OperatorContext operatorContext, ImageLayout imageLayout) {
        super(imageLayout,
              operatorContext.getRenderingHints(),
              imageLayout.getSampleModel(null),
              imageLayout.getMinX(null),
              imageLayout.getMinY(null),
              imageLayout.getWidth(null),
              imageLayout.getHeight(null));
        this.targetBand = targetBand;
        this.operatorContext = operatorContext;
        OperatorContext.setTileCache(this);
    }

    public OperatorContext getOperatorContext() {
        return operatorContext;
    }

    public Band getTargetBand() {
        return targetBand;
    }


    @Override
    protected void computeRect(PlanarImage[] ignored, WritableRaster tile, Rectangle destRect) {

        operatorContext.executeOperator(ProgressMonitor.NULL);

        long startNanos = System.nanoTime();

        Tile targetTile;
        if (operatorContext.isComputingImageOf(getTargetBand())) {
            targetTile = createTargetTile(getTargetBand(), tile, destRect);
        } else if (requiresAllBands()) {
            targetTile = operatorContext.getSourceTile(getTargetBand(), destRect);
        } else {
            targetTile = null;
        }
        operatorContext.startWatch();
        // computeTile() may have been deactivated
        if (targetTile != null && operatorContext.getOperator().canComputeTile()) {
            operatorContext.getOperator().computeTile(getTargetBand(), targetTile, ProgressMonitor.NULL);
        }
        operatorContext.stopWatch();
//        long nettoNanos = operatorContext.getNettoTime();

        operatorContext.fireTileComputed(this, destRect, startNanos);
    }

    protected boolean requiresAllBands() {
        return operatorContext.requiresAllBands();
    }

    @Override
    public synchronized void dispose() {
        targetBand = null;
        super.dispose();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("[");
        OperatorDescriptor operatorDescriptor = operatorContext.getOperatorSpi().getOperatorDescriptor();
        sb.append(operatorDescriptor.getAlias());
        sb.append(",");
        if (targetBand != null) {
            sb.append(targetBand.getName());
        }
        sb.append("]");
        return sb.toString();
    }

    protected static TileImpl createTargetTile(Band band, WritableRaster targetTileRaster, Rectangle targetRectangle) {
        return new TileImpl(band, targetTileRaster, targetRectangle, true);
    }

}
