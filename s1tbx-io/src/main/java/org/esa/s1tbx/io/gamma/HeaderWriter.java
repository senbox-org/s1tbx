/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.gamma;

import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Writer par header
 */
public class HeaderWriter {

    private final File outputFile;
    private final Product srcProduct;
    private final MetadataElement absRoot;
    private String baseFileName;
    private boolean isComplex = false;
    private boolean isCoregistered = false;

    private final static String sep = ":\t";

    public HeaderWriter(final Product srcProduct, final File userOutputFile) {
        this.srcProduct = srcProduct;

        absRoot = AbstractMetadata.getAbstractedMetadata(srcProduct);
        if (absRoot != null) {
            try {
                isComplex = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE).equals("COMPLEX");
                isCoregistered = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.coregistered_stack);
            } catch (Exception e) {
                SystemUtils.LOG.severe("Unable to read metadata " + e.getMessage());
            }
        }
        this.outputFile = createParFile(userOutputFile);
        this.baseFileName = FileUtils.getFilenameWithoutExtension(this.outputFile);
    }

    public String getBaseFileName() {
        return baseFileName;
    }

    public void writeParFile() {
        PrintStream p = null;
        try {
            final FileOutputStream out = new FileOutputStream(outputFile);
            p = new PrintStream(out);

            p.println(GammaConstants.HEADER_KEY_NAME + sep + srcProduct.getName());
            p.println(GammaConstants.HEADER_KEY_SENSOR_TYPE + sep + absRoot.getAttributeString(AbstractMetadata.MISSION));
            p.println(GammaConstants.HEADER_KEY_SAMPLES + sep + srcProduct.getSceneRasterWidth());
            p.println(GammaConstants.HEADER_KEY_LINES + sep + srcProduct.getSceneRasterHeight());
            p.println(GammaConstants.HEADER_KEY_DATA_TYPE + sep + getDataType());
            p.println(GammaConstants.HEADER_KEY_LINE_TIME_INTERVAL + sep + absRoot.getAttributeString(AbstractMetadata.line_time_interval));
            p.println(GammaConstants.HEADER_KEY_RANGE_LOOKS + sep + absRoot.getAttributeInt(AbstractMetadata.range_looks));
            p.println(GammaConstants.HEADER_KEY_AZIMUTH_LOOKS + sep + absRoot.getAttributeInt(AbstractMetadata.azimuth_looks));
            p.println(GammaConstants.HEADER_KEY_RADAR_FREQUENCY + sep + absRoot.getAttributeString(AbstractMetadata.radar_frequency));
            p.println(GammaConstants.HEADER_KEY_PRF + sep + absRoot.getAttributeString(AbstractMetadata.pulse_repetition_frequency));

        } catch (Exception e) {
            System.out.println("GammaWriter unable to write par file " + e.getMessage());
        } finally {
            if (p != null)
                p.close();
        }
    }

    private String getDataType() {
        final Band band = srcProduct.getBandAt(0);
        int elemSize = ProductData.getElemSize(band.getDataType());
        if(elemSize >= 4) {
            return "FCOMPLEX";
        } else {
            return "SCOMPLEX";
        }
    }

    private File createParFile(final File file) {
        String name = FileUtils.getFilenameWithoutExtension(file);
        String ext = FileUtils.getExtension(name);
        String newExt = GammaConstants.PAR_EXTENSION;
        if (ext != null && !ext.endsWith("slc")) {
            if (isComplex) {
                if (isCoregistered) {
                    newExt = ".rslc" + newExt;
                } else {
                    newExt = ".slc" + newExt;
                }
            }
        }
        name += newExt;

        return new File(file.getParent(), name);
    }
}
