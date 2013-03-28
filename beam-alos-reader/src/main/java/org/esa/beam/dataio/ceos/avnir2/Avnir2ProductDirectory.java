/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.ceos.avnir2;

import org.esa.beam.dataio.ceos.CeosHelper;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FXYGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.Ellipsoid;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.maptransf.MapTransformFactory;
import org.esa.beam.framework.dataop.maptransf.StereographicDescriptor;
import org.esa.beam.framework.dataop.maptransf.UTM;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.TreeNode;
import org.esa.beam.util.math.FXYSum;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class represents a product directory of an Avnir-2 product.
 * <p/>
 * <p>This class is public for the benefit of the implementation of another (internal) class and its API may
 * change in future releases of the software.</p>
 *
 * @author Marco Peters
 */
class Avnir2ProductDirectory {

    private static final int METER_PER_KILOMETER = 1000;

    private final File baseDir;
    private Avnir2VolumeDirectoryFile volumeDirectoryFile;
    private Avnir2ImageFile[] imageFiles;
    private Map<Band, Avnir2ImageFile> imageFileMap;
    private Avnir2LeaderFile leaderFile;
    private Avnir2TrailerFile trailerFile;
    private Avnir2SupplementalFile supplementalFile = null;

    private final int sceneWidth;
    private final int sceneHeight;

    Avnir2ProductDirectory(final File dir) throws IOException, IllegalCeosFormatException {
        Guardian.assertNotNull("dir", dir);

        baseDir = dir;
        volumeDirectoryFile = new Avnir2VolumeDirectoryFile(baseDir);
        leaderFile = new Avnir2LeaderFile(createInputStream(volumeDirectoryFile.getLeaderFileName()));
        trailerFile = new Avnir2TrailerFile(createInputStream(volumeDirectoryFile.getTrailerFileName()));
        if (!leaderFile.getProductLevel().equalsIgnoreCase(Avnir2Constants.PRODUCT_LEVEL_1B2)) {
            final File supplementalFile = new File(baseDir, volumeDirectoryFile.getSupplementalFileName());
            if (supplementalFile.exists()) {
                this.supplementalFile = new Avnir2SupplementalFile(
                        createInputStream(volumeDirectoryFile.getSupplementalFileName()));
            }
        }

        final String[] imageFileNames = volumeDirectoryFile.getImageFileNames();
        imageFiles = new Avnir2ImageFile[imageFileNames.length];
        for (int i = 0; i < imageFiles.length; i++) {
            imageFiles[i] = new Avnir2ImageFile(createInputStream(imageFileNames[i]));
        }

        sceneWidth = imageFiles[0].getRasterWidth();
        sceneHeight = imageFiles[0].getRasterHeight();
        assertSameWidthAndHeightForAllImages();
    }

    Product createProduct() throws IOException, IllegalCeosFormatException {
        final Product product = new Product(volumeDirectoryFile.getProductName(),
                                            getProductType(),
                                            sceneWidth, sceneHeight);
        product.setFileLocation(baseDir);
        imageFileMap = new HashMap<Band, Avnir2ImageFile>(imageFiles.length);
        for (final Avnir2ImageFile avnir2ImageFile : imageFiles) {
            Band band = createBand(avnir2ImageFile);
            product.addBand(band);
            imageFileMap.put(band, avnir2ImageFile);

        }
        product.setStartTime(getUTCScanStartTime());
        product.setEndTime(getUTCScanStopTime());
        product.setDescription(getProductDescription());


        addGeoCoding(product);

        addMetaData(product);

        return product;
    }

    TreeNode<File> getProductComponents() {
        try {
            File parentDir = baseDir.getParentFile();
            if (parentDir == null) {
                throw new IllegalStateException("Could not retrieve the parent directory of '" + baseDir.getAbsolutePath() + "'.");
            }
            final TreeNode<File> root = new TreeNode<File>(parentDir.getCanonicalPath());
            root.setContent(parentDir);

            final TreeNode<File> dir = new TreeNode<File>(baseDir.getName());
            dir.setContent(baseDir);
            root.addChild(dir);

            File volumeFile = CeosHelper.getVolumeFile(baseDir);
            addFileToDir(dir, volumeFile);

            addFileToDir(dir, new File(volumeDirectoryFile.getLeaderFileName()));
            addFileToDir(dir, new File(volumeDirectoryFile.getTrailerFileName()));
            String supplemental = volumeDirectoryFile.getSupplementalFileName();
            if (StringUtils.isNotNullAndNotEmpty(supplemental)) {
                addFileToDir(dir, new File(supplemental));
            }
            final String[] imageFileNames = volumeDirectoryFile.getImageFileNames();
            for (String imageFileName : imageFileNames) {
                addFileToDir(dir, new File(imageFileName));
            }

            return root;
            // @todo 2 tb/** log exceptions somewhere
        } catch (IOException e) {
            return null;
        } catch (IllegalCeosFormatException e) {
            return null;
        }
    }

