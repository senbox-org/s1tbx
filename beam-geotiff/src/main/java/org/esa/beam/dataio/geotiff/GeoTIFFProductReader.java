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
import com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.GeoTIFFTagSet;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import com.sun.media.jai.codec.TIFFDirectory;
import com.sun.media.jai.codec.TIFFField;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.*;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.geotiff.EPSGCodes;
import org.esa.beam.util.geotiff.GeoTIFFCodes;
import org.esa.beam.util.io.FileUtils;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.BandSelectDescriptor;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

public class GeoTIFFProductReader extends AbstractProductReader {

    private FileSeekableStream inputStream;

    GeoTIFFProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile = Utils.getFile(getInput());
        inputStream = new FileSeekableStream(inputFile);

        return readGeoTIFFProduct(inputStream, inputFile);
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        final RenderedImage image = destBand.getSourceImage();
        java.awt.image.Raster data = image.getData(new Rectangle(destOffsetX,
                                                                 destOffsetY,
                                                                 destWidth,
                                                                 destHeight));
        data.getDataElements(destOffsetX, destOffsetY, destWidth, destHeight, destBuffer.getElems());
    }

    @Override
    public void close() throws IOException {
        super.close();
        inputStream.close();
    }

    Product readGeoTIFFProduct(final SeekableStream stream, final File inputFile) throws ProductIOException {
        final ParameterBlock pb = new ParameterBlock();
        pb.add(stream);
        final TIFFDecodeParam param = new TIFFDecodeParam();
        pb.add(param);

        final RenderedOp geoTiff = JAI.create("tiff", pb);

        final TIFFDirectory dir = (TIFFDirectory) geoTiff.getProperty("tiff_directory");
        final TIFFFileInfo info = new TIFFFileInfo(dir);


        final int width = geoTiff.getWidth();
        final int height = geoTiff.getHeight();

        final BeamMetadata.Metadata metadata = getBeamMetadata(info);

        final String productName;
        final String productType;
        if (metadata != null) {
            productName = metadata.getProductProperty(BeamMetadata.NODE_NAME);
            productType = metadata.getProductProperty(BeamMetadata.NODE_PRODUCTTYPE);
        } else {
            if (info.containsField(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION)) {
                final TIFFField field = info.getField(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION);
                final String s = field.getAsString(0);
                productName = s.substring(0, s.length() - 1);
            } else {
                productName = FileUtils.getFilenameWithoutExtension(inputFile);
            }
            productType = getReaderPlugIn().getFormatNames()[0];
        }

        final Product product = new Product(productName, productType, width, height, this);
        product.setFileLocation(inputFile);

        final int numBands = geoTiff.getSampleModel().getNumBands();
        final int productDataType = ImageManager.getProductDataType(geoTiff.getSampleModel().getDataType());
        for (int i = 0; i < numBands; i++) {
            final Band band;
            if (metadata != null) {
                final String bandName = metadata.getBandProperty(i, BeamMetadata.NODE_NAME);
                band = product.addBand(bandName, productDataType);
                double scalingFactor = Double.parseDouble(metadata.getBandProperty(i, BeamMetadata.NODE_SCALING_FACTOR));
                double scalingOffset = Double.parseDouble(metadata.getBandProperty(i, BeamMetadata.NODE_SCALING_OFFSET));
                boolean log10Scaled = Boolean.parseBoolean(metadata.getBandProperty(i, BeamMetadata.NODE_LOG_10_SCALED));
                band.setScalingFactor(scalingFactor);
                band.setScalingOffset(scalingOffset);
                band.setLog10Scaled(log10Scaled);
                double noDataValue = Double.parseDouble(metadata.getBandProperty(i, BeamMetadata.NODE_NO_DATA_VALUE));
                boolean noDataValueUsed = Boolean.parseBoolean(metadata.getBandProperty(i, BeamMetadata.NODE_NO_DATA_VALUE_USED));
                String validExpression = metadata.getBandProperty(i, BeamMetadata.NODE_VALID_EXPRESION);
                band.setNoDataValue(noDataValue);
                band.setNoDataValueUsed(noDataValueUsed);
                band.setValidPixelExpression(validExpression);
            } else {
                band = product.addBand(String.format("band_%d", i + 1), productDataType);
            }

            final RenderedOp bandSourceImage = BandSelectDescriptor.create(geoTiff, new int[]{i}, null);
            band.setSourceImage(bandSourceImage);

            // todo - here for future implementation
//            if(info.containsField(BaselineTIFFTagSet.TAG_COLOR_MAP) && geoTiff.getColorModel() instanceof IndexColorModel) {
//                final IndexColorModel colorModel = (IndexColorModel) geoTiff.getColorModel();
//                final IndexCoding indexCoding = new IndexCoding("color_map");
//                final int colorCount = colorModel.getMapSize();
//                final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[colorCount];
//                for(int j=0; j < colorCount; j++) {
//                    indexCoding.addIndex("I"+j, j,"");
//                    points[j] = new ColorPaletteDef.Point(j, new Color(colorModel.getRGB(j)));
//                }
//                product.getIndexCodingGroup().add(indexCoding);
//                band.setSampleCoding(indexCoding);
//
//                band.setImageInfo(new ImageInfo(new ColorPaletteDef(points)));
//            }
        }
        if (info.isGeotiff()) {
            applyGeoCoding(info, product);
        }
        return product;
    }

    private static BeamMetadata.Metadata getBeamMetadata(TIFFFileInfo info) throws ProductIOException {

        final TIFFField field = info.getField(BeamMetadata.PRIVATE_TIFF_TAG_NUMBER);
        if (field == null || field.getType() != TIFFField.TIFF_ASCII) {
            return null;
        }
        final String s = field.getAsString(0).trim();
        if (s.contains("<beam_metadata")) {
            try {
                final Document document = new SAXBuilder().build(new StringReader(s));
                return BeamMetadata.createMetadata(document);
            } catch (JDOMException e) {
                final ProductIOException ioe = new ProductIOException(e.getMessage());
                ioe.initCause(e);
                throw ioe;
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
        return null;
    }

    private static void applyGeoCoding(TIFFFileInfo info, Product product) {
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

    private static void applyProjectedGeocoding(TIFFFileInfo info, Product product) {
        final Map<Integer, GeoKeyEntry> keyEntries = info.getGeoKeyEntries();
        final Datum datum = getDatum(keyEntries);
        if (keyEntries.containsKey(GeoTIFFCodes.ProjectedCSTypeGeoKey)) {
            final int pcsCode = keyEntries.get(GeoTIFFCodes.ProjectedCSTypeGeoKey).getIntValue();
            final MapProjection projection;
            if (isUTM_PCSCode(pcsCode)) {
                final MapInfo mapInfo = createMapInfoPCS(pcsCode);
                if (mapInfo != null) {
                    mapInfo.setSceneWidth(product.getSceneRasterWidth());
                    mapInfo.setSceneHeight(product.getSceneRasterHeight());
                    product.setGeoCoding(new MapGeoCoding(mapInfo));
                    return;
                }
                return; //todo message "gocoding is not supported" continuing as standard Tiff reader
            } else if (isUserdefinedPCSCode(pcsCode)) {
                if (isProjectionUserDefined(keyEntries)) {
                    if (isProjectionTransverseMercator(keyEntries)) {
                        projection = getProjectionTransverseMercator(keyEntries);
                    } else if (isProjectionLambertConfConic(keyEntries)) {
                        projection = getProjectionLambertConfConic(keyEntries);
                    } else if (isProjectionPolarStereographic(keyEntries)) {
                        projection = getProjectionPolarStereographic(keyEntries);
                    } else if (isProjectionAlbersEqualArea(keyEntries)) {
                        projection = getProjectionAlbersEqualAreaConic(keyEntries);
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
                final MapInfo mapInfo = new MapInfo(projection, 0, 0, 0, 0, 1, 1, datum);
                if (info.containsField(GeoTIFFTagSet.TAG_MODEL_TRANSFORMATION)) {
                    applyTransform(mapInfo, info);
                } else {
                    final TIFFField scaleField = info.getField(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE);
                    final TIFFField modelTiePointField = info.getField(GeoTIFFTagSet.TAG_MODEL_TIE_POINT);

                    if (scaleField != null) {
                        mapInfo.setPixelSizeX((float) scaleField.getAsDouble(0));
                        mapInfo.setPixelSizeY((float) scaleField.getAsDouble(1));
                    }
                    if (modelTiePointField != null) {
                        mapInfo.setPixelX((float) modelTiePointField.getAsDouble(0));
                        mapInfo.setPixelY((float) modelTiePointField.getAsDouble(1));
                        mapInfo.setEasting((float) modelTiePointField.getAsDouble(3));
                        mapInfo.setNorthing((float) modelTiePointField.getAsDouble(4));
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

    private static boolean isProjectionPolarStereographic(Map<Integer, GeoKeyEntry> keyEntries) {
        return containsProjCoordTrans(keyEntries) &&
               keyEntries.get(GeoTIFFCodes.ProjCoordTransGeoKey).getIntValue() == GeoTIFFCodes.CT_PolarStereographic;
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

    private static void applyTransform(MapInfo mapInfo, TIFFFileInfo info) {
        final TIFFField transformationField = info.getField(GeoTIFFTagSet.TAG_MODEL_TRANSFORMATION);
        final double[] transformValues = TIFFFileInfo.getDoubleValues(transformationField);

        final double m00 = transformValues[0];
        final double m01 = transformValues[1];
        final double m02 = transformValues[3];
        final double m10 = transformValues[4];
        final double m11 = transformValues[5];
        final double m12 = transformValues[7];

        final AffineTransform transform = new AffineTransform(m00, m10, m01, m11, m02, m12);

        mapInfo.setPixelSizeX((float) transform.getScaleX());
        mapInfo.setPixelSizeY((float) transform.getScaleY() * -1.0f);
        mapInfo.setEasting((float) transform.getTranslateX());
        mapInfo.setNorthing((float) transform.getTranslateY());

        // shearing not supported by MapInfo
        // todo: extend by using an affine transformation or add property shear
        // mapInfo.setShearX((float) transform.getShearX());
        // mapInfo.setShearY((float) transform.getShearY());

        final double a = transform.getScaleX();
        final double c = transform.getShearY();
        final double aPow = Math.pow(a, 2);
        final double cPow = Math.pow(c, 2);
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
        }
        if (keyEntries.containsKey(GeoTIFFCodes.ProjNatOriginLongGeoKey)) {
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
        final MapProjection mapProjection = new MapProjection(descriptor.getTypeID(), transform);
        return mapProjection;
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
        final MapTransform transform = descriptor.createTransform(values);
        final MapProjection mapProjection = new MapProjection(descriptor.getTypeID(), transform);
        return mapProjection;
    }

    private static MapProjection getProjectionPolarStereographic(Map<Integer, GeoKeyEntry> keyEntries) {
        final MapTransformDescriptor descriptor = MapProjectionRegistry.getDescriptor(
                    StereographicDescriptor.TYPE_ID);
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
        final MapProjection mapProjection = new MapProjection(descriptor.getTypeID(), transform);
        return mapProjection;
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
        final MapProjection mapProjection = new MapProjection(descriptor.getTypeID(), transform);
        return mapProjection;
    }

    private static void applyGeographicGeocoding(TIFFFileInfo info, Product product) {

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

    private static void applyTiePointGeocoding(TIFFFileInfo info, double[] tiePoints, Product product) {
        final SortedMap<Integer, GeoKeyEntry> geoKeyEntries = info.getGeoKeyEntries();
        final Datum datum = getDatum(geoKeyEntries);
        final SortedSet<Double> xSet = new TreeSet<Double>();
        final SortedSet<Double> ySet = new TreeSet<Double>();
        for (int i = 0; i < tiePoints.length; i += 6) {
            xSet.add(tiePoints[i]);
            ySet.add(tiePoints[i + 1]);
        }
        final double xMin = xSet.first().doubleValue();
        final double xMax = xSet.last().doubleValue();
        final double xDiff = (xMax - xMin) / (xSet.size() - 1);
        final double yMin = ySet.first().doubleValue();
        final double yMax = ySet.last().doubleValue();
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

        final TiePointGrid latGrid = new TiePointGrid(
                    "latitude", width, height, (float) xMin, (float) yMin, (float) xDiff, (float) yDiff, lats);
        final TiePointGrid lonGrid = new TiePointGrid(
                    "longitude", width, height, (float) xMin, (float) yMin, (float) xDiff, (float) yDiff, lons);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, datum));
    }

    static boolean isAbleToUseForTiePointGeocoding(final double[] tiePoints) {
        final SortedSet<Double> xSet = new TreeSet<Double>();
        final SortedSet<Double> ySet = new TreeSet<Double>();
        for (int i = 0; i < tiePoints.length; i += 6) {
            xSet.add(tiePoints[i]);
            ySet.add(tiePoints[i + 1]);
        }
        if (!isEquiDistance(xSet) || !isEquiDistance(ySet)) {
            return false;
        }
        return true;
    }

    private static boolean isEquiDistance(SortedSet<Double> set) {
        final double min = set.first().doubleValue();
        final double max = set.last().doubleValue();
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

    private static void applyLatLonProjection(final TIFFFileInfo info,
                                              final double[] tiePoints,
                                              final Product product) {
        final SortedMap<Integer, GeoKeyEntry> geoKeyEntries = info.getGeoKeyEntries();
        final Datum datum = getDatum(geoKeyEntries);
        final TIFFField modelPixelScaleField = info.getField(GeoTIFFTagSet.TAG_MODEL_PIXEL_SCALE);
        final float pixelX;
        final float pixelY;
        final float easting;
        final float northing;
        final float pixelSizeX;
        final float pixelSizeY;
        if (modelPixelScaleField != null) {
            final double[] doubles = TIFFFileInfo.getDoubleValues(modelPixelScaleField);
            pixelSizeX = (float) doubles[0];
            pixelSizeY = (float) doubles[1];
        } else {
            pixelSizeX = 1;
            pixelSizeY = 1;
        }
        pixelX = (float) tiePoints[0];
        pixelY = (float) tiePoints[1];
        easting = (float) tiePoints[3];
        northing = (float) tiePoints[4];
        final MapProjection projection = MapProjectionRegistry.getProjection(IdentityTransformDescriptor.NAME);
        final MapInfo mapInfo = new MapInfo(
                    projection,
                    pixelX, pixelY, easting, northing, pixelSizeX, pixelSizeY, datum

        );
        mapInfo.setSceneWidth(product.getSceneRasterWidth());
        mapInfo.setSceneHeight(product.getSceneRasterHeight());
        product.setGeoCoding(new MapGeoCoding(mapInfo));

    }

    private static void applyGcpGeoCoding(final TIFFFileInfo info,
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
            final PixelPos pixelPos = new PixelPos(x, y);

            final float lat = (float) tiePoints[offset + 4];
            final float lon = (float) tiePoints[offset + 3];
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
            } else {
                //@todo if user defined ... make user defined datum
                datum = Datum.WGS_84;
            }
        } else {
            datum = Datum.WGS_84;
        }
        return datum;
    }

    private static void applyIdentityGeoCoding(TIFFFileInfo info, Product product) {

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

    private static MapInfo createMapInfoPCS(int pcsCode) {
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
            final MapInfo mapInfo = new MapInfo(projection,
                                                0.5f, 0.5f,
                                                0.0f, 0.0f,
                                                1.0f, 1.0f,
                                                Datum.WGS_84);
            return mapInfo;
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
