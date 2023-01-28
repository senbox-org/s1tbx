/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.kompsat5;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.io.binary.ArrayCopy;
import org.esa.s1tbx.io.netcdf.NcAttributeMap;
import org.esa.s1tbx.io.netcdf.NcRasterDim;
import org.esa.s1tbx.io.netcdf.NcVariableMap;
import org.esa.s1tbx.io.netcdf.NetCDFReader;
import org.esa.s1tbx.io.netcdf.NetCDFUtils;
import org.esa.s1tbx.io.netcdf.NetcdfConstants;
import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by luis on 12/08/2016.
 */
public class K5HDF implements K5Format {

    private final ProductReaderPlugIn readerPlugIn;
    private final Kompsat5Reader reader;
    private Product product = null;
    private NetcdfFile netcdfFile = null;
    private NcVariableMap variableMap = null;
    private boolean yFlipped = false;
    private boolean useFloatBands = false;
    private boolean isComplex = false;
    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");
    private final Map<Band, Variable> bandMap = new HashMap<>(10);
    private final Map<Band, TiePointGeoCoding> bandGeocodingMap = new HashMap<>(5);


    public K5HDF(final ProductReaderPlugIn readerPlugIn, final Kompsat5Reader reader) {
        this.readerPlugIn = readerPlugIn;
        this.reader = reader;

    }

    public Product open(final File inputFile) throws IOException {
        final NetcdfFile netcdfFile = NetcdfFile.open(inputFile.getPath());
        if (netcdfFile == null) {
            close();
            throw new IllegalFileFormatException(inputFile.getName() +
                    " Could not be interpreted by the reader.");
        }

        final Map<NcRasterDim, List<Variable>> variableListMap = NetCDFUtils.getVariableListMap(netcdfFile.getRootGroup());
        if (variableListMap.isEmpty()) {
            close();
            throw new IllegalFileFormatException("No netCDF variables found which could\n" +
                    "be interpreted as remote sensing bands.");  /*I18N*/
        }
        removeQuickLooks(variableListMap);

        final Variable[] rasterVariables = getRasterVariables(variableListMap);
        this.netcdfFile = netcdfFile;
        variableMap = new NcVariableMap(rasterVariables);
        yFlipped = false;

        final NcAttributeMap globalAttributes = NcAttributeMap.create(this.netcdfFile);
        final String productType = NetCDFUtils.getProductType(globalAttributes, readerPlugIn.getFormatNames()[0]);

        // set product name
        final String productName;
        if (inputFile.getName().toUpperCase().endsWith(".H5")) {
            productName = inputFile.getName().toUpperCase().replace(".H5", "");
        } else if (inputFile.getName().toUpperCase().endsWith("_AUX.XML")) {
            productName = inputFile.getName().toUpperCase().replace("_AUX.XML", "");
        } else {
            productName = inputFile.getName();
        }

        int productWidth = 0;
        int productHeight = 0;

        for (Variable var : rasterVariables) {
            productWidth += var.getDimension(1).getLength();
            if (var.getDimension(0).getLength() > productHeight) {
                productHeight = var.getDimension(0).getLength();
            }
        }

        product = new Product(productName, productType, productWidth, productHeight, reader);

        product.setFileLocation(inputFile);
        product.setDescription(NetCDFUtils.getProductDescription(globalAttributes));

        addMetadataToProduct();
        addBandsToProduct(rasterVariables);

        addGeocodingToProduct(product);

        return product;
    }

    public void close() throws IOException {
        if (product != null) {
            product.dispose();
            product = null;
            variableMap.clear();
            variableMap = null;
            netcdfFile.close();
            netcdfFile = null;
        }
    }

    private static Variable[] getRasterVariables(final Map<NcRasterDim, List<Variable>> variableLists) {
        final List<Variable> list = new ArrayList<>(5);
        // WIDE SWATH's L1A reads S01 swath only
        variableLists.forEach((NcRasterDim rasterDim, List<Variable> varList) -> {
            for (Variable var : varList) {
                if (!var.getShortName().equals("GIM")) {
                    String varParent = var.getParentGroup().getShortName();
                    if (varParent.equals("S01") || var.getShortName().equals("MBI")) {
                        list.add(var);
                        break;
                    }
                }
            }
        });
        return list.toArray(new Variable[0]);
    }

