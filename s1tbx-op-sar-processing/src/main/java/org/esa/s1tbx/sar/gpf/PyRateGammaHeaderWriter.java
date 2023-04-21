package org.esa.s1tbx.sar.gpf;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;

import java.io.File;

public class PyRateGammaHeaderWriter {

    private final Product srcProduct;
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

    protected String toSentenceCase(String word){
        char [] chars = word.toUpperCase().toCharArray();
        for (int x = 1; x < chars.length; x++){
            chars[x] = Character.toLowerCase(chars[x]);
        }
        return String.valueOf(chars);
    }

    private String utcDateToGAMMATime(String date){
        // TODO figure out how gamma time format works
        return date.split(" ")[1];

    }


    // Write out the individual image metadata files as PyRate compatible metadata files,
    // store the file location in
    public File writeHeaderFiles(File destinationFolder, File headerListFile){


        if (headerListFile == null){
            return new File(destinationFolder.getParentFile(), "headers.txt");
        }else{
            return headerListFile;
        }


    }

    // Convert abstracted metadata into file contents for .PAR header.
    private String convertMetadataRootToPyRateGamma(MetadataElement root){
        StringBuilder contents = new StringBuilder("Gamma Interferometric SAR Processor (ISP) - Image Parameter File\n\n");
        String date = root.getAttributeString("first_line_time").split(" ")[0].replace("-", "");

        String stateVectors = "";
        int numStateVectors = 0;
        String firstStateVectorTime = "";
        for (MetadataElement element : root.getElements()){
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
        }

        String [] contentLines = new String[]{
                PyRateCommons.createTabbedVariableLine("title",root.getAttributeString("PRODUCT") ),
                PyRateCommons.createTabbedVariableLine("sensor", root.getAttributeString("ACQUISITION_MODE") + " " +
                        root.getAttributeString("SWATH") + " " + root.getAttributeString("mds1_tx_rx_polar")),
                PyRateCommons.createTabbedVariableLine("date", PyRateCommons.bandNameDateToPyRateDate(date, true)),
                PyRateCommons.createTabbedVariableLine("start_time", utcDateToGAMMATime(root.getAttributeString("first_line_time"))),
                PyRateCommons.createTabbedVariableLine("end_time", utcDateToGAMMATime(root.getAttributeString("last_line_time"))),
                PyRateCommons.createTabbedVariableLine("range_looks", root.getAttributeString("range_looks")),
                PyRateCommons.createTabbedVariableLine("azimuth_looks", root.getAttributeString("azimuth_looks")),
                PyRateCommons.createTabbedVariableLine("number_of_state_vectors", String.valueOf(numStateVectors)),
                PyRateCommons.createTabbedVariableLine("time_of_first_state_vector", firstStateVectorTime),
                PyRateCommons.createTabbedVariableLine("center_latitude", root.getAttributeString("centre_lat") + "\tdegrees"),
                PyRateCommons.createTabbedVariableLine("center_longitude", root.getAttributeString("centre_lon") + "\tdegrees"),
                PyRateCommons.createTabbedVariableLine("range_pixel_spacing", root.getAttributeString("range_spacing") + "\tm"),
                PyRateCommons.createTabbedVariableLine("azimuth_pixel_spacing", root.getAttributeString("azimuth_spacing") + "\tm"),
                PyRateCommons.createTabbedVariableLine("incidence_angle",
                        String.valueOf((root.getAttributeDouble("incidence_near") +
                        root.getAttributeDouble("incidence_far")) / 2)), // Use average of near and far incidence angle.
                PyRateCommons.createTabbedVariableLine("radar_frequency",
                        root.getAttributeDouble("radar_frequency") * 1000000 + "\tHz"),    // Radar frequency is stored as MHz in abstracted metadata.
                                                                                                 // Convert to Hz by multiplying by 1 million.
                PyRateCommons.createTabbedVariableLine("adc_sampling_rate",
                        root.getAttributeDouble("range_sampling_rate") * 1000000 + "\tHz"),// See above - conversion from MHz to Hz.

                PyRateCommons.createTabbedVariableLine("chirp_bandwidth",
                        root.getAttributeDouble("range_bandwidth") * 1000000 + "\tHz"),

                PyRateCommons.createTabbedVariableLine("azimuth_proc_bandwidth",
                        root.getAttributeString("azimuth_bandwidth")),

                PyRateCommons.createTabbedVariableLine("")






        };
        for (String line : contentLines){
            contents.append(line);
        }












        contents.append("center_time:\t" + "\n"); // TODO calculate avg between start and end time

        contents.append("image_format:\tFLOAT\n"); // Not sure if this should be constant. TODO determine if should be constant.

        // TODO find equivalent metadata for these parts.
        /*
        contents.append("line_header_size:\t\n");
        contents.append("range_samples:\t\n");
        contents.append("azimuth_lines:\t\n");
        contents.append("image_geometry:\t\n");
        contents.append("range_scale_factor:\t\n");
        contents.append("azimuth_scale_factor:\t\n");
        contents.append("heading:\t"); // Degrees?
        contents.append("near_range_slc:\t"); // meters
        contents.append("center_range_slc:\t"); // meters
        contents.append("far_range_slc:\t"); // meters
        contents.append("first_slant_range_polynomial:\t");
        contents.append("center_slant_range_polynomial:\t");
        contents.append("last_slant_range_polynomial:\t");
        contents.append("azimuth_deskew:\t");
        contents.append("azimuth_angle:\t");
        contents.append("prf:\t");
        contents.append("doppler_polynomial:\t");
        contents.append("doppler_poly_dot:\t");
        contents.append("doppler_poly_ddot");
        contents.append("receiver_gain");
        contents.append("calibration_gain");
        contents.append("sar_to_earth_center");
        contents.append("earth_radius_below_sensor");
        contents.append("earth_semi_major_axis");
        contents.append("earth_semi_minor_axis");



         */






        contents.append(stateVectors);

        return contents.toString();
    }


}
