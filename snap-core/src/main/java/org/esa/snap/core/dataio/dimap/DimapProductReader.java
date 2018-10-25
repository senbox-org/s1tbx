/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.dataio.dimap;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.geometry.VectorDataNodeIO;
import org.esa.snap.core.dataio.geometry.VectorDataNodeReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.GeometryDescriptor;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.PlacemarkDescriptorRegistry;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.jdom.Document;
import org.jdom.input.DOMBuilder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * The <code>DimapProductReader</code> class is an implementation of the <code>ProductReader</code> interface
 * exclusively for data products having the BEAM-DIMAP product format.
 *
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see DimapProductReaderPlugIn
 * @see DimapProductWriterPlugIn
 */
public class DimapProductReader extends AbstractProductReader {

    private Product product;

    private File inputDir;
    private File inputFile;
    private Map<Band, ImageInputStream> bandInputStreams;

    private Map<Band, File> bandDataFiles;
    private Set<ReaderExtender> readerExtenders;

    /**
     * Construct a new instance of a product reader for the given BEAM-DIMAP product reader plug-in.
     *
     * @param readerPlugIn the given BEAM-DIMAP product writer plug-in, must not be <code>null</code>
     */
    public DimapProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    public Product getProduct() {
        return product;
    }

    public File getInputDir() {
        return inputDir;
    }

    public File getInputFile() {
        return inputFile;
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException                                      if an I/O error occurs
     * @throws IllegalFileFormatException if the input file in not decodeable
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Product product = processProduct(null);
        if (readerExtenders != null) {
            for (ReaderExtender readerExtender : readerExtenders) {
                readerExtender.completeProductNodesReading(product);
            }
        }
        return product;
    }

    // todo - Put this into interface ReconfigurableProductReader and make DimapProductReader implement it
    public void bindProduct(Object input, Product existingProduct) throws IOException {
        Assert.notNull(input, "input");
        Assert.notNull(existingProduct, "existingProduct");
        setInput(input);
        processProduct(existingProduct);
    }

    protected Product processProduct(Product existingProduct) throws IOException {
        initInput();
        Document dom = readDom();

        this.product = existingProduct == null ? DimapProductHelpers.createProduct(dom) : existingProduct;
        this.product.setProductReader(this);

        if (existingProduct == null) {
            readTiePointGrids(dom);
        }

        bindBandsToFiles(dom);
        if (existingProduct == null) {
            readVectorData(Product.DEFAULT_IMAGE_CRS, true);

            // read GCPs and pins from DOM (old-style)
            DimapProductHelpers.addGcps(dom, this.product);
            DimapProductHelpers.addPins(dom, this.product);

            initGeoCodings(dom);
            readVectorData(product.getSceneCRS(), false);
            DimapProductHelpers.addMaskUsages(dom, this.product);
        }
        this.product.setFileLocation(inputFile);
        this.product.setModified(false);
        return this.product;
    }


