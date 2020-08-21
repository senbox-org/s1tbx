/*
 * Copyright (C) 2020 Skywatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx.stac;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.geometry.VectorDataNodeIO;
import org.esa.snap.core.dataio.geometry.VectorDataNodeReader;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

/**
 * The product reader for SkyWatch products.
 */
public class STACProductReader extends SARReader {

    private final static GeoTiffProductReaderPlugIn geoTiffReaderPlugin = new GeoTiffProductReaderPlugIn();

    private final List<ProductReader> imageReaderList = new ArrayList<>();
    private final List<Product> bandProductList = new ArrayList<>();
    private final Map<Band, ImageInputStream> bandInputStreams = new Hashtable<>();
    private final Map<Band, File> bandDataFiles = new HashMap<>();

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public STACProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    public void close() throws IOException {
        for(Product b : bandProductList) {
            b.dispose();
        }
        for(ProductReader r : imageReaderList) {
            r.close();
        }
        super.close();
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {

        Object input = getInput();
        if(input instanceof InputStream) {
            throw new IOException("InputStream not supported");
        }

        final Path path = getPathFromInput(input);
        File metadataFile = path.toFile();
        if (metadataFile.isDirectory()) {
            metadataFile = null;//STACProductReaderPlugIn.findMetadataFile(path);
        } else if(metadataFile.getName().toLowerCase().endsWith(".zip")) {
            throw new IOException("Zipped STAC not supported");
        }
        final File[] imageFiles = null;//STACProductReaderPlugIn.findImageFile(metadataFile);

        Product product = readProductPersistables(metadataFile);
        if(product == null) {
            if(imageFiles[0].getName().endsWith(STACProductConstants.IMAGE_GEOTIFF_EXT)) {
                final File productFile = FileUtils.exchangeExtension(imageFiles[0], STACProductConstants.METADATA_EXT);
                product = readProductPersistables(productFile);
                //readMetadata(metadataFile, product);
            }
        }

        for(File imageFile : imageFiles) {
            final ProductReader imageReader = geoTiffReaderPlugin.createReaderInstance();
            imageReaderList.add(imageReader);

            Product bandProduct = imageReader.readProductNodes(imageFile, null);
            bandProductList.add(bandProduct);

            for (Band band : bandProduct.getBands()) {
                if (product.containsBand(band.getName())) {
                    Band trgBand = product.getBand(band.getName());
                    trgBand.setSourceImage(band.getSourceImage());
                } else {
                    ProductUtils.copyBand(band.getName(), bandProduct, product, true);
                }
            }
        }

        final String name = FileUtils.getFilenameWithoutExtension(metadataFile.getName());
        final String[] fileNames = metadataFile.getParentFile().list();
        if(fileNames != null) {
            for (String fileName : fileNames) {
                if (fileName.startsWith(name)) {
                    final File file = new File(metadataFile.getParentFile(), fileName);
                    if (fileName.endsWith(".csv")) {
                        addVectorData(product, file, product.getSceneCRS());
                    }
                }
            }
        }

        addMasks(product);

        product.getGcpGroup();
        product.setFileLocation(metadataFile);
        product.setProductReader(this);

        //setGeoCoding(product);

        return product;
    }

    private Product readProductPersistables(final File productFile) {
        try {
            if(!productFile.exists())
                return null;

            final JSONParser parser = new JSONParser();
            final JSONObject json = (JSONObject) parser.parse(new FileReader(productFile));
            //final HeaderReaderInterface headerReader = MetadataFactory.createHeaderReader(json);
            //return headerReader.createProduct();
            return null; //todo
        } catch (Exception e) {
            return null;
        }
    }

//    private static void readMetadata(final File metadataFile, final Product product) {
//        try {
//            final JSONParser parser = new JSONParser();
//            final JSONObject json = (JSONObject) parser.parse(new FileReader(metadataFile));
//            final Element metaElem = JSONProductDirectory.jsonToXML(STACProductConstants.SKYWATCH_METADATA, json);
//
//            final MetadataElement root = product.getMetadataRoot();
//            AbstractMetadataIO.AddXMLMetadata(metaElem, root);
//
//        } catch (Exception e) {
//            SystemUtils.LOG.severe("Unable to parse metadata " + e.getMessage());
//        }
//    }

//    public static void setGeoCoding(final Product product) {
//        final MetadataElement root = product.getMetadataRoot();
//        if(root.containsElement(STACProductConstants.SKYWATCH_METADATA)) {
//            final MetadataElement skywatchMetadata = root.getElement(STACProductConstants.SKYWATCH_METADATA);
//            final String geocodingType = skywatchMetadata.getAttributeString(AbstractedMetadata.GEOCODING, "");
//
//            if (geocodingType != null && geocodingType.equals("TiePointGeoCoding")) {
//                if (product.containsTiePointGrid(OperatorUtils.TPG_LATITUDE) &&
//                        product.containsTiePointGrid(OperatorUtils.TPG_LONGITUDE)) {
//
//                    final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(
//                            product.getTiePointGrid(OperatorUtils.TPG_LATITUDE),
//                            product.getTiePointGrid(OperatorUtils.TPG_LONGITUDE));
//                    product.setSceneGeoCoding(tpGeoCoding);
//                }
//            }
//        }
//    }

    private void addRasterBand(final Product product, final File rasterFile) throws IOException {
        final ProductReader imgReader = geoTiffReaderPlugin.createReaderInstance();
        imageReaderList.add(imgReader);
        final Product bandProduct = imgReader.readProductNodes(rasterFile, null);
        bandProductList.add(bandProduct);

        for(Band band : bandProduct.getBands()) {
            if(product.containsBand(band.getName())) {
                Band trgBand = product.getBand(band.getName());
                trgBand.setSourceImage(band.getSourceImage());
            } else {
                ProductUtils.copyBand(band.getName(), bandProduct, product, true);
            }
        }
    }

    public static synchronized void addVectorData(final Product product, final File vectorFile,
                                     final CoordinateReferenceSystem modelCrs) throws IOException {
        FileReader reader = null;
        try {
            reader = new FileReader(vectorFile);
            FeatureUtils.FeatureCrsProvider crsProvider = new FeatureUtils.FeatureCrsProvider() {
                @Override
                public CoordinateReferenceSystem getFeatureCrs(Product product) {
                    return modelCrs;
                }

                @Override
                public boolean clipToProductBounds() {
                    return false;
                }
            };
            String name = getVectorName(vectorFile.getName(), product);

            OptimalPlacemarkDescriptorProvider descriptorProvider = new OptimalPlacemarkDescriptorProvider();
            VectorDataNode vectorDataNode = VectorDataNodeReader.read(name, reader, product,
                    crsProvider, descriptorProvider, modelCrs,
                    VectorDataNodeIO.DEFAULT_DELIMITER_CHAR,
                    ProgressMonitor.NULL);
            if (vectorDataNode != null) {
                final ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
                final VectorDataNode existing = vectorDataGroup.get(vectorDataNode.getName());
                if (existing != null) {
                    vectorDataGroup.remove(existing);
                }
                //Log.debug("Adding vector " + vectorDataNode.getName());
                vectorDataGroup.add(vectorDataNode);
            }
        } catch (IOException e) {
            SystemUtils.LOG.log(Level.SEVERE, "Error reading '" + vectorFile + "'", e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private static String getVectorName(String name, final Product product) {
        if(name.startsWith(product.getName())) {
            name = name.substring(product.getName().length()+1);
        }
        if(name.endsWith("_vector.csv")) {
            name = name.substring(0, name.indexOf("_vector"));
        }
        return name;
    }

    private void addMasks(final Product product) {
//        if(!product.getMaskGroup().contains(AbstractedBands.cloud_mask) && product.containsBand(AbstractedBands.flags)) {
//            Mask cloudMask = new Mask(AbstractedBands.cloud_mask,
//                    product.getSceneRasterWidth(), product.getSceneRasterHeight(), Mask.BandMathsType.INSTANCE);
//            cloudMask.setDescription("Clouded areas");
//            cloudMask.getImageConfig().setValue("expression", "flags == 1");
//            cloudMask.setValidPixelExpression(AbstractedBands.flags + " != NaN");
//            product.getMaskGroup().add(cloudMask);
//        }
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        final int sourceMinX = sourceOffsetX;
        final int sourceMinY = sourceOffsetY;
        final int sourceMaxX = sourceOffsetX + sourceWidth - 1;
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;

        final File dataFile = bandDataFiles.get(destBand);
        final ImageInputStream inputStream = getOrCreateImageInputStream(destBand, dataFile);
        if (inputStream == null) {
            return;
        }

        int destPos = 0;

        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceMinY);
        // For each scan in the data source
        try {
            synchronized (inputStream) {
                for (int sourceY = sourceMinY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    final long sourcePosY = (long) sourceY * destBand.getRasterWidth();
                    if (sourceStepX == 1) {
                        long inputPos = sourcePosY + sourceMinX;
                        destBuffer.readFrom(destPos, destWidth, inputStream, inputPos);
                        destPos += destWidth;
                    } else {
                        for (int sourceX = sourceMinX; sourceX <= sourceMaxX; sourceX += sourceStepX) {
                            long inputPos = sourcePosY + sourceX;
                            destBuffer.readFrom(destPos, 1, inputStream, inputPos);
                            destPos++;
                        }
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private ImageInputStream getOrCreateImageInputStream(Band band, File file) {
        ImageInputStream inputStream = getImageInputStream(band);
        if (inputStream == null) {
            try {
                inputStream = new FileImageInputStream(file);
            } catch (IOException e) {
                SystemUtils.LOG.log(Level.WARNING,
                        "DimapProductReader: Unable to read file '" + file + "' referenced by '" + band.getName() + "'.",
                        e);
            }
            if (inputStream == null) {
                return null;
            }
            bandInputStreams.put(band, inputStream);
        }
        return inputStream;
    }

    private ImageInputStream getImageInputStream(Band band) {
        if (bandInputStreams != null) {
            return bandInputStreams.get(band);
        }
        return null;
    }

    private static class OptimalPlacemarkDescriptorProvider implements VectorDataNodeReader.PlacemarkDescriptorProvider {

        @Override
        public PlacemarkDescriptor getPlacemarkDescriptor(SimpleFeatureType simpleFeatureType) {
            PlacemarkDescriptorRegistry placemarkDescriptorRegistry = PlacemarkDescriptorRegistry.getInstance();
            if (simpleFeatureType.getUserData().containsKey(
                    PlacemarkDescriptorRegistry.PROPERTY_NAME_PLACEMARK_DESCRIPTOR)) {
                String placemarkDescriptorClass = simpleFeatureType.getUserData().get(
                        PlacemarkDescriptorRegistry.PROPERTY_NAME_PLACEMARK_DESCRIPTOR).toString();
                PlacemarkDescriptor placemarkDescriptor = placemarkDescriptorRegistry.getPlacemarkDescriptor(
                        placemarkDescriptorClass);
                if (placemarkDescriptor != null) {
                    return placemarkDescriptor;
                }
            }
            final PlacemarkDescriptor placemarkDescriptor = placemarkDescriptorRegistry.getPlacemarkDescriptor(
                    simpleFeatureType);
            if (placemarkDescriptor != null) {
                return placemarkDescriptor;
            } else {
                return placemarkDescriptorRegistry.getPlacemarkDescriptor(GeometryDescriptor.class);
            }
        }
    }
}
