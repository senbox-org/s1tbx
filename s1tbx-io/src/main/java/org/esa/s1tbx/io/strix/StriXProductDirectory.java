package org.esa.s1tbx.io.strix;

import Jama.Matrix;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.io.binary.BinaryRecord;
import org.esa.s1tbx.io.ceos.CEOSImageFile;
import org.esa.s1tbx.io.ceos.CEOSProductDirectory;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.OrbitStateVector;
import org.esa.snap.engine_utilities.datamodel.Orbits;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.eo.GeoUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.util.Maths;

import java.awt.*;
import java.io.IOException;
import java.text.DateFormat;
import java.util.*;
import java.util.List;

public class StriXProductDirectory extends CEOSProductDirectory {


    protected StriXImageFile[] imageFiles = null;
    protected StriXLeaderFile leaderFile = null;
    protected StriXTrailerFile trailerFile = null;

    protected final transient Map<String, StriXImageFile> bandImageFileMap = new HashMap<>(1);
    private final DateFormat dateFormat1 = ProductData.UTC.createDateFormat("yyyyMMddHHmmssSSS");
    private final DateFormat dateFormat2 = ProductData.UTC.createDateFormat("yyyyMMdd HH:mm:ss");

    private final DateFormat dateFormat2_2 = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");
    private final DateFormat dateFormat3 = ProductData.UTC.createDateFormat("yyyyDDDSSSSSSSS");

    private float[] rangeDist;
    private boolean isProductIPF = false;

    public StriXProductDirectory(final VirtualDir dir) {
        Guardian.assertNotNull("dir", dir);

        constants = new StriXConstants();
        productDir = dir;
    }

    public int getProductLevel() {
        return leaderFile.getProductLevel();
    }

    @Override
    public CEOSImageFile getImageFile(final Band band) {
        return bandImageFileMap.get(band.getName());
    }

    @Override
    public void close() throws IOException {
        for (int i = 0; i < imageFiles.length; i++) {
            imageFiles[i].close();
            imageFiles[i] = null;
        }
        imageFiles = null;
    }

    @Override
    protected void readProductDirectory() throws IOException {
        readVolumeDirectoryFileStream();

        updateProductType();

        leaderFile = new StriXLeaderFile(getCEOSFile(constants.getLeaderFilePrefix())[0].imgInputStream);
        final CeosFile[] trlFile = getCEOSFile(constants.getTrailerFilePrefix());
        if (trlFile != null) {
            trailerFile = new StriXTrailerFile(trlFile[0].imgInputStream);
        }

        final CeosFile[] ceosFiles = getCEOSFile(constants.getImageFilePrefix());
        final List<StriXImageFile> imgArray = new ArrayList<>(ceosFiles.length);
        for (CeosFile imageFile : ceosFiles) {
            if(!isValid(imageFile.fileName)) {
                continue;
            }
            try {
                final StriXImageFile imgFile = new StriXImageFile(imageFile.imgInputStream,
                        leaderFile.getProductLevel(), imageFile.fileName);
                imgArray.add(imgFile);
            } catch (Exception e) {
                e.printStackTrace();
                // continue
            }
        }
        imageFiles = imgArray.toArray(new StriXImageFile[imgArray.size()]);

        sceneWidth = imageFiles[0].getRasterWidth();
        sceneHeight = imageFiles[0].getRasterHeight();

        if(leaderFile.getProductLevel() == StriXConstants.LEVEL1_1) {
            isProductSLC = true;
        }
    }

    @Override
    public Product createProduct() throws IOException {
        final Product product = new Product(getProductName(),
                productType, sceneWidth, sceneHeight);

        for (final StriXImageFile imageFile : imageFiles) {
            final String pol = imageFile.getPolarization();

            if (isProductSLC) {
                final Band bandI = createBand(product, "i_" + pol, Unit.REAL, imageFile);
                final Band bandQ = createBand(product, "q_" + pol, Unit.IMAGINARY, imageFile);
                ReaderUtils.createVirtualIntensityBand(product, bandI, bandQ, '_' + pol);
            } else {
                final Band band = createBand(product, "Amplitude_" + pol, Unit.AMPLITUDE, imageFile);
                SARReader.createVirtualIntensityBand(product, band, '_' + pol);
            }
        }

        product.setStartTime(getUTCScanStartTime(leaderFile.getSceneRecord(), null));
        product.setEndTime(getUTCScanStopTime(leaderFile.getSceneRecord(), null));
        product.setDescription(getProductDescription());


// Original code for products of JAXA origin
            if (isSLC()) {
                addGeoCodingFromPixelToLatLonCoefficients(product, leaderFile.getFacilityRecord());
            }

            if (product.getSceneGeoCoding() == null) {
                ReaderUtils.addGeoCoding(product, leaderFile.getLatCorners(leaderFile.getMapProjRecord()),
                        leaderFile.getLonCorners(leaderFile.getMapProjRecord()));
            }
            addTiePointGrids(product);
            addMetaData(product);
//            if (product.getSceneGeoCoding() == null) {
//                addGeoCodingFromWorkReport(product);
//            }
//
            if (product.getSceneGeoCoding() == null &&
                    leaderFile.getSceneRecord() != null && leaderFile.getFacilityRecord() != null) {
                Double refLat = leaderFile.getSceneRecord().getAttributeDouble("scene centre geodetic latitude");
                Double refLon = leaderFile.getSceneRecord().getAttributeDouble("scene centre geodetic longitude");
                if (refLat == null || refLat == 0 || refLon == null || refLon == 0) {
                    refLat = leaderFile.getFacilityRecord().getAttributeDouble("Origin Latitude");
                    refLon = leaderFile.getFacilityRecord().getAttributeDouble("Origin Longitude");
                }
                if (refLat != null && refLon != null) {
                    addTPGGeoCoding(product, refLat, refLon); //in the event that JAXA products get here
                    //this will not work because the range
                    //TiePoint grid is not initialised ...
                    //but, methods addGeoCodingFromPixelToLatLonCoefficients or
                    //(for GEC products) ReaderUtils.addGeoCoding will invariably
                    //work for JAXA product data. Should probably delete this and
                    //replace with an exception.
                }
            }
        updateMetadata(product);
        return product;
    }

