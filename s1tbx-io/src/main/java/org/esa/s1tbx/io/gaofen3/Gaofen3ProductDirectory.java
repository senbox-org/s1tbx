/*
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
package org.esa.s1tbx.io.gaofen3;

import org.esa.s1tbx.commons.OrbitStateVectors;
import org.esa.s1tbx.commons.SARGeocoding;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.XMLProductDirectory;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.io.geotiffxml.GeoTiffUtils;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.downloadable.XMLSupport;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.datamodel.*;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.eo.GeoUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import org.jdom2.Element;
import org.jlinda.core.Orbit;
import org.jlinda.core.Point;
import org.jlinda.core.utils.DateUtils;

import org.jblas.DoubleMatrix;
import org.jblas.Solve;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.*;
import java.util.List;


/**
 * @author Jakob Grahn
 */
public class Gaofen3ProductDirectory extends XMLProductDirectory  {

    private int width, height;
    private final String productName;
    private String[] polarisations;
    private String polarisationTag;
    private Product bandProduct;
    private final Map<String,Double> qValMap = new HashMap<>();
    private final Map<String,Double> calConstMap = new HashMap<>();
    private final Map<String,List<Double>> rpcParameters = new HashMap<>();
    private double[] incidenceAngleList;
    private Orbit polyOrbit;

    private static final GeoTiffProductReaderPlugIn geoTiffPlugIn = new GeoTiffProductReaderPlugIn();
    private final DateFormat standardDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

    public Gaofen3ProductDirectory(final File inputFile) {
        super(inputFile);
        productName = inputFile.getName().replace(".meta.xml", "");

        //SystemUtils.LOG.info("Reading GF3 product: " + productName);

        // Read RPCs, use first file found and assume it is applicable to all bands:
        final File folder =  new File(inputFile.getParent());
        final File[] fileList = folder.listFiles();
        if (fileList != null) {
            for (File f : fileList) {
                if (f.getName().endsWith(".rpc")) {
                    readRpcParameters(f);
                    break;
                }
            }
        }

        // Read incidence angles:
        final String incFile = inputFile.getName().replace(".meta.xml", ".incidence.xml");
        if(exists(incFile)) {
            try (final InputStream is = getInputStream(incFile)) {
                xmlDoc = XMLSupport.LoadXML(is);
            } catch (IOException e) {
                e.printStackTrace();
            }

            final int nval = Integer.parseInt(xmlDoc.getRootElement().getChild("numberofIncidenceValue").getValue());
            final double[] _incidenceAngleList = new double[nval];
            int cnt = 0;
            for (Element child : xmlDoc.getRootElement().getChildren()) {
                switch (child.getName()) {
                    case "incidenceValue":
                        double v = Double.parseDouble(child.getValue());
                        _incidenceAngleList[cnt] = v;
                        cnt += 1;
                }
            }
            incidenceAngleList = _incidenceAngleList;
        }
    }

    @Override
    public void close() throws IOException  {
        super.close();
        if(bandProduct != null) {
            bandProduct.dispose();
        }
    }

