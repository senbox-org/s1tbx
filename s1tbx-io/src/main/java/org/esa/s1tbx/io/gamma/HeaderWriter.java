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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.OrbitStateVector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;

/**
 * Writer par header
 */
class HeaderWriter {

    private final File outputFile;
    private final Product srcProduct;
    private final MetadataElement absRoot;
    private String baseFileName;
    private boolean isComplex;
    private boolean isCoregistered;
    private final GammaProductWriter writer;
    private final ProductData.UTC dateDay;  // start date to the day

    private final static String sep = ":\t";
    private final static double daysToSeconds = 12 * 60 * 60;

    HeaderWriter(final GammaProductWriter writer, final Product srcProduct, final File userOutputFile) {
        this.writer = writer;
        this.srcProduct = srcProduct;
        this.isComplex = false;
        this.isCoregistered = false;

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

        Calendar cal = srcProduct.getStartTime().getAsCalendar();
        dateDay = new ProductData.UTC(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
    }

    String getBaseFileName() {
        return baseFileName;
    }

    void writeParFile() throws IOException {
        final String oldEOL = System.getProperty("line.separator");
        System.setProperty("line.separator", "\n");
        final FileOutputStream out = new FileOutputStream(outputFile);
        try(final PrintStream p = new PrintStream(out)) {

            p.println(GammaConstants.HEADER_KEY_NAME + sep + srcProduct.getName());
            p.println(GammaConstants.HEADER_KEY_SENSOR_TYPE + sep + absRoot.getAttributeString(AbstractMetadata.MISSION));
            p.println(GammaConstants.HEADER_KEY_DATE + sep + writeDate());
            p.println(GammaConstants.HEADER_KEY_START_TIME + sep + writeStartTime());
            p.println(GammaConstants.HEADER_KEY_CENTER_TIME + sep + writeCenterTime());
            p.println(GammaConstants.HEADER_KEY_END_TIME + sep + writeEndTime());
            p.println(GammaConstants.HEADER_KEY_LINE_TIME_INTERVAL + sep + absRoot.getAttributeString(AbstractMetadata.line_time_interval));
            p.println(GammaConstants.HEADER_KEY_SAMPLES + sep + srcProduct.getSceneRasterWidth());
            p.println(GammaConstants.HEADER_KEY_LINES + sep + srcProduct.getSceneRasterHeight());
            p.println(GammaConstants.HEADER_KEY_RANGE_LOOKS + sep + absRoot.getAttributeInt(AbstractMetadata.range_looks));
            p.println(GammaConstants.HEADER_KEY_AZIMUTH_LOOKS + sep + absRoot.getAttributeInt(AbstractMetadata.azimuth_looks));
            p.println(GammaConstants.HEADER_KEY_DATA_TYPE + sep + getDataType());
            p.println(GammaConstants.HEADER_KEY_IMAGE_GEOMETRY + sep + writeImageGeometry());
            writeCenterLatLon(p);
            p.println(GammaConstants.HEADER_KEY_RANGE_PIXEL_SPACING + sep + absRoot.getAttributeInt(AbstractMetadata.range_spacing));
            p.println(GammaConstants.HEADER_KEY_AZIMUTH_PIXEL_SPACING + sep + absRoot.getAttributeInt(AbstractMetadata.azimuth_spacing));
            p.println(GammaConstants.HEADER_KEY_RADAR_FREQUENCY + sep + absRoot.getAttributeString(AbstractMetadata.radar_frequency));
            p.println(GammaConstants.HEADER_KEY_PRF + sep + absRoot.getAttributeString(AbstractMetadata.pulse_repetition_frequency));
            writeOrbitStateVectors(p);

            p.flush();
        } catch (Exception e) {
            throw new IOException("GammaWriter unable to write par file " + e.getMessage());
        } finally {
            System.setProperty("line.separator", oldEOL);
        }
    }

    private String writeDate() {
        Calendar cal = srcProduct.getStartTime().getAsCalendar();
        return cal.get(Calendar.YEAR) + "  " + cal.get(Calendar.MONTH) + "  " + cal.get(Calendar.DAY_OF_MONTH);
    }

    private String writeStartTime() {
        double diff = srcProduct.getStartTime().getMJD() - dateDay.getMJD();
        double seconds = diff * daysToSeconds;
        return seconds + sep + "s";
    }

    private String writeCenterTime() {
        double center = (srcProduct.getStartTime().getMJD() +
                (srcProduct.getEndTime().getMJD() - srcProduct.getStartTime().getMJD())/2.0);
        double seconds = (center - dateDay.getMJD()) * daysToSeconds;
        return seconds + sep + "s";
    }

    private String writeEndTime() {
        double diff = srcProduct.getEndTime().getMJD() - dateDay.getMJD();
        double seconds = diff * daysToSeconds;
        return seconds + sep + "s";
    }

    private String writeImageGeometry() {
        if(absRoot.getAttributeString(AbstractMetadata.sample_type).equals("COMPLEX") ||
                absRoot.getAttributeInt(AbstractMetadata.srgr_flag, 0) == 0) {
            return "SLANT_RANGE";
        }
        return "GROUND_RANGE";
    }

    private void writeCenterLatLon(final PrintStream p) {
        GeoPos geoPos = srcProduct.getSceneGeoCoding().getGeoPos(
                new PixelPos(srcProduct.getSceneRasterWidth()/2, srcProduct.getSceneRasterHeight()/2), null);

        p.println(GammaConstants.HEADER_KEY_CENTER_LATITUDE + sep + geoPos.getLat() + sep + "degrees");
        p.println(GammaConstants.HEADER_KEY_CENTER_LONGITUDE + sep + geoPos.getLon() + sep + "degrees");
    }

    private void writeOrbitStateVectors(final PrintStream p) {
        final OrbitStateVector[] osvList = AbstractMetadata.getOrbitStateVectors(absRoot);
        if(osvList != null && osvList.length > 0) {
            double seconds = (osvList[0].time_mjd - dateDay.getMJD()) * daysToSeconds;
            double seconds2 = (osvList[1].time_mjd - dateDay.getMJD()) * daysToSeconds;
            double interval = seconds2 - seconds;

            p.println(GammaConstants.HEADER_KEY_NUM_STATE_VECTORS + sep + osvList.length);
            p.println(GammaConstants.HEADER_KEY_TIME_FIRST_STATE_VECTORS + sep + seconds + sep + "s");
            p.println(GammaConstants.HEADER_KEY_STATE_VECTOR_INTERVAL + sep + interval + sep + "s");

            int num = 1;
            for(OrbitStateVector osv : osvList) {
                p.println(GammaConstants.HEADER_KEY_STATE_VECTOR_POSITION +'_' + num + sep +
                    osv.x_pos +sep+ osv.y_pos +sep+ osv.z_pos +sep+ "m   m   m");
                p.println(GammaConstants.HEADER_KEY_STATE_VECTOR_VELOCITY +'_' + num + sep +
                        osv.x_vel +sep+ osv.y_vel +sep+ osv.z_vel +sep+ "m/s m/s m/s");
                ++num;
            }
        }
    }

    int getHighestElemSize() {
        int highestElemSize = 0;
        for(Band band : srcProduct.getBands()) {
            if(writer.shouldWrite(band)) {
                int elemSize = ProductData.getElemSize(band.getDataType());
                if(elemSize > highestElemSize) {
                    highestElemSize = elemSize;
                }
            }
        }
        return highestElemSize;
    }

    private String getDataType() {
        int highestElemSize = getHighestElemSize();

        if(highestElemSize >= 4) {
            return "FCOMPLEX";
        } else {
            return "SCOMPLEX";
        }
    }

    private File createParFile(final File file) {
        String name = FileUtils.getFilenameWithoutExtension(file);
        String ext = FileUtils.getExtension(name);
        String newExt = GammaConstants.PAR_EXTENSION;
        if (ext == null) {
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
