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
package org.esa.nest.dataio.cosmo;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Guardian;
import org.esa.nest.dataio.SARReader;
import org.esa.nest.dataio.netcdf.*;
import org.esa.nest.datamodel.metadata.AbstractMetadata;
import org.esa.nest.datamodel.metadata.AbstractMetadataIO;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.datamodel.metadata.AbstractMetadataSAR;
import org.esa.nest.eo.Constants;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.util.XMLSupport;
import org.jdom2.Document;
import org.jdom2.Element;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The product reader for CosmoSkymed products.
 */
public class CosmoSkymedReader extends SARReader {

    private NetcdfFile netcdfFile = null;
    private Product product = null;
    private NcVariableMap variableMap = null;
    private boolean yFlipped = false;
    private boolean isComplex = false;
    private final ProductReaderPlugIn readerPlugIn;

    private final Map<Band, Variable> bandMap = new HashMap<Band, Variable>(10);

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public CosmoSkymedReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        this.readerPlugIn = readerPlugIn;
    }

    private void initReader() {
        product = null;
        netcdfFile = null;
        variableMap = null;
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p/>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile = ReaderUtils.getFileFromInput(getInput());
        initReader();

        final NetcdfFile netcdfFile = NetcdfFile.open(inputFile.getPath());
        if (netcdfFile == null) {
            close();
            throw new IllegalFileFormatException(inputFile.getName() +
                    " Could not be interpretted by the reader.");
        }

        final Map<NcRasterDim, List<Variable>> variableListMap = NetCDFUtils.getVariableListMap(netcdfFile.getRootGroup());
        if (variableListMap.isEmpty()) {
            close();
            throw new IllegalFileFormatException("No netCDF variables found which could\n" +
                    "be interpreted as remote sensing bands.");  /*I18N*/
        }
        removeQuickLooks(variableListMap);

        final NcRasterDim rasterDim = NetCDFUtils.getBestRasterDim(variableListMap);
        final Variable[] rasterVariables = getRasterVariables(variableListMap, rasterDim);
        final Variable[] tiePointGridVariables = NetCDFUtils.getTiePointGridVariables(variableListMap, rasterVariables);

        this.netcdfFile = netcdfFile;
        variableMap = new NcVariableMap(rasterVariables);
        yFlipped = false;

        final NcAttributeMap globalAttributes = NcAttributeMap.create(this.netcdfFile);

        final String productType = NetCDFUtils.getProductType(globalAttributes, readerPlugIn.getFormatNames()[0]);
        final int rasterWidth = rasterDim.getDimX().getLength();
        final int rasterHeight = rasterDim.getDimY().getLength();

        product = new Product(inputFile.getName(),
                productType,
                rasterWidth, rasterHeight,
                this);
        product.setFileLocation(inputFile);
        product.setDescription(NetCDFUtils.getProductDescription(globalAttributes));
        product.setStartTime(NetCDFUtils.getSceneRasterStartTime(globalAttributes));
        product.setEndTime(NetCDFUtils.getSceneRasterStopTime(globalAttributes));

        addMetadataToProduct();
        addBandsToProduct(rasterVariables);
        addTiePointGridsToProduct(tiePointGridVariables);
        addGeoCodingToProduct(rasterDim);
        addSlantRangeToFirstPixel();
        addFirstLastLineTimes(rasterHeight);
        addSRGRCoefficients();
        addDopplerCentroidCoefficients();
        setQuicklookBandName(product);

        product.getGcpGroup();
        product.setModified(false);

        return product;
    }

    @Override
    public void close() throws IOException {
        if (product != null) {
            product = null;
            variableMap.clear();
            variableMap = null;
            netcdfFile.close();
            netcdfFile = null;
        }
        super.close();
    }

    private static Variable[] getRasterVariables(Map<NcRasterDim, List<Variable>> variableLists,
                                                 NcRasterDim rasterDim) {
        final List<Variable> varList = variableLists.get(rasterDim);
        final List<Variable> list = new ArrayList<>(5);
        for (Variable var : varList) {
            if (!var.getShortName().equals("GIM")) {
                list.add(var);
            }
        }
        return list.toArray(new Variable[list.size()]);
    }

    private static void removeQuickLooks(Map<NcRasterDim, List<Variable>> variableListMap) {
        final String[] excludeList = {"qlk"};
        final NcRasterDim[] keys = variableListMap.keySet().toArray(new NcRasterDim[variableListMap.keySet().size()]);
        final List<NcRasterDim> removeList = new ArrayList<NcRasterDim>();

        for (final NcRasterDim rasterDim : keys) {
            final List<Variable> varList = variableListMap.get(rasterDim);
            boolean found = false;
            for (Variable v : varList) {
                if (found) break;

                final String vName = v.getShortName().toLowerCase();
                for (String str : excludeList) {
                    if (vName.contains(str)) {
                        removeList.add(rasterDim);
                        found = true;
                        break;
                    }
                }
            }
        }
        for (NcRasterDim key : removeList) {
            variableListMap.remove(key);
        }
    }

    private void addMetadataToProduct() throws IOException {

        final MetadataElement origMetadataRoot = AbstractMetadata.addOriginalProductMetadata(product);
        NetCDFUtils.addAttributes(origMetadataRoot, NetcdfConstants.GLOBAL_ATTRIBUTES_NAME,
                netcdfFile.getGlobalAttributes());

        addDeliveryNote(product);

        for (final Variable variable : variableMap.getAll()) {
            NetCDFUtils.addAttributes(origMetadataRoot, variable.getShortName(), variable.getAttributes());
        }

        //final Group rootGroup = netcdfFile.getRootGroup();
        //NetCDFUtils.addGroups(product.getMetadataRoot(), rootGroup);

        addAbstractedMetadataHeader(product, product.getMetadataRoot());
    }

    private static void addDeliveryNote(final Product product) {
        try {
            final File folder = product.getFileLocation().getParentFile();
            File dnFile = null;
            for (File f : folder.listFiles()) {
                final String name = f.getName().toLowerCase();
                if (name.startsWith("dfdn") && name.endsWith("xml")) {
                    dnFile = f;
                    break;
                }
            }
            if (dnFile != null) {
                final Document xmlDoc = XMLSupport.LoadXML(dnFile.getAbsolutePath());
                final Element rootElement = xmlDoc.getRootElement();

                AbstractMetadataIO.AddXMLMetadata(rootElement, AbstractMetadata.getOriginalProductMetadata(product));
            }
        } catch (IOException e) {
            //System.out.println("Unable to read Delivery Note for "+product.getName());
        }
    }

    private void addAbstractedMetadataHeader(Product product, MetadataElement root) throws IOException {

        final AbstractMetadata absMeta = AbstractMetadata.getAbstractedMetadata(product);
        AbstractMetadataSAR sarMeta = AbstractMetadataSAR.getSARAbstractedMetadata(product);

        final String defStr = AbstractMetadata.NO_METADATA_STRING;
        final int defInt = AbstractMetadata.NO_METADATA;

        final MetadataElement globalElem = AbstractMetadata.addOriginalProductMetadata(product).
                getElement(NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);

        absMeta.setAttribute(AbstractMetadata.product_name, globalElem.getAttributeString("Product_Filename", defStr));
        final String productType = globalElem.getAttributeString("Product_Type", defStr);
        absMeta.setAttribute(AbstractMetadata.product_type, productType);
        final String mode = globalElem.getAttributeString("Acquisition_Mode", defStr);
        absMeta.setAttribute(AbstractMetadata.descriptor, mode);
        absMeta.setAttribute(AbstractMetadata.acquisition_mode, mode);

        if (mode.contains("HUGE") && productType.contains("SCS")) {
            throw new IOException("Complex " + mode + " products are not supported for Cosmo-Skymed");
        }

        absMeta.setAttribute(AbstractMetadata.mission, globalElem.getAttributeString("Satellite_Id", "CSK"));
        absMeta.setAttribute(AbstractMetadata.processing_time,
                ReaderUtils.getTime(globalElem, "Product_Generation_UTC", AbstractMetadata.dateFormat));

        absMeta.setAttribute(AbstractMetadata.processing_system,
                globalElem.getAttributeString("Processing_Centre", defStr));

        absMeta.setAttribute(AbstractMetadata.abs_orbit, globalElem.getAttributeInt("Orbit_Number", defInt));
        absMeta.setAttribute(AbstractMetadata.pass, globalElem.getAttributeString("Orbit_Direction", defStr));
        absMeta.setAttribute(AbstractMetadata.sample_type, getSampleType(globalElem));

        absMeta.setAttribute(AbstractMetadata.num_output_lines,
                product.getSceneRasterHeight());
        absMeta.setAttribute(AbstractMetadata.num_samples_per_line,
                product.getSceneRasterWidth());
        absMeta.setAttribute(AbstractMetadata.total_size, ReaderUtils.getTotalSize(product));

        if (productType.contains("GEC")) {
            absMeta.setAttribute(AbstractMetadata.map_projection,
                    globalElem.getAttributeString("Projection_ID", defStr));
        }

        sarMeta.setAttribute(AbstractMetadataSAR.radar_frequency,
                globalElem.getAttributeDouble("Radar_Frequency", defInt) / Constants.oneMillion);

        sarMeta.setAttribute(AbstractMetadataSAR.geo_ref_system,
                globalElem.getAttributeString("Ellipsoid_Designator", defStr));

        sarMeta.setAttribute(AbstractMetadataSAR.antenna_pointing,
                globalElem.getAttributeString("Look_Side", defStr).toLowerCase());
        sarMeta.setAttribute(AbstractMetadataSAR.range_looks,
                globalElem.getAttributeDouble("Range_Processing_Number_of_Looks", defInt));
        sarMeta.setAttribute(AbstractMetadataSAR.azimuth_looks,
                globalElem.getAttributeDouble("Azimuth_Processing_Number_of_Looks", defInt));

        // Global calibration attributes
        /*
        sarMeta.setAttribute(absRoot, AbstractMetadata.abs_calibration_flag,
        		globalElem.getAttributeInt("Calibration_Constant_Compensation_Flag"));
        */
        sarMeta.setAttribute(AbstractMetadataSAR.coregistered_stack, 0);

        final String rngSpreadComp = globalElem.getAttributeString(
                "Range_Spreading_Loss_Compensation_Geometry", defStr);
        if (rngSpreadComp.equals("NONE"))
            sarMeta.setAttribute(AbstractMetadataSAR.range_spread_comp_flag, 0);
        else
            sarMeta.setAttribute(AbstractMetadataSAR.range_spread_comp_flag, 1);

        final String incAngComp = globalElem.getAttributeString(
                "Incidence_Angle_Compensation_Geometry", defStr);
        if (incAngComp.equals("NONE"))
            sarMeta.setAttribute(AbstractMetadataSAR.inc_angle_comp_flag, 0);
        else
            sarMeta.setAttribute(AbstractMetadataSAR.inc_angle_comp_flag, 1);

        final String antElevComp = globalElem.getAttributeString(
                "Range_Antenna_Pattern_Compensation_Geometry", defStr);
        if (antElevComp.equals("NONE")) {
            sarMeta.setAttribute(AbstractMetadataSAR.ant_elev_corr_flag, 0);
        } else {
            sarMeta.setAttribute(AbstractMetadataSAR.ant_elev_corr_flag, 1);
        }

        sarMeta.setAttribute(AbstractMetadataSAR.ref_inc_angle,
                globalElem.getAttributeDouble("Reference_Incidence_Angle", defInt));
        sarMeta.setAttribute(AbstractMetadataSAR.ref_slant_range,
                globalElem.getAttributeDouble("Reference_Slant_Range", defInt));
        sarMeta.setAttribute(AbstractMetadataSAR.ref_slant_range_exp,
                globalElem.getAttributeDouble("Reference_Slant_Range_Exponent", defInt));
        sarMeta.setAttribute(AbstractMetadataSAR.rescaling_factor,
                globalElem.getAttributeDouble("Rescaling_Factor", defInt));

        final MetadataElement s01Elem = globalElem.getElement("S01");
        if (s01Elem != null) {
            sarMeta.setAttribute(AbstractMetadataSAR.pulse_repetition_frequency,
                    s01Elem.getAttributeDouble("PRF", defInt));
            sarMeta.setAttribute(AbstractMetadataSAR.range_sampling_rate,
                    s01Elem.getAttributeDouble("Sampling_Rate", defInt) / Constants.oneMillion);
            sarMeta.setAttribute(AbstractMetadataSAR.mds1_tx_rx_polar,
                    s01Elem.getAttributeString("Polarisation", defStr));

            // add Range and Azimuth bandwidth
            final double rangeBW = s01Elem.getAttributeDouble("Range_Focusing_Bandwidth"); // Hz
            final double azimuthBW = s01Elem.getAttributeDouble("Azimuth_Focusing_Bandwidth"); // Hz

            sarMeta.setAttribute(AbstractMetadataSAR.range_bandwidth, rangeBW / Constants.oneMillion);
            sarMeta.setAttribute(AbstractMetadataSAR.azimuth_bandwidth, azimuthBW);

            // Calibration constant read from Global_Metadata during calibration initialization
        }
        final MetadataElement s02Elem = globalElem.getElement("S02");
        if (s02Elem != null) {
            sarMeta.setAttribute(AbstractMetadataSAR.mds2_tx_rx_polar,
                    s02Elem.getAttributeString("Polarisation", defStr));
        }

        if (isComplex) {
            sarMeta.setAttribute(AbstractMetadataSAR.srgr_flag, 0);
        } else {
            sarMeta.setAttribute(AbstractMetadataSAR.srgr_flag, 1);
        }

        addOrbitStateVectors(absMeta, globalElem);
    }

    private void addSlantRangeToFirstPixel() throws IOException {
        final AbstractMetadataSAR sarMeta = AbstractMetadataSAR.getSARAbstractedMetadata(product);
        final MetadataElement bandElem = getBandElement(product.getBandAt(0));
        if (bandElem != null) {
            final double slantRangeTime = bandElem.getAttributeDouble("Zero_Doppler_Range_First_Time", 0); //s
            sarMeta.setAttribute(AbstractMetadataSAR.slant_range_to_first_pixel,
                    slantRangeTime * Constants.halfLightSpeed);
        }
    }

    private static void addOrbitStateVectors(final AbstractMetadata absRoot, final MetadataElement globalElem) {

        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);
        final ProductData.UTC referenceUTC = ReaderUtils.getTime(globalElem, "Reference_UTC", AbstractMetadata.dateFormat);
        final int numPoints = globalElem.getAttributeInt("Number_of_State_Vectors");

        for (int i = 0; i < numPoints; i++) {
            final double stateVectorTime = globalElem.getAttribute("State_Vectors_Times").getData().getElemDoubleAt(i);
            final ProductData.UTC orbitTime =
                    new ProductData.UTC(referenceUTC.getMJD() + stateVectorTime / Constants.secondsInDay);

            final ProductData pos = globalElem.getAttribute("ECEF_Satellite_Position").getData();
            final ProductData vel = globalElem.getAttribute("ECEF_Satellite_Velocity").getData();

            final double satellitePositionX = pos.getElemDoubleAt(3 * i);
            final double satellitePositionY = pos.getElemDoubleAt(3 * i + 1);
            final double satellitePositionZ = pos.getElemDoubleAt(3 * i + 2);
            final double satelliteVelocityX = vel.getElemDoubleAt(3 * i);
            final double satelliteVelocityY = vel.getElemDoubleAt(3 * i + 1);
            final double satelliteVelocityZ = vel.getElemDoubleAt(3 * i + 2);

            final MetadataElement orbitVectorElem = new MetadataElement(AbstractMetadata.orbit_vector + (i + 1));

            orbitVectorElem.setAttributeUTC(AbstractMetadata.orbit_vector_time, orbitTime);
            orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_pos, satellitePositionX);
            orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_pos, satellitePositionY);
            orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_pos, satellitePositionZ);
            orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_x_vel, satelliteVelocityX);
            orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_y_vel, satelliteVelocityY);
            orbitVectorElem.setAttributeDouble(AbstractMetadata.orbit_vector_z_vel, satelliteVelocityZ);

            orbitVectorListElem.addElement(orbitVectorElem);
        }
    }

    private void addDopplerCentroidCoefficients() throws IOException {

        final AbstractMetadata absMeta = AbstractMetadata.getAbstractedMetadata(product);
        final AbstractMetadataSAR sarMeta = AbstractMetadataSAR.getSARAbstractedMetadata(product);
        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement globalElem = root.getElement(NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);

        final MetadataElement dopplerCentroidCoefficientsElem = sarMeta.getElement(AbstractMetadataSAR.dop_coefficients);
        final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadataSAR.dop_coef_list + ".1");
        dopplerCentroidCoefficientsElem.addElement(dopplerListElem);

        final ProductData.UTC utcTime = absMeta.getAttributeUTC(AbstractMetadata.first_line_time);
        dopplerListElem.setAttributeUTC(AbstractMetadataSAR.dop_coef_time, utcTime);

        sarMeta.addAbstractedAttribute(dopplerListElem, AbstractMetadataSAR.slant_range_time,
                ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
        sarMeta.setAttribute(dopplerListElem, AbstractMetadataSAR.slant_range_time, 0.0);

        for (int i = 0; i < 6; i++) {
            final double coefValue =
                    globalElem.getAttribute("Centroid_vs_Range_Time_Polynomial").getData().getElemDoubleAt(i);
            final MetadataElement coefElem = new MetadataElement(AbstractMetadataSAR.coefficient + '.' + (i + 1));
            dopplerListElem.addElement(coefElem);
            sarMeta.addAbstractedAttribute(coefElem, AbstractMetadataSAR.dop_coef,
                    ProductData.TYPE_FLOAT64, "", "Doppler Centroid Coefficient");
            sarMeta.setAttribute(coefElem, AbstractMetadataSAR.dop_coef, coefValue);
        }
    }

    private void addFirstLastLineTimes(final int rasterHeight) {
        final AbstractMetadata absMeta = AbstractMetadata.getAbstractedMetadata(product);
        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement globalElem = root.getElement(NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);
        final MetadataElement bandElem = getBandElement(product.getBandAt(0));

        final double referenceUTC = ReaderUtils.getTime(globalElem, "Reference_UTC", AbstractMetadata.dateFormat).getMJD(); // in days
        double firstLineTime = bandElem.getAttributeDouble("Zero_Doppler_Azimuth_First_Time", 0) / (24 * 3600); // in days
        if (firstLineTime == 0) {
            firstLineTime = globalElem.getElement("S01").getElement("B001").getAttributeDouble("Azimuth_First_Time") / (24 * 3600); // in days
        }
        double lastLineTime = bandElem.getAttributeDouble("Zero_Doppler_Azimuth_Last_Time", 0) / (24 * 3600); // in days
        if (lastLineTime == 0) {
            lastLineTime = globalElem.getElement("S01").getElement("B001").getAttributeDouble("Azimuth_Last_Time") / (24 * 3600); // in days
        }
        double lineTimeInterval = bandElem.getAttributeDouble("Line_Time_Interval", 0); // in s
        final ProductData.UTC startTime = new ProductData.UTC(referenceUTC + firstLineTime);
        final ProductData.UTC stopTime = new ProductData.UTC(referenceUTC + lastLineTime);

        absMeta.setAttribute(AbstractMetadata.first_line_time, startTime);
        absMeta.setAttribute(AbstractMetadata.last_line_time, stopTime);
        product.setStartTime(startTime);
        product.setEndTime(stopTime);
        if (lineTimeInterval == 0) {
            lineTimeInterval = ReaderUtils.getLineTimeInterval(startTime, stopTime, rasterHeight);
        }
        absMeta.setAttribute(AbstractMetadata.line_time_interval, lineTimeInterval);
    }

    private void addSRGRCoefficients() throws IOException {

        // For detail of ground range to slant range conversion, please see P80 in COSMO-SkyMed SAR Products Handbook.
        final AbstractMetadata absMeta = AbstractMetadata.getAbstractedMetadata(product);
        final AbstractMetadataSAR sarMeta = AbstractMetadataSAR.getSARAbstractedMetadata(product);
        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement globalElem = root.getElement(NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);

        final MetadataAttribute attribute = globalElem.getAttribute("Ground_Projection_Polynomial_Reference_Range");
        if (attribute == null) {
            return;
        }

        final double referenceRange = attribute.getData().getElemDouble();

        final MetadataElement bandElem = getBandElement(product.getBandAt(0));
        final double rangeSpacing = bandElem.getAttributeDouble("Column_Spacing", AbstractMetadata.NO_METADATA);

        final MetadataElement srgrCoefficientsElem = sarMeta.getElement(AbstractMetadataSAR.srgr_coefficients);
        final MetadataElement srgrListElem = new MetadataElement(AbstractMetadataSAR.srgr_coef_list);
        srgrCoefficientsElem.addElement(srgrListElem);

        final ProductData.UTC utcTime = absMeta.getAttributeUTC(AbstractMetadata.first_line_time);
        srgrListElem.setAttributeUTC(AbstractMetadataSAR.srgr_coef_time, utcTime);
        sarMeta.addAbstractedAttribute(srgrListElem, AbstractMetadataSAR.ground_range_origin,
                ProductData.TYPE_FLOAT64, "m", "Ground Range Origin");
        sarMeta.setAttribute(srgrListElem, AbstractMetadataSAR.ground_range_origin, 0.0);

        final int numCoeffs = 6;
        for (int i = 0; i < numCoeffs; i++) {
            double srgrCoeff = globalElem.getAttribute("Ground_to_Slant_Polynomial").getData().getElemDoubleAt(i);
            if (i == 0) {
                srgrCoeff += referenceRange;
            } else {
                srgrCoeff /= Math.pow(rangeSpacing, i);
            }

            final MetadataElement coefElem = new MetadataElement(AbstractMetadataSAR.coefficient + '.' + (i + 1));
            srgrListElem.addElement(coefElem);

            sarMeta.addAbstractedAttribute(coefElem, AbstractMetadataSAR.srgr_coef,
                    ProductData.TYPE_FLOAT64, "", "SRGR Coefficient");

            sarMeta.setAttribute(coefElem, AbstractMetadataSAR.srgr_coef, srgrCoeff);
        }
    }

    private String getSampleType(final MetadataElement globalElem) {
        if (globalElem.getAttributeInt("Samples_per_Pixel", 0) > 1) {
            isComplex = true;
            return "COMPLEX";
        }
        isComplex = false;
        return "DETECTED";
    }

    private void addBandsToProduct(final Variable[] variables) {
        int cnt = 1;
        for (Variable variable : variables) {
            final int height = variable.getDimension(0).getLength();
            final int width = variable.getDimension(1).getLength();
            String cntStr = "";
            if (variables.length > 1) {
                final String polStr = getPolarization(product, cnt);
                if (polStr != null) {
                    cntStr = "_" + polStr;
                } else {
                    cntStr = "_" + cnt;
                }
                ++cnt;
            }

            if (isComplex) {     // add i and q
                final Band bandI = NetCDFUtils.createBand(variable, width, height);
                createUniqueBandName(product, bandI, "i" + cntStr);
                bandI.setUnit(Unit.REAL);
                product.addBand(bandI);
                bandMap.put(bandI, variable);

                final Band bandQ = NetCDFUtils.createBand(variable, width, height);
                createUniqueBandName(product, bandQ, "q" + cntStr);
                bandQ.setUnit(Unit.IMAGINARY);
                product.addBand(bandQ);
                bandMap.put(bandQ, variable);

                ReaderUtils.createVirtualIntensityBand(product, bandI, bandQ, cntStr);
            } else {
                final Band band = NetCDFUtils.createBand(variable, width, height);
                createUniqueBandName(product, band, "Amplitude" + cntStr);
                band.setUnit(Unit.AMPLITUDE);
                product.addBand(band);
                bandMap.put(band, variable);
            }
        }
    }

    private static String getPolarization(final Product product, final int cnt) {

        final MetadataElement globalElem = AbstractMetadata.getOriginalProductMetadata(product).getElement(NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);
        if (globalElem != null) {
            final MetadataElement s01Elem = globalElem.getElement("S0" + cnt);
            if (s01Elem != null) {
                final String polStr = s01Elem.getAttributeString("Polarisation", "");
                if (!polStr.isEmpty())
                    return polStr;
            }
        }
        return null;
    }

    private static void createUniqueBandName(final Product product, final Band band, final String origName) {
        int cnt = 1;
        band.setName(origName);
        while (product.getBand(band.getName()) != null) {
            band.setName(origName + cnt);
            ++cnt;
        }
    }

    private void addTiePointGridsToProduct(final Variable[] variables) throws IOException {
        for (Variable variable : variables) {
            final int rank = variable.getRank();
            final int gridWidth = variable.getDimension(rank - 1).getLength();
            int gridHeight = variable.getDimension(rank - 2).getLength();
            if (rank >= 3 && gridHeight <= 1)
                gridHeight = variable.getDimension(rank - 3).getLength();
            final TiePointGrid tpg = NetCDFUtils.createTiePointGrid(variable, gridWidth, gridHeight,
                    product.getSceneRasterWidth(), product.getSceneRasterHeight());

            product.addTiePointGrid(tpg);
        }

        final MetadataElement bandElem = getBandElement(product.getBandAt(0));
        addIncidenceAnglesSlantRangeTime(product, bandElem);
        addGeocodingFromMetadata(product, bandElem);
    }

    private static void addIncidenceAnglesSlantRangeTime(final Product product, final MetadataElement bandElem) {
        if (bandElem == null) return;

        final int gridWidth = 11;
        final int gridHeight = 11;
        final float subSamplingX = (float) product.getSceneRasterWidth() / (float) (gridWidth - 1);
        final float subSamplingY = (float) product.getSceneRasterHeight() / (float) (gridHeight - 1);

        final float nearRangeAngle = (float) bandElem.getAttributeDouble("Near_Incidence_Angle", 0);
        final float farRangeAngle = (float) bandElem.getAttributeDouble("Far_Incidence_Angle", 0);

        final float firstRangeTime = (float) bandElem.getAttributeDouble("Zero_Doppler_Range_First_Time", 0) * 1000000000.0f;
        final float lastRangeTime = (float) bandElem.getAttributeDouble("Zero_Doppler_Range_Last_Time", 0) * 1000000000.0f;

        final float[] incidenceCorners = new float[]{nearRangeAngle, farRangeAngle, nearRangeAngle, farRangeAngle};
        final float[] slantRange = new float[]{firstRangeTime, lastRangeTime, firstRangeTime, lastRangeTime};

        final float[] fineAngles = new float[gridWidth * gridHeight];
        final float[] fineTimes = new float[gridWidth * gridHeight];

        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, incidenceCorners, fineAngles);
        ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, slantRange, fineTimes);

        final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, fineAngles);
        incidentAngleGrid.setUnit(Unit.DEGREES);
        product.addTiePointGrid(incidentAngleGrid);

        final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME, gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, fineTimes);
        slantRangeGrid.setUnit(Unit.NANOSECONDS);
        product.addTiePointGrid(slantRangeGrid);
    }

    private MetadataElement getBandElement(final Band band) {
        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(product);
        final Variable variable = bandMap.get(band);
        final String varName = variable.getShortName();
        MetadataElement bandElem = null;
        for (MetadataElement elem : root.getElements()) {
            if (elem.getName().equalsIgnoreCase(varName)) {
                bandElem = elem;
                break;
            }
        }
        return bandElem;
    }

    private static void addGeocodingFromMetadata(final Product product, final MetadataElement bandElem) {
        if (bandElem == null) return;

        try {
            final AbstractMetadata absMeta = AbstractMetadata.getAbstractedMetadata(product);
            final AbstractMetadataSAR sarMeta = AbstractMetadataSAR.getSARAbstractedMetadata(product);

            String str = bandElem.getAttributeString("Top_Left_Geodetic_Coordinates");
            final double latUL = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final double lonUL = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("Top_Right_Geodetic_Coordinates");
            final double latUR = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final double lonUR = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("Bottom_Left_Geodetic_Coordinates");
            final double latLL = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final double lonLL = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("Bottom_Right_Geodetic_Coordinates");
            final double latLR = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final double lonLR = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));

            absMeta.setAttribute(AbstractMetadata.first_near_lat, latUL);
            absMeta.setAttribute(AbstractMetadata.first_near_long, lonUL);
            absMeta.setAttribute(AbstractMetadata.first_far_lat, latUR);
            absMeta.setAttribute(AbstractMetadata.first_far_long, lonUR);
            absMeta.setAttribute(AbstractMetadata.last_near_lat, latLL);
            absMeta.setAttribute(AbstractMetadata.last_near_long, lonLL);
            absMeta.setAttribute(AbstractMetadata.last_far_lat, latLR);
            absMeta.setAttribute(AbstractMetadata.last_far_long, lonLR);

            sarMeta.setAttribute(AbstractMetadataSAR.range_spacing,
                    bandElem.getAttributeDouble("Column_Spacing", AbstractMetadata.NO_METADATA));
            sarMeta.setAttribute(AbstractMetadataSAR.azimuth_spacing,
                    bandElem.getAttributeDouble("Line_Spacing", AbstractMetadata.NO_METADATA));

            final float[] latCorners = new float[]{(float)latUL, (float)latUR, (float)latLL, (float)latLR};
            final float[] lonCorners = new float[]{(float)lonUL, (float)lonUR, (float)lonLL, (float)lonLR};

            ReaderUtils.addGeoCoding(product, latCorners, lonCorners);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            // continue
        }
    }

    private void addGeoCodingToProduct(final NcRasterDim rasterDim) throws IOException {
        if (product.getGeoCoding() == null) {
            NetCDFReader.setTiePointGeoCoding(product);
        }
        if (product.getGeoCoding() == null) {
            NetCDFReader.setPixelGeoCoding(product);
        }
        if (product.getGeoCoding() == null) {
            yFlipped = NetCDFReader.setMapGeoCoding(rasterDim, product, netcdfFile, yFlipped);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        Guardian.assertTrue("sourceStepX == 1 && sourceStepY == 1", sourceStepX == 1 && sourceStepY == 1);
        Guardian.assertTrue("sourceWidth == destWidth", sourceWidth == destWidth);
        Guardian.assertTrue("sourceHeight == destHeight", sourceHeight == destHeight);

        final int sceneHeight = product.getSceneRasterHeight();
        final int y0 = yFlipped ? (sceneHeight - 1) - sourceOffsetY : sourceOffsetY;

        final Variable variable = bandMap.get(destBand);
        final int rank = variable.getRank();
        final int[] origin = new int[rank];
        final int[] shape = new int[rank];
        for (int i = 0; i < rank; i++) {
            shape[i] = 1;
            origin[i] = 0;
        }
        shape[0] = 1;
        shape[1] = destWidth;
        origin[1] = sourceOffsetX;
        if (isComplex && destBand.getUnit().equals(Unit.IMAGINARY)) {
            origin[2] = 1;
        }

        pm.beginTask("Reading data from band " + destBand.getName(), destHeight);
        try {
            for (int y = 0; y < destHeight; y++) {
                origin[0] = yFlipped ? y0 - y : y0 + y;
                final Array array;
                synchronized (netcdfFile) {
                    array = variable.read(origin, shape);
                }
                System.arraycopy(array.getStorage(), 0, destBuffer.getElems(), y * destWidth, destWidth);
                pm.worked(1);
                if (pm.isCanceled()) {
                    throw new IOException("Process terminated by user."); /*I18N*/
                }
            }
        } catch (InvalidRangeException e) {
            final IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        } finally {
            pm.done();
        }
    }

}