    protected void addAbstractedMetadataHeader(final MetadataElement root) throws IOException {

        final String defStr = AbstractMetadata.NO_METADATA_STRING;

        // Declare meta elements:
        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(root);
        final MetadataElement productMetadata = origProdRoot.getElement("product");
        final MetadataElement sensorMetadata = productMetadata.getElement("sensor");
        final MetadataElement platformMetadata = productMetadata.getElement("platform");
        final MetadataElement productInfoMetadata = productMetadata.getElement("productinfo");
        final MetadataElement imageInfoMetadata = productMetadata.getElement("imageinfo");
        final MetadataElement processInfoMetadata = productMetadata.getElement("processinfo");

        width = imageInfoMetadata.getAttributeInt("width");
        height = imageInfoMetadata.getAttributeInt("height");

        // General:
        setSLC(productInfoMetadata.getAttributeString("productType", defStr).equals("SLC"));
        final String pass = productMetadata.getAttributeString("Direction", defStr);
        final String lookDir = sensorMetadata.getAttributeString("lookDirection");
        final String imagingMode = sensorMetadata.getAttributeString("imagingMode", defStr);
        final ArrayList<String> testedModes = new ArrayList<String>() {{add("ss");add("fsi");add("fsii");add("ufs");}};
        if (!testedModes.contains(imagingMode.toLowerCase())){
            throw new IOException("Imaging mode \"" + imagingMode + "\" not tested! ");
        }
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.MISSION,
                productMetadata.getAttributeString("satellite", defStr));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, getProductName());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, getProductType());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SPH_DESCRIPTOR,
                "Level-" + productInfoMetadata.getAttributeString("productLevel", defStr));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.SAMPLE_TYPE, isSLC() ? "COMPLEX" : "DETECTED");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ACQUISITION_MODE, imagingMode);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PASS,
                pass.equalsIgnoreCase("DEC") ? "DESCENDING" : "ASCENDING");
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.avg_scene_height,
                sensorMetadata.getElement("waveParams").getElement(
                        "wave").getAttributeDouble("averageAltitude"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.antenna_pointing,
                lookDir.equalsIgnoreCase("R") ? "RIGHT" : "LEFT");

        // Processor:
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.ProcessingSystemIdentifier,
                processInfoMetadata.getAttributeString("algorithm", defStr));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PROC_TIME,
                ReaderUtils.getTime(productInfoMetadata, "productGentime", standardDateFormat));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.algorithm,
                processInfoMetadata.getAttributeString("algorithm", defStr));

        // Image:
        final ProductData.UTC startTime = ReaderUtils.getTime(
                imageInfoMetadata.getElement("imagingTime"), "start", standardDateFormat);
        final ProductData.UTC stopTime = ReaderUtils.getTime(
                imageInfoMetadata.getElement("imagingTime"), "end", standardDateFormat);

        final double eqvPrf = imageInfoMetadata.getAttributeDouble("eqvPRF");
        final double prf = sensorMetadata.getElement("waveParams").getElement("wave").getAttributeDouble("prf");

        final double eqvFs = imageInfoMetadata.getAttributeDouble("eqvFs")*Constants.oneMillion;
        final double fs = sensorMetadata.getElement("waveParams").getElement("wave").getAttributeDouble(
                "sampleRate")*Constants.oneMillion;

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, startTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, stopTime);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,1/eqvPrf);
        //AbstractMetadata.setAttribute(absRoot, AbstractMetadata.line_time_interval,
        //        ReaderUtils.getLineTimeInterval(startTime, stopTime, height));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel,
                imageInfoMetadata.getAttributeDouble("nearRange"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, width);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, height);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_near,
                processInfoMetadata.getAttributeDouble("incidenceAngleNearRange"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.incidence_far,
                processInfoMetadata.getAttributeDouble("incidenceAngleFarRange"));

        final double rangeSpacing = Constants.halfLightSpeed/eqvFs;
        final double azimuthSpacing = platformMetadata.getAttributeDouble("satVelocity")/eqvPrf;
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_spacing, rangeSpacing);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_spacing, azimuthSpacing);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_looks,
                processInfoMetadata.getAttributeDouble("MultilookRange"));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.azimuth_looks,
                processInfoMetadata.getAttributeDouble("MultilookAzimuth"));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.coregistered_stack, 0);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.pulse_repetition_frequency, eqvPrf); // Hz
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.range_sampling_rate, eqvFs/Constants.oneMillion); // MHz

        polarisationTag = sensorMetadata.getElement("polarParams").getElement("polar").getAttributeString("polarMode");
        if (polarisationTag.equals("AHV")){
            polarisations = new String[]{"HH", "HV", "VH", "VV"};
        } else if (polarisationTag.equals("DH")) {
            polarisations = new String[]{"HH"};
        } else if (polarisationTag.equals("DV")) {
            polarisations = new String[]{"VV"};
        } else if (polarisationTag.equals("HHHV") || polarisationTag.equals("HVHH")) {
            polarisations = new String[]{"HH", "HV"};
        } else if (polarisationTag.equals("VVVH") || polarisationTag.equals("VHVV")) {
            polarisations = new String[]{"VV", "VH"};
        } else if (polarisationTag.equals("HH")) {
            polarisations = new String[]{"HH"};
        } else if (polarisationTag.equals("VV")) {
            polarisations = new String[]{"VV"};
        } else {
            throw new IOException("Unrecognized polarisation tag = " + polarisationTag);
        }
        int i = 0;
        for (String pol : polarisations) {
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.polarTags[i], pol);
            i++;
        }
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.radar_frequency,
                sensorMetadata.getAttributeDouble("RadarCenterFrequency")*1000); //in MHz

        // Calibration:
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.abs_calibration_flag, 1);
        for (String p : polarisations) {
            qValMap.put(p, imageInfoMetadata.getElement("QualifyValue").getAttributeDouble(p));
            calConstMap.put(p, processInfoMetadata.getElement("CalibrationConst").getAttributeDouble(p));
        }

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.bistatic_correction_applied, 1);

        // Doppler centroid coefficients:
        final MetadataElement dopplerCentroidCoefficientsElem = absRoot.getElement(AbstractMetadata.dop_coefficients);
        final MetadataElement dopplerElem = new MetadataElement(AbstractMetadata.dop_coef);
        dopplerCentroidCoefficientsElem.addElement(dopplerElem);

        AbstractMetadata.addAbstractedAttribute(dopplerElem, AbstractMetadata.slant_range_time,
                ProductData.TYPE_FLOAT64, "ms", "Slant Range Time");
        AbstractMetadata.setAttribute(dopplerElem, AbstractMetadata.slant_range_time,
                processInfoMetadata.getAttributeDouble("DopplerParametersReferenceTime"));

        final MetadataAttribute[] coefficients = processInfoMetadata.getElement(
                "DopplerCentroidCoefficients").getAttributes();
        int cnt = 1;
        for (MetadataAttribute coefficient : coefficients) {
            final double coefValue = Double.parseDouble(coefficient.getData().getElemString());
            final MetadataElement coefElem = new MetadataElement(AbstractMetadata.coefficient + '.' + cnt);
            dopplerElem.addElement(coefElem);
            AbstractMetadata.addAbstractedAttribute(coefElem, AbstractMetadata.dop_coef,
                    ProductData.TYPE_FLOAT64, "", "Doppler Centroid Coefficient");
            AbstractMetadata.setAttribute(coefElem, AbstractMetadata.dop_coef, coefValue);
            ++cnt;
        }
    }

    protected Orbit getPolyOrbit(final Product product) {

        if (polyOrbit==null) {

            // Metadata elements:
            final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
            final MetadataElement productMetadata = origProdRoot.getElement("product");
            final MetadataElement svecMetadata = productMetadata.getElement("GPS");
            final MetadataElement[] svecElems = svecMetadata.getElements();

            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
            final String firstLineTimeString = absRoot.getAttributeString(AbstractMetadata.first_line_time);
            final ProductData.UTC firstLineTimeUtc = AbstractMetadata.parseUTC(firstLineTimeString);
            final double firstLineTimeSec = DateUtils.dateTimeToSecOfDay(firstLineTimeUtc.toString());

            // Make smoothed state vector interpolator (using Orbit-class/polynomial interpolation):
            final int nStateVectors = svecElems.length;
            final int polyOrder = Math.min(nStateVectors - 2, 5);
            SystemUtils.LOG.info("State vectors (n=" + nStateVectors + ") fitted to " + polyOrder + "-order polynomial.");
            final int step = 1; // Math.floorDiv(nStateVectors, polyOrder+1);
            final int nInterpStateVectors = Math.floorDiv(nStateVectors, step);
            double[] timeInterp = new double[nInterpStateVectors];
            double[] xPosInterp = new double[nInterpStateVectors];
            double[] yPosInterp = new double[nInterpStateVectors];
            double[] zPosInterp = new double[nInterpStateVectors];

            for (int i = 0; i < nInterpStateVectors; i++) {
                timeInterp[i] = DateUtils.dateTimeToSecOfDay(svecElems[i * step].getAttributeString("TimeStamp"));
                xPosInterp[i] = svecElems[i * step].getAttributeDouble("xPosition");
                yPosInterp[i] = svecElems[i * step].getAttributeDouble("yPosition");
                zPosInterp[i] = svecElems[i * step].getAttributeDouble("zPosition");
            }
            polyOrbit = new Orbit(timeInterp, xPosInterp, yPosInterp, zPosInterp, polyOrder);
        }

        return polyOrbit;
    }

    private static int argMin(double[] a) {
        int loc = 0;
        double min = a[loc];
        for (int i = 1; i < a.length; i++) {
            if (a[i] < min) {
                min = a[i];
                loc = i;
            }
        }
        return loc;
    }

    protected void addStateVectorsFromMeta(final Product product) {

        // Metadata elements:
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement productMetadata = origProdRoot.getElement("product");
        final MetadataElement svecMetadata = productMetadata.getElement("GPS");
        final MetadataElement[] svecElems = svecMetadata.getElements();

        // Make smoothed state vector interpolator (using Orbit-class/polynomial interpolation):
        final Orbit orbit = getPolyOrbit(product);

        // Generate new state vectors by interpolation:
        final double mjdFirstSV = ReaderUtils.getTime(svecElems[0],
                "TimeStamp", standardDateFormat).getMJD() - 10/Constants.secondsInDay;
        final double mjdLastSV = ReaderUtils.getTime(svecElems[svecElems.length-1],
                "TimeStamp", standardDateFormat).getMJD() + 10/Constants.secondsInDay;
        final double mjdTimeStep = 1/Constants.secondsInDay;
        final int nStateVectorsFinal = (int) Math.floor((mjdLastSV-mjdFirstSV)/(mjdTimeStep))+1;
        final OrbitStateVector[] stateVectorsFinal = new OrbitStateVector[nStateVectorsFinal];

        for (int i = 0; i < nStateVectorsFinal; i++) {
            final ProductData.UTC utcTime = new ProductData.UTC(mjdFirstSV+i*mjdTimeStep);
            final double azTime = DateUtils.dateTimeToSecOfDay(utcTime.toString());
            final Point pos = orbit.getXYZ(azTime);
            final Point vel = orbit.getXYZDot(azTime);
            stateVectorsFinal[i] = new OrbitStateVector(
                    utcTime, pos.getX(), pos.getY(), pos.getZ(), vel.getX(), vel.getY(), vel.getZ()
            );
        }

        // Add to abstracted metadata:
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME, stateVectorsFinal[0].time);
        try {
            AbstractMetadata.setOrbitStateVectors(absRoot, stateVectorsFinal);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void constructStateVectorsFromRPC(final Product product) {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        assert absRoot != null;

        final Gaofen3Geocoding geocoding = getGeoCoding(product);

        final String firstLineTimeString = absRoot.getAttributeString(AbstractMetadata.first_line_time);
        final ProductData.UTC firstLineTimeUtc = AbstractMetadata.parseUTC(firstLineTimeString);
        final double firstLineTimeMjd = firstLineTimeUtc.getMJD();
        final double firstLineTimeSec = DateUtils.dateTimeToSecOfDay(firstLineTimeUtc.toString());
        final double lineTimeIntervalInSecs = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval);
        final double lineTimeIntervalInDays = lineTimeIntervalInSecs/Constants.secondsInDay;
        final double rangeSpacing = absRoot.getAttributeDouble(AbstractMetadata.range_spacing);
        final double nearEdgeSlantRange = absRoot.getAttributeDouble(AbstractMetadata.slant_range_to_first_pixel);

        final Orbit orbit = getPolyOrbit(product);
        final int nx = 50;
        final int nsv = 100;
        int maxIter = 30;
        double scaleFactor = 1e5;
        double rmsResidualThreshold = 1.5*scaleFactor/1e5;
        double lambda0 = 1.0;
        double[] dx = new double[] {1,0,0};
        double[] dy = new double[] {0,1,0};
        double[] dz = new double[] {0,0,1};
        double dp = 1.0;
        double dv = 0.01;

        SystemUtils.LOG.info("Max iterations:     " + maxIter);
        SystemUtils.LOG.info("RMS residual thres: " + rmsResidualThreshold);

        double[] xArr = new double[nx];
        double[] yArr = new double[nsv];
        for (int i = 0; i < nx; i++) {
            xArr[i] = ((double) i)/((double) nx-1) * (width-1);
        }
        for (int i = 0; i < nsv; i++) {
            yArr[i] = ((double) i)/((double) nsv-1) * (height-1);
        }

        Orbits.OrbitVector[] svCorrected = new Orbits.OrbitVector[nsv];

        for (int i = 0; i < nsv; i++) { // Loop over azimuth:
            double iy = yArr[i];  // Image index in azimuth
            double azTimeMjd = firstLineTimeMjd + iy*lineTimeIntervalInDays;
            double azTimeSec = firstLineTimeSec + iy*lineTimeIntervalInSecs;

            final Point pos = orbit.getXYZ(azTimeSec);
            final Point vel = orbit.getXYZDot(azTimeSec);
            Orbits.OrbitVector svCurrent = new Orbits.OrbitVector(azTimeMjd,
                    pos.getX(), pos.getY(), pos.getZ(),
                    vel.getX(), vel.getY(), vel.getZ()
            );

            GeoPos[] geoRPCList = new GeoPos[nx];
            for (int j = 0; j < nx; j++) { // Loop over range:
                // Get target geo from RPC:
                double ix = xArr[j];
                PixelPos idx = new PixelPos(ix, iy);
                GeoPos geoRPC = new GeoPos();
                geocoding.getGeoPos(idx, geoRPC);
                geoRPCList[j] = geoRPC;

                // Sanity check:
                PixelPos _idx = new PixelPos();
                geocoding.getPixelPos(geoRPC, _idx);
                assert Math.abs(idx.x - _idx.x) < 0.001;
                assert Math.abs(idx.y - _idx.y) < 0.001;
            }

            // Initiate Jacobian and residual:
            DoubleMatrix jac = new DoubleMatrix(6, 2*nx);
            DoubleMatrix residual = new DoubleMatrix(2*nx, 1);

            double[] rmsResidualList = new double[maxIter];
            Orbits.OrbitVector[] svList = new Orbits.OrbitVector[maxIter];
            boolean converged;
            double lambda = lambda0;
            for (int iter = 0; iter < maxIter; iter++) {
                //SystemUtils.LOG.info("iter: " + iter);
                svList[iter] = new Orbits.OrbitVector(svCurrent.utcMJD,
                        svCurrent.xPos, svCurrent.yPos, svCurrent.zPos,
                        svCurrent.xVel, svCurrent.yVel, svCurrent.zVel
                );
                for (int j = 0; j < nx; j++) { // Loop over range:
                    double ix = xArr[j];  // Image index in range
                    GeoPos geoRPC = geoRPCList[j];

                    // Get slant range time:
                    double srMeter = nearEdgeSlantRange + ix * rangeSpacing;
                    double srSec = srMeter / Constants.halfLightSpeed;

                    // Update target geo using orbit:
                    GeoPos geoRD = computeLatLon(geoRPC.lat, geoRPC.lon, srSec, svCurrent);
                    double rLat = geoRD.lat - geoRPC.lat;
                    double rLon = geoRD.lon - geoRPC.lon;
                    residual.put(2*j, 0, rLat * scaleFactor);
                    residual.put(2*j + 1, 0, rLon * scaleFactor);

                    // Estimate Jacobian by perturbing state vectors:
                    for (int k = 0; k < 3; k++) {
                        // Perturb position:
                        Orbits.OrbitVector sv_dp0 = new Orbits.OrbitVector(svCurrent.utcMJD,
                                svCurrent.xPos - dx[k]*dp,
                                svCurrent.yPos - dy[k]*dp,
                                svCurrent.zPos - dz[k]*dp,
                                svCurrent.xVel,
                                svCurrent.yVel,
                                svCurrent.zVel
                        );
                        Orbits.OrbitVector sv_dp1 = new Orbits.OrbitVector(svCurrent.utcMJD,
                                svCurrent.xPos + dx[k]*dp,
                                svCurrent.yPos + dy[k]*dp,
                                svCurrent.zPos + dz[k]*dp,
                                svCurrent.xVel,
                                svCurrent.yVel,
                                svCurrent.zVel
                        );

                        // Perturb velocity:
                        Orbits.OrbitVector sv_dv0 = new Orbits.OrbitVector(svCurrent.utcMJD,
                                svCurrent.xPos,
                                svCurrent.yPos,
                                svCurrent.zPos,
                                svCurrent.xVel - dx[k]*dv,
                                svCurrent.yVel - dy[k]*dv,
                                svCurrent.zVel - dz[k]*dv
                        );
                        Orbits.OrbitVector sv_dv1 = new Orbits.OrbitVector(svCurrent.utcMJD,
                                svCurrent.xPos,
                                svCurrent.yPos,
                                svCurrent.zPos,
                                svCurrent.xVel + dx[k]*dv,
                                svCurrent.yVel + dy[k]*dv,
                                svCurrent.zVel + dz[k]*dv
                        );

                        // Compute perturbed target geo:
                        GeoPos geo_dp0 = computeLatLon(geoRPC.lat, geoRPC.lon, srSec, sv_dp0);
                        GeoPos geo_dp1 = computeLatLon(geoRPC.lat, geoRPC.lon, srSec, sv_dp1);
                        GeoPos geo_dv0 = computeLatLon(geoRPC.lat, geoRPC.lon, srSec, sv_dv0);
                        GeoPos geo_dv1 = computeLatLon(geoRPC.lat, geoRPC.lon, srSec, sv_dv1);

                        // Numerical derivative of residual wrt position and velocity along dim k:
                        double drLat_dp = (geo_dp1.lat - geo_dp0.lat)/(2*dp);
                        double drLon_dp = (geo_dp1.lon - geo_dp0.lon)/(2*dp);
                        double drLat_dv = (geo_dv1.lat - geo_dv0.lat)/(2*dv);
                        double drLon_dv = (geo_dv1.lon - geo_dv0.lon)/(2*dv);

                        jac.put(k, 2*j, drLat_dp * scaleFactor);
                        jac.put(k, 2*j+1, drLon_dp * scaleFactor);
                        jac.put(k+3, 2*j, drLat_dv * scaleFactor);
                        jac.put(k+3, 2*j+1, drLon_dv * scaleFactor);
                    }
                }

                // Check residual:
                int nResidual = residual.columns*residual.rows;
                double rmsResidual = Math.sqrt(residual.transpose().mmul(residual).get(0,0) / nResidual);
                rmsResidualList[iter] = rmsResidual;
                converged = rmsResidual < rmsResidualThreshold;
                if (converged) {
                    //SystemUtils.LOG.info("RMS residual criteria reached after " + iter + " iterations: " +
                    //        rmsResidual + " < " + rmsResidualThreshold);
                    break;
                } else if (iter == maxIter-1){
                    //SystemUtils.LOG.warning("Max iteration reached without RMS residual criteria reached: " +
                    //        rmsResidual + " >= " + rmsResidualThreshold);
                    //SystemUtils.LOG.warning("Selecting state vector with smallest RMS residual = " +
                    //        Collections.min(Arrays.asList(ArrayUtils.toObject(rmsResidualList))));
                    //SystemUtils.LOG.warning("Smallest RMS residual at iteration: " + argMin(rmsResidualList));
                    svCurrent = svList[argMin(rmsResidualList)];
                    break;
                }

                // Damping factor:
                //lambda = lambda0;
                if ( iter < 5 ){
                    lambda = lambda0;
                } else {
                    if (rmsResidualList[iter] > rmsResidualList[iter-1]) {
                        lambda = lambda*2;
                    } else {
                        lambda = lambda/2;
                    }
                }

                // Solve:
                DoubleMatrix A = jac.mmul(jac.transpose()).add(DoubleMatrix.eye(6).mul(lambda));
                DoubleMatrix rhs = jac.mmul(residual);
                DoubleMatrix dbeta = Solve.solveLeastSquares(A, rhs);

                // Update state vector:
                svCurrent.xPos = svCurrent.xPos - dbeta.get(0,0);
                svCurrent.yPos = svCurrent.yPos - dbeta.get(1,0);
                svCurrent.zPos = svCurrent.zPos - dbeta.get(2,0);
                svCurrent.xVel = svCurrent.xVel - dbeta.get(3,0);
                svCurrent.yVel = svCurrent.yVel - dbeta.get(4,0);
                svCurrent.zVel = svCurrent.zVel - dbeta.get(5,0);

            }
            svCorrected[i] = svCurrent;
        }

        // Add to abstracted metadata:
        final OrbitStateVector[] stateVectorsFinal = new OrbitStateVector[nsv];
        for (int i = 0; i < nsv; i++) {
            final ProductData.UTC utcTime = new ProductData.UTC(svCorrected[i].utcMJD);
            stateVectorsFinal[i] = new OrbitStateVector(utcTime,
                    svCorrected[i].xPos, svCorrected[i].yPos, svCorrected[i].zPos,
                    svCorrected[i].xVel, svCorrected[i].yVel, svCorrected[i].zVel
            );
        }
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.STATE_VECTOR_TIME, stateVectorsFinal[0].time);
        try {
            AbstractMetadata.setOrbitStateVectors(absRoot, stateVectorsFinal);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void addMetadataCorners(final Product product) {
        final Gaofen3Geocoding geocoding = getGeoCoding(product);
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        double[] geo;
        geo = geocoding.pixel2geo(0.0, 0.0);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, geo[0]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, geo[1]);

        geo = geocoding.pixel2geo(width, 0.0);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, geo[0]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, geo[1]);

        geo = geocoding.pixel2geo(0.0, height);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, geo[0]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, geo[1]);

        geo = geocoding.pixel2geo(width, height);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, geo[0]);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, geo[1]);
    }

    @Override
    protected String getProductName() {
        return productName;
    }

    @Override
    protected String getProductType() {
        if (getProductName().contains("_L1A_")) {
            return "L1A";
        } else if (getProductName().contains("_L1B_")) {
            return "L1B";
        } else {
            SystemUtils.LOG.severe("Failed to parse product type from file name");
            return "-";
        }
    }

    @Override
    protected String getProductDescription() {
        return getProductType().equals("SLC") ? "Single Look Complex" : "GEO";
    }

    String[] getPolarisations() {
        return polarisations;
    }

    double getQVal(final String pol) {
        return qValMap.get(pol);
    }

    double getCalConst(final String pol) {
        return calConstMap.get(pol);
    }

    protected void readRpcParameters(File rpcFile){
        try {
            Scanner myReader = new Scanner(rpcFile);
            String[] keys = {
                    "errBias", "errRand", "lineOffset", "sampOffset", "latOffset", "longOffset",
                    "heightOffset", "lineScale", "sampScale", "latScale", "longScale", "heightScale",
                    "lineNumCoef", "lineDenCoef", "sampNumCoef", "sampDenCoef"
            };
            while (myReader.hasNextLine()) {
                String line = myReader.nextLine().trim();
                for (String key : keys) {
                    if (line.startsWith(key)) {
                        List<Double> val = new ArrayList<>();
                        if (line.endsWith(";")) { // starts with key, ends with ; --> scalar case
                            line = line.replaceAll(key, "").replaceAll("[=|;]", "");
                            val.add(Double.parseDouble(line));
                        } else if (line.endsWith("(") || line.endsWith(",")) { // starts with key, ends with , --> vector case
                            line = line.replaceAll(key, "").replaceAll("[=|,|,(]", "").trim();
                            if (line.length() > 0) {
                                val.add(Double.parseDouble(line));
                            }
                            while (myReader.hasNextLine()) {
                                line = myReader.nextLine().trim();
                                val.add(Double.parseDouble(line.replaceAll("[,|);]", "")));
                                if (line.endsWith(");")) {
                                    break;
                                }
                            }
                        } else {
                            SystemUtils.LOG.warning("Failed to parse line in RPC-file: " + line);
                        }
                        rpcParameters.put(key, val);
                    }
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    protected void addImageFile(final String imgPath, final MetadataElement newRoot) {
        final String name = getBandFileNameFromImage(imgPath);
        if (name.endsWith("tiff")) {
            try {
                ImageInputStream imgStream = null;
                if(!productDir.isCompressed()) {
                    File file = productDir.getFile(imgPath);
                    imgStream = new FileImageInputStream(file);
                } else {
                    final Dimension bandDimensions = new Dimension(width, height);
                    final InputStream inStream = getInputStream(imgPath);
                    if (inStream.available() > 0) {
                        imgStream = ImageIOFile.createImageInputStream(inStream, bandDimensions);
                    } else {
                        inStream.close();
                    }
                }
                if(imgStream != null) {
                    final ImageIOFile img = new ImageIOFile(name, imgStream, GeoTiffUtils.getTiffIIOReader(imgStream),
                            1, 1, ProductData.TYPE_INT32, productInputFile);
                    bandImageFileMap.put(img.getName(), img);

                    ProductReader reader = geoTiffPlugIn.createReaderInstance();
                    bandProduct = reader.readProductNodes(productDir.getFile(imgPath), null);
                }
            } catch (Exception e) {
                SystemUtils.LOG.severe(imgPath + " not found");
            }
        }
    }

    @Override
    protected void addBands(final Product product) {

        double NoDataValue = 0;

        for (Map.Entry<String, ImageIOFile> stringImageIOFileEntry : bandImageFileMap.entrySet()) {
            final ImageIOFile img = stringImageIOFileEntry.getValue();
            int numBands = img.getNumBands();
            String suffix = "";

            if (polarisations.length == 1) {
                suffix = polarisations[0];
            } else {
                for (String p : polarisations) {
                    if (img.getName().contains(p)) {
                        suffix = p;
                        break;
                    }
                }
            }

            if (isSLC()) {
                numBands *= 2; // real + imaginary
            }

            String bandName;
            boolean real = true;
            Band lastRealBand = null;
            if (isSLC()) {
                String unit;

                for (int b = 0; b < numBands; ++b) {
                    if (real) {
                        bandName = "i" + '_' + suffix;
                        unit = Unit.REAL;
                    } else {
                        bandName = "q" + '_' + suffix;
                        unit = Unit.IMAGINARY;
                    }

                    final Band band = new Band(bandName, ProductData.TYPE_FLOAT32, width, height);
                    band.setUnit(unit);
                    band.setNoDataValueUsed(true);
                    band.setNoDataValue(NoDataValue);

                    product.addBand(band);
                    final ImageIOFile.BandInfo bandInfo = new ImageIOFile.BandInfo(band, img, 0, b);
                    bandMap.put(band, bandInfo);

                    if (real) {
                        lastRealBand = band;
                    } else {
                        ReaderUtils.createVirtualIntensityBand(product, lastRealBand, band, '_' + suffix);
                        bandInfo.setRealBand(lastRealBand);
                        bandMap.get(lastRealBand).setImaginaryBand(band);
                    }
                    real = !real;

                    // reset to null so it doesn't adopt a geocoding from the bands
                    product.setSceneGeoCoding(null);
                }
            } else {
                for (int b = 0; b < numBands; ++b) {
                    bandName = "Amplitude" + '_' + suffix;
                    final Band band = new Band(bandName, ProductData.TYPE_FLOAT32, width, height);
                    band.setUnit(Unit.AMPLITUDE);
                    band.setNoDataValueUsed(true);
                    band.setNoDataValue(NoDataValue);

                    product.addBand(band);
                    bandMap.put(band, new ImageIOFile.BandInfo(band, img, 0, b));

                    SARReader.createVirtualIntensityBand(product, band, '_' + suffix);

                    // reset to null so it doesn't adopt a geocoding from the bands
                    product.setSceneGeoCoding(null);
                }
            }
        }
    }

    private Gaofen3Geocoding getGeoCoding(final Product product) {
        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement productMetadata = origProdRoot.getElement("product");
        final MetadataElement imageInfoMetadata = productMetadata.getElement("imageinfo");
        final double[] centerGeoPos = new double[] {
                imageInfoMetadata.getElement("center").getAttributeDouble("latitude"),
                imageInfoMetadata.getElement("center").getAttributeDouble("longitude")
        };
        return new Gaofen3Geocoding(
                0.5f, 0.5f, 1.0f, 1.0f, rpcParameters, centerGeoPos
        );
    }

    private static GeoPos computeLatLon(
            final double latMid, final double lonMid, double slrgTime, Orbits.OrbitVector data) {

        final double[] xyz = new double[3];
        final GeoPos geoPos = new GeoPos(latMid, lonMid);

        // compute initial (x,y,z) coordinate from lat/lon
        GeoUtils.geo2xyz(geoPos, xyz);

        // compute accurate (x,y,z) coordinate using Newton's method
        GeoUtils.computeAccurateXYZ(data, xyz, slrgTime);

        // compute (lat, lon, alt) from accurate (x,y,z) coordinate
        GeoUtils.xyz2geo(xyz, geoPos);

        return geoPos;
    }

    @Override
    protected void addGeoCoding(final Product product) {
        product.setSceneGeoCoding(getGeoCoding(product));
    }

    @Override
    protected void addTiePointGrids(final Product product) {
        addTiePointGridInc(product);
        addTiePointGridLatLon(product);
        addTiePointGridSlantRangeTime(product);
    }

    private void addTiePointGridInc(final Product product) {

        if(incidenceAngleList != null) {
            final int xStep = 10;
            final int yStep = 1;
            final int gridWidth = Math.max(incidenceAngleList.length / xStep, 2);
            final int gridHeight = Math.max(height / yStep, 2);
            final float[] incidenceAngleListRep = new float[gridWidth * gridHeight];
            int cnt = 0;
            for (int i = 0; i < gridHeight; i++) {
                for (int j = 0; j < gridWidth; j++) {
                    incidenceAngleListRep[cnt] = (float) incidenceAngleList[j * xStep];
                    cnt++;
                }
            }

            final float subSamplingX = width / (float) (gridWidth - 1);
            final float subSamplingY = height / (float) (gridHeight - 1);

            if (product.getTiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE) == null) {
                final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE,
                        gridWidth, gridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, incidenceAngleListRep);
                incidentAngleGrid.setUnit(Unit.DEGREES);
                product.addTiePointGrid(incidentAngleGrid);
            }
        }
    }

    private void addTiePointGridLatLon(final Product product) {

        final Gaofen3Geocoding geocoding = getGeoCoding(product);
        final int gridWidth = 50;
        final int gridHeight = 50;
        final float xStep = width / (float) (gridWidth - 1);
        final float yStep = height / (float) (gridHeight - 1);
        final float[] latList = new float[gridWidth * gridHeight];
        final float[] lonList = new float[gridWidth * gridHeight];
        int cnt = 0;
        for (int i = 0; i < gridHeight; i++) {
            for (int j = 0; j < gridWidth; j++) {
                final double[] geo = geocoding.pixel2geo(j*xStep, i*yStep);
                latList[cnt] = (float) geo[0];
                lonList[cnt] = (float) geo[1];
                cnt++;
            }
        }

        if (product.getTiePointGrid(OperatorUtils.TPG_LATITUDE) == null) {
            final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE,
                    gridWidth, gridHeight, 0.5f, 0.5f, xStep,  yStep, latList);
            latGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(latGrid);
        }

        if (product.getTiePointGrid(OperatorUtils.TPG_LONGITUDE) == null) {
            final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE,
                    gridWidth, gridHeight, 0.5f, 0.5f, xStep,  yStep, lonList);
            lonGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(lonGrid);
        }

    }

    private void addTiePointGridSlantRangeTime(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        final int gridWidth = 50;
        final int gridHeight = 50;
        final float xStep = width / (float) (gridWidth - 1);
        final float yStep = height / (float) (gridHeight - 1);
        final float[] srgList = new float[gridWidth * gridHeight];
        final double srgFirst = Constants.oneBillion*
                absRoot.getAttributeDouble(AbstractMetadata.slant_range_to_first_pixel)/
                Constants.halfLightSpeed; //ns
        final double xSampleFreq = absRoot.getAttributeDouble(
                AbstractMetadata.range_sampling_rate)*Constants.oneMillion; //Hz
        final double dx = Constants.oneBillion/xSampleFreq; //ns
        int cnt = 0;
        for (int i = 0; i < gridHeight; i++) {
            for (int j = 0; j < gridWidth; j++) {
                srgList[cnt] = (float) (srgFirst + j*xStep*dx);
                cnt++;
            }
        }

        if (product.getTiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME) == null) {
            final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME,
                    gridWidth, gridHeight, 0.5f, 0.5f, xStep,  yStep, srgList);
            latGrid.setUnit(Unit.NANOSECONDS);
            product.addTiePointGrid(latGrid);
        }

    }

    protected void fixAzimuthRangeOffsets_onepoint(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final Gaofen3Geocoding geocoding = getGeoCoding(product);
        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);

        double f0 = 0.5;
        double[] idxRef0 = {width*f0, height*f0};
        double[] geoRef0 = geocoding.pixel2geo(idxRef0[0],idxRef0[1]);

        SystemUtils.LOG.info(String.format("Correction ref. point 1 : geo=(%f, %f), idx=(%f, %f)",
                geoRef0[0], geoRef0[1], idxRef0[0], idxRef0[1]));

        final double[] idxRpc0 = geocoding.geo2pixel(geoRef0[0], geoRef0[1]);

        double averageSatSpeed = origProdRoot.getElement("product").
                getElement("platform").getAttributeDouble("satVelocity");

        final int maxIter = 10;
        boolean converged = false;
        for (int i = 0; i < maxIter; i++) {
            final double[] idxRd0 = geo2pixelRangeDoppler(product, geoRef0[0], geoRef0[1], 0.0);

            // --- Fix range --- //
            final double dx = absRoot.getAttributeDouble(AbstractMetadata.range_spacing);  //meter
            final double corRg0 = (idxRd0[0] - idxRpc0[0])*dx;

            // ... slant_range_to_first_pixel:
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.slant_range_to_first_pixel,
                    absRoot.getAttributeDouble(AbstractMetadata.slant_range_to_first_pixel) + corRg0);

            // --- Fix azimuth --- //
            final double dy = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval); //sec
            final double corAz0 = (idxRd0[1] - idxRpc0[1])*dy;
            final double corAz0meters = corAz0*averageSatSpeed;

            // ... first_line_time:
            final double cor_first_line_time_MJD = corAz0/Constants.secondsInDay;
            final double first_line_time_MJD = AbstractMetadata.parseUTC(
                    absRoot.getAttributeString(AbstractMetadata.first_line_time)).getMJD();
            final ProductData.UTC first_line_time_corrected = new ProductData.UTC(
                    first_line_time_MJD + cor_first_line_time_MJD);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_line_time, first_line_time_corrected);

            // ... last_line_time:
            final double cor_last_line_time_MJD = cor_first_line_time_MJD;
            final double last_line_time_MJD = AbstractMetadata.parseUTC(
                    absRoot.getAttributeString(AbstractMetadata.last_line_time)).getMJD();
            final ProductData.UTC last_line_time_corrected = new ProductData.UTC(
                    last_line_time_MJD + cor_last_line_time_MJD);
            AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_line_time, last_line_time_corrected);

            // Log:
            final String msg = String.format("Range/Azimuth correction, iteration %d (max %d)...\n", i+1, maxIter) +
                    String.format("\tRange correction: %f m (at point 1)\n", corRg0) +
                    String.format("\tAzimuth correction: %f m (at point 1)\n", corAz0meters) +
                    String.format("\tfirst_line_time corrected (UTC): %s sec\n",
                            first_line_time_corrected.format()) +
                    String.format("\tlast_line_time corrected (UTC): %s sec\n",
                            last_line_time_corrected.format()) +
                    String.format("\tfirst_line_time correction: %f sec\n",
                            cor_first_line_time_MJD*Constants.secondsInDay) +
                    String.format("\tlast_line_time correction: %f sec\n",
                            cor_last_line_time_MJD*Constants.secondsInDay);
            SystemUtils.LOG.info(msg);
            if (
                    Math.abs(corAz0meters) < 0.1 &&
                            Math.abs(corRg0) < 0.1
            ) {
                converged = true;
                break;
            }
        }

        if (!converged){
            SystemUtils.LOG.severe("Range/azimuth offset correction did not converge!");
        }

    }

    private double[] geo2pixelRangeDoppler(final Product product, final double lat, final double lon, final double alt) {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        assert absRoot != null;

        final String firstLineTimeString = absRoot.getAttributeString(AbstractMetadata.first_line_time);
        final ProductData.UTC firstLineTimeUtc = AbstractMetadata.parseUTC(firstLineTimeString);
        final double firstLineTimeMjd = firstLineTimeUtc.getMJD();
        final double firstLineTimeSec = DateUtils.dateTimeToSecOfDay(firstLineTimeUtc.toString());

        final double lineTimeIntervalInSecs = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval);
        final double lineTimeIntervalInDays = lineTimeIntervalInSecs/Constants.secondsInDay;

        final double rangeSpacing = absRoot.getAttributeDouble(AbstractMetadata.range_spacing);
        final double nearEdgeSlantRange = absRoot.getAttributeDouble(AbstractMetadata.slant_range_to_first_pixel);
        final double wavelength = Constants.lightSpeed/absRoot.getAttributeDouble(AbstractMetadata.radar_frequency)/
                Constants.oneMillion;

        List<PosVector> sensorPosition = new ArrayList<>();
        List<PosVector> sensorVelocity = new ArrayList<>();
        final Orbit orbit = getPolyOrbit(product);
        final OrbitStateVector[] stateVectors = new OrbitStateVector[height];
        for (int i = 0; i < height; i++) {
            double azTimeSec = firstLineTimeSec + i*lineTimeIntervalInSecs;
            double azTimeMjd = firstLineTimeMjd + i*lineTimeIntervalInDays;
            final ProductData.UTC utcTime = new ProductData.UTC(azTimeMjd);
            final Point pos = orbit.getXYZ(azTimeSec);
            final Point vel = orbit.getXYZDot(azTimeSec);

            sensorPosition.add(new PosVector(pos.getX(), pos.getY(), pos.getZ()));
            sensorVelocity.add(new PosVector(vel.getX(), vel.getY(), vel.getZ()));
            stateVectors[i] = new OrbitStateVector(
                    utcTime, pos.getX(), pos.getY(), pos.getZ(), vel.getX(), vel.getY(), vel.getZ()
            );
        }
        final OrbitStateVectors orbitStateVectors = new OrbitStateVectors(
                stateVectors, firstLineTimeMjd, lineTimeIntervalInDays, height);

        final PosVector xyz = new PosVector();
        GeoUtils.geo2xyzWGS84(lat, lon, alt, xyz);
        final PosVector[] sensorPositionArray = new PosVector[height];
        final PosVector[] sensorVelocityArray = new PosVector[height];
        sensorPosition.toArray(sensorPositionArray);
        sensorVelocity.toArray(sensorVelocityArray);
        double zeroDopplerTime = SARGeocoding.getEarthPointZeroDopplerTime(firstLineTimeMjd,
                lineTimeIntervalInDays, wavelength, xyz, sensorPositionArray, sensorVelocityArray);
        final PosVector xysSensor = new PosVector();
        double slantRange = SARGeocoding.computeSlantRange(zeroDopplerTime, orbitStateVectors, xyz, xysSensor);

        final double rangeIndex = (slantRange - nearEdgeSlantRange) / rangeSpacing;
        final double azimuthIndex = (zeroDopplerTime - firstLineTimeMjd) / lineTimeIntervalInDays;

        return new double[] {rangeIndex, azimuthIndex};
    }

    @Override
    public Product createProduct() throws IOException {

        final MetadataElement newRoot = addMetaData();
        findImages(newRoot);

        final MetadataElement absRoot = newRoot.getElement(AbstractMetadata.ABSTRACT_METADATA_ROOT);
        final int sceneWidth = absRoot.getAttributeInt(AbstractMetadata.num_samples_per_line);
        final int sceneHeight = absRoot.getAttributeInt(AbstractMetadata.num_output_lines);

        final Product product = new Product(getProductName(), getProductType(), sceneWidth, sceneHeight);
        updateProduct(product, newRoot);
        addBands(product);
        addGeoCoding(product);
        addMetadataCorners(product);
        addTiePointGrids(product);
        ReaderUtils.addMetadataIncidenceAngles(product);
        ReaderUtils.addMetadataProductSize(product);

        SystemUtils.LOG.info("Fix range/azimuth offsets from RPC... ");
        fixAzimuthRangeOffsets_onepoint(product);

        SystemUtils.LOG.info("Reconstructing state vectors from RPC... ");
        //addStateVectorsFromMeta(product);
        constructStateVectorsFromRPC(product);

        return product;
    }
}
