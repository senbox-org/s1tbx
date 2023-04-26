package org.esa.s1tbx.sar.gpf;

import org.apache.commons.io.FileUtils;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.eo.GeoUtils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

// Class & functions for writing out & modifying the GAMMA header files to work with the external PyRATE InSAR software
// Written by Alex McVittie April 2023.
public class PyRateGammaHeaderWriter {

    private final Product srcProduct;

    private ArrayList<String> bannedDates = new ArrayList<>();
    final MetadataElement[] roots;
    public PyRateGammaHeaderWriter(Product product){
        this.srcProduct = product;
        final MetadataElement[] secondaryS = srcProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElements();
        roots = new MetadataElement[secondaryS.length + 1];
        roots[0] = srcProduct.getMetadataRoot().getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);
        for (int x = 0; x < secondaryS.length; x++){
            roots[x + 1] = secondaryS[x];
        }
    }

     // PyRate expects a couple extra pieces of metadata in the GAMMA headers. This method adjusts and adds these
     // missing fields.
     public static void adjustGammaHeader(Product product, File gammaHeader) throws IOException {
        String contents = FileUtils.readFileToString(gammaHeader, "utf-8");


        GeoPos geoPosUpperLeft = product.getSceneGeoCoding().getGeoPos( new PixelPos(0, 0), null);
        GeoPos geoPosLowerRight = product.getSceneGeoCoding().getGeoPos(new PixelPos(product.getSceneRasterWidth() - 1, product.getSceneRasterHeight() - 1), null);

        contents += "corner_lat:\t" + geoPosUpperLeft.lat + " decimal degrees";
        contents += "\ncorner_lon:\t" + geoPosUpperLeft.lon + " decimal degrees";
        contents += "\npost_lat:\t" + geoPosLowerRight.lat + " decimal degrees";
        contents += "\npost_lon:\t" + geoPosLowerRight.lon + " decimal degrees";
        contents += "\nellipsoid_name:\t WGS84";
        FileUtils.write(gammaHeader, contents);



    }

    private Date parseDate(String utcDateString) throws ParseException {
        Date date = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSSSSS").parse(utcDateString);
        return date;
    }

    // Gets average/middle point between two dates and returns as a string in dd-MMM-yyyy HH:mm:ss.SSSSSS format.
    private String getMiddleDate(Date date1, Date date2){

        long differenceBetweenDates = date2.getTime() - date1.getTime();

        date1.setTime(date1.getTime() + (differenceBetweenDates / 2));
        DateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSSSSS");

        return formatter.format(date1);
    }

    // Lifted from org.esa.s1tbx.calibration.gpf.calibrators
    private double getSatelliteToEarthCenterDistance(MetadataElement root) {

        final MetadataElement orbit_state_vectors = root.getElement(AbstractMetadata.orbit_state_vectors);
        final MetadataElement orbit_vector = orbit_state_vectors.getElement(AbstractMetadata.orbit_vector + 3);
        final float xpos = (float)orbit_vector.getAttributeDouble("x_pos");
        final float ypos = (float)orbit_vector.getAttributeDouble("y_pos");
        final float zpos = (float)orbit_vector.getAttributeDouble("z_pos");

        final double rSat = Math.sqrt(xpos * xpos + ypos * ypos + zpos * zpos); // in m
        if (Double.compare(rSat, 0.0) == 0) {
            throw new OperatorException("x, y and z positions in orbit_state_vectors are all zeros");
        }

        return rSat;
    }



    private String utcDateToGAMMATime(String date){
        // TODO figure out how gamma time format works
        return date.split(" ")[1];

    }

    private String getSensorName(MetadataElement root){
        return root.getAttributeString("ACQUISITION_MODE") + " " +
                root.getAttributeString("SWATH") + " " + root.getAttributeString("mds1_tx_rx_polar");
    }





    // Write out the individual image metadata files as PyRate compatible metadata files,
    // store the file location in
    public File writeHeaderFiles(File destinationFolder, File headerListFile) throws ParseException, IOException {
        if(headerListFile == null){
            headerListFile = new File(destinationFolder.getParentFile(), "headers.txt");
        }
        StringBuilder allHeaderFiles = new StringBuilder();
        ArrayList<MetadataElement> acceptableMetadataElements = new ArrayList<>();

        // Key: range sample value. Value: # of occurrences
        HashMap<Integer, Integer> rangeSampleCounts = new HashMap<>();
        int mostConsistentRangeSampleValue = 0; // Most common range sample value. Don't write anything that isn't this value.

        HashMap<Double, Integer> rangePixelSpacingCounts = new HashMap<>();
        double mostConsistentRangePixelSpacingCountValue = 0;

        for(MetadataElement root : roots){
            int rangeSample = root.getAttributeInt("num_samples_per_line");
            if(rangeSampleCounts.containsKey(rangeSample)){
                rangeSampleCounts.replace(rangeSample, rangeSampleCounts.get(rangeSample) + 1);
            }else{
                rangeSampleCounts.put(rangeSample, 1);
            }

            double rangePixelSpacing = root.getAttributeDouble("range_spacing");
            if(rangePixelSpacingCounts.containsKey(rangePixelSpacing)){
                rangePixelSpacingCounts.replace(rangePixelSpacing, rangePixelSpacingCounts.get(rangePixelSpacing) + 1);
            }else{
                rangePixelSpacingCounts.put(rangePixelSpacing, 1);
            }
        }
        int x = 0;
        for(double key : rangePixelSpacingCounts.keySet()){
            if(rangePixelSpacingCounts.get(key) > x){
                mostConsistentRangePixelSpacingCountValue = key;
                x = rangePixelSpacingCounts.get(key);
            }
        }
        x = 0;
        for(int key : rangeSampleCounts.keySet()){
            if(rangeSampleCounts.get(key) > x){
                mostConsistentRangeSampleValue = key;
                x = rangeSampleCounts.get(key);
            }
        }

        for(MetadataElement root : roots){
            if (root.getAttributeInt("num_samples_per_line") == mostConsistentRangeSampleValue &&
                root.getAttributeDouble("range_spacing") == mostConsistentRangePixelSpacingCountValue){
                acceptableMetadataElements.add(root);
            }else{
                bannedDates.add(PyRateCommons.bandNameDateToPyRateDate(root.getAttributeString("first_line_time").split(" ")[0].replace("-", ""), false));
            }

        }

        for(MetadataElement root : acceptableMetadataElements){
            String contents = convertMetadataRootToPyRateGamma(root);
            String fileNameDate = PyRateCommons.bandNameDateToPyRateDate(root.getAttributeString("first_line_time").split(" ")[0].replace("-", ""), false);
            String fileName = fileNameDate + ".par";
            FileUtils.write(new File(destinationFolder, fileName), contents);
            allHeaderFiles.append(destinationFolder.getName() + "/" + fileName + "\n");

        }
        FileUtils.write(headerListFile, allHeaderFiles.toString());
        return headerListFile;


    }

    // Convert abstracted metadata into file contents for .PAR header.
    private String convertMetadataRootToPyRateGamma(MetadataElement root) throws ParseException {
        StringBuilder contents = new StringBuilder("Gamma Interferometric SAR Processor (ISP) - Image Parameter File\n\n");
        String date = root.getAttributeString("first_line_time").split(" ")[0].replace("-", "");

        // String to store all GAMMA formatted orbit vectors into
        String stateVectors = "";

        // Count of the number of state vectors
        int numStateVectors = 0;

        // Store the first orbit vectors time in this string.
        String firstStateVectorTime = "";

        // GAMMA stores heading (antenna) angle as degrees. SNAP stores as right or left facing. Convert to an angle here.
        int antennaAngle;
        if(root.getAttributeString("antenna_pointing").equals("right")){
            antennaAngle = 90;
        }else{
            antennaAngle = -90;
        }

        // Format orbit vectors to how PyRATE wants them to be formatted in the GAMMA header.
        for (MetadataElement element : root.getElement("Orbit_State_Vectors").getElements()){
            if(element.getName().startsWith("orbit_vector")){
                String curStateVector = element.getName().replace("orbit_vector", "");
                if(curStateVector.equals("1")){
                    firstStateVectorTime = utcDateToGAMMATime(element.getAttributeString("time"));
                }
                String stateVectorPosition = element.getAttributeString("x_pos") + "\t" +
                        element.getAttributeString("y_pos") + "\t" +
                        element.getAttributeString("z_pos") + "\t" +
                        "m m m";
                String stateVectorVelocity = element.getAttributeString("x_vel") + "\t" +
                        element.getAttributeString("y_vel") + "\t" +
                        element.getAttributeString("z_vel") + "\t" +
                        "m/s m/s m/s";
                String vectorPositionName = "state_vector_position_" +
                        element.getName().replace("orbit_vector", "");
                String vectorVelocityName = "state_vector_velocity_" +
                        element.getName().replace("orbit_vector", "");
                stateVectors += vectorPositionName + ":\t" + stateVectorPosition + "\n";
                stateVectors += vectorVelocityName + ":\t" + stateVectorVelocity + "\n";
                numStateVectors++;
            }
        } // End of orbit formatting.

        // Initialize an array of strings for easy concatenation at the end.
        String [] contentLines = new String[]{
                PyRateCommons.createTabbedVariableLine("title",root.getAttributeString("PRODUCT") ),

                PyRateCommons.createTabbedVariableLine("sensor", getSensorName(root)),

                PyRateCommons.createTabbedVariableLine("date", PyRateCommons.bandNameDateToPyRateDate(date, true)),

                PyRateCommons.createTabbedVariableLine("start_time", utcDateToGAMMATime(root.getAttributeString("first_line_time"))),

                PyRateCommons.createTabbedVariableLine("center_time:",
                        utcDateToGAMMATime(
                                getMiddleDate(
                                        parseDate(root.getAttributeString("first_line_time")),
                                        parseDate(root.getAttributeString("last_line_time"))))),

                PyRateCommons.createTabbedVariableLine("end_time", utcDateToGAMMATime(root.getAttributeString("last_line_time"))),

                PyRateCommons.createTabbedVariableLine("range_looks", String.valueOf(root.getAttributeInt("range_looks"))),

                PyRateCommons.createTabbedVariableLine("azimuth_looks", String.valueOf(root.getAttributeInt("azimuth_looks"))),

                PyRateCommons.createTabbedVariableLine("number_of_state_vectors", String.valueOf(numStateVectors)),

                PyRateCommons.createTabbedVariableLine("time_of_first_state_vector", firstStateVectorTime),

                PyRateCommons.createTabbedVariableLine("center_latitude", root.getAttributeString("centre_lat") + "\tdegrees"),

                PyRateCommons.createTabbedVariableLine("center_longitude", root.getAttributeString("centre_lon") + "\tdegrees"),

                PyRateCommons.createTabbedVariableLine("range_pixel_spacing", root.getAttributeString("range_spacing") + "\tm"),

                PyRateCommons.createTabbedVariableLine("azimuth_pixel_spacing", root.getAttributeString("azimuth_spacing") + "\tm"),

                PyRateCommons.createTabbedVariableLine("incidence_angle",
                        (root.getAttributeDouble("incidence_near") +
                                root.getAttributeDouble("incidence_far")) / 2 + "\tdegrees"), // Use average of near and far incidence angle.



                PyRateCommons.createTabbedVariableLine("radar_frequency",
                        root.getAttributeDouble("radar_frequency") * 1000000 + "\tHz"),    // Radar frequency is stored as MHz in abstracted metadata.
                                                                                                 // Convert to Hz by multiplying by 1 million.
                PyRateCommons.createTabbedVariableLine("adc_sampling_rate",
                        root.getAttributeDouble("range_sampling_rate") * 1000000 + "\tHz"),// See above - conversion from MHz to Hz.

                PyRateCommons.createTabbedVariableLine("chirp_bandwidth",
                        root.getAttributeDouble("range_bandwidth") * 1000000 + "\tHz"),

                PyRateCommons.createTabbedVariableLine("azimuth_proc_bandwidth",
                        root.getAttributeString("azimuth_bandwidth")),

                PyRateCommons.createTabbedVariableLine("image_format", "FLOAT"), // Not sure if this should be constant.
                                                                                // TODO determine if should be constant.

                PyRateCommons.createTabbedVariableLine("heading", root.getAttributeString("centre_heading2") + "\tdegrees"),

                PyRateCommons.createTabbedVariableLine("azimuth_angle", antennaAngle + " degrees"),

                PyRateCommons.createTabbedVariableLine("range_samples", root.getAttributeString("num_samples_per_line")),

                PyRateCommons.createTabbedVariableLine("azimuth_lines", root.getAttributeString("num_output_lines")),

                PyRateCommons.createTabbedVariableLine("prf", root.getAttributeString("pulse_repetition_frequency") + "\tHz"),

                PyRateCommons.createTabbedVariableLine("near_range_slc", root.getAttributeString("slant_range_to_first_pixel") + "\tm"),

                PyRateCommons.createTabbedVariableLine("earth_semi_major_axis", GeoUtils.WGS84.a + "\tm"),

                PyRateCommons.createTabbedVariableLine("earth_semi_minor_axis", GeoUtils.WGS84.b + "\tm"),

                PyRateCommons.createTabbedVariableLine("sar_to_earth_center", getSatelliteToEarthCenterDistance(root) + "\tm")


        };
        for (String line : contentLines){
            contents.append(line);
        }
        contents.append("\n"+ stateVectors);

        return contents.toString();
    }


    public ArrayList<String> getBannedDates() {
        return bannedDates;
    }
}
