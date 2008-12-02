/*
 * $Id$
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import com.sun.media.imageio.plugins.tiff.*;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageMetadata;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader;
import com.sun.media.imageioimpl.plugins.tiff.TIFFRenderedImage;
import org.esa.beam.dataio.dimap.DimapProductHelpers;
import org.esa.beam.dataio.geotiff.internal.GeoKeyEntry;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.*;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.geotiff.EPSGCodes;
import org.esa.beam.util.geotiff.GeoTIFFCodes;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.jai.JAIUtils;
import org.jdom.Document;
import org.jdom.input.DOMBuilder;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class GeoTiffProductReader extends AbstractProductReader {

    private static final int FIRST_IMAGE = 0;

    private ImageInputStream inputStream;
    private Map<Band, Integer> bandMap;

    private TIFFImageReader imageReader;

    GeoTiffProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile = Utils.getFile(getInput());
        inputStream = ImageIO.createImageInputStream(inputFile);

        return readGeoTIFFProduct(inputStream, inputFile);
    }

    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                                       int sourceWidth, int sourceHeight,
                                                       int sourceStepX, int sourceStepY,
                                                       Band destBand,
                                                       int destOffsetX, int destOffsetY,
                                                       int destWidth, int destHeight,
                                                       ProductData destBuffer, ProgressMonitor pm) throws IOException {
        final int destSize = destWidth * destHeight;
        pm.beginTask("Reading data...", 3);
        try {
            TIFFImageReadParam readParam = (TIFFImageReadParam) imageReader.getDefaultReadParam();
            readParam.setSourceSubsampling(sourceStepX, sourceStepY,
                                           sourceOffsetX % sourceStepX,
                                           sourceOffsetY % sourceStepY);
            TIFFRenderedImage subsampledImage = (TIFFRenderedImage) imageReader.readAsRenderedImage(0, readParam);
            pm.worked(1);

            final Raster data = subsampledImage.getData(new Rectangle(destOffsetX, destOffsetY,
                                                                      destWidth, destHeight));
            double[] dArray = new double[destSize];
            final Integer bandIdx = bandMap.get(destBand);
            final DataBuffer dataBuffer = data.getDataBuffer();
            final SampleModel sampleModel = data.getSampleModel();
            sampleModel.getSamples(0, 0, destWidth, destHeight, bandIdx, dArray, dataBuffer);
            pm.worked(1);

            for (int i = 0; i < dArray.length; i++) {
                destBuffer.setElemDoubleAt(i, dArray[i]);
            }
            pm.worked(1);

        } finally {
            pm.done();
        }

    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
        inputStream.close();
    }

    Product readGeoTIFFProduct(final ImageInputStream stream, final File inputFile) throws IOException {
        Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(stream);
        imageReader = (TIFFImageReader) imageReaders.next();
        imageReader.setInput(stream);

        Product product = null;

        final TIFFImageMetadata imageMetadata = (TIFFImageMetadata) imageReader.getImageMetadata(FIRST_IMAGE);
        final TiffFileInfo tiffInfo = new TiffFileInfo(imageMetadata.getRootIFD());
        final TIFFField field = tiffInfo.getField(Utils.PRIVATE_BEAM_TIFF_TAG_NUMBER);
        if (field != null && field.getType() == TIFFTag.TIFF_ASCII) {
            final String s = field.getAsString(0).trim();
            if (s.contains("<Dimap_Document")) { // with DIMAP header
                InputStream is = null;
                try {
                    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    final DocumentBuilder builder = factory.newDocumentBuilder();
                    is = new ByteArrayInputStream(s.getBytes());
                    final Document document = new DOMBuilder().build(builder.parse(is));
                    product = DimapProductHelpers.createProduct(document);
                    removeGeocodingAndTiePointGrids(product);
                    initBandsMap(product);
                } catch (ParserConfigurationException ignore) {
                    // ignore if it can not be read
                } catch (SAXException ignore) {
                    // ignore if it can not be read
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        }

        if (product == null) {            // without DIMAP header
            final String productName;
            if (tiffInfo.containsField(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION)) {
                final TIFFField field1 = tiffInfo.getField(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION);
                final String s = field1.getAsString(0);
                productName = s.substring(0, s.length() - 1);
            } else {
                productName = FileUtils.getFilenameWithoutExtension(inputFile);
            }
            final String productType = getReaderPlugIn().getFormatNames()[0];

            final int width = imageReader.getWidth(FIRST_IMAGE);
            final int height = imageReader.getHeight(FIRST_IMAGE);
            product = new Product(productName, productType, width, height, this);
            addBandsToProduct(tiffInfo, product);
        }

        if (tiffInfo.isGeotiff()) {
            applyGeoCoding(tiffInfo, product);
        }

        TiffTagToMetadataConverter.addTiffTagsToMetadata(imageMetadata, tiffInfo, product.getMetadataRoot());

        product.setFileLocation(inputFile);
        setPreferrdTiling(product);

        return product;
    }

    private void initBandsMap(Product product) {
        final Band[] bands = product.getBands();
        bandMap = new HashMap<Band, Integer>(bands.length);
        for (Band band : bands) {
            if (!(band instanceof VirtualBand) && !(band instanceof FilterBand)) {
                bandMap.put(band, bandMap.size());
            }
        }
    }

    private static void removeGeocodingAndTiePointGrids(Product product) {
        product.setGeoCoding(null);
        final TiePointGrid[] pointGrids = product.getTiePointGrids();
        for (TiePointGrid pointGrid : pointGrids) {
            product.removeTiePointGrid(pointGrid);
        }
    }

    private void addBandsToProduct(TiffFileInfo tiffInfo, Product product) throws
                                                                           IOException {
        final ImageReadParam readParam = imageReader.getDefaultReadParam();
        TIFFRenderedImage baseImage = (TIFFRenderedImage) imageReader.readAsRenderedImage(FIRST_IMAGE, readParam);
        SampleModel sampleModel = baseImage.getSampleModel();
        final int numBands = sampleModel.getNumBands();
        final int productDataType = ImageManager.getProductDataType(sampleModel.getDataType());
        bandMap = new HashMap<Band, Integer>(numBands);
        for (int i = 0; i < numBands; i++) {
            final String bandName = String.format("band_%d", i + 1);
            final Band band = product.addBand(bandName, productDataType);
            if (tiffInfo.containsField(
                    BaselineTIFFTagSet.TAG_COLOR_MAP) && baseImage.getColorModel() instanceof IndexColorModel) {
                band.setImageInfo(createIndexedImageInfo(product, baseImage, band));
            }
            bandMap.put(band, i);
        }
    }

    private void setPreferrdTiling(Product product) throws IOException {
        final Dimension dimension;
        if (isBadTiling()) {
            dimension = JAIUtils.computePreferredTileSize(imageReader.getWidth(FIRST_IMAGE),
                                                          imageReader.getHeight(FIRST_IMAGE), 1);
        } else {
            dimension = new Dimension(imageReader.getTileWidth(FIRST_IMAGE), imageReader.getTileHeight(FIRST_IMAGE));
        }
        product.setPreferredTileSize(dimension);
    }

    private boolean isBadTiling() throws IOException {
        final int imageHeight = imageReader.getHeight(FIRST_IMAGE);
        final int tileHeight = imageReader.getTileHeight(FIRST_IMAGE);
        final int imageWidth = imageReader.getWidth(FIRST_IMAGE);
        final int tileWidth = imageReader.getTileWidth(FIRST_IMAGE);
        return tileWidth <= 1 || tileHeight <= 1 || imageWidth == tileWidth || imageHeight == tileHeight;
    }

    private static ImageInfo createIndexedImageInfo(Product product, TIFFRenderedImage baseImage, Band band) {
        final IndexColorModel colorModel = (IndexColorModel) baseImage.getColorModel();
        final IndexCoding indexCoding = new IndexCoding("color_map");
        final int colorCount = colorModel.getMapSize();
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[colorCount];
        for (int j = 0; j < colorCount; j++) {
            final String name = "I%3d";
            indexCoding.addIndex(String.format(name, j), j, "");
            points[j] = new ColorPaletteDef.Point(j, new Color(colorModel.getRGB(j)), name);
        }
        product.getIndexCodingGroup().add(indexCoding);
        band.setSampleCoding(indexCoding);

        return new ImageInfo(new ColorPaletteDef(points, points.length));
    }

    private static void applyGeoCoding(TiffFileInfo info, Product product) {
        final Map<Integer, GeoKeyEntry> keyEntries = info.getGeoKeyEntries();
        if (keyEntries.containsKey(GeoTIFFCodes.GTModelTypeGeoKey)) {
            final int valueModelType = keyEntries.get(GeoTIFFCodes.GTModelTypeGeoKey).getIntValue();
            if (valueModelType == GeoTIFFCodes.ModelTypeProjected) {
                applyProjectedGeocoding(info, product);
            } else if (valueModelType == GeoTIFFCodes.ModelTypeGeographic) {
                applyGeographicGeocoding(info, product);
            }
        }
    }

    private static void applyProjectedGeocoding(TiffFileInfo info, Product product) {
        final Map<Integer, GeoKeyEntry> keyEntries = info.getGeoKeyEntries();
        final Datum datum = getDatum(keyEntries);
        if (keyEntries.containsKey(GeoTIFFCodes.ProjectedCSTypeGeoKey)) {
            final int pcsCode = keyEntries.get(GeoTIFFCodes.ProjectedCSTypeGeoKey).getIntValue();
            MapProjection projection = null;
            MapInfo mapInfo = null;
            if (isUTM_PCSCode(pcsCode)) {
                mapInfo = createMapInfoPCS(pcsCode, info);
                if (mapInfo != null) {
                    projection = mapInfo.getMapProjection();
                    mapInfo.setSceneWidth(product.getSceneRasterWidth());
                    mapInfo.setSceneHeight(product.getSceneRasterHeight());
                }
            } else if (isUserdefinedPCSCode(pcsCode)) {
                if (isProjectionUserDefined(keyEntries)) {
                    if (isProjectionTransverseMercator(keyEntries)) {
                        projection = getProjectionTransverseMercator(keyEntries);
                        mapInfo = new MapInfo(projection, 0, 0, 0, 0, 1, 1, datum);
                    } else if (isProjectionLambertConfConic(keyEntries)) {
                        projection = getProjectionLambertConfConic(keyEntries);
                        mapInfo = new MapInfo(projection, 0, 0, 0, 0, 1, 1, datum);
                    } else if (isProjectionStereographic(keyEntries)) {
                        projection = getProjectionStereographic(keyEntries);
                        mapInfo = new MapInfo(projection, 0, 0, 0, 0, 1, 1, datum);
                    } else if (isProjectionAlbersEqualArea(keyEntries)) {
                        projection = getProjectionAlbersEqualAreaConic(keyEntries);
                        mapInfo = new MapInfo(projection, 0, 0, 0, 0, 1, 1, datum);
                    } else {
                        projection = null;
                    }
                } else {
                    projection = null;
                }
            } else {
                projection = null;
            }
            if (projection != null) {
                if (info.containsField(GeoTIFFTagSet.TAG_MODEL_TRANSFORMATION)) {
                    applyTransform(mapInfo, info);
                } else {
                    final TIFFField scaleField = info.getField(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE);
                    final TIFFField modelTiePointField = info.getField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT);

                    if (scaleField != null) {
                        mapInfo.setPixelSizeX(scaleField.getAsFloat(0));
                        mapInfo.setPixelSizeY(scaleField.getAsFloat(1));
                    }
                    if (modelTiePointField != null) {
                        mapInfo.setPixelX(modelTiePointField.getAsFloat(0));
                        mapInfo.setPixelY(modelTiePointField.getAsFloat(1));
                        mapInfo.setEasting(modelTiePointField.getAsFloat(3));
                        mapInfo.setNorthing(modelTiePointField.getAsFloat(4));
                    }
                }
                mapInfo.setSceneWidth(product.getSceneRasterWidth());
                mapInfo.setSceneHeight(product.getSceneRasterHeight());
                product.setGeoCoding(new MapGeoCoding(mapInfo));
            }
        }
    }

    private static boolean isProjectionUserDefined(Map<Integer, GeoKeyEntry> keyEntries) {
        return keyEntries.containsKey(GeoTIFFCodes.ProjectionGeoKey) &&
               keyEntries.get(GeoTIFFCodes.ProjectionGeoKey).getIntValue() == GeoTIFFCodes.GTUserDefinedGeoKey;
    }

    private static boolean isProjectionTransverseMercator(Map<Integer, GeoKeyEntry> keyEntries) {
        return containsProjCoordTrans(keyEntries) &&
               keyEntries.get(GeoTIFFCodes.ProjCoordTransGeoKey).getIntValue() == GeoTIFFCodes.CT_TransverseMercator;
    }

    private static boolean isProjectionLambertConfConic(Map<Integer, GeoKeyEntry> keyEntries) {
        return containsProjCoordTrans(keyEntries) &&
               keyEntries.get(GeoTIFFCodes.ProjCoordTransGeoKey).getIntValue() == GeoTIFFCodes.CT_LambertConfConic;
    }

    private static boolean isProjectionStereographic(Map<Integer, GeoKeyEntry> keyEntries) {
        return containsProjCoordTrans(keyEntries) &&
               (keyEntries.get(GeoTIFFCodes.ProjCoordTransGeoKey).getIntValue() == GeoTIFFCodes.CT_PolarStereographic ||
                keyEntries.get(GeoTIFFCodes.ProjCoordTransGeoKey).getIntValue() == GeoTIFFCodes.CT_Stereographic);
    }

    private static boolean isProjectionAlbersEqualArea(Map<Integer, GeoKeyEntry> keyEntries) {
        return containsProjCoordTrans(keyEntries) &&
               keyEntries.get(GeoTIFFCodes.ProjCoordTransGeoKey).getIntValue() == GeoTIFFCodes.CT_AlbersEqualArea;
    }

    private static boolean containsProjCoordTrans(Map<Integer, GeoKeyEntry> keyEntries) {
        return keyEntries.containsKey(GeoTIFFCodes.ProjCoordTransGeoKey);
    }

    private static boolean isUserdefinedPCSCode(int pcsCode) {
        return pcsCode == GeoTIFFCodes.GTUserDefinedGeoKey;
    }

    private static void applyTransform(MapInfo mapInfo, TiffFileInfo info) {
        final TIFFField transformationField = info.getField(GeoTIFFTagSet.TAG_MODEL_TRANSFORMATION);
        final double[] transformValues = TiffFileInfo.getDoubleValues(transformationField);

        final double m00 = transformValues[0];
        final double m01 = transformValues[1];
        final double m02 = transformValues[3];
        final double m10 = transformValues[4];
        final double m11 = transformValues[5];
        final double m12 = transformValues[7];

        final AffineTransform transform = new AffineTransform(m00, m10, m01, m11, m02, m12);

        mapInfo.setPixelSizeX((float) transform.getScaleX());
        mapInfo.setPixelSizeY((float) -transform.getScaleY());
        mapInfo.setEasting((float) transform.getTranslateX());
        mapInfo.setNorthing((float) transform.getTranslateY());

        // shearing not supported by MapInfo
        // todo: extend by using an affine transformation or add property shear
        // mapInfo.setShearX((float) transform.getShearX());
        // mapInfo.setShearY((float) transform.getShearY());

        final double a = transform.getScaleX();
        final double c = transform.getShearY();
        final double aPow = a * a;
        final double cPow = c * c;
        final float orientation = (float) Math.toDegrees(Math.acos(a / Math.sqrt(aPow + cPow)));
        mapInfo.setOrientation(orientation);
    }

    private static MapProjection getProjectionTransverseMercator(Map<Integer, GeoKeyEntry> keyEntries) {
        final MapTransformDescriptor descriptor = MapProjectionRegistry.getDescriptor(
                TransverseMercatorDescriptor.TYPE_ID);
        final double[] values = descriptor.getParameterDefaultValues();

        if (keyEntries.containsKey(GeoTIFFCodes.GeogSemiMajorAxisGeoKey)) {
            values[0] = keyEntries.get(GeoTIFFCodes.GeogSemiMajorAxisGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.GeogSemiMinorAxisGeoKey)) {
            values[1] = keyEntries.get(GeoTIFFCodes.GeogSemiMinorAxisGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjNatOriginLatGeoKey)) {
            values[2] = keyEntries.get(GeoTIFFCodes.ProjNatOriginLatGeoKey).getDblValue()[0];
        } else if (keyEntries.containsKey(GeoTIFFCodes.ProjCenterLatGeoKey)) {
            values[2] = keyEntries.get(GeoTIFFCodes.ProjCenterLatGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjNatOriginLongGeoKey)) {
            values[3] = keyEntries.get(GeoTIFFCodes.ProjNatOriginLongGeoKey).getDblValue()[0];
        } else if (keyEntries.containsKey(GeoTIFFCodes.ProjCenterLongGeoKey)) {
            values[3] = keyEntries.get(GeoTIFFCodes.ProjCenterLongGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjScaleAtNatOriginGeoKey)) {
            values[4] = keyEntries.get(GeoTIFFCodes.ProjScaleAtNatOriginGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjFalseEastingGeoKey)) {
            values[5] = keyEntries.get(GeoTIFFCodes.ProjFalseEastingGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjFalseNorthingGeoKey)) {
            values[6] = keyEntries.get(GeoTIFFCodes.ProjFalseNorthingGeoKey).getDblValue()[0];
        }
        final MapTransform transform = descriptor.createTransform(values);
        return new MapProjection(descriptor.getTypeID(), transform);
    }

    private static MapProjection getProjectionLambertConfConic(Map<Integer, GeoKeyEntry> keyEntries) {
        final MapTransformDescriptor descriptor = MapProjectionRegistry.getDescriptor(
                LambertConformalConicDescriptor.TYPE_ID);
        final double[] values = descriptor.getParameterDefaultValues();

        if (keyEntries.containsKey(GeoTIFFCodes.GeogSemiMajorAxisGeoKey)) {
            values[0] = keyEntries.get(GeoTIFFCodes.GeogSemiMajorAxisGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.GeogSemiMinorAxisGeoKey)) {
            values[1] = keyEntries.get(GeoTIFFCodes.GeogSemiMinorAxisGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjFalseOriginLatGeoKey)) {
            values[2] = keyEntries.get(GeoTIFFCodes.ProjFalseOriginLatGeoKey).getDblValue()[0];
        } else if (keyEntries.containsKey(GeoTIFFCodes.ProjNatOriginLatGeoKey)) {
            values[2] = keyEntries.get(GeoTIFFCodes.ProjNatOriginLatGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjFalseOriginLongGeoKey)) {
            values[3] = keyEntries.get(GeoTIFFCodes.ProjFalseOriginLongGeoKey).getDblValue()[0];
        } else if (keyEntries.containsKey(GeoTIFFCodes.ProjNatOriginLongGeoKey)) {
            values[3] = keyEntries.get(GeoTIFFCodes.ProjNatOriginLongGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjStdParallel1GeoKey)) {
            values[4] = keyEntries.get(GeoTIFFCodes.ProjStdParallel1GeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjStdParallel2GeoKey)) {
            values[5] = keyEntries.get(GeoTIFFCodes.ProjStdParallel2GeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjScaleAtNatOriginGeoKey)) {
            values[6] = keyEntries.get(GeoTIFFCodes.ProjScaleAtNatOriginGeoKey).getDblValue()[0];
        }
        final MapTransform transform = descriptor.createTransform(values);
        return new MapProjection(descriptor.getTypeID(), transform);
    }

    private static MapProjection getProjectionStereographic(Map<Integer, GeoKeyEntry> keyEntries) {
        final MapTransformDescriptor descriptor = MapProjectionRegistry.getDescriptor(
                StereographicDescriptor.TYPE_ID);
        final double[] values = descriptor.getParameterDefaultValues();

        if (keyEntries.containsKey(GeoTIFFCodes.GeogSemiMajorAxisGeoKey)) {
            values[0] = keyEntries.get(GeoTIFFCodes.GeogSemiMajorAxisGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.GeogSemiMinorAxisGeoKey)) {
            values[1] = keyEntries.get(GeoTIFFCodes.GeogSemiMinorAxisGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjCenterLatGeoKey)) {
            values[2] = keyEntries.get(GeoTIFFCodes.ProjCenterLatGeoKey).getDblValue()[0];
        } else if (keyEntries.containsKey(GeoTIFFCodes.ProjNatOriginLatGeoKey)) {
            values[2] = keyEntries.get(GeoTIFFCodes.ProjNatOriginLatGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjCenterLongGeoKey)) {
            values[3] = keyEntries.get(GeoTIFFCodes.ProjCenterLongGeoKey).getDblValue()[0];
        } else if (keyEntries.containsKey(GeoTIFFCodes.ProjNatOriginLongGeoKey)) {
            values[3] = keyEntries.get(GeoTIFFCodes.ProjNatOriginLongGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjScaleAtNatOriginGeoKey)) {
            values[4] = keyEntries.get(GeoTIFFCodes.ProjScaleAtNatOriginGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjFalseEastingGeoKey)) {
            values[5] = keyEntries.get(GeoTIFFCodes.ProjFalseEastingGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjFalseNorthingGeoKey)) {
            values[6] = keyEntries.get(GeoTIFFCodes.ProjFalseNorthingGeoKey).getDblValue()[0];
        }
        final MapTransform transform = descriptor.createTransform(values);
        return new MapProjection(descriptor.getTypeID(), transform);
    }

    private static MapProjection getProjectionAlbersEqualAreaConic(Map<Integer, GeoKeyEntry> keyEntries) {
        final MapTransformDescriptor descriptor = MapProjectionRegistry.getDescriptor(
                AlbersEqualAreaConicDescriptor.TYPE_ID);
        final double[] values = descriptor.getParameterDefaultValues();

        if (keyEntries.containsKey(GeoTIFFCodes.GeogSemiMajorAxisGeoKey)) {
            values[0] = keyEntries.get(GeoTIFFCodes.GeogSemiMajorAxisGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.GeogSemiMinorAxisGeoKey)) {
            values[1] = keyEntries.get(GeoTIFFCodes.GeogSemiMinorAxisGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjNatOriginLatGeoKey)) {
            values[2] = keyEntries.get(GeoTIFFCodes.ProjNatOriginLatGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjNatOriginLongGeoKey)) {
            values[3] = keyEntries.get(GeoTIFFCodes.ProjNatOriginLongGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjStdParallel1GeoKey)) {
            values[4] = keyEntries.get(GeoTIFFCodes.ProjStdParallel1GeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjStdParallel2GeoKey)) {
            values[5] = keyEntries.get(GeoTIFFCodes.ProjStdParallel2GeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjScaleAtNatOriginGeoKey)) {
            values[6] = keyEntries.get(GeoTIFFCodes.ProjScaleAtNatOriginGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjFalseEastingGeoKey)) {
            values[7] = keyEntries.get(GeoTIFFCodes.ProjFalseEastingGeoKey).getDblValue()[0];
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjFalseNorthingGeoKey)) {
            values[8] = keyEntries.get(GeoTIFFCodes.ProjFalseNorthingGeoKey).getDblValue()[0];
        }
        final MapTransform transform = descriptor.createTransform(values);
        return new MapProjection(descriptor.getTypeID(), transform);
    }

    private static void applyGeographicGeocoding(TiffFileInfo info, Product product) {

        if (info.containsField(GeoTIFFTagSet.TAG_MODEL_TRANSFORMATION)) {
            applyIdentityGeoCoding(info, product);
        } else if (info.containsField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT)) {

            final double[] tiePoints = info.getField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT).getAsDoubles();
            final int numTiePoints = tiePoints.length / 6;

            if (numTiePoints == 1) {
                applyLatLonProjection(info, tiePoints, product);
            } else {
                if (isAbleToUseForTiePointGeocoding(tiePoints)) {
                    applyTiePointGeocoding(info, tiePoints, product);
                } else {
                    applyGcpGeoCoding(info, tiePoints, product);
                }
            }
        }
    }

    private static void applyTiePointGeocoding(TiffFileInfo info, double[] tiePoints, Product product) {
        final SortedMap<Integer, GeoKeyEntry> geoKeyEntries = info.getGeoKeyEntries();
        final Datum datum = getDatum(geoKeyEntries);
        final SortedSet<Double> xSet = new TreeSet<Double>();
        final SortedSet<Double> ySet = new TreeSet<Double>();
        for (int i = 0; i < tiePoints.length; i += 6) {
            xSet.add(tiePoints[i]);
            ySet.add(tiePoints[i + 1]);
        }
        final double xMin = xSet.first();
        final double xMax = xSet.last();
        final double xDiff = (xMax - xMin) / (xSet.size() - 1);
        final double yMin = ySet.first();
        final double yMax = ySet.last();
        final double yDiff = (yMax - yMin) / (ySet.size() - 1);

        final int width = xSet.size();
        final int height = ySet.size();

        int idx = 0;
        final Map<Double, Integer> xIdx = new HashMap<Double, Integer>();
        for (Double val : xSet) {
            xIdx.put(val, idx);
            idx++;
        }
        idx = 0;
        final Map<Double, Integer> yIdx = new HashMap<Double, Integer>();
        for (Double val : ySet) {
            yIdx.put(val, idx);
            idx++;
        }

        final float[] lats = new float[width * height];
        final float[] lons = new float[width * height];

        for (int i = 0; i < tiePoints.length; i += 6) {
            final int idxX = xIdx.get(tiePoints[i + 0]);
            final int idxY = yIdx.get(tiePoints[i + 1]);
            final int arrayIdx = idxY * width + idxX;
            lons[arrayIdx] = (float) tiePoints[i + 3];
            lats[arrayIdx] = (float) tiePoints[i + 4];
        }

        String[] names = Utils.findSuitableLatLonNames(product);
        final TiePointGrid latGrid = new TiePointGrid(
                names[0], width, height, (float) xMin, (float) yMin, (float) xDiff, (float) yDiff, lats);
        final TiePointGrid lonGrid = new TiePointGrid(
                names[1], width, height, (float) xMin, (float) yMin, (float) xDiff, (float) yDiff, lons);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, datum));
    }

    private static boolean isAbleToUseForTiePointGeocoding(final double[] tiePoints) {
        for (double tiePoint : tiePoints) {
            if (Double.isNaN(tiePoint)) {
                return false;
            }
        }
        final SortedSet<Double> xSet = new TreeSet<Double>();
        final SortedSet<Double> ySet = new TreeSet<Double>();
        for (int i = 0; i < tiePoints.length; i += 6) {
            xSet.add(tiePoints[i]);
            ySet.add(tiePoints[i + 1]);
        }
        return isEquiDistance(xSet) && isEquiDistance(ySet);
    }

    private static boolean isEquiDistance(SortedSet<Double> set) {
        final double min = set.first();
        final double max = set.last();
        final double diff = (max - min) / (set.size() - 1);
        final double diff100000 = diff / 100000;
        final double maxDiff = diff + diff100000;
        final double minDiff = diff - diff100000;

        final Double[] values = set.toArray(new Double[set.size()]);
        for (int i = 1; i < values.length; i++) {
            final double currentDiff = values[i] - values[i - 1];
            if (currentDiff > maxDiff || currentDiff < minDiff) {
                return false;
            }
        }
        return true;
    }

    private static void applyLatLonProjection(final TiffFileInfo info,
                                              final double[] tiePoints,
                                              final Product product) {
        final SortedMap<Integer, GeoKeyEntry> geoKeyEntries = info.getGeoKeyEntries();
        final Datum datum = getDatum(geoKeyEntries);
        final TIFFField modelPixelScaleField = info.getField(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE);
        final float pixelSizeX;
        final float pixelSizeY;
        if (modelPixelScaleField != null) {
            final double[] doubles = TiffFileInfo.getDoubleValues(modelPixelScaleField);
            pixelSizeX = (float) doubles[0];
            pixelSizeY = (float) doubles[1];
        } else {
            pixelSizeX = 1;
            pixelSizeY = 1;
        }
        final float pixelX = (float) tiePoints[0];
        final float pixelY = (float) tiePoints[1];
        final float easting = (float) tiePoints[3];
        final float northing = (float) tiePoints[4];
        final MapProjection projection = MapProjectionRegistry.getProjection(IdentityTransformDescriptor.NAME);
        final MapInfo mapInfo = new MapInfo(
                projection,
                pixelX, pixelY, easting, northing, pixelSizeX, pixelSizeY, datum

        );
        mapInfo.setSceneWidth(product.getSceneRasterWidth());
        mapInfo.setSceneHeight(product.getSceneRasterHeight());
        product.setGeoCoding(new MapGeoCoding(mapInfo));

    }

    private static void applyGcpGeoCoding(final TiffFileInfo info,
                                          final double[] tiePoints,
                                          final Product product) {
        final SortedMap<Integer, GeoKeyEntry> geoKeyEntries = info.getGeoKeyEntries();
        final Datum datum = getDatum(geoKeyEntries);

        int numTiePoints = tiePoints.length / 6;
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        final GcpDescriptor gcpDescriptor = GcpDescriptor.INSTANCE;
        final PinSymbol symbol = gcpDescriptor.createDefaultSymbol();
        final ProductNodeGroup<Pin> gcpGroup = product.getGcpGroup();
        for (int i = 0; i < numTiePoints; i++) {
            final int offset = i * 6;

            final float x = (float) tiePoints[offset + 0];
            final float y = (float) tiePoints[offset + 1];
            final float lon = (float) tiePoints[offset + 3];
            final float lat = (float) tiePoints[offset + 4];

            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(lon) || Double.isNaN(lat)) {
                continue;
            }
            final PixelPos pixelPos = new PixelPos(x, y);
            final GeoPos geoPos = new GeoPos(lat, lon);

            final String name = gcpDescriptor.getRoleName() + "_" + i;
            final String label = gcpDescriptor.getRoleLabel() + "_" + i;
            final Pin gcp = new Pin(name, label, "", pixelPos, geoPos, symbol);
            gcpGroup.add(gcp);
        }

        final GcpGeoCoding.Method method;
        if (gcpGroup.getNodeCount() >= GcpGeoCoding.Method.POLYNOMIAL3.getTermCountP()) {
            method = GcpGeoCoding.Method.POLYNOMIAL3;
        } else if (gcpGroup.getNodeCount() >= GcpGeoCoding.Method.POLYNOMIAL2.getTermCountP()) {
            method = GcpGeoCoding.Method.POLYNOMIAL2;
        } else if (gcpGroup.getNodeCount() >= GcpGeoCoding.Method.POLYNOMIAL1.getTermCountP()) {
            method = GcpGeoCoding.Method.POLYNOMIAL1;
        } else {
            return; // not able to apply GCP geo coding; not enough tie points
        }

        final Pin[] gcps = gcpGroup.toArray(new Pin[gcpGroup.getNodeCount()]);
        product.setGeoCoding(new GcpGeoCoding(method, gcps, width, height, datum));
    }

    private static Datum getDatum(Map<Integer, GeoKeyEntry> geoKeyEntries) {
        final Datum datum;
        if (geoKeyEntries.containsKey(GeoTIFFCodes.GeographicTypeGeoKey)) {
            final int value = geoKeyEntries.get(GeoTIFFCodes.GeographicTypeGeoKey).getIntValue();
            if (value == EPSGCodes.GCS_WGS_72) {
                datum = Datum.WGS_72;
            } else if (value == EPSGCodes.GCS_WGS_84) {
                datum = Datum.WGS_84;
            } else {
                //@todo if user defined ... make user defined datum
                datum = Datum.WGS_84;
            }
        } else {
            datum = Datum.WGS_84;
        }
        return datum;
    }

    private static void applyIdentityGeoCoding(TiffFileInfo info, Product product) {

        if (info.containsField(GeoTIFFTagSet.TAG_MODEL_TRANSFORMATION)) {
            final MapProjection projection = MapProjectionRegistry.getProjection(
                    IdentityTransformDescriptor.NAME);

            final SortedMap<Integer, GeoKeyEntry> geoKeyEntries = info.getGeoKeyEntries();
            float pixelOffset = 0.5f;
            if (geoKeyEntries.containsKey(GeoTIFFCodes.GTRasterTypeGeoKey)) {
                final int value = geoKeyEntries.get(GeoTIFFCodes.GTRasterTypeGeoKey).getIntValue();
                if (value == GeoTIFFCodes.RasterPixelIsPoint) {
                    pixelOffset = 0.0f;
                }
            }

            final MapInfo mapInfo = new MapInfo(
                    projection,
                    pixelOffset, pixelOffset,
                    0, 0,
                    1, 1,
                    Datum.WGS_84);

            mapInfo.setSceneWidth(product.getSceneRasterWidth());
            mapInfo.setSceneHeight(product.getSceneRasterHeight());

            applyTransform(mapInfo, info);

            product.setGeoCoding(new MapGeoCoding(mapInfo));
        }
    }

    private static MapInfo createMapInfoPCS(int pcsCode, TiffFileInfo tiffInfo) {
        final boolean isUtmNord = isUTM_Nord_PCSCode(pcsCode);
        final boolean isUtmSouth = isUTM_South_PCSCode(pcsCode);
        final boolean isUtm = isUTM_PCSCode(pcsCode);
        if (isUtm) {
            final int zoneIdx;
            if (isUtmNord) {
                zoneIdx = pcsCode - EPSGCodes.PCS_WGS84_UTM_zone_1N;
            } else {
                zoneIdx = pcsCode - EPSGCodes.PCS_WGS84_UTM_zone_1S;
            }
            final UTMProjection projection = UTM.createProjection(zoneIdx, isUtmSouth);
            float pixelX = 0.5f;
            float pixelY = 0.5f;
            float easting = (float) projection.getMapTransform().getParameterValues()[5];
            float northing = (float) projection.getMapTransform().getParameterValues()[6];
            if (tiffInfo.containsField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT)) {
                final TIFFField modelTiePoint = tiffInfo.getField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT);
                pixelX = modelTiePoint.getAsFloat(0);
                pixelY = modelTiePoint.getAsFloat(1);
                easting = modelTiePoint.getAsFloat(3);
                northing = modelTiePoint.getAsFloat(4);
            }

            float pixelSizeX = 1.0f;
            float pixelSizeY = 1.0f;
            if (tiffInfo.containsField(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE)) {
                final TIFFField modelPixelScale = tiffInfo.getField(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE);
                pixelSizeX = modelPixelScale.getAsFloat(0);
                pixelSizeY = modelPixelScale.getAsFloat(1);
            }

            return new MapInfo(projection,
                               pixelX, pixelY,
                               easting, northing,
                               pixelSizeX, pixelSizeY,
                               Datum.WGS_84);
        }
        return null;
    }

    private static boolean isUTM_PCSCode(int pcsCode) {
        return isUTM_Nord_PCSCode(pcsCode) || isUTM_South_PCSCode(pcsCode);
    }

    private static boolean isUTM_South_PCSCode(int pcsCode) {
        return pcsCode >= EPSGCodes.PCS_WGS84_UTM_zone_1S && pcsCode <= EPSGCodes.PCS_WGS84_UTM_zone_60S;
    }

    private static boolean isUTM_Nord_PCSCode(int pcsCode) {
        return pcsCode >= EPSGCodes.PCS_WGS84_UTM_zone_1N && pcsCode <= EPSGCodes.PCS_WGS84_UTM_zone_60N;
    }
}
