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
import org.esa.beam.jai.LevelOpImage;
import org.esa.beam.jai.SingleBandedOpImage;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;

public class SmosL1ValidImage extends SingleBandedOpImage {

    private final Band smosBand;
    private PlanarImage rendering;

    public SmosL1ValidImage(Band smosBand, int level) {
        super(DataBuffer.TYPE_BYTE,
              smosBand.getSceneRasterWidth(),
              smosBand.getSceneRasterHeight(),
              smosBand.getProduct().getPreferredTileSize(), null, level,
              null);

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
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
        rendering.copyData(tile);
    }

    @Override
    protected LevelOpImage createDownscaledImage(int level) {
        return new SmosL1ValidImage(smosBand, level);
    }

}