    private static void removeQuickLooks(final Map<NcRasterDim, List<Variable>> variableListMap) {
        final String[] excludeList = {"qlk"};
        final NcRasterDim[] keys = variableListMap.keySet().toArray(new NcRasterDim[0]);
        final List<NcRasterDim> removeList = new ArrayList<>();

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

    private void addGeoCodingToProduct(final Product product, final NcRasterDim rasterDim) throws IOException {
        if (product.getSceneGeoCoding() == null) {
            NetCDFReader.setTiePointGeoCoding(product);
        }
        if (product.getSceneGeoCoding() == null) {
            NetCDFReader.setPixelGeoCoding(product);
        }
        if (product.getSceneGeoCoding() == null) {
            yFlipped = NetCDFReader.setMapGeoCoding(rasterDim, product, netcdfFile, yFlipped);
        }
    }

    private void addMetadataToProduct() {
        try {
            final MetadataElement origMetadataRoot = AbstractMetadata.addOriginalProductMetadata(product.getMetadataRoot());
            NetCDFUtils.addAttributes(origMetadataRoot, NetcdfConstants.GLOBAL_ATTRIBUTES_NAME,
                    netcdfFile.getGlobalAttributes());

            addAuxXML(product);

            // to be updated : read scansar
            final String[] swathId = {"S01", "S02", "S03", "S04"};
            for (String swath : swathId) {
                for (final Variable variable : variableMap.getAll()) {

                    if (variable.getParentGroup().getShortName().equals(swath)) {
                        NetCDFUtils.addAttributes(origMetadataRoot, variable.getShortName(), variable.getAttributes());
                    }
                }
            }

            addAbstractedMetadataHeader(product, product.getMetadataRoot());
            addSlantRangeToFirstPixel();
            addFirstLastLineTimes(product);
            addSRGRCoefficients(product);
        } catch (Exception e) {
            SystemUtils.LOG.severe("Error reading metadata for " + product.getName() + ": " + e.getMessage());
        }
    }

    private void addAbstractedMetadataHeader(Product product, MetadataElement root) {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

        final MetadataElement origRoot = AbstractMetadata.addOriginalProductMetadata(product.getMetadataRoot());
        final MetadataElement globalElem = origRoot.getElement(NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);

        final MetadataElement aux = origRoot.getElement("Auxiliary");
        final MetadataElement auxRoot = aux.getElement("Root");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, globalElem.getAttributeString("Product_Filename"));
        final String productType = globalElem.getAttributeString("Product_Type");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);
        final String mode = globalElem.getAttributeString("Acquisition_Mode");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR, mode);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, mode);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION, "Kompsat5");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME,
                ReaderUtils.getTime(globalElem, "Product_Generation_UTC", standardDateFormat));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier,
                globalElem.getAttributeString("Processing_Centre"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing,
                globalElem.getAttributeString("Look_Side").toLowerCase());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ABS_ORBIT, globalElem.getAttributeInt("Orbit_Number"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS, globalElem.getAttributeString("Orbit_Direction"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, getSampleType(productType));

        useFloatBands = globalElem.getAttributeString("Sample_Format").equals("FLOAT");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines,
                product.getSceneRasterHeight());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line,
                product.getSceneRasterWidth());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.TOT_SIZE, ReaderUtils.getTotalSize(product));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency,
                globalElem.getAttributeDouble("Radar_Frequency") / Constants.oneMillion);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.algorithm,
                globalElem.getAttributeString("Focusing_Algorithm_ID"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.geo_ref_system,
                globalElem.getAttributeString("Ellipsoid_Designator"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks,
                auxRoot.getAttributeDouble("RangeProcessingNumberofLooks", 1));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks,
                auxRoot.getAttributeDouble("AzimuthProcessingNumberofLooks", 1));

        if (productType.contains("GEC")) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.map_projection,
                    globalElem.getAttributeString("Projection_ID"));
        }

        final String rngSpreadComp = globalElem.getAttributeString(
                "Range_Spreading_Loss_Compensation_Geometry");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spread_comp_flag, rngSpreadComp.equals("NONE") ? 0 : 1);

        final String incAngComp = globalElem.getAttributeString(
                "Incidence_Angle_Compensation_Geometry");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.inc_angle_comp_flag, incAngComp.equals("NONE") ? 0 : 1);

        final String antElevComp = globalElem.getAttributeString(
                "Range_Antenna_Pattern_Compensation_Geometry");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ant_elev_corr_flag, antElevComp.equals("NONE") ? 0 : 1);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_inc_angle,
                globalElem.getAttributeDouble("Reference_Incidence_Angle"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_slant_range,
                globalElem.getAttributeDouble("Reference_Slant_Range"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ref_slant_range_exp,
                globalElem.getAttributeDouble("Reference_Slant_Range_Exponent"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.rescaling_factor,
                globalElem.getAttributeDouble("Rescaling_Factor"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.srgr_flag, isComplex ? 0 : 1);

        final MetadataElement s01Elem = globalElem.getElement("S01");
        if (s01Elem != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency,
                    s01Elem.getAttributeDouble("PRF"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate,
                    s01Elem.getAttributeDouble("Sampling_Rate") / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar,
                    s01Elem.getAttributeString("Polarisation"));

            // add Range and Azimuth bandwidth
            final double rangeBW = s01Elem.getAttributeDouble("Range_Focusing_Bandwidth"); // Hz
            final double azimuthBW = s01Elem.getAttributeDouble("Azimuth_Focusing_Bandwidth"); // Hz

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, azimuthBW);

            // Calibration constant read from Global_Metadata during calibration initialization
        } else {
            final String prefix = "S01_";
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency,
                    globalElem.getAttributeDouble(prefix + "PRF"));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate,
                    globalElem.getAttributeDouble(prefix + "Sampling_Rate") / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds1_tx_rx_polar,
                    globalElem.getAttributeString(prefix + "Polarisation"));

            // add Range and Azimuth bandwidth
            final double rangeBW = globalElem.getAttributeDouble(prefix + "Range_Focusing_Bandwidth"); // Hz
            final double azimuthBW = globalElem.getAttributeDouble(prefix + "Azimuth_Focusing_Bandwidth"); // Hz

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_bandwidth, rangeBW / Constants.oneMillion);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_bandwidth, azimuthBW);
        }

        final MetadataElement s02Elem = globalElem.getElement("S02");
        if (s02Elem != null) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds2_tx_rx_polar,
                    s02Elem.getAttributeString("Polarisation"));
        } else if (globalElem.containsAttribute("S02_Polarisation")) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.mds2_tx_rx_polar,
                    globalElem.getAttributeString("S02_Polarisation"));
        }

        addOrbitStateVectors(absRoot, globalElem);
        addDopplerCentroidCoefficients(absRoot, globalElem);
    }

    private void addSlantRangeToFirstPixel() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final MetadataElement oriRoot = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement auxElem = oriRoot.getElement("Auxiliary");
        final MetadataElement rootElem = auxElem.getElement("Root");
        MetadataElement bandElem;
        if (rootElem.getElement("MBI") != null) {
            bandElem = rootElem.getElement("MBI");
        } else {
            MetadataElement subSwathsElem = rootElem.getElement("SubSwaths");
            MetadataElement subSwathElem = subSwathsElem.getElement("SubSwath");
            bandElem = subSwathElem.getElement("SBI");
        }

        final double slantRangeTime = Double.valueOf(bandElem.getAttributeString("ZeroDopplerRangeFirstTime")); //s
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel,
                slantRangeTime * Constants.halfLightSpeed);
    }

    private void addOrbitStateVectors(final MetadataElement absRoot, final MetadataElement globalElem) {

        final MetadataElement orbitVectorListElem = absRoot.getElement(AbstractMetadata.orbit_state_vectors);
        final ProductData.UTC referenceUTC = ReaderUtils.getTime(globalElem, "Reference_UTC", standardDateFormat);
        final int numPoints = globalElem.getAttributeInt("Number_of_State_Vectors");

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME, referenceUTC);

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

    private static void addDopplerCentroidCoefficients(final MetadataElement absRoot, final MetadataElement globalElem) {

        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);
        final MetadataElement dopplerListElem = new MetadataElement(AbstractMetadata.dop_coef_list + ".1");
        dopplerCentroidCoefficientsElem.addElement(dopplerListElem);

        final ProductData.UTC utcTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time, AbstractMetadata.NO_METADATA_UTC);
        dopplerListElem.setAttributeUTC(AbstractMetadata.dop_coef_time, utcTime);

        AbstractMetadata.addAbstractedAttribute(dopplerListElem, AbstractMetadata.slant_range_time,
                ProductData.TYPE_FLOAT64, "ns", "Slant Range Time");
        AbstractMetadata.setAttribute(dopplerListElem, AbstractMetadata.slant_range_time, 0.0);

        for (int i = 0; i < 6; i++) {
            final double coefValue =
                    globalElem.getAttribute("Centroid_vs_Range_Time_Polynomial").getData().getElemDoubleAt(i);
            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + (i + 1));
            dopplerListElem.addElement(coefElem);
            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.dop_coef,
                    ProductData.TYPE_FLOAT64, "", "Doppler Centroid Coefficient");
            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.dop_coef, coefValue);
        }
    }

    private void addFirstLastLineTimes(final Product product) {
        final int rasterHeight = product.getSceneRasterHeight();
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement globalElem = root.getElement(NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);
        final MetadataElement bandElem = getBandElement();

        if (bandElem != null) {
            final double referenceUTC = ReaderUtils.getTime(globalElem, "Reference_UTC", standardDateFormat).getMJD(); // in days
            double firstLineTime = Double.valueOf(bandElem.getAttributeDouble("ZeroDopplerAzimuthFirstTime")) / (24 * 3600); // in days
            if (firstLineTime == 0) {
                final MetadataElement s01Elem = globalElem.getElement("S01");
                if (s01Elem != null) {
                    firstLineTime = s01Elem.getElement("B001").getAttributeDouble("Azimuth_First_Time") / (24 * 3600); // in days
                } else {
                    firstLineTime = globalElem.getAttributeDouble("S01_B001_Azimuth_First_Time") / (24 * 3600); // in days
                }
            }
            double lastLineTime = bandElem.getAttributeDouble("ZeroDopplerAzimuthLastTime") / (24 * 3600); // in days
            if (lastLineTime == 0) {
                final MetadataElement s01Elem = globalElem.getElement("S01");
                if (s01Elem != null) {
                    lastLineTime = s01Elem.getElement("B001").getAttributeDouble("Azimuth_Last_Time") / (24 * 3600); // in days
                } else {
                    lastLineTime = globalElem.getAttributeDouble("S01_B001_Azimuth_Last_Time") / (24 * 3600); // in days
                }
            }
            double lineTimeInterval = Double.valueOf(bandElem.getAttributeDouble("LineTimeInterval", AbstractMetadata.NO_METADATA)); // in s
            final ProductData.UTC startTime = new ProductData.UTC(referenceUTC + firstLineTime);
            final ProductData.UTC stopTime = new ProductData.UTC(referenceUTC + lastLineTime);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);
            product.setStartTime(startTime);
            product.setEndTime(stopTime);
            if (lineTimeInterval == 0 || lineTimeInterval == AbstractMetadata.NO_METADATA) {
                lineTimeInterval = ReaderUtils.getLineTimeInterval(startTime, stopTime, rasterHeight);
            }
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval, lineTimeInterval);
        }
    }

    private void addSRGRCoefficients(final Product product) {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final MetadataElement root = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement globalElem = root.getElement(NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);

        final MetadataAttribute attribute = globalElem.getAttribute("Ground_Projection_Polynomial_Reference_Range");
        if (attribute == null) {
            return;
        }

        final double referenceRange = attribute.getData().getElemDouble();

        final MetadataElement bandElem = getBandElement();

        final double rangeSpacing = Double.valueOf(bandElem.getAttributeString("ColumnSpacing", String.valueOf(AbstractMetadata.NO_METADATA)));
        final MetadataElement srgrCoefficientsElem = absRoot.getElement(AbstractMetadata.srgr_coefficients);
        final MetadataElement srgrListElem = new MetadataElement(AbstractMetadata.srgr_coef_list);
        srgrCoefficientsElem.addElement(srgrListElem);

        final ProductData.UTC utcTime = absRoot.getAttributeUTC(AbstractMetadata.first_line_time, AbstractMetadata.NO_METADATA_UTC);
        srgrListElem.setAttributeUTC(AbstractMetadata.srgr_coef_time, utcTime);
        AbstractMetadata.addAbstractedAttribute(srgrListElem, AbstractMetadata.ground_range_origin,
                ProductData.TYPE_FLOAT64, "m", "Ground Range Origin");
        AbstractMetadata.setAttribute(srgrListElem, AbstractMetadata.ground_range_origin, 0.0);

        final int numCoeffs = 6;
        for (int i = 0; i < numCoeffs; i++) {
            double srgrCoeff = globalElem.getAttribute("Ground_to_Slant_Polynomial").getData().getElemDoubleAt(i);
            if (i == 0) {
                srgrCoeff += referenceRange;
            } else {
                srgrCoeff /= FastMath.pow(rangeSpacing, i);
            }

            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + (i + 1));
            srgrListElem.addElement(coefElem);

            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.srgr_coef,
                    ProductData.TYPE_FLOAT64, "", "SRGR Coefficient");

            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.srgr_coef, srgrCoeff);
        }
    }

    private String getSampleType(final String productType) {
        if (productType.contains("SCS")) {
            isComplex = true;
            return "COMPLEX";
        }
        isComplex = false;
        return "DETECTED";
    }

    private void addBandsToProduct(final Variable[] variables) {
        int cnt = 1;
        for (Variable variable : variables) {
            final int width = variable.getDimension(1).getLength();
            final int height = variable.getDimension(0).getLength();

            String cntStr = "";
            if (variables.length > 0) {
                final String polStr = getPolarization(product, cnt);
                if (variables.length == 1) {
                    cntStr = '_' + polStr;
                }
            }

            if (isComplex) {     // add i and q
                Band bandI = useFloatBands ?
                        NetCDFUtils.createBand(variable, width, height, ProductData.TYPE_FLOAT32) :
                        NetCDFUtils.createBand(variable, width, height);
                createUniqueBandName(product, bandI, 'i' + cntStr);
                bandI.setUnit(Unit.REAL);
                bandI.setNoDataValue(0);
                bandI.setNoDataValueUsed(true);
                product.addBand(bandI);
                bandMap.put(bandI, variable);

                Band bandQ = useFloatBands ?
                        NetCDFUtils.createBand(variable, width, height, ProductData.TYPE_FLOAT32) :
                        NetCDFUtils.createBand(variable, width, height);
                createUniqueBandName(product, bandQ, 'q' + cntStr);
                bandQ.setUnit(Unit.IMAGINARY);
                bandQ.setNoDataValue(0);
                bandQ.setNoDataValueUsed(true);
                product.addBand(bandQ);
                bandMap.put(bandQ, variable);

                ReaderUtils.createVirtualIntensityBand(product, bandI, bandQ, cntStr);
            } else {
                final Band band = useFloatBands ?
                        NetCDFUtils.createBand(variable, width, height, ProductData.TYPE_FLOAT32) :
                        NetCDFUtils.createBand(variable, width, height);
                createUniqueBandName(product, band, "Amplitude" + cntStr);
                band.setUnit(Unit.AMPLITUDE);
                band.setNoDataValue(0);
                band.setNoDataValueUsed(true);
                product.addBand(band);
                bandMap.put(band, variable);

                SARReader.createVirtualIntensityBand(product, band, cntStr);
            }
        }
    }

    private static String getPolarization(final Product product, final int cnt) {
        final MetadataElement auxElem = AbstractMetadata.getOriginalProductMetadata(product).getElement("Auxiliary");
        final MetadataElement rootElem = auxElem.getElement("Root");
        final MetadataElement subSwathsElem = rootElem.getElement("SubSwaths");
        if (subSwathsElem != null) {
            final MetadataElement subSwathElem = subSwathsElem.getElement("SubSwath");
            if (subSwathElem != null) {
                final String polStr = subSwathElem.getAttributeString("Polarisation");
                if (!polStr.isEmpty())
                    return polStr;
            } else {
                final String prefix = "S0" + cnt + '_';
                final String polStr = subSwathElem.getAttributeString(prefix + "Polarisation", "");
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

    // change parameter
    private void addGeocodingToProduct(final Product product) {
//        for (Variable variable : variables) {
//            final int rank = variable.getRank();
//            final int gridWidth = variable.getDimension(rank - 1).getLength();
//            int gridHeight = variable.getDimension(rank - 2).getLength();
//            if (rank >= 3 && gridHeight <= 1)
//                gridHeight = variable.getDimension(rank - 3).getLength();
//            final TiePointGrid tpg = NetCDFUtils.createTiePointGrid(variable, gridWidth, gridHeight,
//                    product.getSceneRasterWidth(), product.getSceneRasterHeight());
//
//            product.addTiePointGrid(tpg);
//        }

        final MetadataElement bandElem = getBandElement();
//      addIncidenceAnglesSlantRangeTime(product, bandElem);
//      addGeocodingFromMetadata(product, bandElem);

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final MetadataElement sbiElem = AbstractMetadata.getOriginalProductMetadata(product).getElement("SBI");

        try {
            String str = bandElem.getAttributeString("TopLeftGeodeticCoordinates");
            final float latUL = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonUL = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("TopRightGeodeticCoordinates");
            final float latUR = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonUR = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("BottomLeftGeodeticCoordinates");
            final float latLL = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonLL = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("BottomRightGeodeticCoordinates");
            final float latLR = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonLR = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));

            absRoot.setAttributeDouble(AbstractMetadata.first_near_lat, latUL);
            absRoot.setAttributeDouble(AbstractMetadata.first_near_long, lonUL);
            absRoot.setAttributeDouble(AbstractMetadata.first_far_lat, latUR);
            absRoot.setAttributeDouble(AbstractMetadata.first_far_long, lonUR);
            absRoot.setAttributeDouble(AbstractMetadata.last_near_lat, latLL);
            absRoot.setAttributeDouble(AbstractMetadata.last_near_long, lonLL);
            absRoot.setAttributeDouble(AbstractMetadata.last_far_lat, latLR);
            absRoot.setAttributeDouble(AbstractMetadata.last_far_long, lonLR);

            final float[] latCorners = new float[]{latUL, latUR, latLL, latLR};
            final float[] lonCorners = new float[]{lonUL, lonUR, lonLL, lonLR};

            int gridWidth = 10;
            int gridHeight = 10;
            final float subSamplingX = product.getSceneRasterWidth() / (float) (gridWidth - 1);
            final float subSamplingY = product.getSceneRasterHeight() / (float) (gridHeight - 1);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                    bandElem.getAttributeDouble("ColumnSpacing", AbstractMetadata.NO_METADATA));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                    bandElem.getAttributeDouble("LineSpacing", AbstractMetadata.NO_METADATA));

            TiePointGrid latGrid;
            TiePointGrid lonGrid;

            if (product.getTiePointGrid(OperatorUtils.TPG_LATITUDE) != null) {
                latGrid = product.getTiePointGrid(OperatorUtils.TPG_LATITUDE);
                return;
            }
            if (product.getTiePointGrid(OperatorUtils.TPG_LONGITUDE) != null) {
                lonGrid = product.getTiePointGrid(OperatorUtils.TPG_LONGITUDE);
                return;
            }

            final double nearRangeAngle = Double.valueOf(bandElem.getAttributeDouble("NearIncidenceAngle", 0));
            final double farRangeAngle = Double.valueOf(bandElem.getAttributeDouble("FarIncidenceAngle", 0));

            final double firstRangeTime = Double.valueOf(bandElem.getAttributeDouble("ZeroDopplerRangeFirstTime", 0)) * Constants.oneBillion;
            final double lastRangeTime = Double.valueOf(bandElem.getAttributeDouble("ZeroDopplerRangeLastTime", 0)) * Constants.oneBillion;

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_near, nearRangeAngle);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_far, farRangeAngle);

            final float[] incidenceCorners = new float[]{(float) nearRangeAngle, (float) farRangeAngle, (float) nearRangeAngle, (float) farRangeAngle};
            final float[] slantRange = new float[]{(float) firstRangeTime, (float) lastRangeTime, (float) firstRangeTime, (float) lastRangeTime};

            float[] fineLatTiePoints = new float[gridWidth * gridHeight];
            float[] fineLonTiePoints = new float[gridWidth * gridHeight];
            final float[] fineAngles = new float[gridWidth * gridHeight];
            final float[] fineTimes = new float[gridWidth * gridHeight];

            ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, latCorners, fineLatTiePoints);
            ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, lonCorners, fineLonTiePoints);
            ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, incidenceCorners, fineAngles);
            ReaderUtils.createFineTiePointGrid(2, 2, gridWidth, gridHeight, slantRange, fineTimes);

            latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE, gridWidth, gridHeight, 0.0, 0.0,
                    subSamplingX, subSamplingY, fineLatTiePoints);
            latGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(latGrid);


            lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE, gridWidth, gridHeight, 0.0, 0.0,
                    subSamplingX, subSamplingY, fineLonTiePoints);
            lonGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(lonGrid);


            final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0.0, 0.0,
                    subSamplingX, subSamplingY, fineAngles);
            incidentAngleGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(incidentAngleGrid);

            final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME, gridWidth, gridHeight, 0.0, 0.0,
                    subSamplingX, subSamplingY, fineTimes);
            slantRangeGrid.setUnit(Unit.NANOSECONDS);
            product.addTiePointGrid(slantRangeGrid);


            TiePointGeoCoding tpGridLatLon = new TiePointGeoCoding(latGrid, lonGrid);
            product.setSceneGeoCoding(tpGridLatLon);

            final Band[] bands = product.getBands();
            for (Band band : bands) {
                band.setGeoCoding(bandGeocodingMap.get(band));
            }