    private Band createBand(final Product product, final String name, final String unit, final StriXImageFile imageFile) {

        final Band band = createBand(product, name, unit, imageFile.getBitsPerSample());
        bandImageFileMap.put(name, imageFile);

        return band;
    }

    private void addTiePointGrids(final Product product) {

        final int gridWidth = 11;
        final int gridHeight = 11;
        final int subSamplingX = product.getSceneRasterWidth() / (gridWidth - 1);
        final int subSamplingY = product.getSceneRasterHeight() / (gridHeight - 1);

        final BinaryRecord sceneRec = leaderFile.getSceneRecord();

        final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME,
                gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY);
        slantRangeGrid.setUnit(Unit.NANOSECONDS);
        product.addTiePointGrid(slantRangeGrid);

//        if (isProductIPF) {
//            setIPFTiePointGridRasterData(slantRangeGrid); //populate sR grid data
//        }

        if (sceneRec != null) {

            final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE,
                    gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY);
            incidentAngleGrid.setDiscontinuity(TiePointGrid.DISCONT_AUTO);

            incidentAngleGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(incidentAngleGrid);
        }
    }

    private static void addTPGGeoCoding(final Product product, final double refLat, final double refLon) throws IOException {

        final int gridWidth = 11;
        final int gridHeight = 11;
        final float[] targetLatTiePoints = new float[gridWidth * gridHeight];
        final float[] targetLonTiePoints = new float[gridWidth * gridHeight];
        final int sourceImageWidth = product.getSceneRasterWidth();
        final int sourceImageHeight = product.getSceneRasterHeight();

        final int isubSamplingX = sourceImageWidth / (gridWidth - 1);
        final int isubSamplingY = sourceImageHeight / (gridHeight - 1);
        final double subSamplingX = (double) isubSamplingX;
        final double subSamplingY = (double) isubSamplingY;

        final TiePointGrid slantRangeTime = product.getTiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME);
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        final double firstLineUTC = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD();
        final double lastLineUTC = absRoot.getAttributeUTC(AbstractMetadata.last_line_time).getMJD();
        final double lineTimeInterval = (lastLineUTC - firstLineUTC) / (double) (sourceImageHeight - 1);
        OrbitStateVector[] orbitStateVectors;
        try {
            orbitStateVectors = AbstractMetadata.getOrbitStateVectors(absRoot);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }

        if (!checkStateVectorValidity(orbitStateVectors))
            return;

        final int numVectors = orbitStateVectors.length;
        int startIdx = 0;
        int endIdx = 0;
        final double t1 = Math.min(firstLineUTC, lastLineUTC);
        final double t2 = Math.max(firstLineUTC, lastLineUTC);
        for (int i = 0; i < numVectors; i++) {
            double time = orbitStateVectors[i].time_mjd;
            if (time < t1) {
                startIdx = i;
            }

            if (time < t2) {
                endIdx = i;
            }
        }

        while (endIdx - startIdx + 1 < Math.min(5, numVectors)) {
            startIdx = Math.max(startIdx - 1, 0);
            endIdx = Math.min(endIdx + 1, numVectors - 1);
        }
        final int numVectorsUsed = endIdx - startIdx + 1;

        final double[] timeArray = new double[numVectorsUsed];
        final double[] xPosArray = new double[numVectorsUsed];
        final double[] yPosArray = new double[numVectorsUsed];
        final double[] zPosArray = new double[numVectorsUsed];
        final double[] xVelArray = new double[numVectorsUsed];
        final double[] yVelArray = new double[numVectorsUsed];
        final double[] zVelArray = new double[numVectorsUsed];

        for (int i = startIdx; i <= endIdx; i++) {
            timeArray[i - startIdx] = orbitStateVectors[i].time_mjd;
            xPosArray[i - startIdx] = orbitStateVectors[i].x_pos; // m
            yPosArray[i - startIdx] = orbitStateVectors[i].y_pos; // m
            zPosArray[i - startIdx] = orbitStateVectors[i].z_pos; // m
            xVelArray[i - startIdx] = orbitStateVectors[i].x_vel; // m/s
            yVelArray[i - startIdx] = orbitStateVectors[i].y_vel; // m/s
            zVelArray[i - startIdx] = orbitStateVectors[i].z_vel; // m/s
        }

        // Create new tie point grid
        int k = 0;
        for (int r = 0; r < gridHeight; r++) {
            // get the zero Doppler time for the rth line
            int y;
            y = (r * isubSamplingY);
            final double curLineUTC = firstLineUTC + y * lineTimeInterval - lineTimeInterval / 2.0; //pixel-is-point

            // compute the satellite position and velocity for the zero Doppler time using cubic interpolation
            final Orbits.OrbitVector data = getOrbitData(curLineUTC, timeArray, xPosArray, yPosArray, zPosArray,
                    xVelArray, yVelArray, zVelArray);

            for (int c = 0; c < gridWidth; c++) {
                int x;
                x = (c * isubSamplingX);
// Tie-point grid needs lat/long at top left corner of pixel - public getPixelDouble returns the tie point value
// corresponding to x+0.5, y+0.5
//
                final double slrgTime = 0.5 * (slantRangeTime.getPixelFloat(x - 1, y - 1) + slantRangeTime.getPixelFloat(x, y)) / Constants.oneBillion; // ns to s;
                final GeoPos geoPos = computeLatLon(refLat, refLon, slrgTime, data);
                targetLatTiePoints[k] = (float) geoPos.lat;
                targetLonTiePoints[k] = (float) geoPos.lon;
                ++k;
            }
        }

        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE, gridWidth, gridHeight,
                0.0f, 0.0f, subSamplingX, subSamplingY, targetLatTiePoints);

        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE, gridWidth, gridHeight,
                0.0f, 0.0f, subSamplingX, subSamplingY, targetLonTiePoints, TiePointGrid.DISCONT_AT_180);

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setSceneGeoCoding(tpGeoCoding);
    }

    /**
     * Compute accurate target geo position.
     *
     * @param refLat   The scene latitude.
     * @param refLon   The scene longitude.
     * @param slrgTime The slant range time of the given pixel.
     * @param data     The orbit data.
     * @return The geo position of the target.
     */
    private static GeoPos computeLatLon(final double refLat, final double refLon, double slrgTime, Orbits.OrbitVector data) {

        final double[] xyz = new double[3];
        final GeoPos geoPos = new GeoPos(refLat, refLon);

        // compute initial (x,y,z) coordinate from lat/lon
        GeoUtils.geo2xyz(geoPos, xyz);

        // compute accurate (x,y,z) coordinate using Newton's method
        GeoUtils.computeAccurateXYZ(data, xyz, slrgTime);

        // compute (lat, lon, alt) from accurate (x,y,z) coordinate
        GeoUtils.xyz2geo(xyz, geoPos);

        return geoPos;
    }

    private static boolean checkStateVectorValidity(OrbitStateVector[] orbitStateVectors) {

        if (orbitStateVectors == null) {
            return false;
        }
        if (orbitStateVectors.length <= 1) {
            return false;
        }

        for (int i = 1; i < orbitStateVectors.length; i++) {
            if (orbitStateVectors[i].time_mjd == orbitStateVectors[0].time_mjd) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get orbit information for given time.
     *
     * @param utc       The UTC in days.
     * @param timeArray Array holding zeros Doppler times for all state vectors.
     * @param xPosArray Array holding x coordinates for sensor positions in all state vectors.
     * @param yPosArray Array holding y coordinates for sensor positions in all state vectors.
     * @param zPosArray Array holding z coordinates for sensor positions in all state vectors.
     * @param xVelArray Array holding x velocities for sensor positions in all state vectors.
     * @param yVelArray Array holding y velocities for sensor positions in all state vectors.
     * @param zVelArray Array holding z velocities for sensor positions in all state vectors.
     * @return The orbit information.
     */
    private static Orbits.OrbitVector getOrbitData(final double utc, final double[] timeArray,
                                                   final double[] xPosArray, final double[] yPosArray, final double[] zPosArray,
                                                   final double[] xVelArray, final double[] yVelArray, final double[] zVelArray) {

        // Lagrange polynomial interpolation
        return new Orbits.OrbitVector(utc,
                Maths.lagrangeInterpolatingPolynomial(timeArray, xPosArray, utc),
                Maths.lagrangeInterpolatingPolynomial(timeArray, yPosArray, utc),
                Maths.lagrangeInterpolatingPolynomial(timeArray, zPosArray, utc),
                Maths.lagrangeInterpolatingPolynomial(timeArray, xVelArray, utc),
                Maths.lagrangeInterpolatingPolynomial(timeArray, yVelArray, utc),
                Maths.lagrangeInterpolatingPolynomial(timeArray, zVelArray, utc));
    }

    private void addMetaData(final Product product) throws IOException {
        final MetadataElement root = AbstractMetadata.addOriginalProductMetadata(product.getMetadataRoot());

        if (leaderFile != null) {
            final MetadataElement leadMetadata = new MetadataElement("Leader");
            leaderFile.addMetadata(leadMetadata);
            root.addElement(leadMetadata);
        }

        if (trailerFile != null) {
            final MetadataElement trailMetadata = new MetadataElement("Trailer");
            trailerFile.addMetadata(trailMetadata);
            root.addElement(trailMetadata);
        }

        final MetadataElement volMetadata = new MetadataElement("Volume");
        volumeDirectoryFile.assignMetadataTo(volMetadata);
        root.addElement(volMetadata);

        int c = 1;
        for (final StriXImageFile imageFile : imageFiles) {
            imageFile.assignMetadataTo(root, c++);
        }

        addSummaryMetadata(findFile(StriXConstants.SUMMARY_FILE_NAME), "Summary Information", root);
        //addSummaryMetadata(findFile(StriXConstants.WORKREPORT_FILE_NAME), "Work Report", root);

        addAbstractedMetadataHeader(product);
    }

    private void addAbstractedMetadataHeader(final Product product) {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(product.getMetadataRoot());
        final MetadataElement origProductMetadata = AbstractMetadata.getOriginalProductMetadata(product);

        final BinaryRecord sceneRec = leaderFile.getSceneRecord();
        final BinaryRecord mapProjRec = leaderFile.getMapProjRecord();
        final BinaryRecord radiometricRec = leaderFile.getRadiometricRecord();

        if (sceneRec == null)
            return;

        //mph
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, getProductName());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, getMode(productType));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR,
                sceneRec.getAttributeString("Product type descriptor"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, getMission());

        final double headingAngle = sceneRec.getAttributeDouble("Sensor clock angle");
        String look_direction;
        // -90 -> left, +90 -> right
        if (headingAngle < 0)
            look_direction = "left";
        else
            look_direction = "right";

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing, look_direction);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME,
                getProcTime(volumeDirectoryFile.getVolumeDescriptorRecord()));
        if (isProductIPF) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier, "ESA ALOS IPF");