    private void initGeoCodings(Document dom) {
        final GeoCoding[] geoCodings = DimapProductHelpers.createGeoCoding(dom, product);
        if (geoCodings != null) {
            if (geoCodings.length == 1) {
                product.setSceneGeoCoding(geoCodings[0]);
            } else if (geoCodings.length == product.getNumBands()) {
                for (int i = 0; i < geoCodings.length; i++) {
                    final Band band = product.getBandAt(i);
                    if (product.getSceneRasterWidth() == band.getRasterWidth() &&
                            product.getSceneRasterHeight() == band.getRasterHeight()) {
                        product.setSceneGeoCoding(geoCodings[i]);
                    }
                    band.setGeoCoding(geoCodings[i]);
                }
            }
        } else {
            final Band lonBand = product.getBand("longitude");
            final Band latBand = product.getBand("latitude");
            if (latBand != null && lonBand != null) {
                final GeoCoding geoCoding = GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, null, 6);
                product.setSceneGeoCoding(geoCoding);
            }
        }
    }

    private void bindBandsToFiles(Document dom) {
        bandDataFiles = DimapProductHelpers.getBandDataFiles(dom, product, getInputDir());
        final Band[] bands = product.getBands();
        for (final Band band : bands) {
            if (band instanceof VirtualBand || band instanceof FilterBand) {
                continue;
            }
            final File dataFile = bandDataFiles.get(band);
            if (dataFile == null || !dataFile.canRead()) {
                SystemUtils.LOG.warning(
                        "DimapProductReader: Unable to read file '" + dataFile + "' referenced by '" + band.getName() + "'.");
                SystemUtils.LOG.warning(
                        "DimapProductReader: Removed band '" + band.getName() + "' from product '" + product.getFileLocation() + "'.");
            }
        }
    }

    private void readTiePointGrids(Document jDomDocument) throws IOException {
        final String[] tiePointGridNames = product.getTiePointGridNames();
        for (String tiePointGridName : tiePointGridNames) {
            final TiePointGrid tiePointGrid = product.getTiePointGrid(tiePointGridName);
            String dataFile = DimapProductHelpers.getTiePointDataFile(jDomDocument, tiePointGrid.getName());
            final int dataType = DimapProductHelpers.getTiePointDataType(jDomDocument.getRootElement(), tiePointGrid.getName());
            dataFile = FileUtils.exchangeExtension(dataFile, DimapProductConstants.IMAGE_FILE_EXTENSION);
            FileImageInputStream inputStream = null;
            try {
                inputStream = new FileImageInputStream(new File(inputDir, dataFile));
                final float[] gridData = ((float[]) tiePointGrid.getGridData().getElems());
                inputStream.seek(0);
                if (dataType == ProductData.TYPE_FLOAT32) {
                    inputStream.readFully(gridData, 0, gridData.length);
                } else {
                    final double[] doubles = new double[gridData.length];
                    inputStream.readFully(doubles, 0, doubles.length);
                    int i = 0;
                    for (double d : doubles) {
                        gridData[i++] = (float) d;
                    }
                }
                inputStream.close();
                inputStream = null;
                // See if we have a -180...+180 or a 0...360 degree discontinuity
                if (tiePointGrid.getDiscontinuity() != TiePointGrid.DISCONT_NONE) {
                    tiePointGrid.setDiscontinuity(TiePointGrid.getDiscontinuity(gridData));
                }
            } catch (Exception e) {
                throw new IOException(
                        MessageFormat.format("I/O error while reading tie-point grid ''{0}''.", tiePointGridName), e);
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
    }

    private Document readDom() throws IOException {
        Document dom;
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            Debug.trace("DimapProductReader: about to open file '" + inputFile + "'..."); /*I18N*/
            final InputStream is = new BufferedInputStream(new FileInputStream(inputFile), 256 * 1024);
            dom = new DOMBuilder().build(builder.parse(is));
            is.close();
        } catch (Exception e) {
            throw new IOException("Failed to read DIMAP XML header.", e);
        }
        return dom;
    }

    private void initInput() throws IOException {
        if (getInput() instanceof String) {
            inputFile = new File((String) getInput());
        } else if (getInput() instanceof File) {
            inputFile = (File) getInput();
        } else {
            throw new IllegalArgumentException("unsupported input source: " + getInput());  /*I18N*/
        }
        Debug.assertNotNull(inputFile); // super.readProductNodes should have checked getInput() != null already
        inputDir = inputFile.getParentFile();
        if (inputDir == null) {
            inputDir = new File(".");
        }
    }

    /**
     * The template method which is called by the {@link AbstractProductReader#readBandRasterDataImpl(int, int, int, int, int, int, Band, int, int, int, int, ProductData, com.bc.ceres.core.ProgressMonitor)} }
     * method after an optional spatial subset has been applied to the input parameters.
     * <p>The destination band, buffer and region parameters are exactly the ones passed to the original {@link
     * AbstractProductReader#readBandRasterDataImpl} call. Since the
     * <code>destOffsetX</code> and <code>destOffsetY</code> parameters are already taken into acount in the
     * <code>sourceOffsetX</code> and <code>sourceOffsetY</code> parameters, an implementor of this method is free to
     * ignore them.
     *
     * @param sourceOffsetX the absolute X-offset in source raster co-ordinates
     * @param sourceOffsetY the absolute Y-offset in source raster co-ordinates
     * @param sourceWidth   the width of region providing samples to be read given in source raster co-ordinates
     * @param sourceHeight  the height of region providing samples to be read given in source raster co-ordinates
     * @param sourceStepX   the sub-sampling in X direction within the region providing samples to be read
     * @param sourceStepY   the sub-sampling in Y direction within the region providing samples to be read
     * @param destBand      the destination band which identifies the data source from which to read the sample values
     * @param destBuffer    the destination buffer which receives the sample values to be read
     * @param destOffsetX   the X-offset in the band's raster co-ordinates
     * @param destOffsetY   the Y-offset in the band's raster co-ordinates
     * @param destWidth     the width of region to be read given in the band's raster co-ordinates
     * @param destHeight    the height of region to be read given in the band's raster co-ordinates
     * @param pm            a monitor to inform the user about progress
     * @throws java.io.IOException if  an I/O error occurs
     * @see #getSubsetDef
     */
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

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (bandInputStreams == null) {
            return;
        }
        for (ImageInputStream imageInputStream : bandInputStreams.values()) {
            (imageInputStream).close();
        }
        bandInputStreams.clear();
        bandInputStreams = null;
        if (readerExtenders != null) {
            readerExtenders.clear();
            readerExtenders = null;
        }
        super.close();
    }

    private ImageInputStream getOrCreateImageInputStream(Band band, File file) throws IOException {
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
            if (bandInputStreams == null) {
                bandInputStreams = new Hashtable<Band, ImageInputStream>();
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

    private void readVectorData(final CoordinateReferenceSystem modelCrs, final boolean onlyGCPs) throws IOException {
        String dataDirName = FileUtils.getFilenameWithoutExtension(
                inputFile) + DimapProductConstants.DIMAP_DATA_DIRECTORY_EXTENSION;
        File dataDir = new File(inputDir, dataDirName);
        File vectorDataDir = new File(dataDir, "vector_data");
        if (vectorDataDir.exists()) {
            File[] vectorFiles = getVectorDataFiles(vectorDataDir, onlyGCPs);
            for (File vectorFile : vectorFiles) {
                addVectorDataToProduct(vectorFile, modelCrs);
            }
        }
    }

    private void addVectorDataToProduct(File vectorFile, final CoordinateReferenceSystem modelCrs) throws IOException {
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
            OptimalPlacemarkDescriptorProvider descriptorProvider = new OptimalPlacemarkDescriptorProvider();
            VectorDataNode vectorDataNode = VectorDataNodeReader.read(vectorFile.getName(), reader, product,
                                                                      crsProvider, descriptorProvider, modelCrs,
                                                                      VectorDataNodeIO.DEFAULT_DELIMITER_CHAR,
                                                                      ProgressMonitor.NULL);
            if (vectorDataNode != null) {
                final ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
                final VectorDataNode existing = vectorDataGroup.get(vectorDataNode.getName());
                if (existing != null) {
                    vectorDataGroup.remove(existing);
                }
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

    private File[] getVectorDataFiles(File vectorDataDir, final boolean onlyGCPs) {
        return vectorDataDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(VectorDataNodeIO.FILENAME_EXTENSION)) {
                    if (onlyGCPs) {
                        return name.equals("ground_control_points.csv");
                    } else {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public void addExtender(ReaderExtender extender) {
        if (extender == null) {
            return;
        }
        if (readerExtenders == null) {
            readerExtenders = new HashSet<ReaderExtender>();
        }
        readerExtenders.add(extender);
    }

    private static class OptimalPlacemarkDescriptorProvider
            implements VectorDataNodeReader.PlacemarkDescriptorProvider {

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

    public static abstract class ReaderExtender {

        public abstract void completeProductNodesReading(Product product);
    }
}
