/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Guardian;
import org.esa.nest.dataio.netcdf.*;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.AbstractMetadataIO;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.util.Constants;
import org.esa.nest.util.XMLSupport;
import org.jdom.Element;
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
 *
 */
public class CosmoSkymedReader extends AbstractProductReader {

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
        final Variable[] rasterVariables = NetCDFUtils.getRasterVariables(variableListMap, rasterDim);
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

    private static void removeQuickLooks(Map<NcRasterDim, List<Variable>> variableListMap) {
        final String[] excludeList = { "qlk" };
        final NcRasterDim[] keys = variableListMap.keySet().toArray(new NcRasterDim[variableListMap.keySet().size()]);
        final List<NcRasterDim> removeList = new ArrayList<NcRasterDim>();

        for (final NcRasterDim rasterDim : keys) {
            final List<Variable> varList = variableListMap.get(rasterDim);
            boolean found = false;
            for(Variable v : varList) {
                if(found) break;

                final String vName = v.getName().toLowerCase();
                for(String str : excludeList) {
                    if(vName.contains(str)) {
                        removeList.add(rasterDim);
                        found = true;
                        break;
                    }
                }
            }
        }
        for(NcRasterDim key : removeList) {
            variableListMap.remove(key);
        }
    }

    private void addMetadataToProduct() throws IOException {

        NetCDFUtils.addAttributes(product.getMetadataRoot(), NetcdfConstants.GLOBAL_ATTRIBUTES_NAME,
                                  netcdfFile.getGlobalAttributes());

        addDeliveryNote(product);

        for (final Variable variable : variableMap.getAll()) {
            NetCDFUtils.addAttributes(product.getMetadataRoot(), variable.getName(),
                                  variable.getAttributes());
        }

        //final Group rootGroup = netcdfFile.getRootGroup();
        //NetCDFUtils.addGroups(product.getMetadataRoot(), rootGroup);

        addAbstractedMetadataHeader(product, product.getMetadataRoot());
    }

    private static void addDeliveryNote(final Product product) {
        try {
            final File folder = product.getFileLocation().getParentFile();
            File dnFile = null;
            for(File f : folder.listFiles()) {
                final String name = f.getName().toLowerCase();
                if(name.startsWith("dfdn") && name.endsWith("xml")) {
                    dnFile = f;
                    break;
                }
            }
            if(dnFile != null) {
                final org.jdom.Document xmlDoc = XMLSupport.LoadXML(dnFile.getAbsolutePath());
                final Element rootElement = xmlDoc.getRootElement();

                AbstractMetadataIO.AddXMLMetadata(rootElement, product.getMetadataRoot());
            }
        } catch(IOException e) {
            //System.out.println("Unable to read Delivery Note for "+product.getName());
        }
    }

    private void addAbstractedMetadataHeader(Product product, MetadataElement root) throws IOException {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

        final String defStr = AbstractMetadata.NO_METADATA_STRING;
        final int defInt = AbstractMetadata.NO_METADATA;

        final MetadataElement globalElem = root.getElement(NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, globalElem.getAttributeString("Product Filename", defStr));
        final String productType = globalElem.getAttributeString("Product Type", defStr);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);
        final String mode = globalElem.getAttributeString("Acquisition Mode", defStr);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, mode);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, mode);

        if(mode.contains("HUGE") && productType.contains("SCS")) {
            throw new IOException("Complex "+mode+" products are not supported for Cosmo-Skymed");    
        }

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, globalElem.getAttributeString("Satellite Id", "CSK"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME,
                ReaderUtils.getTime(globalElem, "Product Generation UTC", AbstractMetadata.dateFormat));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier,
                globalElem.getAttributeString("Processing Centre", defStr));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing,
                globalElem.getAttributeString("Look Side", defStr).toLowerCase());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, globalElem.getAttributeInt("Orbit Number", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, globalElem.getAttributeString("Orbit Direction", defStr));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, getSampleType(globalElem));
        /*
        final ProductData.UTC startTime = ReaderUtils.getTime(globalElem, "Scene Sensing Start UTC", timeFormat);
        final ProductData.UTC stopTime = ReaderUtils.getTime(globalElem, "Scene Sensing Stop UTC", timeFormat);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);
        product.setStartTime(startTime);
        product.setEndTime(stopTime);
        */
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines,
                product.getSceneRasterHeight());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line,
                product.getSceneRasterWidth());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.TOT_SIZE, ReaderUtils.getTotalSize(product));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency,
                globalElem.getAttributeDouble("Radar Frequency", defInt) / Constants.oneMillion);