/*		AbstractMetadata.setAttribute(absRoot, AbstractMetadata.percent_RFI_power_rejected,
                        sceneRec.getAttributeDouble("RFI %power rejected"));
		int fmethod = sceneRec.getAttributeInt("Faraday rotation method flag");

		if(!(fmethod==0)) {
			AbstractMetadata.setAttribute(absRoot, AbstractMetadata.Faraday_rotation_angle,
                        sceneRec.getAttributeDouble("Faraday rotation angle"));
		}
		if(fmethod==2) {
			AbstractMetadata.setAttribute(absRoot, AbstractMetadata.Faraday_angle_estimation_method,"FROM PLR PRODUCT");
		}
		else if(fmethod==1) {
			AbstractMetadata.setAttribute(absRoot, AbstractMetadata.Faraday_angle_estimation_method,"FROM TEC DATA");
		}
		if(fmethod==2) {
			int val = sceneRec.getAttributeInt("Faraday correction applied flag");
			if(val==1) AbstractMetadata.setAttribute(absRoot, AbstractMetadata.Faraday_correction_applied,"TRUE");
			else AbstractMetadata.setAttribute(absRoot, AbstractMetadata.Faraday_correction_applied,"FALSE");
			val = sceneRec.getAttributeInt("Crosstalk correction applied flag");
			if(val==1) AbstractMetadata.setAttribute(absRoot, AbstractMetadata.cross_talk_correction_applied,"TRUE");
                        else AbstractMetadata.setAttribute(absRoot, AbstractMetadata.cross_talk_correction_applied,"FALSE");
			val = sceneRec.getAttributeInt("Channel imbalance correction applied flag");
                        if(val==1) AbstractMetadata.setAttribute(absRoot, AbstractMetadata.channel_imbalance_correction_applied,"TRUE");
                        else AbstractMetadata.setAttribute(absRoot, AbstractMetadata.channel_imbalance_correction_applied,"FALSE");
			val = sceneRec.getAttributeInt("Channel symmetrisation applied flag");
                        if(val==1) AbstractMetadata.setAttribute(absRoot, AbstractMetadata.symmetrisation_applied,"TRUE");
                        else AbstractMetadata.setAttribute(absRoot, AbstractMetadata.symmetrisation_applied,"FALSE");
		}

		AbstractMetadata.setAttribute(absRoot, AbstractMetadata.irsr_const,
                        sceneRec.getAttributeDouble("Image range to slant constant term"));
		AbstractMetadata.setAttribute(absRoot, AbstractMetadata.irsr_lin,
                        sceneRec.getAttributeDouble("Image range to slant linear term"));
		AbstractMetadata.setAttribute(absRoot, AbstractMetadata.irsr_quad,
                        sceneRec.getAttributeDouble("Image range to slant quadratic term"));
		AbstractMetadata.setAttribute(absRoot, AbstractMetadata.irsr_cubic,
                        sceneRec.getAttributeDouble("Image range to slant cubic term"));
*/
        } else {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier,
                    sceneRec.getAttributeString("Processing system identifier").trim());
        }
        // cycle n/a?

        //AbstractMetadata.setAttribute(absRoot, AbstractMetadata.REL_ORBIT,
        //        Integer.parseInt(sceneRec.getAttributeString("Orbit number").trim()));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT,
                Integer.parseInt(sceneRec.getAttributeString("Orbit number").trim()));

        final ProductData.UTC startTime = getStartTime(sceneRec, origProductMetadata, "StartDateTime");
        product.setStartTime(startTime);
        final ProductData.UTC endTime = getEndTime(sceneRec, origProductMetadata, "EndDateTime", startTime);
        product.setEndTime(endTime);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, endTime);

        if (mapProjRec != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat,
                    mapProjRec.getAttributeDouble("1st line 1st pixel geodetic latitude"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long,
                    mapProjRec.getAttributeDouble("1st line 1st pixel geodetic longitude"));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat,
                    mapProjRec.getAttributeDouble("1st line last valid pixel geodetic latitude"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long,
                    mapProjRec.getAttributeDouble("1st line last valid pixel geodetic longitude"));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat,
                    mapProjRec.getAttributeDouble("Last line 1st pixel geodetic latitude"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long,
                    mapProjRec.getAttributeDouble("Last line 1st pixel geodetic longitude"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat,
                    mapProjRec.getAttributeDouble("Last line last valid pixel geodetic latitude"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long,
                    mapProjRec.getAttributeDouble("Last line last valid pixel geodetic longitude"));

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, getPass(mapProjRec, sceneRec));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                    mapProjRec.getAttributeDouble("Nominal inter-pixel distance in output scene"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                    mapProjRec.getAttributeDouble("Nominal inter-line distance in output scene"));
        } else if (sceneRec != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                    sceneRec.getAttributeDouble("Pixel spacing"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                    sceneRec.getAttributeDouble("Line spacing"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, getPass(mapProjRec, sceneRec));
        }

        //sph
        AbstractMetadata.setAttribute(absRoot, "SAMPLE_TYPE", getSampleType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.algorithm,
                sceneRec.getAttributeString("Processing algorithm identifier"));

        final Set<String> polSet = new TreeSet<>();
        for (StriXImageFile imageFile : imageFiles) {
            if (imageFile != null) {
                polSet.add(imageFile.getPolarization());
            }
        }
        int i = 0;
        for (String pol : polSet) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.polarTags[i++], pol);
        }

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks,
                sceneRec.getAttributeDouble("Nominal number of looks processed in azimuth"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks,
                sceneRec.getAttributeDouble("Nominal number of looks processed in range"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency,
                sceneRec.getAttributeDouble("Pulse Repetition Frequency") / 1000.0);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency, getRadarFrequency(sceneRec));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel,
                imageFiles[0].getSlantRangeToFirstPixel(0));

        // add Range and Azimuth bandwidth
        final double rangeBW = (sceneRec.getAttributeDouble("Total processor bandwidth in range") / 1000.0); // MHz
        final double azimuthBW = sceneRec.getAttributeDouble("Total processor bandwidth in azimuth"); // Hz

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, azimuthBW);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
                ReaderUtils.getLineTimeInterval(startTime, endTime, sceneHeight));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines,
                product.getSceneRasterHeight());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line,
                product.getSceneRasterWidth());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.TOT_SIZE, ReaderUtils.getTotalSize(product));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, isGroundRange());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.map_projection, getMapProjection());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.geo_ref_system,
                sceneRec.getAttributeString("Ellipsoid designator"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ant_elev_corr_flag, 1);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spread_comp_flag, 1);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.replica_power_corr_flag, 0);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.abs_calibration_flag, 0);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, 0);
        if (radiometricRec != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.calibration_factor,
                    radiometricRec.getAttributeDouble("Calibration factor"));
            absRoot.getAttribute(AbstractMetadata.calibration_factor).setUnit("dB");
        }
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate,
                sceneRec.getAttributeDouble("Range sampling rate"));

        addOrbitStateVectors(absRoot, leaderFile.getPlatformPositionRecord());

        addDopplerCentroidCoefficients(absRoot, sceneRec);


    }

    private String getMapProjection() {
        if (leaderFile.getMapProjRecord() == null) return " ";
        final String projDesc = leaderFile.getMapProjRecord().getAttributeString("Map projection descriptor").toLowerCase();
        if (projDesc.contains("geo") || getProductType().contains("1.5G"))
            return "Geocoded";
        return " ";
    }

    private int isGroundRange() {
        if (leaderFile.getMapProjRecord() == null) return isProductSLC ? 0 : 1;
        final String projDesc = leaderFile.getMapProjRecord().getAttributeString("Map projection descriptor").toLowerCase();
        if (projDesc.contains("slant"))
            return 0;
        return 1;
    }

    private ProductData.UTC getStartTime(final BinaryRecord sceneRec, final MetadataElement origProductMetadata,
                                         final String tagInSummary) {
        ProductData.UTC time = getUTCScanStartTime(sceneRec, null);
        if (time.equalElems(AbstractMetadata.NO_METADATA_UTC)) {
            try {
                ProductData.UTC summaryTime = null;
                final MetadataElement summaryElem = origProductMetadata.getElement("Summary Information");
                if (summaryElem != null) {
                    for (MetadataAttribute sum : summaryElem.getAttributes()) {
                        if (sum.getName().contains(tagInSummary)) {
                            summaryTime = AbstractMetadata.parseUTC(
                                    summaryElem.getAttributeString(sum.getName().trim()), dateFormat2_2);
                        }
                    }
                }

                ProductData.UTC workReportTime = null;
                final MetadataElement workReportElem = origProductMetadata.getElement("Work Report");
                if (workReportElem != null) {
                    String valueStr = workReportElem.getAttributeString("Img_SceneStartDateTime");
                    if (valueStr != null && valueStr.length() > 0) {
                        workReportTime = AbstractMetadata.parseUTC(valueStr, dateFormat2_2);
                    }
                    if (workReportTime == null) {
                        valueStr = workReportElem.getAttributeString("Brs_SceneStartDateTime");
                        if (valueStr != null && valueStr.length() > 0) {
                            workReportTime = AbstractMetadata.parseUTC(valueStr, dateFormat2);
                        }
                    }
                }

                ProductData.UTC imgRecTime = null;
                final MetadataElement imageDescriptorElem = origProductMetadata.getElement("Image Descriptor 1");
                if (imageDescriptorElem != null) {
                    final MetadataElement imageRecordElem = imageDescriptorElem.getElement("Image Record");
                    if (imageRecordElem != null) {
                        final int year = imageFiles[0].startYear;
                        final int days = imageFiles[0].startDay;
                        final int seconds = imageFiles[0].startMsec / 1000;
                        final int milliseconds = imageFiles[0].startMsec - seconds * 1000;
                        StringBuilder sb = new StringBuilder(String.valueOf(year));
                        String dayStr = String.valueOf(days);
                        for (int i = dayStr.length(); i < 3; i++) {
                            sb.append('0');
                        }
                        sb.append(dayStr);

                        String secondStr = String.valueOf(seconds);
                        for (int i = secondStr.length(); i < 5; i++) {
                            sb.append('0');
                        }
                        sb.append(secondStr);
                        sb.append("000"); // a 'quirk' of UTC.parse is that it wants msec of day (which it rounds to
//					     integer seconds) - it will take a period '.' followed by the fractional
//					     time in msecs (or microsecs) ...
                        sb.append('.');

                        String millisecondStr = String.valueOf(milliseconds);
                        for (int i = millisecondStr.length(); i < 3; i++) {
                            sb.append('0');
                        }
                        sb.append(millisecondStr);

                        imgRecTime = ProductData.UTC.parse(sb.toString(), dateFormat3);
                    }
                }

                if (summaryTime != null) {
                    return summaryTime;
                } else if (workReportTime != null) {
                    return workReportTime;
                }
                return imgRecTime;

            } catch (Exception e) {
                time = AbstractMetadata.NO_METADATA_UTC;
            }
        }
        return time;
    }

    private ProductData.UTC getEndTime(final BinaryRecord sceneRec, final MetadataElement origProductMetadata,
                                       final String tagInSummary, final ProductData.UTC startTime) {
        ProductData.UTC time = getUTCScanStartTime(sceneRec, null);
        if (time.equalElems(AbstractMetadata.NO_METADATA_UTC)) {
            try {
                ProductData.UTC summaryTime = null;
                final MetadataElement summaryElem = origProductMetadata.getElement("Summary Information");
                if (summaryElem != null) {
                    for (MetadataAttribute sum : summaryElem.getAttributes()) {
                        if (sum.getName().contains(tagInSummary)) {
                            summaryTime = AbstractMetadata.parseUTC(
                                    summaryElem.getAttributeString(sum.getName().trim()), dateFormat2_2);
                        }
                    }
                }

                ProductData.UTC workReportTime = null;
                final MetadataElement workReportElem = origProductMetadata.getElement("Work Report");
                if (workReportElem != null) {
                    String valueStr = workReportElem.getAttributeString("Img_SceneEndDateTime");
                    if (valueStr != null && valueStr.length() > 0) {
                        workReportTime = AbstractMetadata.parseUTC(valueStr, dateFormat2_2);
                    }
                    if (workReportTime == null) {
                        valueStr = workReportElem.getAttributeString("Brs_SceneEndDateTime");
                        if (valueStr != null && valueStr.length() > 0) {
                            workReportTime = AbstractMetadata.parseUTC(valueStr, dateFormat2);
                        }
                    }
                    if (workReportTime == null) {
                        for (MetadataAttribute workRep : workReportElem.getAttributes()) {
                            if (workRep.getName().contains("SceneCenterDateTime")) {
                                final ProductData.UTC centreTime = AbstractMetadata.parseUTC(
                                        workReportElem.getAttributeString(workRep.getName().trim()), dateFormat2_2);
                                final double diff = centreTime.getMJD() - startTime.getMJD();
                                workReportTime = new ProductData.UTC(startTime.getMJD() + (diff * 2.0));
                            }
                        }
                    }
                }

                ProductData.UTC imgRecTime = null;
                final MetadataElement imageDescriptorElem = origProductMetadata.getElement("Image Descriptor 1");
                if (imageDescriptorElem != null) {
//                    final int numRecords = imageDescriptorElem.getAttributeInt("Number of SAR DATA records", 0);
                    final MetadataElement imageRecordElem = imageDescriptorElem.getElement("Image Record");
                    if (imageRecordElem != null) {
                        final int year = imageFiles[0].endYear;
                        final int days = imageFiles[0].endDay;
                        final int seconds = imageFiles[0].endMsec / 1000;
                        final int milliseconds = imageFiles[0].endMsec - seconds * 1000;
//                        final double prf = imageRecordElem.getAttributeDouble("PRF", 0);
//                        milliseconds += (int)((numRecords - 1) * Constants.oneMillion / prf);

                        StringBuilder sb = new StringBuilder(String.valueOf(year));
                        String dayStr = String.valueOf(days);
                        for (int i = dayStr.length(); i < 3; i++) {
                            sb.append('0');
                        }
                        sb.append(dayStr);

                        String secondStr = String.valueOf(seconds);
                        for (int i = secondStr.length(); i < 5; i++) {
                            sb.append('0');
                        }
                        sb.append(secondStr);
                        sb.append("000");
                        sb.append('.');

                        String millisecondStr = String.valueOf(milliseconds);
                        for (int i = millisecondStr.length(); i < 3; i++) {
                            sb.append('0');
                        }
                        sb.append(millisecondStr);

                        imgRecTime = ProductData.UTC.parse(sb.toString(), dateFormat3);
                    }
                }

                if (summaryTime != null) {
                    return summaryTime;
                } else if (workReportTime != null) {
                    return workReportTime;
                } else if (imgRecTime != null) {
                    return imgRecTime;
                }

                final String centreTimeStr = sceneRec.getAttributeString("Scene centre time");
                final ProductData.UTC centreTime = AbstractMetadata.parseUTC(centreTimeStr.trim(), dateFormat1);
                final double diff = centreTime.getMJD() - startTime.getMJD();
                return new ProductData.UTC(startTime.getMJD() + (diff * 2.0));
            } catch (Exception e) {
                time = AbstractMetadata.NO_METADATA_UTC;
            }
        }
        return time;
    }

