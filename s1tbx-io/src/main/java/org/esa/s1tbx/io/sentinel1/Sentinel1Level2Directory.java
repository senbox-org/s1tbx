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
package org.esa.s1tbx.io.sentinel1;

import org.esa.s1tbx.commons.io.XMLProductDirectory;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import java.io.File;
import java.io.IOException;

/**
 * This class represents a product directory.
 */
public class Sentinel1Level2Directory extends XMLProductDirectory implements Sentinel1Directory {

    private Sentinel1OCNReader OCNReader = null;

    public Sentinel1Level2Directory(final File inputFile) {
        super(inputFile);
    }

    protected String getHeaderFileName() {
        return Sentinel1Constants.PRODUCT_HEADER_NAME;
    }

    protected String getRelativePathToImageFolder() {
        return getRootFolder() + "measurement" + '/';
    }

    protected void addImageFile(final String imgPath, final MetadataElement newRoot) throws IOException {
        final String name = getBandFileNameFromImage(imgPath);
        if (name.endsWith(".nc")) {
            if (OCNReader == null) {
                OCNReader = new Sentinel1OCNReader(this);
            }
            File file = new File(getBaseDir(), imgPath);
            if(isCompressed()) {
                file = getFile(imgPath);
            }
            OCNReader.addImageFile(file, name);
        }
    }

    @Override
    protected void addBands(final Product product) {

        OCNReader.addNetCDFBands(product);
    }

    @Override
    protected void addAbstractedMetadataHeader(final MetadataElement root) throws IOException {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(root);

        Sentinel1Level1Directory.addManifestMetadata(getProductName(), absRoot, origProdRoot, true);
        final String acqMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        setSLC(absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE).equals("COMPLEX"));

