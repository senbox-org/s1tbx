/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.db;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.download.opensearch.OpenSearch;
import org.esa.snap.engine_utilities.gpf.CommonReaders;
import org.esa.snap.engine_utilities.util.ProductFunctions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**

 */
public class ProductEntry {

    public final static String FILE_SIZE = "file_size";
    public final static String LAST_MODIFIED = "last_modified";
    public final static String FILE_FORMAT = "file_format";
    public final static String GEO_BOUNDARY = "geo_boundary";
    public final DateFormat yyyMMdd_Format = ProductData.UTC.createDateFormat("yyy-MM-dd");
    private final DecimalFormat decimalFormat = new DecimalFormat("0.0000");

    private int id;
    private File file;
    private long fileSize;
    private String fileSizeString = null; // For SciHub search result only
    private String name;
    private String mission;
    private String productType;
    private String acquisitionMode;
    private ProductData.UTC firstLineTime;
    private String pass;
    private double range_spacing;
    private double azimuth_spacing;
    private long lastModified;
    private String fileFormat;
    private String refID;

    private MetadataElement absRoot = null;

    // corner locations
    private final GeoPos firstNear = new GeoPos();
    private final GeoPos firstFar = new GeoPos();
    private final GeoPos lastNear = new GeoPos();
    private final GeoPos lastFar = new GeoPos();
    private GeoPos[] geoboundary;
    private boolean useGeoboundaryForBox = false;

    private Quicklook quicklook = null;

    public ProductEntry(final int id, final String name, final File file) {
        this.id = id;
        this.name = name;
        this.file = file;
    }

    public ProductEntry(final Product product) {
        file = product.getFileLocation();
        if (file != null)
            lastModified = file.lastModified();
        fileSize = calculateFileSize(product);
        fileFormat = product.getProductReader().getReaderPlugIn().getFormatNames()[0];

        absRoot = AbstractMetadata.getAbstractedMetadata(product).createDeepClone();
        if (absRoot != null) {
            name = absRoot.getAttributeString(AbstractMetadata.PRODUCT);
            mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
            productType = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
            acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
            pass = absRoot.getAttributeString(AbstractMetadata.PASS);
            range_spacing = absRoot.getAttributeDouble(AbstractMetadata.range_spacing);
            azimuth_spacing = absRoot.getAttributeDouble(AbstractMetadata.azimuth_spacing);
            firstLineTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time);
        }

        // get defaults if not available in metadata
        if (name.isEmpty() || name.equals(AbstractMetadata.NO_METADATA_STRING))
            name = product.getName();
        if (productType.isEmpty() || productType.equals(AbstractMetadata.NO_METADATA_STRING))
            productType = product.getProductType();
        if (firstLineTime.equals(AbstractMetadata.NO_METADATA_UTC))
            firstLineTime = product.getStartTime();

        getCornerPoints(product);

        if (mission.equals("SMOS")) {
            geoboundary = getSMOSGeoBoundary(product);
            useGeoboundaryForBox = true;
        } else {
            geoboundary = getGeoBoundary(product);
        }

        quicklook = product.getDefaultQuicklook();

