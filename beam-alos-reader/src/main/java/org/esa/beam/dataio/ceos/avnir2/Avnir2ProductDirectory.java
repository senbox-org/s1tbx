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
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.*;
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
import java.util.*;

/**
 * This class represents a product directory of an Avnir-2 product.
 * <p/>
 * <p>This class is public for the benefit of the implementation of another (internal) class and its API may
 * change in future releases of the software.</p>
 *
 * @author Marco Peters
 */
class Avnir2ProductDirectory {

    private static final double UTM_FALSE_EASTING = 500000.00;
    private static final double UTM_FALSE_NORTHING = 10000000.00;
    private static final int METER_PER_KILOMETER = 1000;

    private final File _baseDir;
    private Avnir2VolumeDirectoryFile _volumeDirectoryFile;
    private Avnir2ImageFile[] _imageFiles;
    private Map<Band, Avnir2ImageFile> _imageFileMap;
    private Avnir2LeaderFile _leaderFile;
    private Avnir2TrailerFile _trailerFile;
    private Avnir2SupplementalFile _supplementalFile = null;

    private final int _sceneWidth;
    private final int _sceneHeight;

    Avnir2ProductDirectory(final File dir) throws IOException,
            IllegalCeosFormatException {
        Guardian.assertNotNull("dir", dir);

        _baseDir = dir;
        _volumeDirectoryFile = new Avnir2VolumeDirectoryFile(_baseDir);
        _leaderFile = new Avnir2LeaderFile(createInputStream(_volumeDirectoryFile.getLeaderFileName()));
        _trailerFile = new Avnir2TrailerFile(createInputStream(_volumeDirectoryFile.getTrailerFileName()));
        if (!_leaderFile.getProductLevel().equalsIgnoreCase(Avnir2Constants.PRODUCT_LEVEL_1B2)) {
            final File supplementalFile = new File(_baseDir, _volumeDirectoryFile.getSupplementalFileName());
            if (supplementalFile.exists()) {
                _supplementalFile = new Avnir2SupplementalFile(
                        createInputStream(_volumeDirectoryFile.getSupplementalFileName()));
            }
        }

        final String[] imageFileNames = _volumeDirectoryFile.getImageFileNames();
        _imageFiles = new Avnir2ImageFile[imageFileNames.length];
        for (int i = 0; i < _imageFiles.length; i++) {
            _imageFiles[i] = new Avnir2ImageFile(createInputStream(imageFileNames[i]));
        }

        _sceneWidth = _imageFiles[0].getRasterWidth();
        _sceneHeight = _imageFiles[0].getRasterHeight();
        assertSameWidthAndHeightForAllImages();
    }