        // get metadata for each band
        addBandAbstractedMetadata(origProdRoot);
    }

    private void addBandAbstractedMetadata(final MetadataElement origProdRoot) throws IOException {

        MetadataElement annotationElement = origProdRoot.getElement("annotation");
        if (annotationElement == null) {
            annotationElement = new MetadataElement("annotation");
            origProdRoot.addElement(annotationElement);
        }

        if (OCNReader != null) {
            // add netcdf metadata for OCN product
            OCNReader.addNetCDFMetadata(annotationElement);
        }
    }

    @Override
    protected void addTiePointGrids(final Product product) {
        // replaced by call to addTiePointGrids(band)
    }

    @Override
    protected String getProductName() {
        String name = getBaseName();
        if (name.toUpperCase().endsWith(".SAFE"))
            return name.substring(0, name.length() - 5);
        else if (name.toUpperCase().endsWith(".ZIP"))
            return name.substring(0, name.length() - 4);
        return name;
    }

    protected String getProductType() {
        return "Level-2 OCN";
    }

    public Sentinel1OCNReader getOCNReader() {
        return OCNReader;
    }

    public ProductData.UTC getTime(final MetadataElement elem, final String tag) {

        String start = elem.getAttributeString(tag, AbstractMetadata.NO_METADATA_STRING);
        start = start.replace("T", "_");

        return AbstractMetadata.parseUTC(start, sentinelDateFormat);
    }

    @Override
    public Product createProduct() throws IOException {

        // If addMetaData() is called before findImages(), the annotation will not show up in the display.
        // This is because...
        // in addBandAbstractedMetadata() (which is called by addAbstractedMetadataHeader() which is called by
        // addMetaData()), for it to add annotations to metadata, OCNReader has to have already been created by
        // addImageFile() (which is called by findImages()).
        findImages(null);
        final MetadataElement newRoot = addMetaData();

        final MetadataElement absRoot = newRoot.getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);

        // They are 99999 which is NOT correct.
        //final int sceneWidth = absRoot.getAttributeInt(AbstractMetadata.num_samples_per_line);
        //final int sceneHeight = absRoot.getAttributeInt(AbstractMetadata.num_output_lines);

        // Only implemented for SM(?), IW and EW, NOT for WV
        // SM, IW and EW only have one .nc file while WV has 1 per vignette
        // For WV, the calls return -1. In that case just use AbstractMetadata.num_samples_per_line
        // and AbstractMetadata.num_output_lines but they are 99999 which is wrong.
        int sceneWidth = OCNReader.getSceneWidth();
        int sceneHeight = OCNReader.getSceneHeight();
        if (sceneWidth < 0) {
            sceneWidth = absRoot.getAttributeInt(AbstractMetadata.num_samples_per_line);
        }
        if (sceneHeight < 0) {
            sceneHeight = absRoot.getAttributeInt(AbstractMetadata.num_output_lines);
        }

        final Product product = new Product(getProductName(), getProductType(), sceneWidth, sceneHeight);
        updateProduct(product, newRoot);

        addBands(product);
        addGeoCoding(product);

        ReaderUtils.addMetadataProductSize(product);

        OCNReader.addWindDataToVectorNodes(product);
        //OCNReader.addOSWDataToVectorNode(product);

        return product;
    }

    public void addGeoCodingToBands(final Product product) {

        OCNReader.addGeoCodingToBands(product);
    }

    @Override
    protected void addGeoCoding(final Product product) {

        float minLat = 999F;
        float maxLat = -999F;
        float minLon = 999F;
        float maxLon = -999F;

        final MetadataElement elem = getMetadataObject(AbstractMetadata.getOriginalProductMetadata(product), "measurementFrameSet");

        if (elem != null) {

            final MetadataElement frameSet = elem.getElement("metadataWrap").getElement("xmlData").getElement("frameSet");
            final MetadataElement[] frames = frameSet.getElements();

            for (MetadataElement frame : frames) {

                final MetadataAttribute coordinates = frame.getElement("footPrint").getAttribute("coordinates");
                final String coordinatesStr = coordinates.getData().getElemString();

                //System.out.println("Sentinel1Level2Directory.addGeoCodingForLevel2Products: coordinates = " + coordinatesStr);

                final String[] latLonPairsStr = coordinatesStr.split(" ");

                for (String s : latLonPairsStr) {

                    final String[] latStrLonStr = s.split(",");

                    final float lat = Float.parseFloat(latStrLonStr[0]);
                    final float lon = Float.parseFloat(latStrLonStr[1]);

                    if (lat < minLat) {
                        minLat = lat;
                    }
                    if (lat > maxLat) {
                        maxLat = lat;
                    }
                    if (lon < minLon) {
                        minLon = lon;
                    }
                    if (lon > maxLon) {
                        maxLon = lon;
                    }
                }
            }

            //System.out.println("Sentinel1Level2Directory.addGeoCodingForLevel2Products: minLat = " + minLat + " maxLat = " + maxLat + " minLon = " + minLon + " maxLon = " + maxLon);
        }

        if (minLat > maxLat || minLon > maxLon) {
            SystemUtils.LOG.severe("Sentinel1Level2Directory.addGeoCodingForLevel2Products: ERROR failed to get valid footprint");
            return;
        }

        final float[] latCorners = new float[4];
        final float[] lonCorners = new float[latCorners.length];

        // The footprint
        // index 0                                          index 1
        // (maxLat, minLon)                                 (maxLat, maxLon)
        //      -----------------------------------------------------
        //      |                                                   |
        //      |                                                   |
        //      |                                                   |
        //      -----------------------------------------------------
        // (minLat, minLon)                                 (minLat, maxLon)
        // index 2                                          index 3

        // top left corner
        latCorners[0] = maxLat;
        lonCorners[0] = minLon;

        // top right corner
        latCorners[1] = maxLat;
        lonCorners[1] = maxLon;

        // bottom left corner
        latCorners[2] = minLat;
        lonCorners[2] = minLon;

        // bottom right corner
        latCorners[3] = minLat;
        lonCorners[3] = maxLon;

        /*
        System.out.println("Sentinel1Level2Directory.addGeoCodingForLevel2Products: corners of footprint (TopL, TopR, bottomL, bottomR):");
        for (int i = 0; i < latCorners.length; i++) {
            System.out.println(" " + latCorners[i] + ", " + lonCorners[i]);
        }
        */

        ReaderUtils.addGeoCoding(product, latCorners, lonCorners);
    }
}
