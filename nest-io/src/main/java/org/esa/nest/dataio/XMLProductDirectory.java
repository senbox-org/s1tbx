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
package org.esa.nest.dataio;

import com.bc.ceres.core.VirtualDir;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.Guardian;
import org.esa.nest.dataio.imageio.ImageIOFile;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.metadata.AbstractMetadataIO;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.util.XMLSupport;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class represents a product directory.
 */
public abstract class XMLProductDirectory {

    private VirtualDir productDir = null;
    private final String baseName;
    private File baseDir;
    private String rootFolder = null;
    private Document xmlDoc = null;

    private boolean isSLC = false;
    protected int sceneWidth = 0;
    protected int sceneHeight = 0;

    protected transient final Map<String, ImageIOFile> bandImageFileMap = new HashMap<>(1);
    protected transient final Map<Band, ImageIOFile.BandInfo> bandMap = new HashMap<>(3);

    protected XMLProductDirectory(final File inputFile) {
        Guardian.assertNotNull("inputFile", inputFile);

        if (SARReader.isZip(inputFile)) {
            productDir = VirtualDir.create(inputFile);
            baseDir = inputFile;
            baseName = inputFile.getName();
        } else {
            productDir = VirtualDir.create(inputFile.getParentFile());
            baseDir = inputFile.getParentFile();
            baseName = inputFile.getParentFile().getName();
        }
    }

    protected final String getRootFolder() {
        if (rootFolder != null)
            return rootFolder;
        try {
            if (productDir.isCompressed()) {
                final ZipFile productZip = new ZipFile(baseDir, ZipFile.OPEN_READ);

                final Optional result = productZip.stream()
                        .filter(ze -> !ze.isDirectory())
                        .filter(ze -> ze.getName().toLowerCase().endsWith(getHeaderFileName()))
                        .findFirst();
                ZipEntry ze = (ZipEntry) result.get();
                String path = ze.toString();
                int sepIndex = path.lastIndexOf('/');
                if (sepIndex > 0) {
                    rootFolder = path.substring(0, sepIndex) + '/';
                } else {
                    rootFolder = "";
                }
            } else {
                rootFolder = "";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rootFolder;
    }

    protected String getRelativePathToImageFolder() {
        return getRootFolder();
    }

    public void readProductDirectory() throws IOException {
        xmlDoc = XMLSupport.LoadXML(productDir.getInputStream(getRootFolder() + getHeaderFileName()));
    }

    protected abstract String getHeaderFileName();

    protected abstract void addImageFile(final Product product, final String imgPath) throws IOException;

    public void setSceneWidthHeight(final Product product, final int width, final int height) {
        sceneWidth = width;
        sceneHeight = height;
        product.setSceneDimensions(width, height);
    }

    public boolean isSLC() {
        return isSLC;
    }

    protected void setSLC(boolean flag) {
        isSLC = flag;
    }

    protected boolean isCompressed() {
        return productDir.isCompressed();
    }

    protected void findImages(final Product product) throws IOException {
        final String parentPath = getRelativePathToImageFolder();
        final String[] listing = productDir.list(parentPath);
        if (listing != null) {
            for (String imgPath : listing) {
                addImageFile(product, parentPath + imgPath);
            }
        }
    }

    public Product createProduct() throws Exception {
        final Product product = new Product(getProductName(),
                getProductType(),
                sceneWidth, sceneHeight);

        addMetaData(product);
        findImages(product);
        addBands(product);

        addGeoCoding(product);
        addTiePointGrids(product);

        product.setName(getProductName());
        product.setProductType(getProductType());
        product.setDescription(getProductDescription());

        ReaderUtils.addMetadataIncidenceAngles(product);
        ReaderUtils.addMetadataProductSize(product);

        return product;
    }

    public ImageIOFile.BandInfo getBandInfo(final Band destBand) {
        return bandMap.get(destBand);
    }

    public void close() throws IOException {
        final Set<String> keys = bandImageFileMap.keySet();                           // The set of keys in the map.
        for (String key : keys) {
            final ImageIOFile img = bandImageFileMap.get(key);
            img.close();
        }
    }

    protected abstract void addBands(final Product product);

    protected abstract void addGeoCoding(final Product product);

    protected abstract void addTiePointGrids(final Product product);

    protected abstract void addAbstractedMetadataHeader(final Product product, final MetadataElement root) throws IOException;

    protected abstract String getProductName();

    protected abstract String getProductType();

    protected String getProductDescription() {
        return "";
    }

    protected void addMetaData(final Product product) throws IOException {
        final MetadataElement root = product.getMetadataRoot();
        final Element rootElement = xmlDoc.getRootElement();
        AbstractMetadataIO.AddXMLMetadata(rootElement, AbstractMetadata.addOriginalProductMetadata(product));

        addAbstractedMetadataHeader(product, root);
    }

    protected Element getXMLRootElement() {
        return xmlDoc.getRootElement();
    }

    protected String[] listFiles(final String path) throws IOException {
        final String[] listing = productDir.list(path);
        final List<String> files = new ArrayList<>(listing.length);
        for (String listEntry : listing) {
            if (!isDirectory(path + '/' + listEntry)) {
                files.add(listEntry);
            }
        }
        return files.toArray(new String[files.size()]);
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

    protected InputStream getInputStream(final String path) throws IOException {
        return productDir.getInputStream(path);
    }

    protected File getBaseDir() {
        return baseDir;
    }

    protected String getBaseName() {
        return baseName;
    }
}