    private void addFileToDir(TreeNode<File> dir, File file) {
        final TreeNode<File> fileNode = new TreeNode<File>(file.getName());
        fileNode.setContent(file);
        dir.addChild(fileNode);
    }

    private String getProductType() throws IOException,
                                           IllegalCeosFormatException {
        return Avnir2Constants.PRODUCT_TYPE_PREFIX + leaderFile.getProductLevel();
    }

    private void addGeoCoding(final Product product) throws IllegalCeosFormatException,
                                                            IOException {

        final String usedProjection = leaderFile.getUsedProjection();
        if (Avnir2Constants.MAP_PROJECTION_RAW.equalsIgnoreCase(usedProjection)) {
            final Band[] bands = product.getBands();
            for (final Band band : bands) {
                final Avnir2ImageFile imageFile = getImageFile(band);
                final int bandIndex = imageFile.getBandIndex();
                final double[][] uncorrectedCoeffs = leaderFile.getUncorrectedTransformationCoeffs(bandIndex);


                final FXYSum.Cubic funcLat = new FXYSum.Cubic(CeosHelper.sortToFXYSumOrder(uncorrectedCoeffs[0]));
                final FXYSum.Cubic funcLon = new FXYSum.Cubic(CeosHelper.sortToFXYSumOrder(uncorrectedCoeffs[1]));
                final FXYSum.Cubic funcX = new FXYSum.Cubic(CeosHelper.sortToFXYSumOrder(uncorrectedCoeffs[2]));
                final FXYSum.Cubic funcY = new FXYSum.Cubic(CeosHelper.sortToFXYSumOrder(uncorrectedCoeffs[3]));

                final FXYGeoCoding gc = new FXYGeoCoding(0.0f, 0.0f, 1.0f, 1.0f,
                                                         funcX, funcY,
                                                         funcLat, funcLon,
                                                         Datum.ITRF_97);
                band.setGeoCoding(gc);
            }

        } else if (Avnir2Constants.MAP_PROJECTION_UTM.equalsIgnoreCase(usedProjection)) {
            final int zoneIndex = (int) leaderFile.getUTMZoneIndex();

            final boolean isSouth = leaderFile.isUTMSouthHemisphere();

            double easting = leaderFile.getUTMEasting() * METER_PER_KILOMETER;     // km -> meter
            double northing = leaderFile.getUTMNorthing() * METER_PER_KILOMETER;    // km -> meter
            // easting and northing already do take into account false-easting and false-northing (rq - 14.10.2008)

            final double pixelSizeX = leaderFile.getNominalInterPixelDistance();
            final double pixelSizeY = leaderFile.getNominalInterLineDistance();
            final float orientationAngle = (float) leaderFile.getUTMOrientationAngle();

            final MapInfo mapInfo = new MapInfo(UTM.createProjection(zoneIndex - 1, isSouth),
                                                sceneWidth * 0.5f, sceneHeight * 0.5f,
                                                (float) easting, (float) northing,
                                                (float) pixelSizeX, (float) pixelSizeY, Datum.ITRF_97);
            // the BEAM convention for rotation angle uses opposite sign (rq - 16.10.2008)
            mapInfo.setOrientation(-orientationAngle);
            mapInfo.setSceneWidth(sceneWidth);
            mapInfo.setSceneHeight(sceneHeight);
            product.setGeoCoding(new MapGeoCoding(mapInfo));


        } else if (Avnir2Constants.MAP_PROJECTION_PS.equalsIgnoreCase(usedProjection)) {
            final double[] parameterValues = StereographicDescriptor.PARAMETER_DEFAULT_VALUES;
            parameterValues[0] = Ellipsoid.GRS_80.getSemiMajor();
            parameterValues[1] = Ellipsoid.GRS_80.getSemiMinor();
            final GeoPos psReferencePoint = leaderFile.getPSReferencePoint();
            final GeoPos psProjectionOrigin = leaderFile.getPSProjectionOrigin();

            parameterValues[2] = psProjectionOrigin.getLat();         // Latitude_True_Scale
            parameterValues[3] = psReferencePoint.getLon();       // Central_Meridian


            final MapTransform transform = MapTransformFactory.createTransform(StereographicDescriptor.TYPE_ID,
                                                                               parameterValues);
            final MapProjection projection = new MapProjection(StereographicDescriptor.NAME, transform);
            final double pixelSizeX = leaderFile.getNominalInterPixelDistance();
            final double pixelSizeY = leaderFile.getNominalInterLineDistance();
            final double easting = leaderFile.getPSXCoordinate() * METER_PER_KILOMETER;
            final double northing = leaderFile.getPSYCoordinate() * METER_PER_KILOMETER;
            final int sceneRasterWidth = product.getSceneRasterWidth();
            final int sceneRasterHeight = product.getSceneRasterHeight();
            final MapInfo mapInfo = new MapInfo(projection,
                                                sceneRasterWidth * 0.5f, sceneRasterHeight * 0.5f,
                                                (float) easting, (float) northing,
                                                (float) pixelSizeX, (float) pixelSizeY, Datum.ITRF_97);
            mapInfo.setOrientation((float) leaderFile.getPSOrientationAngle());
            mapInfo.setSceneWidth(sceneRasterWidth);
            mapInfo.setSceneHeight(sceneRasterHeight);
            product.setGeoCoding(new MapGeoCoding(mapInfo));

            // Alternative geo-coding for polar-stereographic
//            final double[][] l1B2Coeffs = _leaderFile.getCorrectedTransformationCoeffs();
//            final FXYSum.Cubic funcLat = new FXYSum.Cubic(CeosHelper.sortToFXYSumOrder(l1B2Coeffs[0]));
//            final FXYSum.Cubic funcLon = new FXYSum.Cubic(CeosHelper.sortToFXYSumOrder(l1B2Coeffs[1]));
//            final FXYSum.Cubic funcX = new FXYSum.Cubic(CeosHelper.sortToFXYSumOrder(l1B2Coeffs[2]));
//            final FXYSum.Cubic funcY = new FXYSum.Cubic(CeosHelper.sortToFXYSumOrder(l1B2Coeffs[3]));
//            final FXYGeoCoding funcGeoCoding = new FXYGeoCoding(0.0f, 0.0f, 1.0f, 1.0f,
//                                                                funcX, funcY, funcLat, funcLon,
//                                                                Datum.ITRF_97);
//            product.setGeoCoding(funcGeoCoding);

        } else {
            Debug.trace("Unknown map projection method. Could not create geo-coding.");
        }


    }

