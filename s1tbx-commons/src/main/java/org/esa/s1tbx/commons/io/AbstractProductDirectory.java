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
package org.esa.s1tbx.commons.io;

import com.bc.ceres.core.VirtualDir;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.util.ZipUtils;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * This class represents a product directory.
 */
public abstract class AbstractProductDirectory {

    protected VirtualDir productDir = null;
    protected String baseName;
    protected File baseDir;
    protected String rootFolder = null;
    protected final File productInputFile;

    private boolean isSLC = false;
    private boolean isMapProjected;

    protected transient final Map<String, ImageIOFile> bandImageFileMap = new TreeMap<>();
    protected transient final Map<Band, ImageIOFile.BandInfo> bandMap = new HashMap<>(3);

    protected AbstractProductDirectory(final File inputFile) {
        Guardian.assertNotNull("inputFile", inputFile);
        this.productInputFile = inputFile;

        createProductDir(inputFile);
    }

    protected void createProductDir(final File inputFile) {
        if (ZipUtils.isZip(inputFile)) {
            baseDir = inputFile;
            productDir = VirtualDir.create(baseDir);
            baseName = baseDir.getName();
            if(baseName.endsWith(".zip")) {
                baseName = baseName.substring(0, baseName.lastIndexOf(".zip"));
            }
        } else {
            if(inputFile.isDirectory()) {
                baseDir = inputFile;
            } else {
                baseDir = inputFile.getParentFile();
            }
            productDir = VirtualDir.create(baseDir);
            baseName = baseDir.getName();
        }
    }

    public final String getRootFolder() {
        if (rootFolder == null)
            rootFolder = findRootFolder();
        return rootFolder;
    }

    protected String findRootFolder() {
        String rootFolder = "";
        try {
            if (productDir != null && productDir.isCompressed()) {
                rootFolder = ZipUtils.getRootFolder(baseDir, getHeaderFileName());
            }
        } catch (IOException e) {
            SystemUtils.LOG.severe("Unable to get root path from zip file " + e.getMessage());
        }
        return rootFolder;
    }

    protected VirtualDir getProductDir() {
        return productDir;
    }

    protected String getRelativePathToImageFolder() {
        return getRootFolder();
    }

    public abstract void readProductDirectory() throws IOException;

    protected String getHeaderFileName() {
        return productInputFile.getName();
    }

    protected abstract void addImageFile(final String imgPath, final MetadataElement newRoot) throws IOException;

    public boolean isSLC() {
        return isSLC;
    }

    protected void setSLC(boolean flag) {
        isSLC = flag;
    }

    public boolean isMapProjected() {
        return isMapProjected;
    }

    protected boolean isCompressed() {
        return productDir.isCompressed();
    }

    protected final void findImages(final String parentPath, final MetadataElement newRoot) throws IOException {
        String[] listing;
        try {
            listing = getProductDir().list(parentPath);
        } catch (FileNotFoundException e) {
            listing = null;
        }
        if (listing != null) {
            final List<String> sortedList = Arrays.asList(listing);
            Collections.sort(sortedList);

            for (String fileName : sortedList) {
                addImageFile(parentPath + fileName, newRoot);
            }
        }
    }

    protected String getBandFileNameFromImage(final String imgPath) {
        if(imgPath.contains("/"))
            return imgPath.substring(imgPath.lastIndexOf('/') + 1).toLowerCase();
        return imgPath;
    }

