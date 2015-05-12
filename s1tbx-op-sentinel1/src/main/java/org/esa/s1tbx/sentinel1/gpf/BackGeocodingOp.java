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
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.insar.gpf.geometric.SARGeocoding;
import org.esa.s1tbx.insar.gpf.Sentinel1Utils;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.dem.dataio.FileElevationModel;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.PosVector;
import org.esa.snap.datamodel.ProductInformation;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.eo.Constants;
import org.esa.snap.eo.GeoUtils;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.MetadataAttribute;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.RasterDataNode;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.esa.snap.framework.dataop.dem.ElevationModel;
import org.esa.snap.framework.dataop.resamp.Resampling;
import org.esa.snap.framework.dataop.resamp.ResamplingFactory;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProducts;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.gpf.StackUtils;
import org.esa.snap.gpf.TileIndex;
import org.esa.snap.util.ProductUtils;
import org.esa.snap.util.StringUtils;
import org.jlinda.core.delaunay.FastDelaunayTriangulator;
import org.jlinda.core.delaunay.Triangle;
import org.jlinda.core.delaunay.TriangulationException;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * "Backgeocoding" + "Coregistration" processing blocks in The Sentinel-1 TOPS InSAR processing chain.
 * Burst co-registration is performed using orbits and DEM.
 */
