/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.Guardian;
import org.esa.nest.dataio.imageio.ImageIOFile;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.AbstractMetadataIO;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.util.XMLSupport;
import org.jdom.Element;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a product directory.
 * <p/>
 * <p>This class is public for the benefit of the implementation of another (internal) class and its API may
 * change in future releases of the software.</p>
 *
 */
public class XMLProductDirectory {

    private final File xmlHeader;
    private final File baseDir;
    private final File imgFolder;
    private org.jdom.Document xmlDoc = null;

    private boolean isSLC = false;
    private int sceneWidth = 0;
    private int sceneHeight = 0;

    protected transient final Map<String, ImageIOFile> bandImageFileMap = new HashMap<String, ImageIOFile>(1);
    protected transient final Map<Band, ImageIOFile.BandInfo> bandMap = new HashMap<Band, ImageIOFile.BandInfo>(3);

    protected XMLProductDirectory(final File headerFile, final File imageFolder) {
        Guardian.assertNotNull("headerFile", headerFile);

        xmlHeader = headerFile;
        baseDir = headerFile.getParentFile();
        imgFolder = imageFolder;
    }

    public void readProductDirectory() throws IOException {

        xmlDoc = XMLSupport.LoadXML(xmlHeader.getAbsolutePath());

        final File[] fileList = imgFolder.listFiles();
        if(fileList != null) {
            for (File file : fileList) {
                addImageFile(file);
            }
        }
    }

    protected void addImageFile(final File file) throws IOException {
        final String name = file.getName().toLowerCase();
        if ((name.endsWith("tif") || name.endsWith("tiff")) && !name.contains("browse")) {
            final ImageIOFile img = new ImageIOFile(file, ImageIOFile.getTiffIIOReader(file));
            bandImageFileMap.put(img.getName(), img);

            setSceneWidthHeight(img.getSceneWidth(), img.getSceneHeight());
        }
    }

    public void setSceneWidthHeight(final int width, final int height) {
        sceneWidth = width;
        sceneHeight = height;
    }

    public boolean isSLC() {
        return isSLC;
    }

    protected void setSLC(boolean flag) {
        isSLC = flag;
    }

    public Product createProduct() throws IOException {
        final Product product = new Product(getProductName(),
                                            getProductType(),
                                            sceneWidth, sceneHeight);

        addMetaData(product);
        addGeoCoding(product);
        addTiePointGrids(product);

        addBands(product, sceneWidth, sceneHeight);

        product.setName(getProductName());
        product.setProductType(getProductType());
        product.setDescription(getProductDescription());

        AbstractMetadata.setAttribute(AbstractMetadata.getAbstractedMetadata(product),
                AbstractMetadata.TOT_SIZE, ReaderUtils.getTotalSize(product));

        return product;
    }

    public ImageIOFile.BandInfo getBandInfo(Band destBand) {
        return bandMap.get(destBand);
    }

    public void close() throws IOException {
        final Set<String> keys = bandImageFileMap.keySet();                           // The set of keys in the map.
        for (String key : keys) {
            final ImageIOFile img = bandImageFileMap.get(key);
            img.close();
        }
    }

    protected void addBands(final Product product, final int width, final int height) {
        int bandCnt = 1;
        final Set<String> keys = bandImageFileMap.keySet();                           // The set of keys in the map.
        for (String key : keys) {
            final ImageIOFile img = bandImageFileMap.get(key);

            for(int i=0; i < img.getNumImages(); ++i) {

                for(int b=0; b < img.getNumBands(); ++b) {
                    final Band band = new Band(img.getName()+bandCnt++, img.getDataType(), width, height);
                    band.setUnit(Unit.AMPLITUDE);
                    product.addBand(band);
                    bandMap.put(band, new ImageIOFile.BandInfo(img, i, b));
                }
            }
        }
    }

    protected void addGeoCoding(final Product product) {
    }

    protected void addTiePointGrids(final Product product) {

    }

    private void addMetaData(final Product product) throws IOException {
        final MetadataElement root = product.getMetadataRoot();
        final Element rootElement = xmlDoc.getRootElement();
        AbstractMetadataIO.AddXMLMetadata(rootElement, AbstractMetadata.getOriginalProductMetadata(product));

        addAbstractedMetadataHeader(product, root);
    }

    protected Element getXMLRootElement() {
        return xmlDoc.getRootElement();
    }

    protected File getBaseDir() {
        return baseDir;
    }

    protected void addAbstractedMetadataHeader(final Product product, final MetadataElement root) throws IOException  {

        AbstractMetadata.addAbstractedMetadataHeader(root);
    }

    protected String getProductName() {
        return xmlHeader.getName();
    }

    protected String getProductDescription() {
        return "";
    }

    protected String getProductType() {
        return "XML-based Product";
    }

}