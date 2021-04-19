/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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
package org.esa.s1tbx.io.gamma.pyrate.pyrateheader;

import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.io.gamma.pyrate.PyRateGammaProductWriter;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.OrbitStateVector;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.eo.GeoUtils;

import java.io.*;
import java.util.Calendar;

public class PyRateHeaderWriter {
    protected final static String sep = ":\t";
    private final String date = "date";
    private final String frequency = "radar_frequency";
    private final String incidence = "incidence_angle";
    private final String frequency_unit = " Hz";
    private final String incidence_unit = " degrees";
    final static String PAR_EXTENSION = ".par";
    final static String tab = "\t";

    private final PyRateGammaProductWriter writer;
    protected final Product srcProduct;
    private boolean isComplex;
    private boolean isCoregistered;
    protected final MetadataElement absRoot;
    protected final File outputFile;
    protected  File [] outputFiles;
    protected String baseFileName;
    private ProductData.UTC dateDay;  // start date to the day
    public boolean isPhase = false;
    private final static double daysToSeconds = 12 * 60 * 60;


    public PyRateHeaderWriter(PyRateGammaProductWriter writer, Product srcProduct, File userOutputFile) {
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
        if(isPhase){
            this.outputFiles = createParFiles(userOutputFile);
        }else{
            this.outputFiles = null;
        }
        this.baseFileName = FileUtils.getFilenameWithoutExtension(this.outputFile);

        if (srcProduct.getStartTime() != null) {
            Calendar cal = srcProduct.getStartTime().getAsCalendar();
            String dateStr = String.valueOf(cal.get(Calendar.DAY_OF_MONTH)) + '-' + (cal.get(Calendar.MONTH) + 1) + '-' + cal.get(Calendar.YEAR);
            try {
                dateDay = ProductData.UTC.parse(dateStr, "dd-MM-yyyy");
            } catch (Exception e) {
                dateDay = srcProduct.getStartTime();
            }
        }
    }