//    private static void addGeoCodingFromWorkReport(Product product) {
//
//        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
//        final MetadataElement workReportElem = AbstractMetadata.getOriginalProductMetadata(product).getElement("Work Report");
//        if (workReportElem != null) {
//
//            try {
//                float latUL = Float.parseFloat(workReportElem.getAttributeString("Brs_ImageSceneLeftTopLatitude", "0"));
//                float latUR = Float.parseFloat(workReportElem.getAttributeString("Brs_ImageSceneRightTopLatitude", "0"));
//                float latLL = Float.parseFloat(workReportElem.getAttributeString("Brs_ImageSceneLeftBottomLatitude", "0"));
//                float latLR = Float.parseFloat(workReportElem.getAttributeString("Brs_ImageSceneRightBottomLatitude", "0"));
//
//                float lonUL = Float.parseFloat(workReportElem.getAttributeString("Brs_ImageSceneLeftTopLongitude", "0"));
//                float lonUR = Float.parseFloat(workReportElem.getAttributeString("Brs_ImageSceneRightTopLongitude", "0"));
//                float lonLL = Float.parseFloat(workReportElem.getAttributeString("Brs_ImageSceneLeftBottomLongitude", "0"));
//                float lonLR = Float.parseFloat(workReportElem.getAttributeString("Brs_ImageSceneRightBottomLongitude", "0"));
//
//                // The corner point geo positions above are given with respect to the scene, not the SAR image.
//                // Therefore, they should first be mapped to the corner points of the SAR image before being used
//                // in generating the geocoding of the SAR image. Here we assume that the SAR image is always
//                // displayed with the first azimuth line on the top and the near range side on the left.
//                // For level 1.5 detective product, this is not a problem because it is projected product.
//                String pass = absRoot.getAttributeString("PASS");
//                String prodType = absRoot.getAttributeString("PRODUCT_TYPE");
//                if (prodType.contains("1.1")) {
//                    float temp;
//                    if (pass.equals("ASCENDING")) {
//                        temp = latUL;
//                        latUL = latLL;
//                        latLL = temp;
//                        temp = latUR;
//                        latUR = latLR;
//                        latLR = temp;
//                        temp = lonUL;
//                        lonUL = lonLL;
//                        lonLL = temp;
//                        temp = lonUR;
//                        lonUR = lonLR;
//                        lonLR = temp;
//                    } else { // DESCENDING
//                        temp = latUL;
//                        latUL = latUR;
//                        latUR = temp;
//                        temp = latLL;
//                        latLL = latLR;
//                        latLR = temp;
//                        temp = lonUL;
//                        lonUL = lonUR;
//                        lonUR = temp;
//                        temp = lonLL;
//                        lonLL = lonLR;
//                        lonLR = temp;
//                    }
//                }
//
//                final float[] latCorners = new float[]{latUL, latUR, latLL, latLR};
//                final float[] lonCorners = new float[]{lonUL, lonUR, lonLL, lonLR};
//
//                absRoot.setAttributeDouble(AbstractMetadata.first_near_lat, latUL);
//                absRoot.setAttributeDouble(AbstractMetadata.first_near_long, lonUL);
//                absRoot.setAttributeDouble(AbstractMetadata.first_far_lat, latUR);
//                absRoot.setAttributeDouble(AbstractMetadata.first_far_long, lonUR);
//                absRoot.setAttributeDouble(AbstractMetadata.last_near_lat, latLL);
//                absRoot.setAttributeDouble(AbstractMetadata.last_near_long, lonLL);
//                absRoot.setAttributeDouble(AbstractMetadata.last_far_lat, latLR);
//                absRoot.setAttributeDouble(AbstractMetadata.last_far_long, lonLR);
//
//                ReaderUtils.addGeoCoding(product, latCorners, lonCorners);
//            } catch (Exception e) {
//                Debug.trace(e.toString());
//            }
//        }
//    }

    private static void addGeoCodingFromPixelToLatLonCoefficients(final Product product,
                                                                  final BinaryRecord facilityRecord) {

        if (facilityRecord == null || facilityRecord.getAttributeDouble("Origin Line") == null) {
            System.out.format("cannot access facilityRecord\n");
            return;
        }
        final int originLine = (int) Math.floor(facilityRecord.getAttributeDouble("Origin Line"));
        final int originPixel = (int) Math.floor(facilityRecord.getAttributeDouble("Origin Pixel"));

        // get pixel to lat/lon coefficients
        final int numCoefficients = 50;
        double[] a = new double[numCoefficients / 2];       // pixel to lat coefficients
        double[] b = new double[numCoefficients / 2];       // pixel to lon coefficients
        boolean coeffNonZero = false;
        for (int i = 0; i < numCoefficients; i++) {
            final double c = facilityRecord.getAttributeDouble("Pixel to Lat Lon coefficients " + (i + 1));
            if (!coeffNonZero && c != 0) {
                coeffNonZero = true;
            }
            if (i < numCoefficients / 2) {
                a[i] = c;
            } else {
                b[i - numCoefficients / 2] = c;
            }
        }

        if (!coeffNonZero) {
            return;
        }

        //a[24] = facilityRecord.getAttributeDouble("Origin Latitude");
        //b[24] = facilityRecord.getAttributeDouble("Origin Longitude");

        // create geocoding grid
        final int gridWidth = 11;
        final int gridHeight = 11;
        final float[] targetLatTiePoints = new float[gridWidth * gridHeight];
        final float[] targetLonTiePoints = new float[gridWidth * gridHeight];
        final int sourceImageWidth = product.getSceneRasterWidth();
        final int sourceImageHeight = product.getSceneRasterHeight();

        final double subSamplingX = sourceImageWidth / (double) (gridWidth - 1);
        final double subSamplingY = sourceImageHeight / (double) (gridHeight - 1);

        double lat, lon;
        double x, x2, x3, x4, y, y2, y3, y4;
        int k = 0;
        for (int r = 0; r < gridHeight; r++) {
            y = r * subSamplingY - originLine;
            y2 = y * y;
            y3 = y2 * y;
            y4 = y2 * y2;

            for (int c = 0; c < gridWidth; c++) {
                x = c * subSamplingX - originPixel;
                x2 = x * x;
                x3 = x2 * x;
                x4 = x2 * x2;

                lat = a[0] * x4 * y4 + a[1] * x4 * y3 + a[2] * x4 * y2 + a[3] * x4 * y + a[4] * x4 +
                        a[5] * x3 * y4 + a[6] * x3 * y3 + a[7] * x3 * y2 + a[8] * x3 * y + a[9] * x3 +
                        a[10] * x2 * y4 + a[11] * x2 * y3 + a[12] * x2 * y2 + a[13] * x2 * y +
                        a[14] * x2 + a[15] * x * y4 + a[16] * x * y3 + a[17] * x * y2 + a[18] * x * y +
                        a[19] * x + a[20] * y4 + a[21] * y3 + a[22] * y2 + a[23] * y + a[24];

                lon = b[0] * x4 * y4 + b[1] * x4 * y3 + b[2] * x4 * y2 + b[3] * x4 * y + b[4] * x4 +
                        b[5] * x3 * y4 + b[6] * x3 * y3 + b[7] * x3 * y2 + b[8] * x3 * y + b[9] * x3 +
                        b[10] * x2 * y4 + b[11] * x2 * y3 + b[12] * x2 * y2 + b[13] * x2 * y +
                        b[14] * x2 + b[15] * x * y4 + b[16] * x * y3 + b[17] * x * y2 + b[18] * x * y +
                        b[19] * x + b[20] * y4 + b[21] * y3 + b[22] * y2 + b[23] * y + b[24];

                targetLatTiePoints[k] = (float) lat;
                targetLonTiePoints[k] = (float) lon;
                ++k;
            }
        }

        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE, gridWidth, gridHeight,
                0.0f, 0.0f, (int) subSamplingX, (int) subSamplingY, targetLatTiePoints);

        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE, gridWidth, gridHeight,
                0.0f, 0.0f, (int) subSamplingX, (int) subSamplingY, targetLonTiePoints, TiePointGrid.DISCONT_AT_180);

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid);

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setSceneGeoCoding(tpGeoCoding);
    }

    private static void updateMetadata(final Product product) {
        final GeoCoding geoCoding = product.getSceneGeoCoding();
        if (geoCoding == null) return;

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        final GeoPos geo00 = geoCoding.getGeoPos(new PixelPos(0, 0), null);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, geo00.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, geo00.getLon());

        final GeoPos geo01 = geoCoding.getGeoPos(new PixelPos(w-1, 0), null);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, geo01.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, geo01.getLon());

        final GeoPos geo10 = geoCoding.getGeoPos(new PixelPos(0, h-1), null);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, geo10.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, geo10.getLon());

        final GeoPos geo11 = geoCoding.getGeoPos(new PixelPos(w-1, h-1), null);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, geo11.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, geo11.getLon());

