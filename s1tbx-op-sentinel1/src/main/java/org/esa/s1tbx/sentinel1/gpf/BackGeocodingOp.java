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
package org.esa.s1tbx.sentinel1.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.insar.gpf.coregistration.DEMAssistedCoregistrationOp;
import org.esa.s1tbx.insar.gpf.support.SARGeocoding;
import org.esa.s1tbx.insar.gpf.support.Sentinel1Utils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.dem.dataio.EarthGravitationalModel96;
import org.esa.snap.dem.dataio.FileElevationModel;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.PosVector;
import org.esa.snap.engine_utilities.datamodel.ProductInformation;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.eo.GeoUtils;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.jlinda.core.delaunay.TriangleInterpolator;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * "Backgeocoding" + "Coregistration" processing blocks in The Sentinel-1 TOPS InSAR processing chain.
 * Burst co-registration is performed using orbits and DEM.
 */
@OperatorMetadata(alias = "Back-Geocoding",
        category = "Radar/Coregistration/S-1 TOPS Coregistration",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Bursts co-registration using orbit and DEM")
public final class BackGeocodingOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The digital elevation model.",
            defaultValue = "SRTM 3Sec", label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(defaultValue = ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
            label = "DEM Resampling Method")
    private String demResamplingMethod = ResamplingFactory.BICUBIC_INTERPOLATION_NAME;

    @Parameter(label = "External DEM")
    private File externalDEMFile = null;

    @Parameter(label = "DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(defaultValue = ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME,
            description = "The method to be used when resampling the slave grid onto the master grid.",
            label = "Resampling Type")
    private String resamplingType = ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME;

    @Parameter(defaultValue = "true", label = "Mask out areas with no elevation")
    private boolean maskOutAreaWithoutElevation = true;

    @Parameter(defaultValue = "false", label = "Output Range and Azimuth Offset")
    private boolean outputRangeAzimuthOffset = false;

    @Parameter(defaultValue = "false", label = "Output Deramp and Demod Phase")
    private boolean outputDerampDemodPhase = false;

    @Parameter(defaultValue = "false", label = "Disable Reramp")
    private boolean disableReramp = false;

    private Resampling selectedResampling = null;

    private Product masterProduct = null;
    private Product slaveProduct = null;

    private Sentinel1Utils mSU = null;
    private Sentinel1Utils sSU = null;
    private Sentinel1Utils.SubSwathInfo[] mSubSwath = null;
    private Sentinel1Utils.SubSwathInfo[] sSubSwath = null;

    private ElevationModel dem = null;
    private boolean isElevationModelAvailable = false;
    private double demNoDataValue = 0; // no data value for DEM
    private double noDataValue = 0.0;

	private int subSwathIndex = 0;
    private int burstOffset = 0;
    private boolean burstOffsetComputed = false;
    private String swathIndexStr = null;

    private SARGeocoding.Orbit mOrbit = null;
    private SARGeocoding.Orbit sOrbit = null;

    private final HashMap<Band, Band> targetBandToSlaveBandMap = new HashMap<>(2);

    private static final double invalidIndex = -9999.0;

    private static final String PRODUCT_SUFFIX = "_Stack";

    private boolean outputDEM = false;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public BackGeocodingOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            if (sourceProduct == null) {
                return;
            }

            checkSourceProductValidity();

            masterProduct = sourceProduct[0];
            slaveProduct = sourceProduct[1];

            mSU = new Sentinel1Utils(masterProduct);
            sSU = new Sentinel1Utils(slaveProduct);
            sSU.computeDopplerRate();
            sSU.computeReferenceTime();

            mOrbit = mSU.getOrbit();
            sOrbit = sSU.getOrbit();
            /*
            outputToFile("c:\\output\\mSensorPosition.dat", mOrbit.sensorPosition);
            outputToFile("c:\\output\\mSensorVelocity.dat", mOrbit.sensorVelocity);
            outputToFile("c:\\output\\sSensorPosition.dat", sOrbit.sensorPosition);
            outputToFile("c:\\output\\sSensorVelocity.dat", sOrbit.sensorVelocity);
            */
            mSubSwath = mSU.getSubSwath();
            sSubSwath = sSU.getSubSwath();
			
			final String[] mSubSwathNames = mSU.getSubSwathNames();
			final String[] sSubSwathNames = sSU.getSubSwathNames();
			if (mSubSwathNames.length != 1 || sSubSwathNames.length != 1) {
                throw new OperatorException("Split product is expected.");
            }
			
			if (!mSubSwathNames[0].equals(sSubSwathNames[0])) {
				throw new OperatorException("Same sub-swath is expected.");
			}

			subSwathIndex = 1; // subSwathIndex is always 1 because of split product
            swathIndexStr = mSubSwathNames[0].substring(2);

            final String[] mPolarizations = mSU.getPolarizations();
			final String[] sPolarizations = sSU.getPolarizations();
			if (!StringUtils.containsIgnoreCase(sPolarizations, mPolarizations[0])) {
				throw new OperatorException("Same polarization is expected.");
			}

            if (externalDEMFile == null) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            DEMFactory.validateDEM(demName, masterProduct);

            selectedResampling = ResamplingFactory.createResampling(resamplingType);

            createTargetProduct();

            StackUtils.saveMasterProductBandNames(targetProduct, masterProduct.getBandNames());
            StackUtils.saveSlaveProductNames(sourceProduct, targetProduct,
                    masterProduct, targetBandToSlaveBandMap);

            updateTargetProductMetadata();

            final Band masterBandI = getBand(masterProduct, "i_", swathIndexStr, mSU.getPolarizations()[0]);
            noDataValue = masterBandI.getNoDataValue();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private static void outputToFile(final String filePath, double[][] fbuf) throws IOException {

        try{
            FileOutputStream fos = new FileOutputStream(filePath);
            DataOutputStream dos = new DataOutputStream(fos);

            for (double[] aFbuf : fbuf) {
                for (int j = 0; j < fbuf[0].length; j++) {
                    dos.writeDouble(aFbuf[j]);
                }
            }
            //dos.flush();
            dos.close();
            fos.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Check source product validity.
     */
    private void checkSourceProductValidity() throws OperatorException {

        if (sourceProduct.length != 2) {
            throw new OperatorException("Please select two source products");
        }

        final InputProductValidator validator1 = new InputProductValidator(sourceProduct[0]);
        validator1.checkIfSARProduct();
        validator1.checkIfSentinel1Product();
        validator1.checkIfSLC();

        final InputProductValidator validator2 = new InputProductValidator(sourceProduct[1]);
        validator2.checkIfSARProduct();
        validator2.checkIfSentinel1Product();
        validator2.checkIfSLC();

        MetadataElement mAbsRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct[0]);
        MetadataElement sAbsRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct[1]);

        final String mAcquisitionMode = mAbsRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        final String sAcquisitionMode = sAbsRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        if (!mAcquisitionMode.equals(sAcquisitionMode)) {
            throw new OperatorException("Source products should have the same acquisition modes");
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(
                masterProduct.getName() + PRODUCT_SUFFIX,
                masterProduct.getProductType(),
                masterProduct.getSceneRasterWidth(),
                masterProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(masterProduct, targetProduct);
        
        final String[] masterBandNames = masterProduct.getBandNames();
        final String mstSuffix = StackUtils.MST + StackUtils.createBandTimeStamp(masterProduct);
        for (String bandName : masterBandNames) {
            if (masterProduct.getBand(bandName) instanceof VirtualBand) {
                continue;
            }
            final Band targetBand = ProductUtils.copyBand(bandName, masterProduct, bandName + mstSuffix, targetProduct, true);

            if(targetBand.getUnit().equals(Unit.IMAGINARY)) {
                int idx = targetProduct.getBandIndex(targetBand.getName());
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBandAt(idx-1), targetBand, mstSuffix);
            }
        }

        final Band masterBand = masterProduct.getBand(masterBandNames[0]);
        final int masterBandWidth = masterBand.getRasterWidth();
        final int masterBandHeight = masterBand.getRasterHeight();

        final String[] slaveBandNames = slaveProduct.getBandNames();
        final String slvSuffix = StackUtils.SLV+'1' + StackUtils.createBandTimeStamp(slaveProduct);
        for (String bandName : slaveBandNames) {
            final Band srcBand = slaveProduct.getBand(bandName);
            if (srcBand instanceof VirtualBand) {
                continue;
            }
            final Band targetBand = new Band(
                    bandName + slvSuffix,
                    ProductData.TYPE_FLOAT32,
                    masterBandWidth,
                    masterBandHeight);

            targetBand.setUnit(srcBand.getUnit());
            targetBand.setDescription(srcBand.getDescription());
            targetProduct.addBand(targetBand);
            targetBandToSlaveBandMap.put(targetBand, srcBand);

            if(targetBand.getUnit().equals(Unit.IMAGINARY)) {
                int idx = targetProduct.getBandIndex(targetBand.getName());
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBandAt(idx-1), targetBand, slvSuffix);
            }
        }

        // set non-elevation areas to no data value for the master bands using the slave bands
        DEMAssistedCoregistrationOp.setMasterValidPixelExpression(targetProduct, maskOutAreaWithoutElevation);

        copySlaveMetadata();

        if (outputRangeAzimuthOffset) {
            final Band azOffsetBand = new Band(
                    "azOffset" + slvSuffix,
                    ProductData.TYPE_FLOAT32,
                    masterBandWidth,
                    masterBandHeight);

            azOffsetBand.setUnit("Index");
            targetProduct.addBand(azOffsetBand);

            final Band rgOffsetBand = new Band(
                    "rgOffset" + slvSuffix,
                    ProductData.TYPE_FLOAT32,
                    masterBandWidth,
                    masterBandHeight);

            rgOffsetBand.setUnit("Index");
            targetProduct.addBand(rgOffsetBand);
        }

        if (outputDerampDemodPhase) {
            final Band phaseBand = new Band(
                    "derampDemodPhase" + slvSuffix,
                    ProductData.TYPE_FLOAT32,
                    masterBandWidth,
                    masterBandHeight);

            phaseBand.setUnit("radian");
            targetProduct.addBand(phaseBand);
        }

        if (outputDEM) {
            final Band elevBand = new Band(
                    "elevation" + slvSuffix,
                    ProductData.TYPE_FLOAT32,
                    masterBandWidth,
                    masterBandHeight);

            elevBand.setUnit(Unit.METERS);
            targetProduct.addBand(elevBand);
        }
    }

    private void copySlaveMetadata() {

        final MetadataElement targetSlaveMetadataRoot = AbstractMetadata.getSlaveMetadata(targetProduct.getMetadataRoot());
        final MetadataElement slvAbsMetadata = AbstractMetadata.getAbstractedMetadata(slaveProduct);
        if (slvAbsMetadata != null) {
            final String timeStamp = StackUtils.createBandTimeStamp(slaveProduct);
            final MetadataElement targetSlaveMetadata = new MetadataElement(slaveProduct.getName() + timeStamp);
            targetSlaveMetadataRoot.addElement(targetSlaveMetadata);
            ProductUtils.copyMetadata(slvAbsMetadata, targetSlaveMetadata);
        }
    }

    /**
     * Update target product metadata.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.coregistered_stack, 1);

        final MetadataElement inputElem = ProductInformation.getInputProducts(targetProduct);
        final MetadataElement slvInputElem = ProductInformation.getInputProducts(slaveProduct);
        final MetadataAttribute[] slvInputProductAttrbList = slvInputElem.getAttributes();
        for (MetadataAttribute attrib : slvInputProductAttrbList) {
            final MetadataAttribute inputAttrb = AbstractMetadata.addAbstractedAttribute(
                    inputElem, "InputProduct", ProductData.TYPE_ASCII, "", "");
            inputAttrb.getData().setElems(attrib.getData().getElemString());
        }
    }


    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
     public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
             throws OperatorException {

        try {
            final int tx0 = targetRectangle.x;
            final int ty0 = targetRectangle.y;
            final int tw = targetRectangle.width;
            final int th = targetRectangle.height;
            final int tyMax = ty0 + th;
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            if (!isElevationModelAvailable) {
                getElevationModel();
            }

            if (!burstOffsetComputed) {
                computeBurstOffset();
            }

            for (int burstIndex = 0; burstIndex < mSubSwath[subSwathIndex - 1].numOfBursts; burstIndex++) {
                final int firstLineIdx = burstIndex*mSubSwath[subSwathIndex - 1].linesPerBurst;
                final int lastLineIdx = firstLineIdx + mSubSwath[subSwathIndex - 1].linesPerBurst - 1;

                if (tyMax <= firstLineIdx || ty0 > lastLineIdx) {
                    continue;
                }

				final int ntx0 = tx0;
				final int ntw = tw;
                final int nty0 = Math.max(ty0, firstLineIdx);
                final int ntyMax = Math.min(tyMax, lastLineIdx + 1);
                final int nth = ntyMax - nty0;
                //System.out.println("burstIndex = " + burstIndex + ": ntx0 = " + ntx0 + ", nty0 = " + nty0 + ", ntw = " + ntw + ", nth = " + nth);

                double[] extendedAmount = {0.0, 0.0, 0.0, 0.0};
                computeExtendedAmount(ntx0, nty0, ntw, nth, extendedAmount);

                computePartialTile(subSwathIndex, burstIndex, ntx0, nty0, ntw, nth, targetTileMap,
                        extendedAmount, pm);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private static int getSubswathIndex(final String targetBandName) {
        for (int i = 0; i < 5; i++) {
            if (targetBandName.contains(String.valueOf(i+1))){
                return (i+1);
            }
        }
        return -1;
    }
    /**
     * Get elevation model.
     *
     * @throws Exception The exceptions.
     */
    private synchronized void getElevationModel() throws Exception {

        if (isElevationModelAvailable) return;
        try {
            if (externalDEMFile != null) { // if external DEM file is specified by user
                dem = new FileElevationModel(externalDEMFile, demResamplingMethod, externalDEMNoDataValue);
                demNoDataValue = externalDEMNoDataValue;
                demName = externalDEMFile.getPath();
            } else {
                dem = DEMFactory.createElevationModel(demName, demResamplingMethod);
                demNoDataValue = dem.getDescriptor().getNoDataValue();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        isElevationModelAvailable = true;
    }

    private synchronized void computeBurstOffset() throws Exception {

        if (burstOffsetComputed) return;
        try {
            final int h = mSubSwath[subSwathIndex - 1].latitude.length;
            final int w = mSubSwath[subSwathIndex - 1].latitude[0].length;
            final PosVector earthPoint = new PosVector();
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    final double lat = mSubSwath[subSwathIndex - 1].latitude[i][j];
                    final double lon = mSubSwath[subSwathIndex - 1].longitude[i][j];
                    final double alt = dem.getElevation(new GeoPos(lat, lon));
                    if (alt == demNoDataValue) {
                        continue;
                    }
                    GeoUtils.geo2xyzWGS84(lat, lon, alt, earthPoint);
                    final BurstIndices mBurstIndices = getBurstIndices(subSwathIndex, mSU, mOrbit, earthPoint);
                    final BurstIndices sBurstIndices = getBurstIndices(subSwathIndex, sSU, sOrbit, earthPoint);
                    if (mBurstIndices == null || sBurstIndices == null ||
                            (mBurstIndices.firstBurstIndex == -1 && mBurstIndices.secondBurstIndex == -1) ||
                            (sBurstIndices.firstBurstIndex == -1 && sBurstIndices.secondBurstIndex == -1 )) {
                        continue;
                    }

                    if (mBurstIndices.inUpperPartOfFirstBurst == sBurstIndices.inUpperPartOfFirstBurst) {
                        burstOffset = sBurstIndices.firstBurstIndex - mBurstIndices.firstBurstIndex;
                    } else if (sBurstIndices.secondBurstIndex != -1 &&
                            mBurstIndices.inUpperPartOfFirstBurst == sBurstIndices.inUpperPartOfSecondBurst) {
                        burstOffset = sBurstIndices.secondBurstIndex - mBurstIndices.firstBurstIndex;
                    } else if (mBurstIndices.secondBurstIndex != -1 &&
                            mBurstIndices.inUpperPartOfSecondBurst == sBurstIndices.inUpperPartOfFirstBurst) {
                        burstOffset = sBurstIndices.firstBurstIndex - mBurstIndices.secondBurstIndex;
                    } else if (mBurstIndices.secondBurstIndex != -1 && sBurstIndices.secondBurstIndex != -1 &&
                            mBurstIndices.inUpperPartOfSecondBurst == sBurstIndices.inUpperPartOfSecondBurst) {
                        burstOffset = sBurstIndices.secondBurstIndex - mBurstIndices.secondBurstIndex;
                    } else {
                        continue;
                    }

                    burstOffsetComputed = true;
                    return;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static BurstIndices getBurstIndices(final int subSwathIndex, final Sentinel1Utils su,
                                         final SARGeocoding.Orbit orbit, final PosVector earthPoint) {

        try {
            Sentinel1Utils.SubSwathInfo subSwath = su.getSubSwath()[subSwathIndex - 1];

            final double zeroDopplerTimeInDays = SARGeocoding.getZeroDopplerTime(
                    su.firstLineUTC, su.lineTimeInterval, su.wavelength, earthPoint, orbit);

            if (zeroDopplerTimeInDays == SARGeocoding.NonValidZeroDopplerTime) {
                return null;
            }

            final double zeroDopplerTime = zeroDopplerTimeInDays * Constants.secondsInDay;

            BurstIndices burstIndices = new BurstIndices();
            int k = 0;
            for (int i = 0; i < subSwath.numOfBursts; i++) {
                if (zeroDopplerTime >= subSwath.burstFirstLineTime[i] && zeroDopplerTime < subSwath.burstLastLineTime[i]) {
                    boolean inUpperPartOfBurst = (zeroDopplerTime >=
                            (subSwath.burstFirstLineTime[i] + subSwath.burstLastLineTime[i])/2.0);

                    if (k == 0) {
                        burstIndices.firstBurstIndex = i;
                        burstIndices.inUpperPartOfFirstBurst = inUpperPartOfBurst;
                    } else {
                        burstIndices.secondBurstIndex = i;
                        burstIndices.inUpperPartOfSecondBurst = inUpperPartOfBurst;
                        break;
                    }
                    ++k;
                }
            }
            return burstIndices;

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("getBurstIndices", e);
        }
        return null;
    }

    private void computeExtendedAmount(final int x0, final int y0, final int w, final int h,
                                       final double[] extendedAmount)
            throws Exception {

        final EarthGravitationalModel96 egm = EarthGravitationalModel96.instance();

        final GeoPos geoPos = new GeoPos();
        final PositionData posData = new PositionData();
        double azExtendedAmountMax = -Double.MAX_VALUE;
        double azExtendedAmountMin = Double.MAX_VALUE;
        double rgExtendedAmountMax = -Double.MAX_VALUE;
        double rgExtendedAmountMin = Double.MAX_VALUE;

        for (int y = y0; y < y0 + h; y += 20) {
            final int burstIndex = getBurstIndex(y);

            for (int x = x0; x < x0 + w; x += 20) {
                final double azTime = getAzimuthTime(y, burstIndex);
                final double rgTime = getSlantRangeTime(x);
                final double lat = mSU.getLatitude(azTime, rgTime, subSwathIndex);
                final double lon = mSU.getLongitude(azTime, rgTime, subSwathIndex);
                geoPos.setLocation(lat, lon);
                double alt = dem.getElevation(geoPos);
                if (alt == demNoDataValue) {
                    alt = egm.getEGM(lat, lon);
                }

                GeoUtils.geo2xyzWGS84(geoPos.getLat(), geoPos.getLon(), alt, posData.earthPoint);

                if (getPosition(subSwathIndex, burstIndex, mSU, mOrbit, posData)) {
                    double azExtendedAmount = posData.azimuthIndex - y;
                    double rgExtendedAmount = posData.rangeIndex - x;
                    if (azExtendedAmount > azExtendedAmountMax) {
                        azExtendedAmountMax = azExtendedAmount;
                    }
                    if (azExtendedAmount < azExtendedAmountMin) {
                        azExtendedAmountMin = azExtendedAmount;
                    }
                    if (rgExtendedAmount > rgExtendedAmountMax) {
                        rgExtendedAmountMax = rgExtendedAmount;
                    }
                    if (rgExtendedAmount < rgExtendedAmountMin) {
                        rgExtendedAmountMin = rgExtendedAmount;
                    }
                }
            }
        }

        if (azExtendedAmountMin != Double.MAX_VALUE && azExtendedAmountMin < 0.0) {
            extendedAmount[0] = azExtendedAmountMin;
        } else {
            extendedAmount[0] = 0.0;
        }

        if (azExtendedAmountMax != -Double.MAX_VALUE && azExtendedAmountMax > 0.0) {
            extendedAmount[1] = azExtendedAmountMax;
        } else {
            extendedAmount[1] = 0.0;
        }

        if (rgExtendedAmountMin != Double.MAX_VALUE && rgExtendedAmountMin < 0.0) {
            extendedAmount[2] = rgExtendedAmountMin;
        } else {
            extendedAmount[2] = 0.0;
        }

        if (rgExtendedAmountMax != -Double.MAX_VALUE && rgExtendedAmountMax > 0.0) {
            extendedAmount[3] = rgExtendedAmountMax;
        } else {
            extendedAmount[3] = 0.0;
        }
    }

    private int getBurstIndex(final int y) {
        for (int burstIndex = 0; burstIndex < mSubSwath[subSwathIndex - 1].numOfBursts; burstIndex++) {
            final int firstLineIdx = burstIndex*mSubSwath[subSwathIndex - 1].linesPerBurst;
            final int lastLineIdx = firstLineIdx + mSubSwath[subSwathIndex - 1].linesPerBurst - 1;
            if (y >= firstLineIdx && y <= lastLineIdx) {
                return burstIndex;
            }
        }
        return -1;
    }

    private double getAzimuthTime(final int y, final int burstIndex) {
        final Sentinel1Utils.SubSwathInfo subSwath = mSubSwath[subSwathIndex - 1];
        return subSwath.burstFirstLineTime[burstIndex] +
                (y - burstIndex * subSwath.linesPerBurst) * subSwath.azimuthTimeInterval;
    }

    private double getSlantRangeTime(final int x) {
        return mSubSwath[subSwathIndex - 1].slrTimeToFirstPixel + x * mSU.rangeSpacing / Constants.lightSpeed;
    }

    private void computePartialTile(final int subSwathIndex, final int mBurstIndex,
                                    final int x0, final int y0, final int w, final int h,
                                    final Map<Band, Tile> targetTileMap,
                                    final double[] extendedAmount,
                                    ProgressMonitor pm)
            throws Exception {

        final int sBurstIndex = mBurstIndex + burstOffset;
        if (sBurstIndex < 0 || sBurstIndex >= sSubSwath[subSwathIndex - 1].numOfBursts) {
            return;
        }

        double[][] elevation = null;
        if (outputDEM) {
            elevation = new double[h][w];
        }

        final PixelPos[][] slavePixPos = new PixelPos[h][w];
        final boolean isSuccessful = computeSlavePixPos(
                subSwathIndex, mBurstIndex, sBurstIndex, x0, y0, w, h, extendedAmount, slavePixPos, elevation, pm);

        if (!isSuccessful) {
            return;
        }

        if (outputRangeAzimuthOffset) {
            outputRangeAzimuthOffsets(x0, y0, w, h, targetTileMap, slavePixPos, subSwathIndex, mBurstIndex, sBurstIndex);
        }

        if (outputDEM) {
            outputDEM(x0, y0, w, h, targetTileMap, elevation);
        }

        final int margin = selectedResampling.getKernelSize();
        final Rectangle sourceRectangle = getBoundingBox(slavePixPos, margin, subSwathIndex, sBurstIndex);

        if (sourceRectangle == null) {
            return;
        }

        final double[][] derampDemodPhase = computeDerampDemodPhase(subSwathIndex, sBurstIndex, sourceRectangle);

        if (derampDemodPhase == null) {
            return;
        }

        for(String polarization : mSU.getPolarizations()) {
            final Band slaveBandI = getBand(slaveProduct, "i_", swathIndexStr, polarization);
            final Band slaveBandQ = getBand(slaveProduct, "q_", swathIndexStr, polarization);
            final Tile slaveTileI = getSourceTile(slaveBandI, sourceRectangle);
            final Tile slaveTileQ = getSourceTile(slaveBandQ, sourceRectangle);

            if (slaveTileI == null || slaveTileQ == null) {
                return;
            }

            final double[][] derampDemodI = new double[sourceRectangle.height][sourceRectangle.width];
            final double[][] derampDemodQ = new double[sourceRectangle.height][sourceRectangle.width];

            performDerampDemod(slaveTileI, slaveTileQ, sourceRectangle, derampDemodPhase, derampDemodI, derampDemodQ);

            performInterpolation(x0, y0, w, h, sourceRectangle, slaveTileI, slaveTileQ, targetTileMap, derampDemodPhase,
                                 derampDemodI, derampDemodQ, slavePixPos, subSwathIndex, sBurstIndex, polarization);
        }
    }

    private boolean computeSlavePixPos(final int subSwathIndex, final int mBurstIndex, final int sBurstIndex,
                                       final int x0, final int y0, final int w, final int h,
                                       final double[] extendedAmount, final PixelPos[][] slavePixelPos,
                                       final double[][] elevation, ProgressMonitor pm)
            throws Exception {

        try {
            final int xmin = x0 - (int)extendedAmount[3];
            final int ymin = y0 - (int)extendedAmount[1];
            final int ymax = y0 + h + (int)Math.abs(extendedAmount[0]);
            final int xmax = x0 + w + (int)Math.abs(extendedAmount[2]);

            // Compute lat/lon boundaries (with extensions) for target tile
            final double[] latLonMinMax = new double[4];

            computeImageGeoBoundary(subSwathIndex, mBurstIndex, xmin, xmax, ymin, ymax, latLonMinMax);

            final double delta = (double)dem.getDescriptor().getTileWidthInDegrees() / (double)dem.getDescriptor().getTileWidth();
//            final double extralat = 1.5*delta + 4.0/25.0;
//            final double extralon = 1.5*delta + 4.0/25.0;
            final double extralat = 20*delta;
            final double extralon = 20*delta;

            final double latMin = latLonMinMax[0] - extralat;
            final double latMax = latLonMinMax[1] + extralat;
            final double lonMin = latLonMinMax[2] - extralon;
            final double lonMax = latLonMinMax[3] + extralon;

            // Compute lat/lon indices in DEM for the boundaries;
            final PixelPos upperLeft = dem.getIndex(new GeoPos(latMax, lonMin));
            final PixelPos lowerRight = dem.getIndex(new GeoPos(latMin, lonMax));
            final int latMaxIdx = (int)Math.floor(upperLeft.getY());
            final int latMinIdx = (int)Math.ceil(lowerRight.getY());
            final int lonMinIdx = (int)Math.floor(upperLeft.getX());
            final int lonMaxIdx = (int)Math.ceil(lowerRight.getX());

            // Loop through all DEM points bounded by the indices computed above. For each point,
            // get its lat/lon and its azimuth/range indices in target image;
            final int numLines = latMinIdx - latMaxIdx;
            final int numPixels = lonMaxIdx - lonMinIdx;
            double[][] masterAz = new double[numLines][numPixels];
            double[][] masterRg = new double[numLines][numPixels];
            double[][] slaveAz = new double[numLines][numPixels];
            double[][] slaveRg = new double[numLines][numPixels];
            double[][] lat = new double[numLines][numPixels];
            double[][] lon = new double[numLines][numPixels];
            final PositionData posData = new PositionData();
            final PixelPos pix = new PixelPos();

            final EarthGravitationalModel96 egm = EarthGravitationalModel96.instance();

            boolean noValidSlavePixPos = true;
            for (int l = 0; l < numLines; l++) {
                for (int p = 0; p < numPixels; p++) {

                    pix.setLocation(lonMinIdx + p, latMaxIdx + l);
                    GeoPos gp = dem.getGeoPos(pix);
                    lat[l][p] = gp.lat;
                    lon[l][p] = gp.lon;

                    double alt = dem.getElevation(gp);
                    if (alt == demNoDataValue && !maskOutAreaWithoutElevation) { // get corrected elevation for 0
                        alt = egm.getEGM(gp.lat, gp.lon);
                    }

                    if (alt != demNoDataValue) {
                        GeoUtils.geo2xyzWGS84(gp.lat, gp.lon, alt, posData.earthPoint);
                        if(getPosition(subSwathIndex, mBurstIndex, mSU, mOrbit, posData)) {

                            masterAz[l][p] = posData.azimuthIndex;
                            masterRg[l][p] = posData.rangeIndex;
                            if (getPosition(subSwathIndex, sBurstIndex, sSU, sOrbit, posData)) {

                                slaveAz[l][p] = posData.azimuthIndex;
                                slaveRg[l][p] = posData.rangeIndex;
                                noValidSlavePixPos = false;
                                continue;
                            }
                        }
                    }

                    masterAz[l][p] = invalidIndex;
                    masterRg[l][p] = invalidIndex;
                }
            }

            if (noValidSlavePixPos) {
                return false;
            }

            // Compute azimuth/range offsets for pixels in target tile using Delaunay interpolation
            final org.jlinda.core.Window tileWindow = new org.jlinda.core.Window(y0, y0 + h - 1, x0, x0 + w - 1);

            //final double rgAzRatio = computeRangeAzimuthSpacingRatio(w, h, latLonMinMax);
            final double rgAzRatio = mSU.rangeSpacing / mSU.azimuthSpacing;

            final double[][] latArray = new double[(int)tileWindow.lines()][(int)tileWindow.pixels()];
            final double[][] lonArray = new double[(int)tileWindow.lines()][(int)tileWindow.pixels()];
            final double[][] azArray = new double[(int)tileWindow.lines()][(int)tileWindow.pixels()];
            final double[][] rgArray = new double[(int)tileWindow.lines()][(int)tileWindow.pixels()];
            for (double[] data : azArray) {
                Arrays.fill(data, invalidIndex);
            }
            for (double[] data : rgArray) {
                Arrays.fill(data, invalidIndex);
            }

            TriangleInterpolator.ZData[] dataList = new TriangleInterpolator.ZData[] {
                    new TriangleInterpolator.ZData(slaveAz, azArray),
                    new TriangleInterpolator.ZData(slaveRg, rgArray),
                    new TriangleInterpolator.ZData(lat, latArray),
                    new TriangleInterpolator.ZData(lon, lonArray)
            };

            TriangleInterpolator.gridDataLinear(masterAz, masterRg, dataList,
                    tileWindow, rgAzRatio, 1, 1, invalidIndex, 0);

            boolean allElementsAreNull = true;
            double alt = 0;
            for(int yy = 0; yy < h; yy++) {
                for (int xx = 0; xx < w; xx++) {
                    if (rgArray[yy][xx] == invalidIndex || azArray[yy][xx] == invalidIndex) {
                        slavePixelPos[yy][xx] = null;
                    } else {
                        if (maskOutAreaWithoutElevation || elevation != null) {
                            alt = dem.getElevation(new GeoPos(latArray[yy][xx], lonArray[yy][xx]));
                            if(elevation != null) {
                                elevation[yy][xx] = alt;
                            }
                            if (alt != demNoDataValue) {
                                slavePixelPos[yy][xx] = new PixelPos(rgArray[yy][xx], azArray[yy][xx]);
                                allElementsAreNull = false;
                            } else {
                                slavePixelPos[yy][xx] = null;
                            }
                        } else {
                            slavePixelPos[yy][xx] = new PixelPos(rgArray[yy][xx], azArray[yy][xx]);
                            allElementsAreNull = false;
                        }
                    }
                }
            }

            return !allElementsAreNull;

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeSlavePixPos", e);
        }

        return false;
    }

    /**
     * Compute source image geodetic boundary (minimum/maximum latitude/longitude) from the its corner
     * latitude/longitude.
     *
     * @throws Exception The exceptions.
     */
    private void computeImageGeoBoundary(final int subSwathIndex, final int burstIndex,
                                         final int xMin, final int xMax, final int yMin, final int yMax,
                                         double[] latLonMinMax) throws Exception {

        final Sentinel1Utils.SubSwathInfo subSwath = mSubSwath[subSwathIndex - 1];

        final double azTimeMin = subSwath.burstFirstLineTime[burstIndex] +
                (yMin - burstIndex * subSwath.linesPerBurst) * subSwath.azimuthTimeInterval;

        final double azTimeMax = subSwath.burstFirstLineTime[burstIndex] +
                (yMax - burstIndex * subSwath.linesPerBurst) * subSwath.azimuthTimeInterval;

        final double rgTimeMin = subSwath.slrTimeToFirstPixel + xMin * mSU.rangeSpacing / Constants.lightSpeed;

        final double rgTimeMax = subSwath.slrTimeToFirstPixel + xMax * mSU.rangeSpacing / Constants.lightSpeed;

        final double latUL = mSU.getLatitude(azTimeMin, rgTimeMin, subSwathIndex);
        final double lonUL = mSU.getLongitude(azTimeMin, rgTimeMin, subSwathIndex);
        final double latUR = mSU.getLatitude(azTimeMin, rgTimeMax, subSwathIndex);
        final double lonUR = mSU.getLongitude(azTimeMin, rgTimeMax, subSwathIndex);
        final double latLL = mSU.getLatitude(azTimeMax, rgTimeMin, subSwathIndex);
        final double lonLL = mSU.getLongitude(azTimeMax, rgTimeMin, subSwathIndex);
        final double latLR = mSU.getLatitude(azTimeMax, rgTimeMax, subSwathIndex);
        final double lonLR = mSU.getLongitude(azTimeMax, rgTimeMax, subSwathIndex);

        final double[] lats = {latUL, latUR, latLL, latLR};
        final double[] lons = {lonUL, lonUR, lonLL, lonLR};

        double latMin = 90.0;
        double latMax = -90.0;
        for (double lat : lats) {
            if (lat < latMin) {
                latMin = lat;
            }
            if (lat > latMax) {
                latMax = lat;
            }
        }

        double lonMin = 180.0;
        double lonMax = -180.0;
        for (double lon : lons) {
            if (lon < lonMin) {
                lonMin = lon;
            }
            if (lon > lonMax) {
                lonMax = lon;
            }
        }

        latLonMinMax[0] = latMin;
        latLonMinMax[1] = latMax;
        latLonMinMax[2] = lonMin;
        latLonMinMax[3] = lonMax;
    }

    /**
     * Compute azimuth and range indices in SAR image for a given target point on the Earth's surface.
     */
    private static boolean getPosition(final int subSwathIndex, final int burstIndex, final Sentinel1Utils su,
                                final SARGeocoding.Orbit orbit, final PositionData data) {

        try {
            Sentinel1Utils.SubSwathInfo subSwath = su.getSubSwath()[subSwathIndex - 1];

            final double zeroDopplerTimeInDays = SARGeocoding.getZeroDopplerTime(
                    su.firstLineUTC, su.lineTimeInterval, su.wavelength, data.earthPoint, orbit);

            if (zeroDopplerTimeInDays == SARGeocoding.NonValidZeroDopplerTime) {
                return false;
            }

            final double zeroDopplerTime = zeroDopplerTimeInDays * Constants.secondsInDay;

            data.azimuthIndex = burstIndex * subSwath.linesPerBurst +
                    (zeroDopplerTime - subSwath.burstFirstLineTime[burstIndex]) / subSwath.azimuthTimeInterval;

            final double slantRange = SARGeocoding.computeSlantRange(
                    zeroDopplerTimeInDays, orbit, data.earthPoint, data.sensorPos);

            if (!su.srgrFlag) {
                data.rangeIndex = (slantRange - subSwath.slrTimeToFirstPixel*Constants.lightSpeed) / su.rangeSpacing;
            } else {
                data.rangeIndex = SARGeocoding.computeRangeIndex(
                        su.srgrFlag, su.sourceImageWidth, su.firstLineUTC, su.lastLineUTC,
                        su.rangeSpacing, zeroDopplerTimeInDays, slantRange, su.nearEdgeSlantRange, su.srgrConvParams);
            }

            if (!su.nearRangeOnLeft) {
                data.rangeIndex = su.sourceImageWidth - 1 - data.rangeIndex;
            }
            return true;
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("getPosition", e);
        }
        return false;
    }

    /**
     * Get the source rectangle in slave image that contains all the given pixels.
     */
    private Rectangle getBoundingBox(
            final PixelPos[][] slavePixPos, final int margin, final int subSwathIndex, final int sBurstIndex) {

        final int firstLineIndex = sBurstIndex*sSubSwath[subSwathIndex - 1].linesPerBurst;
        final int lastLineIndex = firstLineIndex + sSubSwath[subSwathIndex - 1].linesPerBurst - 1;
        final int firstPixelIndex = 0;
        final int lastPixelIndex = sSubSwath[subSwathIndex - 1].samplesPerBurst - 1;

        int minX = Integer.MAX_VALUE;
        int maxX = -Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = -Integer.MAX_VALUE;

        for (PixelPos[] slavePixPo : slavePixPos) {
            for (int j = 0; j < slavePixPos[0].length; j++) {
                if (slavePixPo[j] != null) {
                    final int x = (int) Math.floor(slavePixPo[j].getX());
                    final int y = (int) Math.floor(slavePixPo[j].getY());

                    if (x < minX) {
                        minX = x;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }

        minX = Math.max(minX - margin, firstPixelIndex);
        maxX = Math.min(maxX + margin, lastPixelIndex);
        minY = Math.max(minY - margin, firstLineIndex);
        maxY = Math.min(maxY + margin, lastLineIndex);

        if (minX > maxX || minY > maxY) {
            return null;
        }

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /**
     * Compute combined deramp and demodulation phase for area in slave image defined by rectangle.
     * @param subSwathIndex Sub-swath index
     * @param sBurstIndex Burst index
     * @param rectangle Rectangle that defines the area in slave image
     * @return The combined deramp and demodulation phase
     */
    private double[][] computeDerampDemodPhase(
            final int subSwathIndex, final int sBurstIndex, final Rectangle rectangle) {

        try {
            final int x0 = rectangle.x;
            final int y0 = rectangle.y;
            final int w = rectangle.width;
            final int h = rectangle.height;
            final int xMax = x0 + w;
            final int yMax = y0 + h;
            final int s = subSwathIndex - 1;

            final double[][] phase = new double[h][w];
            final int firstLineInBurst = sBurstIndex*sSubSwath[s].linesPerBurst;
            for (int y = y0; y < yMax; y++) {
                final int yy = y - y0;
                final double ta = (y - firstLineInBurst)*sSubSwath[s].azimuthTimeInterval;
                for (int x = x0; x < xMax; x++) {
                    final int xx = x - x0;
                    final double kt = sSubSwath[s].dopplerRate[sBurstIndex][x];
                    final double deramp = -Constants.PI * kt * FastMath.pow(ta - sSubSwath[s].referenceTime[sBurstIndex][x], 2);
                    final double demod = -Constants.TWO_PI * sSubSwath[s].dopplerCentroid[sBurstIndex][x] * ta;
                    phase[yy][xx] = deramp + demod;
                }
            }

            return phase;
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeDerampDemodPhase", e);
        }

        return null;
    }

    public static void performDerampDemod(final Tile slaveTileI, final Tile slaveTileQ,
                                          final Rectangle slaveRectangle, final double[][] derampDemodPhase,
                                          final double[][] derampDemodI, final double[][] derampDemodQ) {

        try {
            final int x0 = slaveRectangle.x;
            final int y0 = slaveRectangle.y;
            final int xMax = x0 + slaveRectangle.width;
            final int yMax = y0 + slaveRectangle.height;

            final ProductData slaveDataI = slaveTileI.getDataBuffer();
            final ProductData slaveDataQ = slaveTileQ.getDataBuffer();
            final TileIndex slvIndex = new TileIndex(slaveTileI);

            for (int y = y0; y < yMax; y++) {
                slvIndex.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < xMax; x++) {
                    final int idx = slvIndex.getIndex(x);
                    final int xx = x - x0;
                    final double valueI = slaveDataI.getElemDoubleAt(idx);
                    final double valueQ = slaveDataQ.getElemDoubleAt(idx);
                    final double cosPhase = FastMath.cos(derampDemodPhase[yy][xx]);
                    final double sinPhase = FastMath.sin(derampDemodPhase[yy][xx]);
                    derampDemodI[yy][xx] = valueI*cosPhase - valueQ*sinPhase;
                    derampDemodQ[yy][xx] = valueI*sinPhase + valueQ*cosPhase;
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("performDerampDemod", e);
        }
    }

    private void performInterpolation(final int x0, final int y0, final int w, final int h,
                                      final Rectangle sourceRectangle, final Tile slaveTileI, final Tile slaveTileQ,
                                      final Map<Band, Tile> targetTileMap, final double[][] derampDemodPhase,
                                      final double[][] derampDemodI, final double[][] derampDemodQ,
                                      final PixelPos[][] slavePixPos, final int subswathIndex, final int sBurstIndex,
                                      final String polarization) {

        try {
            final ResamplingRaster resamplingRasterI = new ResamplingRaster(slaveTileI, derampDemodI);
            final ResamplingRaster resamplingRasterQ = new ResamplingRaster(slaveTileQ, derampDemodQ);
            final ResamplingRaster resamplingRasterPhase = new ResamplingRaster(slaveTileI, derampDemodPhase);

            final Band iBand = getTargetBand("i_", StackUtils.SLV, polarization);
            final Band qBand = getTargetBand("q_", StackUtils.SLV, polarization);
            final Band phaseBand = getTargetBand("derampDemodPhase");

            if (iBand == null || qBand == null) {
                return;
            }

            final Tile tgtTileI = targetTileMap.get(iBand);
            final Tile tgtTileQ = targetTileMap.get(qBand);
            final ProductData tgtBufferI = tgtTileI.getDataBuffer();
            final ProductData tgtBufferQ = tgtTileQ.getDataBuffer();
            final TileIndex tgtIndex = new TileIndex(tgtTileI);

            Tile tgtTilePhase;
            ProductData tgtBufferPhase = null;
            if (outputDerampDemodPhase) {
                tgtTilePhase = targetTileMap.get(phaseBand);
                tgtBufferPhase = tgtTilePhase.getDataBuffer();
            }

            final Resampling.Index resamplingIndex = selectedResampling.createIndex();

            for (int y = y0; y < y0 + h; y++) {
                tgtIndex.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < x0 + w; x++) {
                    final int tgtIdx = tgtIndex.getIndex(x);
                    final PixelPos slavePixelPos = slavePixPos[yy][x - x0];

                    if (slavePixelPos == null) {
                        tgtBufferI.setElemDoubleAt(tgtIdx, noDataValue);
                        tgtBufferQ.setElemDoubleAt(tgtIdx, noDataValue);

                        if (outputDerampDemodPhase) {
                            tgtBufferPhase.setElemFloatAt(tgtIdx, (float)noDataValue);
                        }
                        continue;
                    }

                    if (isSlavePixPosValid(slavePixelPos, subswathIndex, sBurstIndex)) {

                        selectedResampling.computeCornerBasedIndex(
                                slavePixelPos.x - sourceRectangle.x, slavePixelPos.y - sourceRectangle.y,
                                sourceRectangle.width, sourceRectangle.height, resamplingIndex);

                        final double samplePhase = selectedResampling.resample(resamplingRasterPhase, resamplingIndex);
                        final double sampleI = selectedResampling.resample(resamplingRasterI, resamplingIndex);
                        final double sampleQ = selectedResampling.resample(resamplingRasterQ, resamplingIndex);
                        final double cosPhase = FastMath.cos(samplePhase);
                        final double sinPhase = FastMath.sin(samplePhase);
                        double rerampRemodI = sampleI * cosPhase + sampleQ * sinPhase;
                        double rerampRemodQ = -sampleI * sinPhase + sampleQ * cosPhase;

                        if (Double.isNaN(rerampRemodI)) {
                            rerampRemodI = noDataValue;
                        }

                        if (Double.isNaN(rerampRemodQ)) {
                            rerampRemodQ = noDataValue;
                        }

                        if (disableReramp) {
                            tgtBufferI.setElemDoubleAt(tgtIdx, sampleI);
                            tgtBufferQ.setElemDoubleAt(tgtIdx, sampleQ);
                        } else {
                            tgtBufferI.setElemDoubleAt(tgtIdx, rerampRemodI);
                            tgtBufferQ.setElemDoubleAt(tgtIdx, rerampRemodQ);
                        }

                        if (outputDerampDemodPhase) {
                            tgtBufferPhase.setElemFloatAt(tgtIdx, (float)samplePhase);
                        }

                    } else {
                        tgtBufferI.setElemDoubleAt(tgtIdx, noDataValue);
                        tgtBufferQ.setElemDoubleAt(tgtIdx, noDataValue);
                        if (outputDerampDemodPhase) {
                            tgtBufferPhase.setElemFloatAt(tgtIdx, (float)noDataValue);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("performInterpolation", e);
        }
    }

    private static Band getBand(
            final Product product, final String prefix, final String swathIndexStr, final String polarization) {

        final String[] bandNames = product.getBandNames();
        for (String bandName:bandNames) {
            if (bandName.contains(prefix) && bandName.contains(swathIndexStr) && bandName.contains(polarization)) {
                return product.getBand(bandName);
            }
        }
        return null;
    }

    private boolean isSlavePixPosValid(final PixelPos slavePixPos, final int subswathIndex, final int sBurstIndex) {
        return (slavePixPos != null &&
                slavePixPos.y >= sSubSwath[subswathIndex - 1].linesPerBurst*sBurstIndex &&
                slavePixPos.y < sSubSwath[subswathIndex - 1].linesPerBurst*(sBurstIndex+1));
    }

    private void outputRangeAzimuthOffsets(final int x0, final int y0, final int w, final int h,
                                           final Map<Band, Tile> targetTileMap, final PixelPos[][] slavePixPos,
                                           final int subSwathIndex, final int mBurstIndex, final int sBurstIndex) {

        try {
            final Band azOffsetBand = getTargetBand("azOffset");
            final Band rgOffsetBand = getTargetBand("rgOffset");

            if (azOffsetBand == null || rgOffsetBand == null) {
                return;
            }

            Sentinel1Utils.SubSwathInfo mSubSwath = mSU.getSubSwath()[subSwathIndex - 1];
            Sentinel1Utils.SubSwathInfo sSubSwath = sSU.getSubSwath()[subSwathIndex - 1];

            final Tile tgtTileAzOffset = targetTileMap.get(azOffsetBand);
            final Tile tgtTileRgOffset = targetTileMap.get(rgOffsetBand);
            final ProductData tgtBufferAzOffset = tgtTileAzOffset.getDataBuffer();
            final ProductData tgtBufferRgOffset = tgtTileRgOffset.getDataBuffer();
            final TileIndex tgtIndex = new TileIndex(tgtTileAzOffset);

            for (int y = y0; y < y0 + h; y++) {
                tgtIndex.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < x0 + w; x++) {
                    final int tgtIdx = tgtIndex.getIndex(x);
                    final int xx = x - x0;

                    if (slavePixPos[yy][xx] == null) {
                        tgtBufferAzOffset.setElemFloatAt(tgtIdx, (float) noDataValue);
                        tgtBufferRgOffset.setElemFloatAt(tgtIdx, (float) noDataValue);
                    } else {
/*
                        final double mta = mSubSwath.burstFirstLineTime[mBurstIndex] +
                                (y - mBurstIndex*mSubSwath.linesPerBurst)*mSubSwath.azimuthTimeInterval;

                        final double mY = (mta - mSubSwath.burstFirstLineTime[0]) / mSubSwath.azimuthTimeInterval;

                        final double sta = sSubSwath.burstFirstLineTime[sBurstIndex] +
                                (slavePixPos[yy][xx].y - sBurstIndex*sSubSwath.linesPerBurst)*sSubSwath.azimuthTimeInterval;

                        final double sY = (sta - sSubSwath.burstFirstLineTime[0]) / sSubSwath.azimuthTimeInterval;

                        final float yOffset = (float)(mY - sY);

                        tgtBufferAzOffset.setElemFloatAt(tgtIdx, yOffset);
                        tgtBufferRgOffset.setElemFloatAt(tgtIdx, (float)(x - slavePixPos[yy][xx].x));
*/
                        //tgtBufferAzOffset.setElemFloatAt(tgtIdx, (float)(y - slavePixPos[yy][xx].y));
                        //tgtBufferRgOffset.setElemFloatAt(tgtIdx, (float)(x - slavePixPos[yy][xx].x));
                        tgtBufferAzOffset.setElemFloatAt(tgtIdx, (float)(slavePixPos[yy][xx].y));
                        tgtBufferRgOffset.setElemFloatAt(tgtIdx, (float)(slavePixPos[yy][xx].x));
                    }
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("outputRangeAzimuthOffsets", e);
        }
    }

    private void outputDEM(final int x0, final int y0, final int w, final int h,
                           final Map<Band, Tile> targetTileMap, final double[][] elevation) {

        try {
            final Band elevBand = getTargetBand("elevation");
            if (elevBand == null) {
                return;
            }

            final Tile tgtTileElev = targetTileMap.get(elevBand);
            final ProductData tgtBufferElev = tgtTileElev.getDataBuffer();
            final TileIndex tgtIndex = new TileIndex(tgtTileElev);

            for (int y = y0; y < y0 + h; y++) {
                tgtIndex.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < x0 + w; x++) {
                    final int tgtIdx = tgtIndex.getIndex(x);
                    final int xx = x - x0;
                    tgtBufferElev.setElemFloatAt(tgtIdx, (float)(elevation[yy][xx]));
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("outputDEM", e);
        }
    }

    private Band getTargetBand(final String name) {
        return getTargetBand(name, null, null);
    }

    private Band getTargetBand(final String name, final String tag, final String pol) {

        final Band[] targetBands = targetProduct.getBands();
        for (Band band : targetBands) {
            final String bandName = band.getName();
            if (tag != null && bandName.contains(name) && bandName.contains(tag) && pol != null && bandName.contains(pol)) {
                return band;
            } else if (tag == null && bandName.contains(name)) {
                return band;
            }
        }
        return null;
    }

    private static class PositionData {
        final PosVector earthPoint = new PosVector();
        final PosVector sensorPos = new PosVector();
        double azimuthIndex;
        double rangeIndex;
    }

    private static class ResamplingRaster implements Resampling.Raster {

        private final Tile tile;
        private final double[][] data;
        private final boolean usesNoData;
        private final boolean scalingApplied;
        private final double noDataValue;
        private final double geophysicalNoDataValue;

        public ResamplingRaster(final Tile tile, final double[][] data) {
            this.tile = tile;
            this.data = data;
            final RasterDataNode rasterDataNode = tile.getRasterDataNode();
            this.usesNoData = rasterDataNode.isNoDataValueUsed();
            this.noDataValue = rasterDataNode.getNoDataValue();
            this.geophysicalNoDataValue = rasterDataNode.getGeophysicalNoDataValue();
            this.scalingApplied = rasterDataNode.isScalingApplied();
        }

        public final int getWidth() {
            return tile.getWidth();
        }

        public final int getHeight() {
            return tile.getHeight();
        }

        public boolean getSamples(final int[] x, final int[] y, final double[][] samples) throws Exception {
            boolean allValid = true;

            try {
                double val;
                int i = 0;
                while (i < y.length) {
                    int j = 0;
                    while (j < x.length) {
                        val = data[y[i]][x[j]];

                        if (usesNoData) {
                            if (scalingApplied && geophysicalNoDataValue == val || noDataValue == val) {
                                val = Double.NaN;
                                allValid = false;
                            }
                        }
                        samples[i][j] = val;
                        ++j;
                    }
                    ++i;
                }
            } catch (Exception e) {
                SystemUtils.LOG.severe(e.getMessage());
                allValid = false;
            }

            return allValid;
        }
    }

    private static class BurstIndices {
        int firstBurstIndex = -1;
        int secondBurstIndex = -1;
        boolean inUpperPartOfFirstBurst = false;
        boolean inUpperPartOfSecondBurst = false;
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(BackGeocodingOp.class);
        }
    }
}