    Avnir2ImageFile getImageFile(final Band band) throws IOException, IllegalCeosFormatException {
        return imageFileMap.get(band);
    }

    void close() throws IOException {
        for (int i = 0; i < imageFiles.length; i++) {
            imageFiles[i].close();
            imageFiles[i] = null;
        }
        imageFiles = null;
        imageFileMap.clear();
        volumeDirectoryFile.close();
        volumeDirectoryFile = null;
        leaderFile.close();
        leaderFile = null;
        trailerFile.close();
        trailerFile = null;
        if (supplementalFile != null) {
            supplementalFile.close();
            supplementalFile = null;
        }
    }

    private Band createBand(final Avnir2ImageFile avnir2ImageFile) throws IOException,
                                                                          IllegalCeosFormatException {
        final Band band = new Band(avnir2ImageFile.getBandName(), ProductData.TYPE_UINT8,
                                   sceneWidth, sceneHeight);
        final int bandIndex = avnir2ImageFile.getBandIndex();
        band.setSpectralBandIndex(bandIndex - 1);
        band.setSpectralWavelength(avnir2ImageFile.getSpectralWavelength());
        band.setSpectralBandwidth(avnir2ImageFile.getSpectralBandwidth());
        band.setUnit(avnir2ImageFile.getGeophysicalUnit());
        final double scalingFactor = leaderFile.getAbsoluteCalibrationGain(bandIndex);
        final double scalingOffset = leaderFile.getAbsoluteCalibrationOffset(bandIndex);
        band.setScalingFactor(scalingFactor);
        band.setScalingOffset(scalingOffset);
        band.setNoDataValueUsed(false);
        band.setDescription("Radiance band " + avnir2ImageFile.getBandIndex());

        return band;
    }