    protected Dimension getBandDimensions(final MetadataElement newRoot, final String bandMetadataName) {
        final MetadataElement absRoot = newRoot.getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);
        final MetadataElement bandMetadata = absRoot.getElement(bandMetadataName);
        final int width, height;
        if(bandMetadata != null) {
            width = bandMetadata.getAttributeInt(AbstractMetadata.num_samples_per_line);
            height = bandMetadata.getAttributeInt(AbstractMetadata.num_output_lines);
        } else {
            width = absRoot.getAttributeInt(AbstractMetadata.num_samples_per_line);
            height = absRoot.getAttributeInt(AbstractMetadata.num_output_lines);
        }
        return new Dimension(width, height);
    }

    protected void findImages(final MetadataElement newRoot) throws IOException {
        final String parentPath = getRelativePathToImageFolder();
        findImages(parentPath, newRoot);
    }

    public ImageIOFile.BandInfo getBandInfo(final Band destBand) {
        ImageIOFile.BandInfo bandInfo = bandMap.get(destBand);
        if(bandInfo == null) {
            for(Band srcBand : bandMap.keySet()) {
                if(srcBand.getName().equals(destBand.getName())) {
                    bandInfo = bandMap.get(srcBand);
                }
            }
        }
        return bandInfo;
    }

    public void close() throws IOException {
        final Set<String> keys = bandImageFileMap.keySet();                           // The set of keys in the map.
        for (String key : keys) {
            final ImageIOFile img = bandImageFileMap.get(key);
            img.close();
        }
        productDir.close();
    }

    protected abstract void addBands(final Product product) throws IOException;

    protected abstract void addGeoCoding(final Product product);

    protected abstract void addTiePointGrids(final Product product);

    protected abstract void addAbstractedMetadataHeader(final MetadataElement root) throws IOException;

    protected abstract String getProductName();

    protected abstract String getProductType();

    protected String getProductDescription() {
        return "";
    }

    protected abstract MetadataElement addMetaData() throws IOException;

    public String[] listFiles(final String path) throws IOException {
        try {
            final String[] listing = productDir.list(path);
            final List<String> sortedList = Arrays.asList(listing);
            Collections.sort(sortedList);

            final List<String> files = new ArrayList<>(listing.length);
            for (String listEntry : sortedList) {
                String entryPath = listEntry;
                if(!path.isEmpty()) {
                    entryPath = path +"/"+ listEntry;
                }
                if (!isDirectory(entryPath)) {
                    files.add(listEntry);
                }
            }
            return files.toArray(new String[0]);
        } catch (Exception e) {
            throw new IOException("Product is corrupt or incomplete\n"+e.getMessage());
        }
    }

    public String[] findFilesContaining(final String path, final String searchString) {
        final List<String> list = new ArrayList<>();
        try {
            final String[] files = listFiles(path);
            for (String file : files) {
                if (file.contains(searchString)) {
                    list.add(file);
                }
            }
        } catch (IOException e) {
            SystemUtils.LOG.severe("Error listing files in " + path);
        }
        return list.toArray(new String[0]);
    }

    private boolean isDirectory(final String path) throws IOException {
        if (productDir.isCompressed()) {
            if (path.contains(".")) {
                int sepIndex = path.lastIndexOf('/');
                int dotIndex = path.lastIndexOf('.');
                return dotIndex < sepIndex;
            } else {
                final ZipFile productZip = new ZipFile(baseDir, ZipFile.OPEN_READ);

                final Optional result = productZip.stream()
                        .filter(ze -> ze.isDirectory()).filter(ze -> ze.getName().equals(path)).findFirst();
                return result.isPresent();
            }
        } else {
            return productDir.getFile(path).isDirectory();
        }
    }

    public File getFile(final String path) throws IOException {
        return getProductDir().getFile(path);
    }

    public boolean exists(final String path) {
        return getProductDir().exists(path);
    }

    public InputStream getInputStream(final String path) throws IOException {
        InputStream inStream = getProductDir().getInputStream(path);
        if(inStream == null) {
            throw new IOException("Product is corrupt or incomplete: unreadable "+path);
        }
        return inStream;
    }

    protected File getBaseDir() {
        return baseDir;
    }

    protected String getBaseName() {
        return baseName;
    }

    public Product createProduct() throws Exception {

        final MetadataElement newRoot = addMetaData();
        findImages(newRoot);

        Dimension dim = getProductDimensions(newRoot);

        final Product product = new Product(getProductName(), getProductType(), dim.width, dim.height);
        updateProduct(product, newRoot);

        addBands(product);

        isMapProjected = InputProductValidator.isMapProjected(product);

        addGeoCoding(product);
        addTiePointGrids(product);
        setLatLongMetadata(product);

        ReaderUtils.addMetadataIncidenceAngles(product);
        ReaderUtils.addMetadataProductSize(product);

        return product;
    }

    protected Dimension getProductDimensions(final MetadataElement newRoot) {
        final MetadataElement absRoot = newRoot.getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);
        final int sceneWidth = absRoot.getAttributeInt(AbstractMetadata.num_samples_per_line);
        final int sceneHeight = absRoot.getAttributeInt(AbstractMetadata.num_output_lines);
        return new Dimension(sceneWidth, sceneHeight);
    }

    public static void updateProduct(final Product product, final MetadataElement newRoot) {
        if(newRoot != null) {
            final MetadataElement root = product.getMetadataRoot();
            for (MetadataElement elem : newRoot.getElements()) {
                root.addElement(elem);
            }
        }

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        product.setStartTime(absRoot.getAttributeUTC(AbstractMetadata.first_line_time));
        product.setEndTime(absRoot.getAttributeUTC(AbstractMetadata.last_line_time));

        product.setProductType(absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE));
        product.setDescription(absRoot.getAttributeString(AbstractMetadata.SPH_DESCRIPTOR));
    }

    private static void setLatLongMetadata(final Product product) {

        final GeoCoding geoCoding = product.getSceneGeoCoding();
        if(geoCoding != null) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
            
            final GeoPos geoPos = new GeoPos();
            final int w = product.getSceneRasterWidth();
            final int h = product.getSceneRasterHeight();

            geoCoding.getGeoPos(new PixelPos(0, 0), geoPos);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, geoPos.lat);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, geoPos.lon);
            geoCoding.getGeoPos(new PixelPos(w, 0), geoPos);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, geoPos.lat);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, geoPos.lon);

            geoCoding.getGeoPos(new PixelPos(0, h), geoPos);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, geoPos.lat);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, geoPos.lon);
            geoCoding.getGeoPos(new PixelPos(w, h), geoPos);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, geoPos.lat);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, geoPos.lon);
        }
    }

    protected static float[][] getBoundingBox(float [][] coordinateList){
        final float [][] boundingBox = new float[4][2];
        float minX, minY, maxX, maxY;
        minX = coordinateList[0][0];
        minY = coordinateList[0][1];
        maxX = coordinateList[0][0];
        maxY = coordinateList[0][1];

        for(float [] coordinate: coordinateList){
            if (coordinate[0] < minX){
                minX = coordinate[0];
            }
            else if (coordinate[0] > maxX){
                maxX = coordinate[0];
            }

            if (coordinate[1] < minY){
                minY = coordinate[1];
            }
            else if (coordinate[1] > maxY){
                maxY = coordinate[1];
            }
        }

        boundingBox[0] = new float[]{minX, maxY}; // UL
        boundingBox[1] = new float[]{maxX, maxY}; // UR
        boundingBox[2] = new float[]{minX, minY}; // LL
        boundingBox[3] = new float[]{maxX, minY}; // LR

        return boundingBox;
    }
}
