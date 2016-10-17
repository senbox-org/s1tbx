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
import org.esa.snap.engine_utilities.eo.GeoUtils;
import org.esa.snap.engine_utilities.eo.Constants;

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
    private ProductData.UTC dateDay;  // start date to the day

    private final static String sep = ":\t";
    private final static String tab = "\t";
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
        String dateStr = ""+ cal.get(Calendar.DAY_OF_MONTH) +'-'+ (cal.get(Calendar.MONTH)+1) +'-'+ cal.get(Calendar.YEAR);
        try {
            dateDay = ProductData.UTC.parse(dateStr, "dd-MM-yyyy");
        } catch (Exception e) {
            dateDay = srcProduct.getStartTime();
        }
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
            p.println(GammaConstants.HEADER_KEY_RANGE_PIXEL_SPACING + sep + absRoot.getAttributeInt(AbstractMetadata.range_spacing) + tab + "m");
            p.println(GammaConstants.HEADER_KEY_AZIMUTH_PIXEL_SPACING + sep + absRoot.getAttributeInt(AbstractMetadata.azimuth_spacing) + tab + "m");
            p.println(GammaConstants.HEADER_KEY_RADAR_FREQUENCY + sep + absRoot.getAttributeString(AbstractMetadata.radar_frequency)  + tab + "Hz");
            p.println(GammaConstants.HEADER_KEY_PRF + sep + absRoot.getAttributeString(AbstractMetadata.pulse_repetition_frequency)  + tab + "Hz");
            p.println(GammaConstants.HEADER_KEY_AZIMUTH_PROC_BANDWIDTH + sep + absRoot.getAttributeString(AbstractMetadata.azimuth_bandwidth)  + tab + "Hz");

            writeEarthParams(p);

            p.println(GammaConstants.HEADER_KEY_NEAR_RANGE_SLC + sep + absRoot.getAttributeString(AbstractMetadata.slant_range_to_first_pixel)  + tab + "m");
            p.println(GammaConstants.HEADER_KEY_CENTER_RANGE_SLC + sep + absRoot.getAttributeString(AbstractMetadata.slant_range_to_first_pixel)  + tab + "m");
            p.println(GammaConstants.HEADER_KEY_FAR_RANGE_SLC + sep + absRoot.getAttributeString(AbstractMetadata.slant_range_to_first_pixel)  + tab + "m");

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
        return cal.get(Calendar.YEAR) + "  " + (cal.get(Calendar.MONTH)+1) + "  " + cal.get(Calendar.DAY_OF_MONTH);
    }

    private String writeStartTime() {
        double diff = srcProduct.getStartTime().getMJD() - dateDay.getMJD();
        double seconds = diff * daysToSeconds;
        return seconds + tab + "s";
    }

    private String writeCenterTime() {
        double center = (srcProduct.getStartTime().getMJD() +
                (srcProduct.getEndTime().getMJD() - srcProduct.getStartTime().getMJD())/2.0);
        double seconds = (center - dateDay.getMJD()) * daysToSeconds;
        return seconds + tab + "s";
    }

    private String writeEndTime() {
        double diff = srcProduct.getEndTime().getMJD() - dateDay.getMJD();
        double seconds = diff * daysToSeconds;
        return seconds + tab + "s";
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

        p.println(GammaConstants.HEADER_KEY_CENTER_LATITUDE + sep + geoPos.getLat() + tab + "degrees");
        p.println(GammaConstants.HEADER_KEY_CENTER_LONGITUDE + sep + geoPos.getLon() + tab + "degrees");

        GeoPos geoPos2 = srcProduct.getSceneGeoCoding().getGeoPos(
                new PixelPos(srcProduct.getSceneRasterWidth()/2, (srcProduct.getSceneRasterHeight()/2) + 100), null);
        GeoUtils.DistanceHeading heading = GeoUtils.vincenty_inverse(geoPos.lat, geoPos.lon,
                geoPos2.lat, geoPos2.lon);
        p.println(GammaConstants.HEADER_KEY_HEADING + sep + heading.heading1 + tab + "degrees");
    }

    private void writeOrbitStateVectors(final PrintStream p) {
        final OrbitStateVector[] osvList = AbstractMetadata.getOrbitStateVectors(absRoot);
        if(osvList != null && osvList.length > 0) {
            double seconds = (osvList[0].time_mjd - dateDay.getMJD()) * daysToSeconds;
            double seconds2 = (osvList[1].time_mjd - dateDay.getMJD()) * daysToSeconds;
            double interval = seconds2 - seconds;

            p.println(GammaConstants.HEADER_KEY_NUM_STATE_VECTORS + sep + osvList.length);
            p.println(GammaConstants.HEADER_KEY_TIME_FIRST_STATE_VECTORS + sep + seconds + tab + "s");
            p.println(GammaConstants.HEADER_KEY_STATE_VECTOR_INTERVAL + sep + interval + tab + "s");

            int num = 1;
            for(OrbitStateVector osv : osvList) {
                p.println(GammaConstants.HEADER_KEY_STATE_VECTOR_POSITION +'_' + num + sep +
                    osv.x_pos + tab + osv.y_pos + tab + osv.z_pos + tab + "m   m   m");
                p.println(GammaConstants.HEADER_KEY_STATE_VECTOR_VELOCITY +'_' + num + sep +
                        osv.x_vel + tab + osv.y_vel + tab + osv.z_vel + tab + "m/s m/s m/s");
                ++num;
            }
        }
    }

    private void writeEarthParams(final PrintStream p) {

        final double startTime = srcProduct.getStartTime().getMJD();
        final OrbitStateVector[] osvList = AbstractMetadata.getOrbitStateVectors(absRoot);
        double sensorToEarth = 0.0;
        if(osvList != null && osvList.length > 0) {
            // maybe interpolation should be used here, for now we just pick the nearest orbit state vector
            double dtMin = Double.MAX_VALUE;
            int idx = 0;
            for (int i = 0; i < osvList.length; ++i) {
                final double dt = Math.abs(startTime - osvList[i].time_mjd);
                if (dt < dtMin) {
                    dtMin = dt;
                    idx = i;
                }
            }
            sensorToEarth = Math.sqrt(osvList[idx].x_pos * osvList[idx].x_pos +
                    osvList[idx].y_pos * osvList[idx].y_pos + osvList[idx].z_pos * osvList[idx].z_pos);
        }
        p.println(GammaConstants.HEADER_KEY_SAR_TO_EARTH_CENTER + sep + String.valueOf(sensorToEarth)  + tab + "m");

        GeoPos geoPos = srcProduct.getSceneGeoCoding().getGeoPos(
                new PixelPos(srcProduct.getSceneRasterWidth()/2, srcProduct.getSceneRasterHeight()/2), null);
        final double lat = geoPos.getLat();
        final double tmp1 = Constants.semiMajorAxis * Constants.semiMajorAxis * Math.cos(lat);
        final double tmp2 = Constants.semiMinorAxis * Constants.semiMinorAxis * Math.sin(lat);
        final double r = Math.sqrt((tmp1 * tmp1 + tmp2 * tmp2) / (tmp1 * Math.cos(lat) + tmp2 * Math.sin(lat)));

        p.println(GammaConstants.HEADER_KEY_EARTH_RADIUS_BELOW_SENSOR + sep + String.valueOf(r)  + tab + "m");
        p.println(GammaConstants.HEADER_KEY_EARTH_SEMI_MAJOR_AXIS + sep + String.valueOf(Constants.semiMajorAxis)  + tab + "m");
        p.println(GammaConstants.HEADER_KEY_EARTH_SEMI_MINOR_AXIS + sep + String.valueOf(Constants.semiMinorAxis)  + tab + "m");
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
