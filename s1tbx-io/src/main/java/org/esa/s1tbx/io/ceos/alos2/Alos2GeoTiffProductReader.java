/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.ceos.alos2;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.dataio.geotiff.GeoTiffProductReader;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import java.io.*;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Alos2GeoTiffProductReader extends GeoTiffProductReader {

    private Map<String, String> metadataSummary = null;
    private String imageFileName = null;
    private Product product;
    private ProductReader geoTiffReader = null;
    private GeoTiffProductReader reader = null;
    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyyMMdd HH:mm:ss");


    public Alos2GeoTiffProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        //this.readerPlugIn = readerPlugIn;
        this.reader = reader;

        geoTiffReader = ProductIO.getProductReader("GeoTiff");

    }

    @Override
    protected Product readProductNodesImpl() throws IOException {

        Path inputPath = ReaderUtils.getPathFromInput(getInput());
        File inputFile = inputPath.toFile();

        inputFile = findImageFile(inputFile);
        this.imageFileName = inputFile.getName();
        if (inputFile.getPath().toUpperCase().endsWith("ZIP")) {
            ZipFile productZip = new ZipFile(inputFile, ZipFile.OPEN_READ);
            final Enumeration<? extends ZipEntry> entries = productZip.entries();
            ArrayList<InputStream> bands = new ArrayList<>();
            ArrayList<String> bandNames = new ArrayList<>();
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                final String name = zipEntry.getName().toLowerCase();

                if (name.toUpperCase().contains("SUMMARY.TXT")) {
                    this.metadataSummary = metaDataFileToHashMap(productZip.getInputStream(zipEntry));

                }
                if (name.toUpperCase().endsWith("TIF") || name.toUpperCase().endsWith("TIFF")) {
                    bands.add(productZip.getInputStream(zipEntry));
                    bandNames.add(name);
                }
            }
            product = geoTiffReader.readProductNodes(bands.get(0), null);
            Band curBand = product.getBands()[0];
            curBand.setNoDataValue(0);

            String polarization = bandNames.get(0).substring(4, 6);
            curBand.setName("Amplitude_" + polarization);
            Band curBand_Intensity = new VirtualBand("Intensity_" + polarization,
                    ProductData.TYPE_FLOAT32,
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(),
                    "Amplitude_" + polarization + " * Amplitude_" + polarization);
            curBand_Intensity.setNoDataValue(0);
            for (int x = 1; x < bands.size(); x++) {
                Product nextBandProduct = geoTiffReader.readProductNodes(bands.get(x), null);
                polarization = bandNames.get(x).substring(4, 6);
                Band nextBand = nextBandProduct.getBands()[0];
                nextBand.setName("Amplitude_" + polarization);
                nextBand.setNoDataValue(0);
                product.addBand(nextBand);
                Band nextBand_Intensity = new VirtualBand("Intensity_" + polarization,
                        ProductData.TYPE_FLOAT32,
                        product.getSceneRasterWidth(),
                        product.getSceneRasterHeight(),
                        "Amplitude_" + polarization + " * Amplitude_" + polarization);
                nextBand_Intensity.setNoDataValue(0);
                product.addBand(nextBand_Intensity);
            }


        } else {
            this.metadataSummary = metaDataFileToHashMap(inputPath.getParent().resolve("summary.txt").toFile().getAbsolutePath());
            product = geoTiffReader.readProductNodes(inputFile, null);
            Band curBand = product.getBands()[0];
            String polarization = inputFile.getName().substring(4, 6);
            curBand.setName("Amplitude_" + polarization.toUpperCase());
            curBand.setNoDataValue(0);
            curBand.setNoDataValueUsed(true);

            Band curBand_Intensity = new VirtualBand("Intensity_" + polarization,
                    ProductData.TYPE_FLOAT32,
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(),
                    "Amplitude_" + polarization + " * Amplitude_" + polarization);
            curBand_Intensity.setNoDataValue(0);
            curBand_Intensity.setNoDataValueUsed(true);
            product.addBand(curBand_Intensity);

            // Read in all other bands
            for (File f : inputFile.getParentFile().listFiles()) {
                if (f.getName().toUpperCase().endsWith("TIF") || f.getName().toUpperCase().endsWith("TIFF")) {
                    if (!(f.getName().equals(inputFile.getName()))) {
                        Product nextBandProduct = geoTiffReader.readProductNodes(inputFile, null);
                        polarization = f.getName().substring(4, 6);
                        Band nextBand = nextBandProduct.getBands()[0];
                        nextBand.setName("Amplitude_" + polarization);
                        nextBand.setNoDataValue(0);
                        nextBand.setNoDataValueUsed(true);
                        product.addBand(nextBand);
                        Band nextBand_Intensity = new VirtualBand("Intensity_" + polarization,
                                ProductData.TYPE_FLOAT32,
                                product.getSceneRasterWidth(),
                                product.getSceneRasterHeight(),
                                "Amplitude_" + polarization + " * Amplitude_" + polarization);
                        nextBand_Intensity.setNoDataValue(0);
                        nextBand_Intensity.setNoDataValueUsed(true);
                        product.addBand(nextBand_Intensity);
                    }
                }
            }
        }

        product.setFileLocation(inputFile);
        product.setName(getProduct());

        addAbstractedMetadata(product);
        addOriginalMetaData(product);

        addGeoCoding(product, metadataSummary);

        return product;
    }

    private File findImageFile(File inputFile) {
        if (inputFile.getName().toLowerCase().endsWith(".zip")) {
            return inputFile;
        }

        final File[] files = inputFile.getParentFile().listFiles();
        if (files != null) {
            for (File f : files) {
                String name = f.getName().toUpperCase();
                if (name.contains("ALOS2") && (name.endsWith("TIF") || name.endsWith("TIFF")) &&
                        (name.contains("IMG-") &&
                                (name.contains("-HH-") || name.contains("-HV-") || name.contains("-VH-") || name.contains("-VV-")))) {
                    return f;
                }
            }
        }
        return null;
    }

    // Metadata reading with path to metadata file
    private Map<String, String> metaDataFileToHashMap(String fileName) throws IOException {
        // Return data structure
        Map<String, String> metaDataObject = new HashMap<>();

        // File connection
        File fileConnection = new File(fileName);
        BufferedReader fileBR = new BufferedReader(new FileReader(fileConnection));

        String curLine = null;
        while ((curLine = fileBR.readLine()) != null) {
            metaDataObject.put(curLine.split("\\=")[0].replace("\"", ""),
                    curLine.split("\\=")[1].replace("\"", ""));
        }
        fileBR.close();
        return metaDataObject;
    }

    // Metadata reading with input stream
    private Map<String, String> metaDataFileToHashMap(InputStream fileStream) throws IOException {
        // Return data structure
        Map<String, String> metaDataObject = new HashMap<>();

        BufferedReader fileBR = new BufferedReader(new InputStreamReader(fileStream));

        String curLine = null;
        while ((curLine = fileBR.readLine()) != null) {
            metaDataObject.put(curLine.split("\\=")[0].replace("\"", ""),
                    curLine.split("\\=")[1].replace("\"", ""));
        }
        fileBR.close();
        return metaDataObject;
    }

    private float getRangeSpacing() {
        return Float.parseFloat(metadataSummary.get("Pds_PixelSpacing"));
    }

    private float getAzimuthSpacing() {
        return Float.parseFloat(metadataSummary.get("Pds_PixelSpacing"));
    }

    // Confirmed methods

    protected String getMission() {
        return "ALOS2";
    }

    protected String getProduct() {
        // Gets product value from filename
        String firstSegment = this.imageFileName;
        String lastSegment = this.imageFileName;
        final String PREFIX = "ALOS2";
        final String ORBIT = "-ORBIT__";
        final String product;
        // Image file names start with IMG-{polarization - HH HV VH VV}, get rid of it
        if (lastSegment.startsWith("IMG-")) {
            lastSegment = lastSegment.substring(4, lastSegment.length() - 1);
            if (lastSegment.startsWith("H") || lastSegment.startsWith("V")) {
                lastSegment = lastSegment.substring(3, lastSegment.length() - 1);
            }
        }
        lastSegment = lastSegment.replace("ALOS-2", "ALOS2");
        int ind = lastSegment.indexOf("-");
        String lastPart = this.imageFileName.substring(this.imageFileName.lastIndexOf("-") - 7, this.imageFileName.lastIndexOf("-"));
        lastSegment = lastSegment.substring(0, lastSegment.indexOf("-")) + lastPart;

        firstSegment = firstSegment.replace(".",
                "_").substring(firstSegment.lastIndexOf("-"),
                firstSegment.length() - 4);

        product = PREFIX + firstSegment + ORBIT + lastSegment;

        return product;
    }

    protected String getProductType() {
        // Remove the .tif extension from the filename
        String product = this.imageFileName.substring(0, this.imageFileName.length() - 5);
        if (product.endsWith(".")) {
            product = product.substring(0, product.length() - 2);
        }
        if (product.endsWith("UA")) {
            product = product.substring(0, product.length() - 3); // Remove the UA at the end
        }
        // Remove the contents before the last dash - should result in the productName
        product = product.substring(product.lastIndexOf('-') + 1, product.length() - 1);


        return getMission() + '-' + product;
    }

    protected String getProcessingSystemIdentifier() {
        return metadataSummary.get("Lbi_ProcessFacility");
    }

    private int getNumOutputLines() {
        return Integer.parseInt(metadataSummary.get("Pdi_NoOfLines_0"));

    }

    // Gets the polarizations as found in the summary.txt file
    protected String[] getPolarizations() {
        // First, get all the keys that map to the names of files with polarization information
        Set<String> set = metadataSummary.keySet()
                .stream()
                .filter(s -> s.contains("ProductFileName"))
                .collect(Collectors.toSet());


        Set<String> polarizations = new HashSet<>();
        String curFileName;
        for (String k : set) {
            curFileName = metadataSummary.get(k);
            if (curFileName.startsWith("IMG-")) {
                polarizations.add(curFileName.substring(4, 6));
            }

        }
        return polarizations.toArray(new String[0]);
    }

    private int getNumSamplesPerLine() {
        return Integer.parseInt(metadataSummary.get("Pdi_NoOfPixels_0"));
    }

    private ProductData.UTC getTime(final String elem, final String tag) {

        return AbstractMetadata.parseUTC(elem, standardDateFormat);
    }

    public void addAbstractedMetadata(final Product product) {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(product.getMetadataRoot());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, getProduct());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, getProductType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, getMission());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, "STANDARD GEOCODED IMAGE");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier, getProcessingSystemIdentifier());

        // Get polarizations
        String[] polarizations = getPolarizations();

        try {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar, "-");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds2_tx_rx_polar, "-");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds3_tx_rx_polar, "-");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds4_tx_rx_polar, "-");
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar, polarizations[0]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds2_tx_rx_polar, polarizations[1]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds3_tx_rx_polar, polarizations[2]);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds4_tx_rx_polar, polarizations[3]);
        } catch (Exception e) {
            // Simply doesn't have four polarizations, three or less.
        }

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, getRangeSpacing());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, getAzimuthSpacing());
        String dateFormat = "yyyyMMdd HH:mm:ss";
        ProductData.UTC startTime = getTime(this.metadataSummary.get("Img_SceneStartDateTime"), "UTC");
        ProductData.UTC endTime = getTime(this.metadataSummary.get("Img_SceneEndDateTime"), "UTC");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, getNumOutputLines());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, getNumSamplesPerLine());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, endTime);


        // Radar frequency does not appear to be a constant - changes from sample dataset to sample dataset. Same with pulse repetition
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, (float) 1236.4997597467545);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing, "right");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency, 2122.318448518);

    }

    private void addOriginalMetaData(Product product) {

        final MetadataElement origRoot = AbstractMetadata.addOriginalProductMetadata(product.getMetadataRoot());
        for (Object o : this.metadataSummary.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            AbstractMetadata.setAttribute(origRoot, (String) pair.getKey(), (String) pair.getValue());
        }
    }

    private void addGeoCoding(final Product product, final Map<String, String> metadataSummary) {
        if (product.getSceneGeoCoding() != null) {
            return;
        }

        final float latUL = Float.parseFloat(metadataSummary.get("Img_ImageSceneLeftTopLatitude"));
        final float lonUL = Float.parseFloat(metadataSummary.get("Img_ImageSceneLeftTopLongitude"));
        final float latUR = Float.parseFloat(metadataSummary.get("Img_ImageSceneRightTopLatitude"));
        final float lonUR = Float.parseFloat(metadataSummary.get("Img_ImageSceneRightTopLongitude"));
        final float latLL = Float.parseFloat(metadataSummary.get("Img_ImageSceneLeftBottomLatitude"));
        final float lonLL = Float.parseFloat(metadataSummary.get("Img_ImageSceneLeftBottomLongitude"));
        final float latLR = Float.parseFloat(metadataSummary.get("Img_ImageSceneRightBottomLatitude"));
        final float lonLR = Float.parseFloat(metadataSummary.get("Img_ImageSceneRightBottomLongitude"));

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        absRoot.setAttributeDouble(AbstractMetadata.first_near_lat, latUL);
        absRoot.setAttributeDouble(AbstractMetadata.first_near_long, lonUL);
        absRoot.setAttributeDouble(AbstractMetadata.first_far_lat, latUR);
        absRoot.setAttributeDouble(AbstractMetadata.first_far_long, lonUR);
        absRoot.setAttributeDouble(AbstractMetadata.last_near_lat, latLL);
        absRoot.setAttributeDouble(AbstractMetadata.last_near_long, lonLL);
        absRoot.setAttributeDouble(AbstractMetadata.last_far_lat, latLR);
        absRoot.setAttributeDouble(AbstractMetadata.last_far_long, lonLR);

        final float[] latCorners = new float[]{latUL, latUR, latLL, latLR};
        final float[] lonCorners = new float[]{lonUL, lonUR, lonLL, lonLR};

        ReaderUtils.addGeoCoding(product, latCorners, lonCorners);
    }
}