    private File createParFile(final File file) {
        String name = FileUtils.getFilenameWithoutExtension(file);
        String ext = FileUtils.getExtension(name);
        String newExt = this.PAR_EXTENSION;
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

    private File [] createParFiles(File file){
        try{
            File master = new File(file.getParentFile().getAbsolutePath() + "/" + getDateStringKey(srcProduct, true) + "_slc" + this.PAR_EXTENSION);
            File slave = new File(file.getParentFile().getAbsolutePath() +  "/" + getDateStringKey(srcProduct, false) + "_slc" + this.PAR_EXTENSION);
            return new File[]{master,slave,file};
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public void writeParFile() throws IOException {
        if (isPhase){
            if (outputFiles == null){
                outputFiles = createParFiles(outputFile); // avoid nullpointer exception
            }
            // Write two Par files
            writeHeaders(srcProduct, outputFiles);
        }
        // Write normal header as well
        /*
        final String oldEOL = System.getProperty("line.separator");
        System.setProperty("line.separator", "\n");
        final FileOutputStream out = new FileOutputStream(outputFile);
        try (final PrintStream p = new PrintStream(out)) {

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
            p.println(GammaConstants.HEADER_KEY_RANGE_PIXEL_SPACING + sep + absRoot.getAttributeInt(AbstractMetadata.range_spacing) + tab + 'm');
            p.println(GammaConstants.HEADER_KEY_AZIMUTH_PIXEL_SPACING + sep + absRoot.getAttributeInt(AbstractMetadata.azimuth_spacing) + tab + 'm');
            p.println(GammaConstants.HEADER_KEY_RADAR_FREQUENCY + sep + (absRoot.getAttributeDouble(AbstractMetadata.radar_frequency) * Constants.oneMillion) + tab + "Hz");
            p.println(GammaConstants.HEADER_KEY_PRF + sep + absRoot.getAttributeString(AbstractMetadata.pulse_repetition_frequency) + tab + "Hz");
            p.println(GammaConstants.HEADER_KEY_AZIMUTH_PROC_BANDWIDTH + sep + absRoot.getAttributeString(AbstractMetadata.azimuth_bandwidth) + tab + "Hz");

            writeEarthParams(p);

            p.println(GammaConstants.HEADER_KEY_NEAR_RANGE_SLC + sep + absRoot.getAttributeString(AbstractMetadata.slant_range_to_first_pixel) + tab + 'm');
            p.println(GammaConstants.HEADER_KEY_CENTER_RANGE_SLC + sep + absRoot.getAttributeString(AbstractMetadata.slant_range_to_first_pixel) + tab + 'm');
            p.println(GammaConstants.HEADER_KEY_FAR_RANGE_SLC + sep + absRoot.getAttributeString(AbstractMetadata.slant_range_to_first_pixel) + tab + 'm');





            writeOrbitStateVectors(p);

            p.flush();
        } catch (Exception e) {
            throw new IOException("GammaWriter unable to write par file " + e.getMessage());
        } finally {
            System.setProperty("line.separator", oldEOL);
        }
        */

    }

    public int getHighestElemSize() {
        int highestElemSize = 0;
        for (Band band : srcProduct.getBands()) {
            if (writer.shouldWrite(band)) {
                int elemSize = ProductData.getElemSize(band.getDataType());
                if (elemSize > highestElemSize) {
                    highestElemSize = elemSize;
                }
            }
        }
        return highestElemSize;
    }

    public void writeHeaders(Product phaseProduct, File [] files) throws IOException{
        String mstDate = getDateString(phaseProduct, true);
        String slvDate = getDateString(phaseProduct, false);

        double mstAngle = getIncidenceAngle(phaseProduct, true);
        double slvAngle = getIncidenceAngle(phaseProduct, false);

        double mstFreq = getRadarFrequencyHz(phaseProduct, true);
        double slvFreq = getRadarFrequencyHz(phaseProduct, false);

        writeHeaderFile(mstDate, mstFreq, mstAngle, files[0]);
        writeHeaderFile(slvDate, slvFreq, slvAngle, files[1]);

    }

    private void writeHeaderFile(String date, double frequency, double incidenceAngle, File outputFile) throws IOException {
        final String oldEOL = System.getProperty("line.separator");
        System.setProperty("line.separator", "\n");
        final FileOutputStream out = new FileOutputStream(outputFile);
        try (final PrintStream p = new PrintStream(out)) {
            p.println(this.date + sep + date);
            p.println(this.frequency + sep + frequency + this.frequency_unit);
            p.println(this.incidence + sep + incidenceAngle + this.incidence_unit);
            p.flush();
        } catch (Exception e) {
            throw new IOException("GammaWriter unable to write par file " + e.getMessage());
        } finally {
            System.setProperty("line.separator", oldEOL);
        }
    }

    private double getRadarFrequencyHz(Product phaseProduct, boolean master) throws IOException {
        if (! phaseProduct.getMetadataRoot().containsElement(AbstractMetadata.SLAVE_METADATA_ROOT)){
            throw new IOException("Product is not a coregistered slave/master pair.");
        }

        MetadataElement absRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);

        MetadataElement slvRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements()[0];
        if (absRoot.containsAttribute("multimaster_split")){
            absRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements()[0];
            slvRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements()[1];
        }
        double freq;
        if(master){
            freq = absRoot.getAttributeDouble("radar_frequency") * 1000000;
        }else{
            freq = slvRoot.getAttributeDouble("radar_frequency") * 1000000;
        }
        return freq;
    }

    private double getIncidenceAngle(Product phaseProduct, boolean master) throws IOException{
        if (! phaseProduct.getMetadataRoot().containsElement(AbstractMetadata.SLAVE_METADATA_ROOT)){
            throw new IOException("Product is not a coregistered slave/master pair.");
        }

        MetadataElement absRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);

        MetadataElement slvRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements()[0];
        if (absRoot.containsAttribute("multimaster_split")){
            absRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements()[0];
            slvRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements()[1];
        }
        double [] angles = new double[2];
        if(master){
            angles[0] = absRoot.getAttributeDouble("incidence_near");
            angles[1] = absRoot.getAttributeDouble("incidence_far");
        }else{
            angles[0] = slvRoot.getAttributeDouble("incidence_near");
            angles[1] = slvRoot.getAttributeDouble("incidence_far");
        }
        // Return average of near and far angles TODO assess if this is the correct approach
        return (angles[0] + angles[1]) / 2;
    }

    public static  String getDoubleDateString(Product phaseProduct) throws IOException{
        return getDateStringKey(phaseProduct, true) + "-" + getDateStringKey(phaseProduct, false);
    }

    public static String getDateStringKey(Product phaseProduct, boolean master) throws IOException {
        if (! phaseProduct.getMetadataRoot().containsElement(AbstractMetadata.SLAVE_METADATA_ROOT)){
            throw new IOException("Product is not a coregistered slave/master pair.");
        }

        MetadataElement absRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);

        MetadataElement slvRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements()[0];
        if (absRoot.containsAttribute("multimaster_split")){
            absRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements()[0];
            slvRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements()[1];
        }
        String dateString = "";


        String svt = "";
        if(master){
            // Get master time
            svt = absRoot.getAttributeString("STATE_VECTOR_TIME");
        }else{
            //Get slave data
            svt = slvRoot.getAttributeString("STATE_VECTOR_TIME");
        }
        String d = svt.split(" ")[0];

        String day = d.split("-")[0];
        String month = getMonthNum(d.split("-")[1]);
        String year = d.split("-")[2];


        dateString = year + month  + day;
        return dateString;
    }

