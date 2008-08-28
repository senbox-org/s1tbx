/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.dataio.smos;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.SingleBandedOpImage;
import org.esa.beam.jai.DownscalableImageSupport;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import com.bc.ceres.glevel.DownscalableImage;

public class SmosL1ValidImage extends SingleBandedOpImage {

    private Band smosBand;
    private PlanarImage rendering;

    public SmosL1ValidImage(Band smosBand) {
        super(DataBuffer.TYPE_BYTE,
              smosBand.getSceneRasterWidth(),
              smosBand.getSceneRasterHeight(),
              smosBand.getProduct().getPreferredTileSize(),
              null);
        init(smosBand, 0);
    }

    public SmosL1ValidImage(Band smosBand, DownscalableImageSupport level0, int level) {
        super(level0, level, null);
        init(smosBand, level);
    }

    private void init(Band smosBand, int level) {
        this.smosBand = smosBand;
        ParameterBlock pb;

        pb = new ParameterBlock();
        pb.addSource(ImageManager.getInstance().getBandImage(smosBand, level));
        pb.add(new double[]{0.0});
        pb.add(new double[]{32000.0});
        pb.add(new double[]{255.0});
        RenderedOp op = JAI.create("Threshold", pb, null);

        pb = new ParameterBlock();
        pb.addSource(op);
        pb.add(new double[]{-32000.0});
        pb.add(new double[]{0.0});
        pb.add(new double[]{0.0});
        op = JAI.create("Threshold", pb, null);

        pb = new ParameterBlock();
        pb.addSource(op);
        pb.add(DataBuffer.TYPE_BYTE);
        op = JAI.create("Format", pb, null);

        rendering = op.getRendering();
    }

    @Override
    public DownscalableImage createDownscalableImage(int level) {
        return new SmosL1ValidImage(smosBand, getDownscalableImageSupport().getLevel0(), level);
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
        rendering.copyData(tile);
    }
}