//        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
//                ReaderUtils.getLineTimeInterval(startTime, stopTime, product.getSceneRasterHeight()));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.algorithm,
                globalElem.getAttributeString("Focusing Algorithm ID", defStr));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.geo_ref_system,
                globalElem.getAttributeString("Ellipsoid Designator", defStr));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks,
                globalElem.getAttributeDouble("Range Processing Number of Looks", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks,
                globalElem.getAttributeDouble("Azimuth Processing Number of Looks", defInt));

        if(productType.contains("GEC")) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.map_projection,
                globalElem.getAttributeString("Projection ID", defStr));
        }

        // Global calibration attributes
        /*
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.abs_calibration_flag,
        		globalElem.getAttributeInt("Calibration Constant Compensation Flag"));
        */
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, 0);

        final String rngSpreadComp = globalElem.getAttributeString(
        		"Range Spreading Loss Compensation Geometry", defStr);
        if (rngSpreadComp.equals("NONE"))
        	AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spread_comp_flag, 0);
        else
        	AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spread_comp_flag, 1);
        
        final String incAngComp = globalElem.getAttributeString(
        		"Incidence Angle Compensation Geometry", defStr);
        if (incAngComp.equals("NONE"))
        	AbstractMetadata.setAttribute(absRoot, AbstractMetadata.inc_angle_comp_flag, 0);
        else
        	AbstractMetadata.setAttribute(absRoot, AbstractMetadata.inc_angle_comp_flag, 1);

        final String antElevComp = globalElem.getAttributeString(
        		"Range Antenna Pattern Compensation Geometry", defStr);
        if (antElevComp.equals("NONE"))
        	AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ant_elev_corr_flag, 0);
        else
        	AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ant_elev_corr_flag, 1);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_inc_angle,
                globalElem.getAttributeDouble("Reference Incidence Angle", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_slant_range,
                globalElem.getAttributeDouble("Reference Slant Range", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_slant_range_exp,
                globalElem.getAttributeDouble("Reference Slant Range Exponent", defInt));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.rescaling_factor,
                globalElem.getAttributeDouble("Rescaling Factor", defInt));
        
        final MetadataElement s01Elem = globalElem.getElement("S01");
        if(s01Elem != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency,
                s01Elem.getAttributeDouble("PRF", defInt));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate,
                s01Elem.getAttributeDouble("Sampling Rate", defInt) / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar,
                s01Elem.getAttributeString("Polarisation", defStr));

            // add Range and Azimuth bandwidth
            final double rangeBW = s01Elem.getAttributeDouble("Range Focusing Bandwidth"); // Hz
            final double azimuthBW = s01Elem.getAttributeDouble("Azimuth Focusing Bandwidth"); // Hz

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, azimuthBW);

            // Calibration constant read from Global_Metadata during calibration initialization
        }
        final MetadataElement s02Elem = globalElem.getElement("S02");
        if(s02Elem != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds2_tx_rx_polar,
                s02Elem.getAttributeString("Polarisation", defStr));
            
        }

        if(isComplex) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, 0);
        } else {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, 1);
        }

        addOrbitStateVectors(absRoot, globalElem);
    }

    private void addSlantRangeToFirstPixel() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final MetadataElement bandElem = getBandElement(product.getBandAt(0));
        if(bandElem != null) {
            final double slantRangeTime = bandElem.getAttributeDouble("Zero Doppler Range First Time", 0); //s
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel,
                slantRangeTime*Constants.halfLightSpeed);
        }
    }

    private static void addOrbitStateVectors(final MetadataElement absRoot, final MetadataElement globalElem) {

        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);
        final ProductData.UTC referenceUTC = ReaderUtils.getTime(globalElem, "Reference UTC", AbstractMetadata.dateFormat);
        final int numPoints = globalElem.getAttributeInt("Number of State Vectors");

        for (int i = 0; i < numPoints; i++) {
            final double stateVectorTime = globalElem.getAttribute("State Vectors Times").getData().getElemDoubleAt(i);
            final ProductData.UTC orbitTime =
                    new ProductData.UTC(referenceUTC.getMJD() + stateVectorTime/86400.0);
             
            final double satellitePositionX =
                    globalElem.getAttribute("ECEF Satellite Position").getData().getElemDoubleAt(3*i);
            final double satellitePositionY =
                    globalElem.getAttribute("ECEF Satellite Position").getData().getElemDoubleAt(3*i+1);
            final double satellitePositionZ =
                    globalElem.getAttribute("ECEF Satellite Position").getData().getElemDoubleAt(3*i+2);
            final double satelliteVelocityX =
                    globalElem.getAttribute("ECEF Satellite Velocity").getData().getElemDoubleAt(3*i);
            final double satelliteVelocityY =
                    globalElem.getAttribute("ECEF Satellite Velocity").getData().getElemDoubleAt(3*i+1);
            final double satelliteVelocityZ =
                    globalElem.getAttribute("ECEF Satellite Velocity").getData().getElemDoubleAt(3*i+2);

            final MetadataElement orbitVectorElem = new MetadataElement(AbstractMetadata.orbit_vector + (i+1));

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

    private void addDopplerCentroidCoefficients() {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement globalElem = root.getElement(NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);

        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);
        final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list + ".1");
        dopplerCentroidCoefficientsElem.addElement(dopplerListElem);

        final ProductData.UTC utcTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time, new ProductData.UTC(0));
        dopplerListElem.setAttributeUTC(AbstractMetadata.dop_coef_time, utcTime);

        AbstractMetadata.addAbstractedAttribute(dopplerListElem, AbstractMetadata.slant_range_time,
                ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
        AbstractMetadata.setAttribute(dopplerListElem, AbstractMetadata.slant_range_time, 0.0);

        for (int i = 0; i < 6; i++) {
            final double coefValue =
                    globalElem.getAttribute("Centroid vs Range Time Polynomial").getData().getElemDoubleAt(i);
            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + (i+1));
            dopplerListElem.addElement(coefElem);
            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.dop_coef,
                    ProductData.TYPE_FLOAT64, "", "Doppler Centroid Coefficient");
            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.dop_coef, coefValue);
        }
    }

    private void addFirstLastLineTimes(final int rasterHeight) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement globalElem = root.getElement(NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);
        final MetadataElement bandElem = getBandElement(product.getBandAt(0));
        
        final double referenceUTC = ReaderUtils.getTime(globalElem, "Reference UTC", AbstractMetadata.dateFormat).getMJD(); // in days
        /*
        final double firstLineTime = globalElem.getElement("S01").getElement("B001").getAttributeDouble("Azimuth First Time") / (24*3600); // in days
        final double lastLineTime = globalElem.getElement("S01").getElement("B001").getAttributeDouble("Azimuth Last Time") / (24*3600); // in days
        double lineTimeInterval = (24*3600)*(lastLineTime - firstLineTime) / (product.getSceneRasterHeight() - 1);
        */
        final double firstLineTime = bandElem.getAttributeDouble("Zero Doppler Azimuth First Time") / (24*3600); // in days
        final double lastLineTime = bandElem.getAttributeDouble("Zero Doppler Azimuth Last Time") / (24*3600); // in days
        double lineTimeInterval = bandElem.getAttributeDouble("Line Time Interval", 0); // in s
        final ProductData.UTC startTime = new ProductData.UTC(referenceUTC + firstLineTime);
        final ProductData.UTC stopTime = new ProductData.UTC(referenceUTC + lastLineTime);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);
        product.setStartTime(startTime);
        product.setEndTime(stopTime);
        if(lineTimeInterval == 0) {
            lineTimeInterval = ReaderUtils.getLineTimeInterval(startTime, stopTime, rasterHeight);
        }
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval, lineTimeInterval);
    }

    private void addSRGRCoefficients() {

        // For detail of ground range to slant range conversion, please see P80 in COSMO-SkyMed SAR Products Handbook.
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement globalElem = root.getElement(NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);

        final MetadataAttribute attribute = globalElem.getAttribute("Ground Projection Polynomial Reference Range");
        if (attribute == null) {
            return;
        }

        final double referenceRange = attribute.getData().getElemDouble();

        final MetadataElement bandElem = getBandElement(product.getBandAt(0));
        final double rangeSpacing = bandElem.getAttributeDouble("Column Spacing", AbstractMetadata.NO_METADATA);

        final MetadataElement srgrCoefficientsElem = absRoot.getElement(AbstractMetadata.srgr_coefficients);
        final MetadataElement srgrListElem = new MetadataElement(AbstractMetadata.srgr_coef_list);
        srgrCoefficientsElem.addElement(srgrListElem);

        final ProductData.UTC utcTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time, new ProductData.UTC(0));
        srgrListElem.setAttributeUTC(AbstractMetadata.srgr_coef_time, utcTime);
        AbstractMetadata.addAbstractedAttribute(srgrListElem, AbstractMetadata.ground_range_origin,
                ProductData.TYPE_FLOAT64, "m", "Ground Range Origin");
        AbstractMetadata.setAttribute(srgrListElem, AbstractMetadata.ground_range_origin, 0.0);
        
        final int numCoeffs = 6;
        for (int i = 0; i < numCoeffs; i++) {
            double srgrCoeff = globalElem.getAttribute("Ground to Slant Polynomial").getData().getElemDoubleAt(i);
            if (i == 0) {
                srgrCoeff += referenceRange;
            } else {
                srgrCoeff /= Math.pow(rangeSpacing, i);
            }

            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + (i+1));
            srgrListElem.addElement(coefElem);

            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.srgr_coef,
                    ProductData.TYPE_FLOAT64, "", "SRGR Coefficient");

            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.srgr_coef, srgrCoeff);
        }
    }

    private String getSampleType(final MetadataElement globalElem) {
        if(globalElem.getAttributeInt("Samples per Pixel", 0) > 1) {
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
            if(variables.length > 1) {
                final String polStr = getPolarization(product, cnt);
                if(polStr != null) {
                    cntStr = "_"+polStr;
                } else {
                    cntStr = "_"+cnt;
                }
                ++cnt;
            }

            if(isComplex) {     // add i and q
                final Band bandI = NetCDFUtils.createBand(variable, width, height);
                createUniqueBandName(product, bandI, "i"+cntStr);
                bandI.setUnit(Unit.REAL);
                product.addBand(bandI);
                bandMap.put(bandI, variable);

                final Band bandQ = NetCDFUtils.createBand(variable, width, height);
                createUniqueBandName(product, bandQ, "q"+cntStr);
                bandQ.setUnit(Unit.IMAGINARY);
                product.addBand(bandQ);
                bandMap.put(bandQ, variable);

                ReaderUtils.createVirtualIntensityBand(product, bandI, bandQ, cntStr);
                ReaderUtils.createVirtualPhaseBand(product, bandI, bandQ, cntStr);
            } else {
                final Band band = NetCDFUtils.createBand(variable, width, height);
                createUniqueBandName(product, band, "Amplitude"+cntStr);
                band.setUnit(Unit.AMPLITUDE);
                product.addBand(band);
                bandMap.put(band, variable);
                ReaderUtils.createVirtualIntensityBand(product, band, cntStr);
            }
        }
    }

    private static String getPolarization(final Product product, final int cnt) {

        final MetadataElement globalElem = product.getMetadataRoot().getElement(NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);
        if(globalElem != null) {
            final MetadataElement s01Elem = globalElem.getElement("S0"+cnt);
            if(s01Elem != null) {
                final String polStr = s01Elem.getAttributeString("Polarisation", "");
                if(!polStr.isEmpty())
                    return polStr;
            }
        }
        return null;
    }

    private static void createUniqueBandName(final Product product, final Band band, final String origName) {
        int cnt = 1;
        band.setName(origName);
        while(product.getBand(band.getName()) != null) {
            band.setName(origName + cnt);
            ++cnt;
        }
    }

    private void addTiePointGridsToProduct(final Variable[] variables) throws IOException {
        for (Variable variable : variables) {
            final int rank = variable.getRank();
            final int gridWidth = variable.getDimension(rank - 1).getLength();
            int gridHeight = variable.getDimension(rank - 2).getLength();
            if(rank >= 3 && gridHeight <= 1)
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
        if(bandElem == null) return;

        final int gridWidth = 11;
        final int gridHeight = 11;
        final float subSamplingX = (float)product.getSceneRasterWidth() / (float)(gridWidth - 1);
        final float subSamplingY = (float)product.getSceneRasterHeight() / (float)(gridHeight - 1);

        final float nearRangeAngle = (float)bandElem.getAttributeDouble("Near Incidence Angle", 0);
        final float farRangeAngle = (float)bandElem.getAttributeDouble("Far Incidence Angle", 0);

        final float firstRangeTime = (float)bandElem.getAttributeDouble("Zero Doppler Range First Time", 0) * 1000000000.0f;
        final float lastRangeTime = (float)bandElem.getAttributeDouble("Zero Doppler Range Last Time", 0) * 1000000000.0f;

        final float[] incidenceCorners = new float[] { nearRangeAngle, farRangeAngle, nearRangeAngle, farRangeAngle };
        final float[] slantRange = new float[] { firstRangeTime, lastRangeTime, firstRangeTime, lastRangeTime };

        final float[] fineAngles = new float[gridWidth*gridHeight];
        final float[] fineTimes = new float[gridWidth*gridHeight];

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
        final MetadataElement root = product.getMetadataRoot();
        final Variable variable = bandMap.get(band);
        final String varName = variable.getName();
        MetadataElement bandElem = null;
        for(MetadataElement elem : root.getElements()) {
            if(elem.getName().equalsIgnoreCase(varName)) {
                bandElem = elem;
                break;
            }
        }
        return bandElem;
    }

    private static void addGeocodingFromMetadata(final Product product, final MetadataElement bandElem) {
        if(bandElem == null) return;

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        try {
            String str = bandElem.getAttributeString("Top Left Geodetic Coordinates");
            final float latUL = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonUL = Float.parseFloat(str.substring(str.indexOf(',')+1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("Top Right Geodetic Coordinates");
            final float latUR = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonUR = Float.parseFloat(str.substring(str.indexOf(',')+1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("Bottom Left Geodetic Coordinates");
            final float latLL = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonLL = Float.parseFloat(str.substring(str.indexOf(',')+1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("Bottom Right Geodetic Coordinates");
            final float latLR = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonLR = Float.parseFloat(str.substring(str.indexOf(',')+1, str.lastIndexOf(',')));

            absRoot.setAttributeDouble(AbstractMetadata.first_near_lat, latUL);
            absRoot.setAttributeDouble(AbstractMetadata.first_near_long, lonUL);
            absRoot.setAttributeDouble(AbstractMetadata.first_far_lat, latUR);
            absRoot.setAttributeDouble(AbstractMetadata.first_far_long, lonUR);
            absRoot.setAttributeDouble(AbstractMetadata.last_near_lat, latLL);
            absRoot.setAttributeDouble(AbstractMetadata.last_near_long, lonLL);
            absRoot.setAttributeDouble(AbstractMetadata.last_far_lat, latLR);
            absRoot.setAttributeDouble(AbstractMetadata.last_far_long, lonLR);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                    bandElem.getAttributeDouble("Column Spacing", AbstractMetadata.NO_METADATA));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                    bandElem.getAttributeDouble("Line Spacing", AbstractMetadata.NO_METADATA));

            final float[] latCorners = new float[]{latUL, latUR, latLL, latLR};
            final float[] lonCorners = new float[]{lonUL, lonUR, lonLL, lonLR};

            ReaderUtils.addGeoCoding(product, latCorners, lonCorners);
        } catch(Exception e) {
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
        if(isComplex && destBand.getUnit().equals(Unit.IMAGINARY)) {
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