//        //todo causes lazy tpg to load
//        // ReaderUtils.addMetadataIncidenceAngles(product);
//
//        if(isESAProduct(product)){
//            addSRGR(product);
//        }
    }

    private String getProductName() {
        return getMission() + '-' + volumeDirectoryFile.getProductName();
    }

    private static final String[] excludeExt = new String[] {".jpg",".hdr"};

    private static boolean isValid(final String name) {
        for(String ext : excludeExt) {
            if(name.endsWith(ext))
                return false;
        }
        return true;
    }

    private void updateProductType() {
        String prodType = productType.toUpperCase();
        while (prodType.endsWith("A") || prodType.endsWith("D") || prodType.endsWith("U") || prodType.endsWith("_")) {
            prodType = prodType.substring(0, prodType.length() - 1);
        }
        productType = prodType;
    }

    private String getMode(final String productType) {
        if(productType.contains("SM"))
            return "StripMap";
        else if(productType.contains("SL"))
            return "Spotlight";
        else if(productType.contains("SC"))
            return "ScanSAR";
        return "-";
    }

    public void readTiePointGridRasterData(final TiePointGrid tpg, Rectangle destRect, ProductData destBuffer, ProgressMonitor pm) {

        final int gridWidth = destRect.width;
        final int gridHeight = destRect.height;
        final int subSamplingX = (int) tpg.getSubSamplingX();
        final float[] rangeTime = new float[gridWidth * gridHeight];

        final BinaryRecord sceneRec = leaderFile.getSceneRecord();

        if (rangeDist == null) {
            rangeDist = new float[gridWidth * gridHeight];

            // slant range time (2-way)
            if (leaderFile.getProductLevel() == StriXConstants.LEVEL1_1 && sceneRec != null) {

                final double samplingRate = sceneRec.getAttributeDouble("Range sampling rate") * Constants.oneMillion;  // MHz to Hz

                final double rangePixelSpacing = subSamplingX * Constants.halfLightSpeed / samplingRate;
                final double slantRangeToFirstPixel = imageFiles[0].getSlantRangeToFirstPixel(0);
                int k = 0;
                for (int j = destRect.y; j < gridHeight; j++) {
                    for (int i = destRect.x; i < gridWidth; i++) {
                        rangeDist[k++] = (float) (slantRangeToFirstPixel + i * rangePixelSpacing);
                    }
                }

            } else if (leaderFile.getProductLevel() == StriXConstants.LEVEL1_5) {

                final double slantRangeToFirstPixel = imageFiles[0].getSlantRangeToFirstPixel(0); // meters
                final double slantRangeToMidPixel = imageFiles[0].getSlantRangeToMidPixel(0);
                final double slantRangeToLastPixel = imageFiles[0].getSlantRangeToLastPixel(0);

                int k = 0;
                for (int j = destRect.y; j < gridHeight; j++) {
                    final double[] polyCoef = computePolynomialCoefficients(slantRangeToFirstPixel,
                            slantRangeToMidPixel,
                            slantRangeToLastPixel,
                            sceneWidth);

                    for (int i = destRect.x; i < gridWidth; i++) {
                        final int x = i * subSamplingX;
                        rangeDist[k++] = (float) (polyCoef[0] + polyCoef[1] * x + polyCoef[2] * x * x);
                    }
                }
            }
        }

        if (tpg.getName().equals(OperatorUtils.TPG_SLANT_RANGE_TIME)) {
            // get slant range time in nanoseconds from range distance in meters
            for (int k = 0; k < rangeDist.length; k++) {
                rangeTime[k] = (float) (rangeDist[k] / Constants.halfLightSpeed * Constants.oneBillion); // in ns
            }

            destBuffer.setElems(rangeTime);
        } else if (tpg.getName().equals(OperatorUtils.TPG_INCIDENT_ANGLE)) {

            if (sceneRec != null) {
                final double a0 = sceneRec.getAttributeDouble("Incidence angle constant term");
                final double a1 = sceneRec.getAttributeDouble("Incidence angle linear term");
                final double a2 = sceneRec.getAttributeDouble("Incidence angle quadratic term");
//                final double a3 = sceneRec.getAttributeDouble("Incidence angle cubic term");
//                final double a4 = sceneRec.getAttributeDouble("Incidence angle fourth term");
//                final double a5 = sceneRec.getAttributeDouble("Incidence angle fifth term");
                final double inc_cen = sceneRec.getAttributeDouble("Incidence angle at scene centre");
                final float[] angles = new float[gridWidth * gridHeight];
                int k = 0;
                for (int j = destRect.y; j < gridHeight; j++) {
                    for (int i = destRect.x; i < gridWidth; i++) {
                        angles[k] = (float) ((a0 + a1 * rangeDist[k] / 1000.0 +
                                a2 * FastMath.pow(rangeDist[k] / 1000.0, 2.0)) * Constants.RTOD);
                        k++;
                    }
                }

                destBuffer.setElems(angles);
            }
        }
    }

    private static double[] computePolynomialCoefficients(
            double slantRangeToFirstPixel, double slantRangeToMidPixel, double slantRangeToLastPixel, int imageWidth) {

        final int firstPixel = 0;
        final int midPixel = imageWidth / 2;
        final int lastPixel = imageWidth - 1;
        final double[] idxArray = {firstPixel, midPixel, lastPixel};
        final double[] rangeArray = {slantRangeToFirstPixel, slantRangeToMidPixel, slantRangeToLastPixel};
        final Matrix A = Maths.createVandermondeMatrix(idxArray, 2);
        final Matrix b = new Matrix(rangeArray, 3);
        final Matrix x = A.solve(b);
        return x.getColumnPackedCopy();
    }

    public boolean isSTRIX() throws IOException {
        final String volumeId = getVolumeId().toUpperCase();
        final String logicalVolumeId = getLogicalVolumeId().toUpperCase();
        return (volumeId.contains("STRIX") || logicalVolumeId.contains("STRIX"));
    }

    protected String getMission() {
        return "STRIX";
    }

    protected String getProductDescription() {
        return StriXConstants.PRODUCT_DESCRIPTION_PREFIX + leaderFile.getProductLevel();
    }
}