    private void addMetaData(final Product product) throws IOException,
                                                           IllegalCeosFormatException {
        final MetadataElement metadata = new MetadataElement("SPH");
        metadata.addElement(leaderFile.getMapProjectionMetadata());
        metadata.addElement(leaderFile.getRadiometricMetadata());
        metadata.addElement(leaderFile.getPlatformMetadata());
        addSummaryMetadata(metadata);

        product.getMetadataRoot().addElement(metadata);

        final MetadataElement volumeDescriptor = new MetadataElement("VOLUME_DESCRIPTOR");
        volumeDirectoryFile.assignMetadataTo(volumeDescriptor);
        product.getMetadataRoot().addElement(volumeDescriptor);
    }

    private void addSummaryMetadata(final MetadataElement parent) throws IOException {
        final MetadataElement summaryMetadata = new MetadataElement("Summary Information");
        final Properties properties = new Properties();
        final File file = new File(baseDir, Avnir2Constants.SUMMARY_FILE_NAME);
        if (!file.exists()) {
            return;
        }
        properties.load(new FileInputStream(file));
        final Set unsortedEntries = properties.entrySet();
        final TreeSet sortedEntries = new TreeSet(new Comparator() {
            public int compare(final Object a, final Object b) {
                final Map.Entry entryA = (Map.Entry) a;
                final Map.Entry entryB = (Map.Entry) b;
                return ((String) entryA.getKey()).compareTo((String) entryB.getKey());
            }
        });
        sortedEntries.addAll(unsortedEntries);
        for (Object sortedEntry : sortedEntries) {
            final Map.Entry entry = (Map.Entry) sortedEntry;
            final String data = (String) entry.getValue();
            // stripp of double quotes
            final String strippedData = data.substring(1, data.length() - 1);
            final MetadataAttribute attribute = new MetadataAttribute((String) entry.getKey(),
                                                                      new ProductData.ASCII(strippedData),
                                                                      true);
            summaryMetadata.addAttribute(attribute);
        }

        parent.addElement(summaryMetadata);
    }

    private String getProductDescription() throws IOException,
                                                  IllegalCeosFormatException {
        return Avnir2Constants.PRODUCT_DESCRIPTION_PREFIX + leaderFile.getProductLevel();
    }

    private void assertSameWidthAndHeightForAllImages() throws IOException,
                                                               IllegalCeosFormatException {
        for (int i = 0; i < imageFiles.length; i++) {
            final Avnir2ImageFile imageFile = imageFiles[i];
            Guardian.assertTrue("_sceneWidth == imageFile[" + i + "].getRasterWidth()",
                                sceneWidth == imageFile.getRasterWidth());
            Guardian.assertTrue("_sceneHeight == imageFile[" + i + "].getRasterHeight()",
                                sceneHeight == imageFile.getRasterHeight());
        }
    }

    private ProductData.UTC getUTCScanStartTime() throws IOException,
                                                         IllegalCeosFormatException {
        final Calendar imageStartDate = leaderFile.getDateImageWasTaken();
        imageStartDate.add(Calendar.MILLISECOND, imageFiles[0].getTotalMillisInDayOfLine(0));
        return ProductData.UTC.create(imageStartDate.getTime(), imageFiles[0].getMicrosecondsOfLine(0));
    }

    private ProductData.UTC getUTCScanStopTime() throws IOException,
                                                        IllegalCeosFormatException {
        final Calendar imageStartDate = leaderFile.getDateImageWasTaken();
        imageStartDate.add(Calendar.MILLISECOND, imageFiles[0].getTotalMillisInDayOfLine(sceneHeight - 1));
        return ProductData.UTC.create(imageStartDate.getTime(), imageFiles[0].getMicrosecondsOfLine(sceneHeight - 1));
    }

    private ImageInputStream createInputStream(final String fileName) throws IOException {
        return new FileImageInputStream(new File(baseDir, fileName));
    }
}