        this.id = -1;
    }

    public ProductEntry(final ResultSet results) throws SQLException {
        this.id = results.getInt(1);
        this.file = new File(results.getString(AbstractMetadata.PATH));
        this.name = results.getString(AbstractMetadata.PRODUCT);
        this.mission = results.getString(AbstractMetadata.MISSION);
        this.productType = results.getString(AbstractMetadata.PRODUCT_TYPE);
        this.acquisitionMode = results.getString(AbstractMetadata.ACQUISITION_MODE);
        this.pass = results.getString(AbstractMetadata.PASS);
        this.range_spacing = results.getDouble(AbstractMetadata.range_spacing);
        this.azimuth_spacing = results.getDouble(AbstractMetadata.azimuth_spacing);
        Date date = results.getDate(AbstractMetadata.first_line_time);
        this.firstLineTime = AbstractMetadata.parseUTC(date.toString(), yyyMMdd_Format);
        this.fileSize = (long) results.getDouble(FILE_SIZE);
        this.lastModified = (long) results.getDouble(LAST_MODIFIED);
        this.fileFormat = results.getString(FILE_FORMAT);

        this.firstNear.setLocation(results.getDouble(AbstractMetadata.first_near_lat),
                results.getDouble(AbstractMetadata.first_near_long));
        this.firstFar.setLocation(results.getDouble(AbstractMetadata.first_far_lat),
                results.getDouble(AbstractMetadata.first_far_long));
        this.lastNear.setLocation(results.getDouble(AbstractMetadata.last_near_lat),
                results.getDouble(AbstractMetadata.last_near_long));
        this.lastFar.setLocation(results.getDouble(AbstractMetadata.last_far_lat),
                results.getDouble(AbstractMetadata.last_far_long));
        this.geoboundary = parseGeoBoundaryStr(results.getString(GEO_BOUNDARY));

        this.quicklook = new Quicklook(this.file);

        if (mission.equals("SMOS")) {
            useGeoboundaryForBox = true;
        }
    }

    public ProductEntry(OpenSearch.ProductResult productResult) {

        this.id = -1;
        this.name = productResult.name;
        this.mission = productResult.mission;
        this.firstLineTime = productResult.utc;
        this.refID = productResult.id;

        this.acquisitionMode = productResult.mode;

        this.pass = "";
        this.fileFormat = "";

        final String productType = productResult.name.substring(7, 10);
        if (productType.equals("GRD") || productType.equals("SLC") || productType.equals("OCN")) {
            this.productType = productType;
        } else {
            this.productType = "";
        }

        this.fileSizeString = productResult.size;

        this.quicklook = new Quicklook(null);
        this.quicklook.setQuicklookLink(productResult.quicklookLink);
        // Need to call getImage() to force it to get the image from the link
        if (this.quicklook.getImage(null) == null) {
            SystemUtils.LOG.warning("No quicklook for SciHub search product " + this.name + "; quicklookLink = " + productResult.quicklookLink);
        };

        useGeoboundaryForBox = true;

        // TODO
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            Document doc = factory.newDocumentBuilder().parse(new URL(productResult.manifestLink).openStream());
            if (doc == null) {
                SystemUtils.LOG.warning("Null manifest for SciHub search product; manifestLink = " + productResult.manifestLink);
            } else {
                // instrumentMode
            }
        } catch (Exception e) {
            SystemUtils.LOG.warning("No manifest for SciHub search product; " + e.getMessage() + "; manifestLink = " + productResult.manifestLink);
        }

        //System.out.println("ProductEntry quicklookLink = " + productResult.quicklookLink);
    }

    public void dispose() {
        if (absRoot != null)
            absRoot.dispose();
    }

    public static void dispose(final ProductEntry[] productEntryList) {
        for (ProductEntry e : productEntryList) {
            e.dispose();
        }
    }

    private static long calculateFileSize(final Product product) {
        File file = product.getFileLocation();
        if(file != null && file.getName().toLowerCase().endsWith(".zip")) {
            return file.length();
        }
        return ProductFunctions.getRawStorageSize(product);
    }

    private void getCornerPoints(final Product product) {
        final GeoCoding geoCoding = product.getSceneGeoCoding();
        if (geoCoding == null) return;
        if (!geoCoding.canGetGeoPos()) return;

        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        geoCoding.getGeoPos(new PixelPos(0, 0), firstNear);
        geoCoding.getGeoPos(new PixelPos(w, 0), firstFar);
        geoCoding.getGeoPos(new PixelPos(0, h), lastNear);
        geoCoding.getGeoPos(new PixelPos(w, h), lastFar);
    }

    private static GeoPos[] getSMOSGeoBoundary(final Product product) {
        final GeoCoding gc = product.getSceneGeoCoding();
        if (gc == null)
            return new GeoPos[0];

        Band band = product.getBand("Latitude");
        if (band == null)
            band = product.getBandAt(0);
        final int width = band.getRasterWidth();
        final int height = band.getRasterHeight();
        final float[] line = new float[width];
        final double nodata = band.getNoDataValue();
        final PixelPos pix = new PixelPos();
        final int stepX = 20;
        final int stepY = 300;

        final int min = 1500;
        final int max = Math.min(height, 6000);

        final int size = 2 * (max - min) / stepY;
        final List<GeoPos> geoPoints = new ArrayList<>(size);
        final List<GeoPos> geoPoints2 = new ArrayList<>(size);
        try {
            for (int y = min; y < max; y += stepY) {
                band.readPixels(0, y, width, 1, line, ProgressMonitor.NULL);

                int x1 = stepX;
                int x2 = width - stepX;
                boolean haveX1 = false;
                boolean haveX2 = false;
                while (x1 < width && x2 > 0 && !(haveX1 && haveX2)) {
                    if (!haveX1) {
                        if (line[x1] != nodata)
                            haveX1 = true;
                        x1 += stepX;
                    }
                    if (!haveX2) {
                        if (line[x2] != nodata)
                            haveX2 = true;
                        x2 -= stepX;
                    }
                }
                if (haveX1 && haveX2 && x2 - x1 > 200) {
                    pix.setLocation(x1, y);
                    geoPoints.add(gc.getGeoPos(pix, null));
                    pix.setLocation(x2, y);
                    geoPoints2.add(gc.getGeoPos(pix, null));
                }
            }
            // add x2 in reverse
            for (int i = geoPoints2.size() - 1; i >= 0; --i) {
                geoPoints.add(geoPoints2.get(i));
            }
        } catch (Exception e) {
            SystemUtils.LOG.severe("Error reading SMOS " + e.getMessage());
        }
        return geoPoints.toArray(new GeoPos[geoPoints.size()]);
    }

    private static GeoPos[] getGeoBoundary(final Product product) {
        final GeoCoding gc = product.getSceneGeoCoding();
        if (gc == null)
            return new GeoPos[0];
        final int step = Math.max(30, (product.getSceneRasterWidth() + product.getSceneRasterHeight()) / 10);
        GeoPos[] geoPoints = ProductUtils.createGeoBoundary(product, null, step, true);
        if(geoPoints.length > 1 && !geoPoints[0].equals(geoPoints[geoPoints.length-1])) {
            final GeoPos[] newgeoPoints = new GeoPos[geoPoints.length+1];
            int i=0;
            for(GeoPos geoPos : geoPoints) {
                newgeoPoints[i++] = geoPos;
            }
            newgeoPoints[i] = new GeoPos(geoPoints[0]);
            geoPoints = newgeoPoints;
        }
        ProductUtils.normalizeGeoPolygon(geoPoints);
        return geoPoints;
    }

    public String formatGeoBoundayString() {
        final StringBuilder str = new StringBuilder(geoboundary.length * 20);
        for (GeoPos geo : geoboundary) {
            str.append(decimalFormat.format(geo.getLat()));
            str.append(',');
            str.append(decimalFormat.format(geo.getLon()));
            str.append(',');
        }
        return str.toString();
    }

    private static GeoPos[] parseGeoBoundaryStr(final String str) {
        final List<GeoPos> geoPos = new ArrayList<>(100);
        if (str != null && !str.isEmpty()) {
            final StringTokenizer st = new StringTokenizer(str, ",");
            while (st.hasMoreTokens()) {
                geoPos.add(new GeoPos(Float.parseFloat(st.nextToken()), Float.parseFloat(st.nextToken())));
            }
        }

        return geoPos.toArray(new GeoPos[geoPos.size()]);
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public String getMission() {
        return mission;
    }

    public String getProductType() {
        return productType;
    }

    public String getAcquisitionMode() {
        return acquisitionMode;
    }

    public String getPass() {
        return pass;
    }

    public double getRangeSpacing() {
        return range_spacing;
    }

    public double getAzimuthSpacing() {
        return azimuth_spacing;
    }

    public GeoPos getFirstNearGeoPos() {
        return firstNear;
    }

    public GeoPos getFirstFarGeoPos() {
        return firstFar;
    }

    public GeoPos getLastNearGeoPos() {
        return lastNear;
    }

    public GeoPos getLastFarGeoPos() {
        return lastFar;
    }

    public GeoPos[] getGeoBoundary() {
        if (geoboundary == null || geoboundary.length == 0) {
            return getBox();
        }
        return geoboundary;
    }

    public void setGeoBoundary(final GeoPos[] geo) {
        this.geoboundary = geo;
    }

    public ProductData.UTC getFirstLineTime() {
        return firstLineTime;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFileSizeString() { return  fileSizeString; }

    public long getLastModified() {
        return lastModified;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getRefID() {
        return refID;
    }

    public MetadataElement getMetadata() {
        if (absRoot == null) {
            try {
                absRoot = ProductDB.instance().getProductMetadata(id);
            } catch (Exception e) {
                SystemUtils.LOG.severe(e.getMessage());
            }
        }
        return absRoot;
    }

    public boolean quickLookExists() {
        return quicklook != null && (quicklook.hasImage() || quicklook.hasCachedImage());
    }

    public Quicklook getQuickLook() {
        return quicklook;
    }

    public boolean equals(Object other) {
        boolean bEqual = false;
        if (this == other) {
            bEqual = true;
        } else if (other instanceof ProductEntry) {
            ProductEntry entry = (ProductEntry) other;
            if ((file == null ? entry.file == null : file.equals(entry.file))) {
                // don't use id in determining equality
                bEqual = true;
            }
        }
        return bEqual;
    }

    public static File[] getFileList(final ProductEntry[] productEntryList) {
        final File[] fileList = new File[productEntryList.length];
        int i = 0;
        for (ProductEntry entry : productEntryList) {
            fileList[i++] = entry.getFile();
        }
        return fileList;
    }

    public GeoPos[] getBox() {
        if (useGeoboundaryForBox && geoboundary != null && geoboundary.length != 0) {
            return geoboundary;
        }
        final GeoPos[] geoBound = new GeoPos[5];
        geoBound[0] = getFirstNearGeoPos();
        geoBound[1] = getFirstFarGeoPos();
        geoBound[2] = getLastFarGeoPos();
        geoBound[3] = getLastNearGeoPos();
        geoBound[4] = getFirstNearGeoPos();
        return geoBound;
    }

    public static ProductEntry[] createProductEntryList(final File[] fileList) {
        final List<ProductEntry> entryList = new ArrayList<>(fileList.length);
        for (File file : fileList) {
            try {
                final Product prod = CommonReaders.readProduct(file);
                entryList.add(new ProductEntry(prod));
                prod.dispose();
            } catch (IOException e) {
                // continue
            }
        }
        return entryList.toArray(new ProductEntry[entryList.size()]);
    }

    public static ProductEntry[] createProductEntryList(final Product[] productList) {
        final List<ProductEntry> entryList = new ArrayList<>(productList.length);
        for (Product prod : productList) {
            entryList.add(new ProductEntry(prod));
        }
        return entryList.toArray(new ProductEntry[entryList.size()]);
    }
}