@OperatorMetadata(alias = "Back-Geocoding",
        category = "SAR Processing/Coregistration/S-1 TOPS Coregistration",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Bursts co-registration using orbit and DEM")
public final class BackGeocodingOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {"ACE", "GETASSE30", "SRTM 3Sec", "ASTER 1sec GDEM"},
            description = "The digital elevation model.",
            defaultValue = "SRTM 3Sec", label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
            ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
            ResamplingFactory.CUBIC_CONVOLUTION_NAME,
            ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
            ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME},
            defaultValue = ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
            label = "DEM Resampling Method")
    private String demResamplingMethod = ResamplingFactory.BICUBIC_INTERPOLATION_NAME;

    @Parameter(label = "External DEM")
    private File externalDEMFile = null;

    @Parameter(label = "DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    @Parameter(valueSet = {ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
            ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME,
            ResamplingFactory.BISINC_21_POINT_INTERPOLATION_NAME},
            defaultValue = ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME,
            description = "The method to be used when resampling the slave grid onto the master grid.",
            label = "Resampling Type")
    private String resamplingType = ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME;

    @Parameter(defaultValue = "true", label = "Mask out areas with no elevation")
    private boolean maskOutAreaWithoutElevation = true;

    @Parameter(defaultValue = "false", label = "Output Range and Azimuth Offset")
    private boolean outputRangeAzimuthOffset = false;

    @Parameter(defaultValue = "false", label = "Output Deramp and Demod Phase")
    private boolean outputDerampDemodPhase = false;

    private Resampling selectedResampling = null;

    private Product masterProduct = null;
    private Product slaveProduct = null;

    private Sentinel1Utils mSU = null;
    private Sentinel1Utils sSU = null;
    private Sentinel1Utils.SubSwathInfo[] mSubSwath = null;
    private Sentinel1Utils.SubSwathInfo[] sSubSwath = null;

    private int numOfSubSwath = 0;
    private String acquisitionMode = null;
    private ElevationModel dem = null;
    private boolean isElevationModelAvailable = false;
    private double demNoDataValue = 0; // no data value for DEM
    private double noDataValue = 0.0;

	private int subSwathIndex = 0;
    private int burstOffset = 0;
    private boolean burstOffsetComputed = false;
    private String swathIndexStr = null;
    private String subSwathName = null;
    private String polarization = null;

    private SARGeocoding.Orbit mOrbit = null;
    private SARGeocoding.Orbit sOrbit = null;

    private final double invalidIndex = -9999.0;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public BackGeocodingOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.snap.framework.datamodel.Product} annotated with the
     * {@link org.esa.snap.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs during operator initialisation.
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

			subSwathName = mSubSwathNames[0];
			subSwathIndex = 1; // subSwathIndex is always 1 because of split product
            swathIndexStr = mSubSwathNames[0].substring(2);

            final String[] mPolarizations = mSU.getPolarizations();
			final String[] sPolarizations = sSU.getPolarizations();
			if (!StringUtils.containsIgnoreCase(sPolarizations, mPolarizations[0])) {
				throw new OperatorException("Same polarization is expected.");
			}

			polarization = mPolarizations[0];

            if (externalDEMFile == null) {
                DEMFactory.checkIfDEMInstalled(demName);
            }

            DEMFactory.validateDEM(demName, masterProduct);

            selectedResampling = ResamplingFactory.createResampling(resamplingType);

            createTargetProduct();

            updateTargetProductMetadata();

            final Band masterBandI = getBand(masterProduct, "i_", swathIndexStr, polarization);
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

        MetadataElement mAbsRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct[0]);
        MetadataElement sAbsRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct[1]);

        final String mMission = mAbsRoot.getAttributeString(AbstractMetadata.MISSION);
        final String sMission = sAbsRoot.getAttributeString(AbstractMetadata.MISSION);
        if (!mMission.startsWith("SENTINEL-1") || !sMission.startsWith("SENTINEL-1")) {
            throw new OperatorException("Source product has invalid mission for Sentinel1 product");
        }

        final String mProductType = mAbsRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
        final String sProductType = sAbsRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
        if (!mProductType.equals("SLC") || !sProductType.equals("SLC")) {
            throw new OperatorException("Source product should be SLC product");
        }

        final String mAcquisitionMode = mAbsRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        final String sAcquisitionMode = sAbsRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        if (!mAcquisitionMode.equals(sAcquisitionMode)) {
            throw new OperatorException("Source products should have the same acquisition modes");
        }
        acquisitionMode = mAcquisitionMode;
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(
                masterProduct.getName(),
                masterProduct.getProductType(),
                masterProduct.getSceneRasterWidth(),
                masterProduct.getSceneRasterHeight());

        final String[] masterBandNames = masterProduct.getBandNames();
        final String mstSuffix = "_mst" + StackUtils.getBandTimeStamp(masterProduct);
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
        final int masterBandWidth = masterBand.getSceneRasterWidth();
        final int masterBandHeight = masterBand.getSceneRasterHeight();

        final String[] slaveBandNames = slaveProduct.getBandNames();
        final String slvSuffix = "_slv1" + StackUtils.getBandTimeStamp(slaveProduct);
        for (String bandName:slaveBandNames) {
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

            if(targetBand.getUnit().equals(Unit.IMAGINARY)) {
                int idx = targetProduct.getBandIndex(targetBand.getName());
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBandAt(idx-1), targetBand, slvSuffix);
            }
        }

        //final Band[] trgBands = targetProduct.getBands();
        //for(int i=0; i < trgBands.length; ++i) {
        //    if(trgBands[i].getUnit().equals(Unit.REAL)) {
        //        final String suffix = trgBands[i].getName().contains("_mst") ? mstSuffix : slvSuffix;
        //        ReaderUtils.createVirtualIntensityBand(targetProduct, trgBands[i], trgBands[i+1], suffix);
        //    }
        //}

        ProductUtils.copyProductNodes(masterProduct, targetProduct);
        copySlaveMetadata();

        if (outputRangeAzimuthOffset) {
            final Band azOffsetBand = new Band(
                    "azOffset",
                    ProductData.TYPE_FLOAT32,
                    masterBandWidth,
                    masterBandHeight);

            azOffsetBand.setUnit("Index");
            targetProduct.addBand(azOffsetBand);

            final Band rgOffsetBand = new Band(
                    "rgOffset",
                    ProductData.TYPE_FLOAT32,
                    masterBandWidth,
                    masterBandHeight);

            rgOffsetBand.setUnit("Index");
            targetProduct.addBand(rgOffsetBand);
        }

        if (outputDerampDemodPhase) {
            final Band phaseBand = new Band(
                    "derampDemodPhase",
                    ProductData.TYPE_FLOAT32,
                    masterBandWidth,
                    masterBandHeight);

            phaseBand.setUnit("radian");
            targetProduct.addBand(phaseBand);
        }
    }

    private void copySlaveMetadata() {

        final MetadataElement targetSlaveMetadataRoot = AbstractMetadata.getSlaveMetadata(targetProduct.getMetadataRoot());
        final MetadataElement slvAbsMetadata = AbstractMetadata.getAbstractedMetadata(slaveProduct);
        if (slvAbsMetadata != null) {
            final String timeStamp = StackUtils.getBandTimeStamp(slaveProduct);
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
     * @throws org.esa.snap.framework.gpf.OperatorException
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
                if (mSU.getPolarizations().length != 1 || sSU.getPolarizations().length != 1) {
                    throw new OperatorException("Split product with one polarization is expected.");
                }

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
                System.out.println("burstIndex = " + burstIndex + ": ntx0 = " + ntx0 + ", nty0 = " + nty0 + ", ntw = " + ntw + ", nth = " + nth);

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

    private int getSubswathIndex(final String targetBandName) {
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

    private BurstIndices getBurstIndices(final int subSwathIndex, final Sentinel1Utils su,
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
                final double alt = dem.getElevation(geoPos);
                if (alt == demNoDataValue) {
                    continue;
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

        final PixelPos[][] slavePixPos = computeSlavePixPos(
                subSwathIndex, mBurstIndex, sBurstIndex, x0, y0, w, h, extendedAmount, pm);

        if (slavePixPos == null) {
            return;
        }

        if (outputRangeAzimuthOffset) {
            outputRangeAzimuthOffsets(x0, y0, w, h, targetTileMap, slavePixPos, subSwathIndex, mBurstIndex, sBurstIndex);
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
                derampDemodI, derampDemodQ, slavePixPos, subSwathIndex, sBurstIndex);
    }

    private PixelPos[][] computeSlavePixPos(final int subSwathIndex, final int mBurstIndex, final int sBurstIndex,
                                            final int x0, final int y0, final int w, final int h,
                                            final double[] extendedAmount, ProgressMonitor pm)
            throws Exception {

        try {
            final int xmin = x0 - (int)extendedAmount[3];
            final int ymin = y0 - (int)extendedAmount[1];
            final int ymax = y0 + h + (int)Math.abs(extendedAmount[0]);
            final int xmax = x0 + w + (int)Math.abs(extendedAmount[2]);

            // Compute lat/lon boundaries (with extensions) for target tile
            final double[] latLonMinMax = new double[4];

            computeImageGeoBoundary(subSwathIndex, mBurstIndex, xmin, xmax, ymin, ymax, latLonMinMax);

            final double delta = (double)dem.getDescriptor().getDegreeRes() / (double)dem.getDescriptor().getPixelRes();
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

            boolean noValidSlavePixPos = true;
            for (int l = 0; l < numLines; l++) {
                for (int p = 0; p < numPixels; p++) {

                    pix.setLocation(lonMinIdx + p, latMaxIdx + l);
                    GeoPos gp = dem.getGeoPos(pix);
                    lat[l][p] = gp.lat;
                    lon[l][p] = gp.lon;
                    final double alt = dem.getElevation(gp);

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
                return null;
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

            TriangleUtils.gridDataLinear(
                    masterAz, masterRg, slaveAz, slaveRg, lat, lon, azArray, rgArray, latArray, lonArray,
                    tileWindow, rgAzRatio, 1, 1, invalidIndex, 0);

            boolean allElementsAreNull = true;
            final PixelPos[][] slavePixelPos = new PixelPos[h][w];

            double alt = 0;
            for(int yy = 0; yy < h; yy++) {
                for (int xx = 0; xx < w; xx++) {
                    if(maskOutAreaWithoutElevation) {
                        alt = dem.getElevation(new GeoPos(latArray[yy][xx], lonArray[yy][xx]));
                    }
                    if (rgArray[yy][xx] == invalidIndex || azArray[yy][xx] == invalidIndex || (maskOutAreaWithoutElevation && alt == demNoDataValue)) {
                        slavePixelPos[yy][xx] = null;
                    } else {
                        slavePixelPos[yy][xx] = new PixelPos(rgArray[yy][xx], azArray[yy][xx]);
                        allElementsAreNull = false;
                    }
                }
            }

            if (allElementsAreNull) {
                return null;
            }

            return slavePixelPos;

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeSlavePixPos", e);
        }

        return null;
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
    private boolean getPosition(final int subSwathIndex, final int burstIndex, final Sentinel1Utils su,
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
                                      final PixelPos[][] slavePixPos, final int subswathIndex, final int sBurstIndex) {

        try {
            final ResamplingRaster resamplingRasterI = new ResamplingRaster(slaveTileI, derampDemodI);
            final ResamplingRaster resamplingRasterQ = new ResamplingRaster(slaveTileQ, derampDemodQ);
            final ResamplingRaster resamplingRasterPhase = new ResamplingRaster(slaveTileI, derampDemodPhase);

            final Band[] targetBands = targetProduct.getBands();
            Band iBand = null;
            Band qBand = null;
            Band phaseBand = null;
            for (Band band : targetBands) {
                final String bandName = band.getName();
                if (bandName.contains("i_") && bandName.contains("_slv")) {
                    iBand = band;
                } else if (bandName.contains("q_") && bandName.contains("_slv")) {
                    qBand = band;
                } else if (bandName.contains("derampDemodPhase")) {
                    phaseBand = band;
                }
            }

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

                        selectedResampling.computeIndex(
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

                        tgtBufferI.setElemDoubleAt(tgtIdx, rerampRemodI);
                        tgtBufferQ.setElemDoubleAt(tgtIdx, rerampRemodQ);

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

    private Band getBand(
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
            final Band azOffsetBand = targetProduct.getBand("azOffset");
            final Band rgOffsetBand = targetProduct.getBand("rgOffset");

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
                System.out.println(e.getMessage());
                allValid = false;
            }

            return allValid;
        }
    }

    private static class TriangleUtils {

        public static void gridDataLinear(final double[][] x_in, final double[][] y_in,
                                          final double[][] z1_in, final double[][] z2_in,
                                          final double[][] z3_in, final double[][] z4_in,
                                          final double[][] z1_out, final double[][] z2_out,
                                          final double[][] z3_out, final double[][] z4_out,
                                          final org.jlinda.core.Window window, final double xyRatio,
                                          final int xScale, final int yScale, final double invalidIndex,
                                          final int offset)
                throws Exception {

            final FastDelaunayTriangulator FDT = triangulate(x_in, y_in, xyRatio, invalidIndex);

            if (FDT == null) {
                return;
            }

            interpolate(xyRatio, window, xScale, yScale, offset, invalidIndex, FDT,
                    z1_in, z2_in, z3_in, z4_in, z1_out, z2_out, z3_out, z4_out);
        }

        private static FastDelaunayTriangulator triangulate(final double[][] x_in, final double[][] y_in,
                                                            final double xyRatio, final double invalidIndex)
                throws Exception {

            java.util.List<Geometry> list = new ArrayList<>();
            GeometryFactory gf = new GeometryFactory();
            for (int i = 0; i < x_in.length; i++) {
                for (int j = 0; j < x_in[0].length; j++) {
                    if (x_in[i][j] == invalidIndex || y_in[i][j] == invalidIndex) {
                        continue;
                    }
                    list.add(gf.createPoint(new Coordinate(x_in[i][j], y_in[i][j] * xyRatio, i*x_in[0].length + j)));
                }
            }

            if (list.size() < 3) {
                return null;
            }

            FastDelaunayTriangulator FDT = new FastDelaunayTriangulator();
            try {
                FDT.triangulate(list.iterator());
            } catch (TriangulationException te) {
                te.printStackTrace();
            }

            return FDT;
        }

        private static void interpolate(final double xyRatio, final org.jlinda.core.Window tileWindow,
                                        final double xScale, final double yScale, final double offset,
                                        final double invalidIndex, FastDelaunayTriangulator FDT,
                                        final double[][] z1_in, final double[][] z2_in,
                                        final double[][] z3_in, final double[][] z4_in,
                                        final double[][] z1_out, final double[][] z2_out,
                                        final double[][] z3_out, final double[][] z4_out) {

            final double x_min = tileWindow.linelo;
            final double y_min = tileWindow.pixlo;

            int i, j; // counters
            long i_min, i_max, j_min, j_max; // minimas/maximas
            double xp, yp;
            double xkj, ykj, xlj, ylj;
            double f; // function
            double a, b, c;
            double zj, zk, zl, zkj, zlj;

            // containers for xy coordinates of Triangles: p1-p2-p3-p1
            double[] vx = new double[4];
            double[] vy = new double[4];
            double[] vz = new double[3];
            double[] abc1 = new double[3];
            double[] abc2 = new double[3];
            double[] abc3 = new double[3];
            double[] abc4 = new double[3];

            // declare demRadarCode_phase
            final int nx = (int) tileWindow.lines();
            final int ny = (int) tileWindow.pixels();

            // interpolate: loop over triangles
            for (Triangle triangle : FDT.triangles) {

                // store triangle coordinates in local variables
                vx[0] = vx[3] = triangle.getA().x;
                vy[0] = vy[3] = triangle.getA().y / xyRatio;

                vx[1] = triangle.getB().x;
                vy[1] = triangle.getB().y / xyRatio;

                vx[2] = triangle.getC().x;
                vy[2] = triangle.getC().y / xyRatio;

                // skip invalid indices
                if (vx[0] == invalidIndex || vx[1] == invalidIndex || vx[2] == invalidIndex ||
                    vy[0] == invalidIndex || vy[1] == invalidIndex || vy[2] == invalidIndex) {
                    continue;
                }

                // Compute grid indices the current triangle may cover
                xp = Math.min(Math.min(vx[0], vx[1]), vx[2]);
                i_min = coordToIndex(xp, x_min, xScale, offset);

                xp = Math.max(Math.max(vx[0], vx[1]), vx[2]);
                i_max = coordToIndex(xp, x_min, xScale, offset);

                yp = Math.min(Math.min(vy[0], vy[1]), vy[2]);
                j_min = coordToIndex(yp, y_min, yScale, offset);

                yp = Math.max(Math.max(vy[0], vy[1]), vy[2]);
                j_max = coordToIndex(yp, y_min, yScale, offset);

                // skip triangle that is above or below the region
                if ((i_max < 0) || (i_min >= nx)) {
                    continue;
                }

                // skip triangle that is on the left or right of the region
                if ((j_max < 0) || (j_min >= ny)) {
                    continue;
                }

                // triangle covers the upper or lower boundary
                if (i_min < 0) {
                    i_min = 0;
                }

                if (i_max >= nx) {
                    i_max = nx - 1;
                }

                // triangle covers left or right boundary
                if (j_min < 0) {
                    j_min = 0;
                }

                if (j_max >= ny) {
                    j_max = ny - 1;
                }

                // compute plane defined by the three vertices of the triangle: z = ax + by + c
                xkj = vx[1] - vx[0];
                ykj = vy[1] - vy[0];
                xlj = vx[2] - vx[0];
                ylj = vy[2] - vy[0];

                f = 1.0 / (xkj * ylj - ykj * xlj);

                vz[0] = triangle.getA().z;
                vz[1] = triangle.getB().z;
                vz[2] = triangle.getC().z;

                abc1 = getABC(vx, vy, vz, z1_in, f, xkj, ykj, xlj, ylj);
                abc2 = getABC(vx, vy, vz, z2_in, f, xkj, ykj, xlj, ylj);
                abc3 = getABC(vx, vy, vz, z3_in, f, xkj, ykj, xlj, ylj);
                abc4 = getABC(vx, vy, vz, z4_in, f, xkj, ykj, xlj, ylj);

                for (i = (int) i_min; i <= i_max; i++) {
                    xp = indexToCoord(i, x_min, xScale, offset);
                    for (j = (int) j_min; j <= j_max; j++) {
                        yp = indexToCoord(j, y_min, yScale, offset);

                        if (!pointInTriangle(vx, vy, xp, yp)) {
                            continue;
                        }

                        z1_out[i][j] = abc1[0] * xp + abc1[1] * yp + abc1[2];
                        z2_out[i][j] = abc2[0] * xp + abc2[1] * yp + abc2[2];
                        z3_out[i][j] = abc3[0] * xp + abc3[1] * yp + abc3[2];
                        z4_out[i][j] = abc4[0] * xp + abc4[1] * yp + abc4[2];
                    }
                }
            }
        }

        private static double[] getABC(
                final double[] vx, final double[] vy, final double[] vz, final double[][] z_in,
                final double f, final double  xkj, final double ykj, final double xlj, final double ylj) {

            final int i0 = (int)(vz[0]/z_in[0].length);
            final int j0 = (int)(vz[0] - i0*z_in[0].length);
            final double zj = z_in[i0][j0];

            final int i1 = (int)(vz[1]/z_in[1].length);
            final int j1 = (int)(vz[1] - i1*z_in[1].length);
            final double zk = z_in[i1][j1];

            final int i2 = (int)(vz[2]/z_in[2].length);
            final int j2 = (int)(vz[2] - i2*z_in[2].length);
            final double zl = z_in[i2][j2];

            final double zkj = zk - zj;
            final double zlj = zl - zj;

            final double[] abc = new double[3];
            abc[0] = -f * (ykj * zlj - zkj * ylj);
            abc[1] = -f * (zkj * xlj - xkj * zlj);
            abc[2] = -abc[0] * vx[1] - abc[1] * vy[1] + zk;

            return abc;
        }

        private static boolean pointInTriangle(double[] xt, double[] yt, double x, double y) {
            int iRet0 = ((xt[2] - xt[0]) * (y - yt[0])) > ((x - xt[0]) * (yt[2] - yt[0])) ? 1 : -1;
            int iRet1 = ((xt[0] - xt[1]) * (y - yt[1])) > ((x - xt[1]) * (yt[0] - yt[1])) ? 1 : -1;
            int iRet2 = ((xt[1] - xt[2]) * (y - yt[2])) > ((x - xt[2]) * (yt[1] - yt[2])) ? 1 : -1;

            return (iRet0 > 0 && iRet1 > 0 && iRet2 > 0) || (iRet0 < 0 && iRet1 < 0 && iRet2 < 0);
        }

        private static long coordToIndex(final double coord, final double coord0, final double deltaCoord, final double offset) {
            return irint((((coord - coord0) / (deltaCoord)) - offset));
        }

        private static double indexToCoord(final long idx, final double coord0, final double deltaCoord, final double offset) {
            return (coord0 + idx * deltaCoord + offset);
        }

        private static long irint(final double coord) {
            return ((long) rint(coord));
        }

        private static double rint(final double coord) {
            return Math.floor(coord + 0.5);
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
     * {@code META-INF/services/org.esa.snap.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.snap.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.snap.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(BackGeocodingOp.class);
        }
    }
}