//          ReaderUtils.addGeoCoding(product, latCorners, lonCorners);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            // continue
        }
    }

    private static void addIncidenceAnglesSlantRangeTime(final Product product, final MetadataElement bandElem) {
        if (bandElem == null) return;
        int gridWidth = Math.min(10, Math.max(2, product.getSceneRasterWidth()));
        int gridHeight = Math.min(10, Math.max(2, product.getSceneRasterHeight()));
        final float subSamplingX = product.getSceneRasterWidth() / (float) (gridWidth - 1);
        final float subSamplingY = product.getSceneRasterHeight() / (float) (gridHeight - 1);

        final double nearRangeAngle = Double.parseDouble(bandElem.getAttributeString("NearIncidenceAngle", "0"));
        final double farRangeAngle = Double.parseDouble(bandElem.getAttributeString("FarIncidenceAngle", "0"));

        final double firstRangeTime = Double.parseDouble(bandElem.getAttributeString("ZeroDopplerRangeFirstTime", "0")) * Constants.oneBillion;
        final double lastRangeTime = Double.parseDouble(bandElem.getAttributeString("ZeroDopplerRangeLastTime", "0")) * Constants.oneBillion;

        final float[] incidenceCorners = new float[]{(float) nearRangeAngle, (float) farRangeAngle, (float) nearRangeAngle, (float) farRangeAngle};
        final float[] slantRange = new float[]{(float) firstRangeTime, (float) lastRangeTime, (float) firstRangeTime, (float) lastRangeTime};

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

    private MetadataElement getBandElement() {
        final MetadataElement originalProductMetadata = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement auxElem = originalProductMetadata.getElement("Auxiliary");
        final MetadataElement rootElem = auxElem.getElement("Root");
        final MetadataElement subSwathElem = rootElem.getElement("SubSwaths").getElement("SubSwath");
        final MetadataElement sbiElem = subSwathElem.getElement("SBI");
        final MetadataElement mbiElem = rootElem.getElement("MBI");

        if (sbiElem == null && mbiElem == null) {
            //
        }

        MetadataElement bandElem;
        if (rootElem.getAttributeString("AcquisitionMode").equals("WIDE SWATH")
                && (rootElem.getAttributeString("ProductType").contains("GEC") || rootElem.getAttributeString("ProductType").contains("GTC"))) {
            bandElem = rootElem.getElement("MBI");
        } else {
            bandElem = subSwathElem.getElement("SBI");
        }
        return bandElem;
    }

    private static void addGeocodingFromMetadata(final Product product, final MetadataElement bandElem) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        try {
            String str = bandElem.getAttributeString("TopLeftGeodeticCoordinates");
            final float latUL = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonUL = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("TopRightGeodeticCoordinates");
            final float latUR = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonUR = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("BottomLeftGeodeticCoordinates");
            final float latLL = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonLL = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));
            str = bandElem.getAttributeString("BottomRightGeodeticCoordinates");
            final float latLR = Float.parseFloat(str.substring(0, str.indexOf(',')));
            final float lonLR = Float.parseFloat(str.substring(str.indexOf(',') + 1, str.lastIndexOf(',')));

            absRoot.setAttributeDouble(AbstractMetadata.first_near_lat, latUL);
            absRoot.setAttributeDouble(AbstractMetadata.first_near_long, lonUL);
            absRoot.setAttributeDouble(AbstractMetadata.first_far_lat, latUR);
            absRoot.setAttributeDouble(AbstractMetadata.first_far_long, lonUR);
            absRoot.setAttributeDouble(AbstractMetadata.last_near_lat, latLL);
            absRoot.setAttributeDouble(AbstractMetadata.last_near_long, lonLL);
            absRoot.setAttributeDouble(AbstractMetadata.last_far_lat, latLR);
            absRoot.setAttributeDouble(AbstractMetadata.last_far_long, lonLR);

            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing,
                    bandElem.getAttributeDouble("ColumnSpacing", AbstractMetadata.NO_METADATA));
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing,
                    bandElem.getAttributeDouble("LineSpacing", AbstractMetadata.NO_METADATA));

            final float[] latCorners = new float[]{latUL, latUR, latLL, latLR};
            final float[] lonCorners = new float[]{lonUL, lonUR, lonLL, lonLR};


            ReaderUtils.addGeoCoding(product, latCorners, lonCorners);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            // continue
        }
    }

    public void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                       int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                       int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                       ProgressMonitor pm) throws IOException {

        Guardian.assertTrue("sourceStepX == 1 && sourceStepY == 1", sourceStepX == 1 && sourceStepY == 1);
        Guardian.assertTrue("sourceWidth == destWidth", sourceWidth == destWidth);
        Guardian.assertTrue("sourceHeight == destHeight", sourceHeight == destHeight);

        final int sceneHeight = product.getSceneRasterHeight();
        final int sceneWidth = product.getSceneRasterWidth();
        destHeight = Math.min(destHeight, sceneHeight - sourceOffsetY);
        destWidth = Math.min(destWidth, sceneWidth - destOffsetX);

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

                if (destBand.getDataType() == ProductData.TYPE_FLOAT32) {
                    for (int x = 0; x < destWidth; x++) {
                        destBuffer.setElemFloatAt(y * destWidth + x, ArrayCopy.toFloat(array.getShort(x)));
                    }
                } else {
                    System.arraycopy(array.getStorage(), 0, destBuffer.getElems(), y * destWidth, destWidth);
                }
                pm.worked(1);
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            pm.done();
        }
    }
}
