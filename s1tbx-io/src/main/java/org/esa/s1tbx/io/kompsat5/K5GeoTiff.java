/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.kompsat5;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by luis on 12/08/2016.
 */
public class K5GeoTiff implements K5Format {
    private final Kompsat5Reader reader;
    private final ProductReaderPlugIn readerPlugIn;
    //private final Kompsat5Reader reader;
    private Product product;
    private final ProductReader geoTiffReader;

    public K5GeoTiff(final ProductReaderPlugIn readerPlugIn, final Kompsat5Reader reader) {
        this.readerPlugIn = readerPlugIn;
        this.reader = reader;

        geoTiffReader = ProductIO.getProductReader("GeoTiff");
    }

    public Product open(final File inputFile) throws IOException {

        product = geoTiffReader.readProductNodes(inputFile, null);
        product.setFileLocation(inputFile);
        addAuxXML(product);
        addExtraBands(inputFile, product);
        product.setStartTime(getStartTime(product));
        product.setEndTime(getEndTime(product));

        return product;
    }
    private String getTime(final Product product, final String tag){
        final MetadataElement m = product.getMetadataRoot();
        MetadataElement eAux  = m.getElement("Auxiliary");
        MetadataElement eRoot = eAux.getElement("Root");

        //final String time = m.getElement("Auxilliary").getElement("Root").getAttributeString(tag);
        return eRoot.getAttributeString(tag);
    }
    private ProductData.UTC getStartTime(final Product product) {
        final String startTime = getTime(product, "DownlinkStartUTC");
        try{
            final ProductData.UTC utc = ProductData.UTC.parse(startTime, "yyyy-MM-dd HH:mm:ss");
            return utc;

        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    private ProductData.UTC getEndTime(final Product product){
        final String endTime = getTime(product, "DownlinkStopUTC");
        try{
            final ProductData.UTC utc = ProductData.UTC.parse(endTime, "yyyy-MM-dd HH:mm:ss");
            return utc;

        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    private String getPolarization(String fname){
        for (String p: new String [] {"HH", "HV", "VH", "VV"}) {
            if (fname.contains(p)) {
                return p;
            }
        }
        return null;
    }



    private void addExtraBands(final File inputFile, final Product product) throws IOException {
        final String name = inputFile.getName().toUpperCase();
        Band [] bands = product.getBands();
        final String [] polarizations = {"HH", "HV", "VH", "VV"};

        String polarization = getPolarization(name);
        HashMap<String, Band []> polarizationGroupedBands = new HashMap<>();
        product.setName(name);
        if(name.contains("I_SCS") ) {
            polarizationGroupedBands.put(polarization, new Band[]{bands[0], null});

            bands[0].setName("i_" + polarization);
            bands[0].setNoDataValue(0);

            final File[] files = inputFile.getParentFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    final String fname = file.getName().toUpperCase();
                    if (fname.endsWith(".TIF") && !fname.equals(name)) {
                        Product product2 = geoTiffReader.readProductNodes(file, null);
                        String polarization2 = getPolarization(fname);
                        Band b = product2.getBands()[0];
                        String bname = "";
                        if (fname.contains("I_SCS")){
                            bname = "i_" + polarization2;
                        } else if (fname.contains("Q_SCS")){
                            bname = "q_" + polarization2;
                        }
                        b.setName(bname);
                        product.addBand(b);
                        if(polarizationGroupedBands.containsKey(polarization2)){
                            Band [] b2 = polarizationGroupedBands.get(polarization2);
                            b2[1] = b;
                            polarizationGroupedBands.put(polarization2, b2);
                        }else{
                            Band [] b2 = new Band[]{b, null};
                            polarizationGroupedBands.put(polarization2, b2);
                        }
                    }
                }
                for (String p : polarizationGroupedBands.keySet()){
                    Band [] bandGroup = polarizationGroupedBands.get(p);
                    bandGroup[0].setNoDataValue(0);
                    bandGroup[1].setNoDataValue(0);

                    ReaderUtils.createVirtualIntensityBand(product, bandGroup[0], bandGroup[1], p);
                }
            }
        } else if (name.contains("Q_SCS")){
            polarizationGroupedBands.put(polarization, new Band[]{bands[0], null});
            bands[0].setName("q_" + polarization);

            final File[] files = inputFile.getParentFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    final String fname = file.getName().toUpperCase();
                    if (fname.endsWith(".TIF") && !fname.equals(name)) {
                        Product product2 = geoTiffReader.readProductNodes(file, null);
                        String polarization2 = getPolarization(fname);
                        Band b = product2.getBands()[0];
                        String bname = "";
                        if (fname.contains("I_SCS")){
                            bname = "i_" + polarization2;
                        } else if (fname.contains("Q_SCS")){
                            bname = "q_" + polarization2;
                        }
                        b.setName(bname);
                        product.addBand(b);
                        if(polarizationGroupedBands.containsKey(polarization2)){
                            Band [] b2 = polarizationGroupedBands.get(polarization2);
                            b2[1] = b;
                            polarizationGroupedBands.put(polarization2, b2);
                        }else{
                            Band [] b2 = new Band[]{b, null};
                            polarizationGroupedBands.put(polarization2, b2);
                        }
                    }
                }
                for (String p : polarizationGroupedBands.keySet()){
                    Band [] bandGroup = polarizationGroupedBands.get(p);
                    ReaderUtils.createVirtualIntensityBand(product, bandGroup[0], bandGroup[1], p);
                }
            }
        }
        else if (name.contains("_GEC_") ||name.contains("_GTC_") || name.contains("_WEC_") || name.contains("_WTC_")  ){

            bands[0].setName("Amplitude_" + polarization);
            reader.createVirtualIntensityBand(product, bands[0],"_" + polarization);
            final File[] files = inputFile.getParentFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    final String fname = file.getName().toUpperCase();
                    if (fname.endsWith(".TIF") && !fname.equals(name)) {
                        Product product2 = geoTiffReader.readProductNodes(file, null);
                        String polarization2 = getPolarization(fname);
                        Band b = product2.getBands()[0];
                        String bname = "Amplitude_" + polarization2;

                        b.setName(bname);
                        product.addBand(b);
                        reader.createVirtualIntensityBand(product, b, "_" + polarization2);

                    }
                }

            }
        }
    }

    public void close() throws IOException {
        if (product != null) {
            product = null;
        }
    }

    public void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                       int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                       int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                       ProgressMonitor pm) throws IOException {

        geoTiffReader.readBandRasterData(destBand, destOffsetX, destOffsetY, destWidth, destHeight, destBuffer, pm);
    }
}