    Product createProduct() throws IOException, IllegalCeosFormatException {
        final Product product = new Product(_volumeDirectoryFile.getProductName(),
                                            getProductType(),
                                            _sceneWidth, _sceneHeight);
        product.setFileLocation(_baseDir);
        _imageFileMap = new HashMap<Band, Avnir2ImageFile>(_imageFiles.length);
        for (final Avnir2ImageFile avnir2ImageFile : _imageFiles) {
            Band band = createBand(avnir2ImageFile);
            product.addBand(band);
            _imageFileMap.put(band, avnir2ImageFile);

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
            File parentDir = _baseDir.getParentFile();
            final TreeNode<File> root = new TreeNode<File>(parentDir.getCanonicalPath());
            root.setContent(parentDir);

            final TreeNode<File> dir = new TreeNode<File>(_baseDir.getName());
            dir.setContent(_baseDir);
            root.addChild(dir);

            File volumeFile = CeosHelper.getVolumeFile(_baseDir);
            addFileToDir(dir, volumeFile);

            addFileToDir(dir, new File(_volumeDirectoryFile.getLeaderFileName()));
            addFileToDir(dir, new File(_volumeDirectoryFile.getTrailerFileName()));
            String supplemental = _volumeDirectoryFile.getSupplementalFileName();
            if (StringUtils.isNotNullAndNotEmpty(supplemental)) {
                addFileToDir(dir, new File(supplemental));
            }
            final String[] imageFileNames = _volumeDirectoryFile.getImageFileNames();
            for (int i = 0; i < imageFileNames.length; i++) {
                addFileToDir(dir, new File(imageFileNames[i]));
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
        return Avnir2Constants.PRODUCT_TYPE_PREFIX + _leaderFile.getProductLevel();
    }

    private void addGeoCoding(final Product product) throws IllegalCeosFormatException,
            IOException {

        final String usedProjection = _leaderFile.getUsedProjection();
        if (Avnir2Constants.MAP_PROJECTION_RAW.equalsIgnoreCase(usedProjection)) {
            final Band[] bands = product.getBands();
            for (int i = 0; i < bands.length; i++) {
                final Band band = bands[i];
                final Avnir2ImageFile imageFile = getImageFile(band);
                final int bandIndex = imageFile.getBandIndex();
                final double[][] uncorrectedCoeffs = _leaderFile.getUncorrectedTransformationCoeffs(bandIndex);


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
            final int zoneIndex = (int) _leaderFile.getUTMZoneIndex();

            final boolean isSouth = _leaderFile.isUTMSouthHemisphere();

            double easting = _leaderFile.getUTMEasting() * METER_PER_KILOMETER;     // km -> meter
            double northing = _leaderFile.getUTMNorthing() * METER_PER_KILOMETER;    // km -> meter
            // easting and northing already do take into account false-easting and false-northing (rq - 14.10.2008)

            final double pixelSizeX = _leaderFile.getNominalInterPixelDistance();
            final double pixelSizeY = _leaderFile.getNominalInterLineDistance();
            final float orientationAngle = (float) _leaderFile.getUTMOrientationAngle();

            final MapInfo mapInfo = new MapInfo(UTM.createProjection(zoneIndex - 1, isSouth),
                                                _sceneWidth * 0.5f, _sceneHeight * 0.5f,
                                                (float) easting, (float) northing,
                                                (float) pixelSizeX, (float) pixelSizeY, Datum.ITRF_97);
            // the BEAM convention for rotation angle uses opposite sign (rq - 16.10.2008)
            mapInfo.setOrientation(-orientationAngle);
            mapInfo.setSceneWidth(_sceneWidth);
            mapInfo.setSceneHeight(_sceneHeight);
            product.setGeoCoding(new MapGeoCoding(mapInfo));


        } else if (Avnir2Constants.MAP_PROJECTION_PS.equalsIgnoreCase(usedProjection)) {
            final double[] parameterValues = StereographicDescriptor.PARAMETER_DEFAULT_VALUES;
            parameterValues[0] = Ellipsoid.GRS_80.getSemiMajor();
            parameterValues[1] = Ellipsoid.GRS_80.getSemiMinor();
            final GeoPos psReferencePoint = _leaderFile.getPSReferencePoint();
            final GeoPos psProjectionOrigin = _leaderFile.getPSProjectionOrigin();

            parameterValues[2] = psProjectionOrigin.getLat();         // Latitude_True_Scale
            parameterValues[3] = psReferencePoint.getLon();       // Central_Meridian


            final MapTransform transform = MapTransformFactory.createTransform(StereographicDescriptor.TYPE_ID,
                                                                               parameterValues);
            final MapProjection projection = new MapProjection(StereographicDescriptor.NAME, transform);
            final double pixelSizeX = _leaderFile.getNominalInterPixelDistance();
            final double pixelSizeY = _leaderFile.getNominalInterLineDistance();
            final double easting = _leaderFile.getPSXCoordinate() * METER_PER_KILOMETER;
            final double northing = _leaderFile.getPSYCoordinate() * METER_PER_KILOMETER;
            final int sceneRasterWidth = product.getSceneRasterWidth();
            final int sceneRasterHeight = product.getSceneRasterHeight();
            final MapInfo mapInfo = new MapInfo(projection,
                                                sceneRasterWidth * 0.5f, sceneRasterHeight * 0.5f,
                                                (float) easting, (float) northing,
                                                (float) pixelSizeX, (float) pixelSizeY, Datum.ITRF_97);
            mapInfo.setOrientation((float) _leaderFile.getPSOrientationAngle());
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
        return _imageFileMap.get(band);
    }

    void close() throws IOException {
        for (int i = 0; i < _imageFiles.length; i++) {
            _imageFiles[i].close();
            _imageFiles[i] = null;
        }
        _imageFiles = null;
        _imageFileMap.clear();
        _volumeDirectoryFile.close();
        _volumeDirectoryFile = null;
        _leaderFile.close();
        _leaderFile = null;
        _trailerFile.close();
        _trailerFile = null;
        if (_supplementalFile != null) {
            _supplementalFile.close();
            _supplementalFile = null;
        }
    }

    private Band createBand(final Avnir2ImageFile avnir2ImageFile) throws IOException,
            IllegalCeosFormatException {
        final Band band = new Band(avnir2ImageFile.getBandName(), ProductData.TYPE_UINT8,
                                   _sceneWidth, _sceneHeight);
        final int bandIndex = avnir2ImageFile.getBandIndex();
        band.setSpectralBandIndex(bandIndex - 1);
        band.setSpectralWavelength(avnir2ImageFile.getSpectralWavelength());
        band.setSpectralBandwidth(avnir2ImageFile.getSpectralBandwidth());
        band.setUnit(avnir2ImageFile.getGeophysicalUnit());
        final double scalingFactor = _leaderFile.getAbsoluteCalibrationGain(bandIndex);
        final double scalingOffset = _leaderFile.getAbsoluteCalibrationOffset(bandIndex);
        band.setScalingFactor(scalingFactor);
        band.setScalingOffset(scalingOffset);
        band.setNoDataValueUsed(false);
        band.setDescription("Radiance band " + avnir2ImageFile.getBandIndex());

        return band;
    }

    private void addMetaData(final Product product) throws IOException,
            IllegalCeosFormatException {
        final MetadataElement metadata = new MetadataElement("SPH");
        metadata.addElement(_leaderFile.getMapProjectionMetadata());
        metadata.addElement(_leaderFile.getRadiometricMetadata());
        metadata.addElement(_leaderFile.getPlatformMetadata());
        addSummaryMetadata(metadata);

        product.getMetadataRoot().addElement(metadata);

        final MetadataElement volumeDescriptor = new MetadataElement("VOLUME_DESCRIPTOR");
        _volumeDirectoryFile.assignMetadataTo(volumeDescriptor);
        product.getMetadataRoot().addElement(volumeDescriptor);
    }

    private void addSummaryMetadata(final MetadataElement parent) throws IOException {
        final MetadataElement summaryMetadata = new MetadataElement("Summary Information");
        final Properties properties = new Properties();
        final File file = new File(_baseDir, Avnir2Constants.SUMMARY_FILE_NAME);
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
        for (Iterator iterator = sortedEntries.iterator(); iterator.hasNext();) {
            final Map.Entry entry = (Map.Entry) iterator.next();
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

    private int getMaxSampleValue(final int[] histogram) {
        // search for first non zero value backwards
        for (int i = histogram.length - 1; i >= 0; i--) {
            if (histogram[i] != 0) {
                return i;
            }
        }
        return 0;
    }

    private String getProductDescription() throws IOException,
            IllegalCeosFormatException {
        return Avnir2Constants.PRODUCT_DESCRIPTION_PREFIX + _leaderFile.getProductLevel();
    }

    private void assertSameWidthAndHeightForAllImages() throws IOException,
            IllegalCeosFormatException {
        for (int i = 0; i < _imageFiles.length; i++) {
            final Avnir2ImageFile imageFile = _imageFiles[i];
            Guardian.assertTrue("_sceneWidth == imageFile[" + i + "].getRasterWidth()",
                                _sceneWidth == imageFile.getRasterWidth());
            Guardian.assertTrue("_sceneHeight == imageFile[" + i + "].getRasterHeight()",
                                _sceneHeight == imageFile.getRasterHeight());
        }
    }

    private ProductData.UTC getUTCScanStartTime() throws IOException,
            IllegalCeosFormatException {
        final Calendar imageStartDate = _leaderFile.getDateImageWasTaken();
        imageStartDate.add(Calendar.MILLISECOND, _imageFiles[0].getTotalMillisInDayOfLine(0));
        return ProductData.UTC.create(imageStartDate.getTime(), _imageFiles[0].getMicrosecondsOfLine(0));
    }

    private ProductData.UTC getUTCScanStopTime() throws IOException,
            IllegalCeosFormatException {
        final Calendar imageStartDate = _leaderFile.getDateImageWasTaken();
        imageStartDate.add(Calendar.MILLISECOND, _imageFiles[0].getTotalMillisInDayOfLine(_sceneHeight - 1));
        return ProductData.UTC.create(imageStartDate.getTime(), _imageFiles[0].getMicrosecondsOfLine(_sceneHeight - 1));
    }

    private ImageInputStream createInputStream(final String fileName) throws IOException {
        return new FileImageInputStream(new File(_baseDir, fileName));
    }
}
