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
package org.esa.beam.cluster;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.media.jai.ROI;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.StringUtils;

import com.bc.ceres.core.ProgressMonitor;


class RoiCombiner {
    
    private ROI combinedRoi;
    
    public RoiCombiner(Band[] sourceBands, Band roiBand) throws IOException {
        createRoi(roiBand);
        createValidMaskRoi(sourceBands);
    }
    
    public ROI getCombinedRoi() {
        return combinedRoi;
    }
    
    private void createRoi(Band roiBand) {
        if (roiBand != null && roiBand.isROIUsable()) {
            try {
                combinedRoi = roiBand.createROI(ProgressMonitor.NULL);
            } catch (IOException e) {
                throw new OperatorException(e);
            }
        }
    }
    
    private void createValidMaskRoi(Band[] sourceBands) throws IOException {
        Set<String> validMaskSet = new HashSet<String>(sourceBands.length);
        for (int i = 0; i < sourceBands.length; i++) {
            Band band = sourceBands[i];
            String validExpression = band.getValidMaskExpression();
            if (StringUtils.isNotNullAndNotEmpty(validExpression)
                    && !validMaskSet.contains(validExpression)) {
                validMaskSet.add(validExpression);
                // using ValidMaskOpImage does not work correctly. Use workaround (mz, 03.07.2008)
                // PlanarImage noDataImage = new ValidMaskOpImage(band);
                BufferedImage noDataImage = createValidMaskImage(band);
                ROI noDataRoi = new ROI(noDataImage, 1);
                if(combinedRoi == null) {
                    combinedRoi = noDataRoi;
                } else {
                    combinedRoi = combinedRoi.intersect(noDataRoi);
                }
            }
        }
    }
    
    private static BufferedImage createValidMaskImage(RasterDataNode raster) throws IOException {
        final byte b00 = (byte) 0;
        final byte b01 = (byte) 1;
        final int width = raster.getSceneRasterWidth();
        final int height = raster.getSceneRasterHeight();
        final Color color = Color.WHITE;
        final IndexColorModel cm = new IndexColorModel(8, 2,
                                                       new byte[]{b00, (byte) color.getRed()},
                                                       new byte[]{b00, (byte) color.getGreen()},
                                                       new byte[]{b00, (byte) color.getBlue()},
                                                       0);
        final BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm);
        final byte[] imageDataBuffer = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        raster.ensureValidMaskComputed(ProgressMonitor.NULL);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (raster.isPixelValid(x, y)) {
                    imageDataBuffer[y * width + x] = b01;
                }
            }
        }
        return bi;
    }
}