    private static String getDateString(Product phaseProduct, boolean master) throws IOException{
        MetadataElement absRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);
        String dateString = "";
        if (! phaseProduct.getMetadataRoot().containsElement(AbstractMetadata.SLAVE_METADATA_ROOT)){
            throw new IOException("Product is not a coregistered slave/master pair.");
        }
        MetadataElement slvRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements()[0];
        if (absRoot.containsAttribute("multimaster_split")){
            absRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements()[0];
            slvRoot = phaseProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements()[1];
        }
        String svt = "";
        if(master){
            // Get master time
            svt = absRoot.getAttributeString("STATE_VECTOR_TIME");
        }else{
            //Get slave data
            svt = slvRoot.getAttributeString("STATE_VECTOR_TIME");
        }
        String d = svt.split(" ")[0];
        String t = svt.split(" ")[1];

        String day = d.split("-")[0];
        String month = getMonthNum(d.split("-")[1]);
        String year = d.split("-")[2];

        String hour = t.split(":")[0];
        String minute = t.split(":")[1];
        String second = t.split(":")[2];

        dateString = year + " " + month + " " + day + " "
                + hour + " " + minute + " " + second;
        return dateString;

    }

    private static String getMonthNum (String m){
        switch(m){
            case "JAN":
                return "01";
            case "FEB":
                return "02";
            case "MAR":
                return "03";
            case "APR":
                return "04";
            case "MAY":
                return "05";
            case "JUN":
                return "06";
            case "JUL":
                return "07";
            case "AUG":
                return "08";
            case "SEP":
                return "09";
            case "OCT":
                return "10";
            case "NOV":
                return "11";
            default:
                return "12";


        }
    }

    private String writeDate() {
        if (srcProduct.getStartTime() != null) {
            Calendar cal = srcProduct.getStartTime().getAsCalendar();
            return cal.get(Calendar.YEAR) + "  " + (cal.get(Calendar.MONTH) + 1) + "  " + cal.get(Calendar.DAY_OF_MONTH);
        }
        return "";
    }

    private String writeStartTime() {
        if (srcProduct.getStartTime() != null) {
            double diff = srcProduct.getStartTime().getMJD() - dateDay.getMJD();
            double seconds = diff * daysToSeconds;
            return seconds + tab + 's';
        }
        return "";
    }

    private String writeCenterTime() {
        if (srcProduct.getStartTime() != null) {
            double center = (srcProduct.getStartTime().getMJD() +
                    (srcProduct.getEndTime().getMJD() - srcProduct.getStartTime().getMJD()) / 2.0);
            double seconds = (center - dateDay.getMJD()) * daysToSeconds;
            return seconds + tab + 's';
        }
        return "";
    }

    private String writeEndTime() {
        if (srcProduct.getEndTime() != null) {
            double diff = srcProduct.getEndTime().getMJD() - dateDay.getMJD();
            double seconds = diff * daysToSeconds;
            return seconds + tab + 's';
        }
        return "";
    }

    private String writeImageGeometry() {
        if (absRoot.getAttributeString(AbstractMetadata.sample_type).equals("COMPLEX") ||
                absRoot.getAttributeInt(AbstractMetadata.srgr_flag, 0) == 0) {
            return "SLANT_RANGE";
        }
        return "GROUND_RANGE";
    }

    private void writeCenterLatLon(final PrintStream p) {
        GeoPos geoPos = srcProduct.getSceneGeoCoding().getGeoPos(
                new PixelPos(srcProduct.getSceneRasterWidth() / 2, srcProduct.getSceneRasterHeight() / 2), null);

        p.println(GammaConstants.HEADER_KEY_CENTER_LATITUDE + sep + geoPos.getLat() + tab + "degrees");
        p.println(GammaConstants.HEADER_KEY_CENTER_LONGITUDE + sep + geoPos.getLon() + tab + "degrees");

        GeoPos geoPos2 = srcProduct.getSceneGeoCoding().getGeoPos(
                new PixelPos(srcProduct.getSceneRasterWidth() / 2, (srcProduct.getSceneRasterHeight() / 2) + 100), null);
        GeoUtils.DistanceHeading heading = GeoUtils.vincenty_inverse(geoPos, geoPos2);
        p.println(GammaConstants.HEADER_KEY_HEADING + sep + heading.heading1 + tab + "degrees");
    }

    private void writeOrbitStateVectors(final PrintStream p) {
        final OrbitStateVector[] osvList = AbstractMetadata.getOrbitStateVectors(absRoot);
        if (osvList != null && osvList.length > 0) {
            double seconds = (osvList[0].time_mjd - dateDay.getMJD()) * daysToSeconds;
            double seconds2 = (osvList[1].time_mjd - dateDay.getMJD()) * daysToSeconds;
            double interval = seconds2 - seconds;

            p.println(GammaConstants.HEADER_KEY_NUM_STATE_VECTORS + sep + osvList.length);
            p.println(GammaConstants.HEADER_KEY_TIME_FIRST_STATE_VECTORS + sep + seconds + tab + 's');
            p.println(GammaConstants.HEADER_KEY_STATE_VECTOR_INTERVAL + sep + interval + tab + 's');

            int num = 1;
            for (OrbitStateVector osv : osvList) {
                p.println(GammaConstants.HEADER_KEY_STATE_VECTOR_POSITION + '_' + num + sep +
                        osv.x_pos + tab + osv.y_pos + tab + osv.z_pos + tab + "m   m   m");
                p.println(GammaConstants.HEADER_KEY_STATE_VECTOR_VELOCITY + '_' + num + sep +
                        osv.x_vel + tab + osv.y_vel + tab + osv.z_vel + tab + "m/s m/s m/s");
                ++num;
            }
        }
    }

    private void writeEarthParams(final PrintStream p) {

        if (srcProduct.getStartTime() == null) {
            return;
        }
        final double startTime = srcProduct.getStartTime().getMJD();
        final OrbitStateVector[] osvList = AbstractMetadata.getOrbitStateVectors(absRoot);
        double sensorToEarth = 0.0;
        if (osvList != null && osvList.length > 0) {
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
        p.println(GammaConstants.HEADER_KEY_SAR_TO_EARTH_CENTER + sep + String.valueOf(sensorToEarth) + sep + 'm');

        GeoPos geoPos = srcProduct.getSceneGeoCoding().getGeoPos(
                new PixelPos(srcProduct.getSceneRasterWidth() / 2, srcProduct.getSceneRasterHeight() / 2), null);
        final double lat = geoPos.getLat();
        final double tmp1 = Constants.semiMajorAxis * Constants.semiMajorAxis * FastMath.cos(lat);
        final double tmp2 = Constants.semiMinorAxis * Constants.semiMinorAxis * FastMath.sin(lat);
        final double r = Math.sqrt((tmp1 * tmp1 + tmp2 * tmp2) / (tmp1 * FastMath.cos(lat) + tmp2 * FastMath.sin(lat)));

        p.println(GammaConstants.HEADER_KEY_EARTH_RADIUS_BELOW_SENSOR + sep + String.valueOf(r) + sep + 'm');
        p.println(GammaConstants.HEADER_KEY_EARTH_SEMI_MAJOR_AXIS + sep + String.valueOf(Constants.semiMajorAxis) + sep + 'm');
        p.println(GammaConstants.HEADER_KEY_EARTH_SEMI_MINOR_AXIS + sep + String.valueOf(Constants.semiMinorAxis) + sep + 'm');
    }

    protected String getDataType() {
        int highestElemSize = getHighestElemSize();

        if (highestElemSize >= 4) {
            return "FCOMPLEX";
        } else {
            return "SCOMPLEX";
        }
    }

}

