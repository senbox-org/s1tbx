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
import org.esa.s1tbx.insar.gpf.Sentinel1Utils;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.DownloadableArchive;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.esa.snap.gpf.InputProductValidator;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.gpf.TileIndex;
import org.esa.snap.util.ProductUtils;
import org.esa.snap.util.Settings;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Rectangle;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This operator performs Elevation Antenna Phase correction for S-1 SLC product with IPF version < v2.43.
 */

@OperatorMetadata(alias = "EAP-Phase-Correction",
        category = "Radar/Sentinel-1 TOPS",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "EAP Phase Correction")
public final class EAPPhaseCorrectionOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    private MetadataElement absRoot = null;
    private String acquisitionMode = null;
    private Sentinel1Utils su = null;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private File auxCalFile = null;
    protected final HashMap<String, String[]> targetBandNameToSourceBandName = new HashMap<>(2);
    private final HashMap<String, EAPVector> swathPolToEAPVector = new HashMap<>();

    private final static DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyyMMdd-HHmmss");


    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public EAPPhaseCorrectionOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.snap.framework.datamodel.Product}
     * annotated with the {@link TargetProduct TargetProduct} annotation or
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
            checkSourceProductValidity();

            getSourceMetadata();

            retrieveAuxCalFile();

            readAuxCalFile();

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void checkSourceProductValidity() throws OperatorException {

        final InputProductValidator validator = new InputProductValidator(sourceProduct);
        validator.checkIfSentinel1Product();
        validator.checkProductType(new String[]{"SLC"});

        absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final String procSysId = absRoot.getAttributeString(AbstractMetadata.ProcessingSystemIdentifier);
        final float version = Float.valueOf(procSysId.substring(procSysId.lastIndexOf(" ")));
        if (version >= 2.43) {
            throw new OperatorException("EAP phase correction has already been performed for the Sentinel1 product");
        }
    }

    /**
     * Get source metadata
     */
    private void getSourceMetadata() throws Exception {

        su = new Sentinel1Utils(sourceProduct);
        subSwath = su.getSubSwath();
        acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
    }

    /**
     * Create target product.
     */
    void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();
    }

    private void addSelectedBands() {

        final Band[] sourceBands = sourceProduct.getBands();
        for (int i = 0; i < sourceBands.length; i++) {

            final Band srcBandI = sourceBands[i];
            if (srcBandI instanceof VirtualBand) {
                continue;
            }

            final String unit = srcBandI.getUnit();
            String nextUnit = null;
            if (unit == null) {
                throw new OperatorException("band " + srcBandI.getName() + " requires a unit");
            } else if (unit.contains(Unit.IMAGINARY)) {
                throw new OperatorException("I and Q bands should be in pairs");
            } else if (unit.contains(Unit.REAL)) {
                if (i + 1 >= sourceBands.length) {
                    throw new OperatorException("I and Q bands should be in pairs");
                }
                nextUnit = sourceBands[i + 1].getUnit();
                if (nextUnit == null || !nextUnit.contains(Unit.IMAGINARY)) {
                    throw new OperatorException("I and Q bands should be in pairs");
                }
            } else {
                throw new OperatorException("I and Q bands are not in pairs");
            }

            final Band srcBandQ = sourceBands[i + 1];
            final String[] srcBandNames = {srcBandI.getName(), srcBandQ.getName()};
            targetBandNameToSourceBandName.put(srcBandNames[0], srcBandNames);
            final Band targetBandI = new Band(srcBandNames[0],
                    ProductData.TYPE_FLOAT32,
                    srcBandI.getSceneRasterWidth(),
                    srcBandI.getSceneRasterHeight());
            targetBandI.setUnit(unit);
            targetBandI.setNoDataValueUsed(true);
            targetProduct.addBand(targetBandI);

            targetBandNameToSourceBandName.put(srcBandNames[1], srcBandNames);
            final Band targetBandQ = new Band(srcBandNames[1],
                    ProductData.TYPE_FLOAT32,
                    srcBandQ.getSceneRasterWidth(),
                    srcBandQ.getSceneRasterHeight());
            targetBandQ.setUnit(nextUnit);
            targetBandQ.setNoDataValueUsed(true);
            targetProduct.addBand(targetBandQ);

            final String suffix = "_" + OperatorUtils.getSuffixFromBandName(srcBandI.getName());
            ReaderUtils.createVirtualIntensityBand(targetProduct, targetBandI, targetBandQ, suffix);
            i++;
        }
    }

    private void retrieveAuxCalFile() throws Exception {

        final double procTime = absRoot.getAttributeUTC(AbstractMetadata.PROC_TIME).getMJD();
        final Calendar calendar = absRoot.getAttributeUTC(AbstractMetadata.PROC_TIME).getAsCalendar();
        int year = calendar.get(Calendar.YEAR);

        auxCalFile = findAuxCalFile(procTime, year);

        if(auxCalFile == null) {
            getRemoteFiles(year);
            auxCalFile = findAuxCalFile(procTime, year);
            if(auxCalFile == null) {
                --year;
                getRemoteFiles(year);
                auxCalFile = findAuxCalFile(procTime, year);
                if (auxCalFile == null) {
                    String timeStr = absRoot.getAttributeUTC(AbstractMetadata.PROC_TIME).format();
                    throw new OperatorException("No valid AUX_CAL file found for " + timeStr);
                }
            }
        }

        if (!auxCalFile.exists()) {
            throw new IOException("EAPPhaseCorrection: Unable to find AUX_CAL file");
        }
    }

    private File findAuxCalFile(final double time, final int year) {

        final String prefix;
        prefix = "S1A_AUX_CAL_";
        final File auxCalFileFolder = new File(
                Settings.getPath("AuxCalFiles.sentinel1AuxCalPath") + File.separator + year);

        final File[] files = auxCalFileFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                name = name.toUpperCase();
                return (name.endsWith(".ZIP") || name.endsWith(".SAFE")) && name.startsWith(prefix);
            }
        });
        if(files == null || files.length == 0)
            return null;

        File auxFile = null;
        double minTimeDuration = Double.MAX_VALUE;
        for(File file : files) {
            try {
                final String filename = file.getName();
                final ProductData.UTC valStart = getValidityStartFromFilenameUTC(filename);
                if(valStart != null) {
                    final double duration = time - valStart.getMJD();
                    if(duration > 0.0 && duration < minTimeDuration) {
                        auxFile = file;
                        minTimeDuration = duration;
                    }
                }
            } catch (ParseException ignored) {
            }
        }
        return auxFile;
    }

    private static ProductData.UTC getValidityStartFromFilenameUTC(String filename) throws ParseException {

        if (filename.substring(12,13).equals("V")) {

            String val = extractTimeFromFilename(filename, 13);
            return ProductData.UTC.parse(val, dateFormat);
        }
        return null;
    }

    private static String extractTimeFromFilename(final String filename, final int offset) {

        return filename.substring(offset,offset+15).replace("T","-");
    }

    private void getRemoteFiles(final int year) throws Exception {

        final File localFolder = new File(Settings.getPath("AuxCalFiles.sentinel1AuxCalPath"), String.valueOf(year));
        final URL remotePath = new URL(Settings.instance().getPath("AuxCalFiles.sentinel1AuxCal_remotePath"));

        final File localFile = new File(localFolder, year + ".zip");
        final DownloadableArchive archive = new DownloadableArchive(localFile, remotePath);
        archive.getContentFiles();
    }

    private void readAuxCalFile() throws Exception {

        try {
            final DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

            final Document doc;
            if(auxCalFile.getName().toLowerCase().endsWith(".zip")) {
                final ZipFile productZip = new ZipFile(auxCalFile, ZipFile.OPEN_READ);
                final Enumeration<? extends ZipEntry> entries = productZip.entries();
                final ZipEntry folderEntry = entries.nextElement();
                final ZipEntry zipEntry = productZip.getEntry(folderEntry.getName()+"data/s1a-aux-cal.xml");

                InputStream is = productZip.getInputStream(zipEntry);
                doc = documentBuilder.parse(is);
            } else {
                doc = documentBuilder.parse(auxCalFile);
            }

            if (doc == null) {
                System.out.println("EAPPhaseCorrection: failed to create Document for AUX_CAL file");
                return;
            }

            doc.getDocumentElement().normalize();

            final org.w3c.dom.Node calibrationParamsListNode =
                    doc.getElementsByTagName("auxiliaryCalibration").item(0).getChildNodes().item(1);

            org.w3c.dom.Node childNode = calibrationParamsListNode.getFirstChild();
            while (childNode != null) {
                if (childNode.getNodeName().equals("calibrationParams")) {
                    final String swath = getNodeTextContent(childNode, "swath");
                    if (swath != null && swath.contains(acquisitionMode)) {
                        final String pol = getNodeTextContent(childNode, "polarisation");
                        readOneEAPVector(childNode, swath, pol.toUpperCase());
                    }
                }
                childNode = childNode.getNextSibling();
            }

        } catch (IOException e) {
            System.out.println("EAPPhaseCorrection: IOException " + e.getMessage());
        } catch (ParserConfigurationException e) {
            System.out.println("EAPPhaseCorrection: ParserConfigurationException " + e.getMessage());
        } catch (SAXException e) {
            System.out.println("EAPPhaseCorrection: SAXException " + e.getMessage());
        } catch (Exception e) {
            System.out.println("EAPPhaseCorrection: Exception " + e.getMessage());
        }
    }

    private String getNodeTextContent(final org.w3c.dom.Node node, final String nodeName) {

        org.w3c.dom.Node childNode = getChildNode(node, nodeName);
        if (childNode != null) {
            return childNode.getTextContent();
        }
        return null;
    }

    private org.w3c.dom.Node getChildNode(final org.w3c.dom.Node node, final String childNodeName) {

        org.w3c.dom.Node childNode = node.getFirstChild();
        while (childNode != null) {
            if (childNode.getNodeName().equals(childNodeName)) {
                return childNode;
            }
            childNode = childNode.getNextSibling();
        }
        return null;
    }

    private void readOneEAPVector(final org.w3c.dom.Node node, final String swath, final String pol) throws Exception {

        org.w3c.dom.Node elevationAntennaPatternNode = getChildNode(node, "elevationAntennaPattern");
        org.w3c.dom.Node elevationAngleIncrementNode = getChildNode(elevationAntennaPatternNode, "elevationAngleIncrement");
        final double elevationAngleIncrement = Double.parseDouble(elevationAngleIncrementNode.getTextContent());

        org.w3c.dom.Node eapNode = getChildNode(elevationAntennaPatternNode, "values");
        final org.w3c.dom.Node attrCount = getAttributeFromNode(eapNode, "count");
        final int count = Integer.parseInt(attrCount.getTextContent());
        final double[] eap = new double[2*count];

        final String eapArrayStr = eapNode.getTextContent();
        if (!eapArrayStr.isEmpty()) {
            final StringTokenizer st = new StringTokenizer(eapArrayStr);
            int k = 0;
            while (st.hasMoreTokens()) {
                eap[k++] = Double.parseDouble(st.nextToken());
            }

            if (k != 2*count) {
                throw new IOException("EAPPhaseCorrection: Complex elevation antenna pattern is required in AUX_CAL file");
            }
        }

        final String key = swath + "_" + pol;
        EAPVector eapVector = new EAPVector(swath, pol, elevationAngleIncrement, eap);
        swathPolToEAPVector.put(key, eapVector);
    }

    private org.w3c.dom.Node getAttributeFromNode(final org.w3c.dom.Node node, final String attrName) {

        NamedNodeMap attr = node.getAttributes();
        org.w3c.dom.Node attrNode = null;
        for (int j = 0; j < attr.getLength(); j++) {
            if (attr.item(j).getNodeName().equals(attrName)) {
                if (attrNode == null) {
                    attrNode = attr.item(j);
                } else {
                    System.out.println("EAPPhaseCorrection: more than one " + attrName + " in " + node.getNodeName());
                }
            }
        }
        return attrNode;
    }



    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancellation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Rectangle targetRectangle = targetTile.getRectangle();
            final int tx0 = targetRectangle.x;
            final int ty0 = targetRectangle.y;
            final int tw = targetRectangle.width;
            final int th = targetRectangle.height;
            final int tyMax = ty0 + th;
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            final int subSwathIndex = getSubSwathIndex(targetBand.getName());
            final String polarization = getPolarization(targetBand.getName());

            for (int burstIndex = 0; burstIndex < subSwath[subSwathIndex - 1].numOfBursts; burstIndex++) {
                final int firstLineIdx = burstIndex*subSwath[subSwathIndex - 1].linesPerBurst;
                final int lastLineIdx = firstLineIdx + subSwath[subSwathIndex - 1].linesPerBurst - 1;

                if (tyMax <= firstLineIdx || ty0 > lastLineIdx) {
                    continue;
                }

                final int nty0 = Math.max(ty0, firstLineIdx);
                final int ntyMax = Math.min(tyMax, lastLineIdx + 1);
                final int nth = ntyMax - nty0;
                //System.out.println("burstIndex = " + burstIndex + ": ntx0 = " + tx0 + ", nty0 = " + nty0 + ", ntw = " + tw + ", nth = " + nth);

                computeTileForOneBurst(
                        subSwathIndex, burstIndex, polarization, tx0, nty0, tw, nth, targetBand, targetTile);
            }

        } catch (Exception e) {
            OperatorUtils.catchOperatorException(this.getId(), e);
        }
    }

    private int getSubSwathIndex(final String targetBandName) {
        for (int i = 0; i < 5; i++) {
            if (targetBandName.contains(String.valueOf(i+1))){
                return (i+1);
            }
        }
        return -1;
    }

    private String getPolarization(final String targetBandName) {

        final String targetBandNameInLowerCase = targetBandName.toLowerCase();
        if (targetBandNameInLowerCase.contains("hh")) {
            return "HH";
        } else if (targetBandNameInLowerCase.contains("hv")) {
            return "HV";
        } else if (targetBandNameInLowerCase.contains("vh")) {
            return "VH";
        } else if (targetBandNameInLowerCase.contains("vv")) {
            return "VV";
        } else {
            return null;
        }
    }

    private void computeTileForOneBurst(final int subSwathIndex, final int burstIndex, final String polarization,
                                        final int x0, final int y0, final int w, final int h, final Band targetBand,
                                        final Tile targetTile) {

        final double rollSteeringAngle = computeRollSteeringAngle(subSwathIndex, burstIndex);

        final Unit.UnitType tgtBandUnit = Unit.getUnitType(targetBand);
        final Rectangle sourceRectangle = new Rectangle(x0, y0, w, h);
        final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
        final Band sourceBandI = sourceProduct.getBand(srcBandNames[0]);
        final Band sourceBandQ = sourceProduct.getBand(srcBandNames[1]);
        final Tile sourceRasterI = getSourceTile(sourceBandI, sourceRectangle);
        final Tile sourceRasterQ = getSourceTile(sourceBandQ, sourceRectangle);
        ProductData srcDataI = sourceRasterI.getDataBuffer();
        ProductData srcDataQ = sourceRasterQ.getDataBuffer();

        final ProductData tgtData = targetTile.getDataBuffer();
        final TileIndex srcIndex = new TileIndex(sourceRasterI);
        final TileIndex trgIndex = new TileIndex(targetTile);

        final String key = acquisitionMode + subSwathIndex + "_" + polarization;
        final EAPVector eapVector = swathPolToEAPVector.get(key);

        final int yMax = y0 + h;
        final int xMax = x0 + w;
        int srcIdx;
        final double[] eap = new double[2];
        double val = targetBand.getNoDataValue();
        for (int y = y0; y < yMax; y++) {
            srcIndex.calculateStride(y);
            trgIndex.calculateStride(y);

            for (int x = x0; x < xMax; x++) {

                final double slantRangeTime = su.getSlantRangeTime(x, subSwathIndex) * 2.0; // 1-way to 2-way
                final double elevationAngle = computeElevationAngle(subSwathIndex, burstIndex, slantRangeTime);
                if (elevationAngle == -1.0) {
                    continue;
                }

                computeEAP(elevationAngle, rollSteeringAngle, eapVector, eap);

                srcIdx = srcIndex.getIndex(x);
                if (tgtBandUnit == Unit.UnitType.REAL) {
                    val = (srcDataI.getElemDoubleAt(srcIdx)*eap[0] + srcDataQ.getElemDoubleAt(srcIdx)*eap[1])
                            / Math.sqrt(eap[0]*eap[0] + eap[1]*eap[1]);
                } else if (tgtBandUnit == Unit.UnitType.IMAGINARY) {
                    val = (srcDataQ.getElemDoubleAt(srcIdx)*eap[0] - srcDataI.getElemDoubleAt(srcIdx)*eap[1])
                            / Math.sqrt(eap[0]*eap[0] + eap[1]*eap[1]);
                }
                tgtData.setElemDoubleAt(trgIndex.getIndex(x), val);
            }
        }
    }

    private double computeRollSteeringAngle(final int subSwathIndex, final int burstIndex) {

        final double ascendingNodeTime = subSwath[subSwathIndex - 1].ascendingNodeTime;
        final double burstFirstLineTime = subSwath[subSwathIndex - 1].burstFirstLineTime[burstIndex];
        final double satelliteAltitude = computeSatelliteAltitude(ascendingNodeTime, burstFirstLineTime);
        return computeRollSteeringAngle(satelliteAltitude);
    }

    private static double computeSatelliteAltitude(final double ascendingNodeTime, final double burstFirstLineTime) {

        final double h0 = 707714.8; // m
        final double h1 = 8351.5; // m
        final double h2 = 8947.9; // m
        final double h3 = 23.32; // m
        final double h4 = 11.74; // m
        final double phi1 = 3.1495; // rad
        final double phi2 = -1.5655; // rad
        final double phi3 = -3.1297; // rad
        final double phi4 = 4.7222; // rad
        final double omega = 2.0*Math.PI / 5924.57; // rad/s (175 orbit / 12 days)
        final double delta = burstFirstLineTime - ascendingNodeTime;
        final double omegaByDelta = omega*delta;

        return h0 + h1* FastMath.sin(omegaByDelta + phi1) + h2*FastMath.sin(2.0*omegaByDelta + phi2) +
                h3*FastMath.sin(3.0*omegaByDelta + phi3) + h4*FastMath.sin(4.0*omegaByDelta + phi4);
    }

    private static double computeRollSteeringAngle(final double satelliteAltitude) {

        final double thetaRef = 29.450; // deg
        final double hRef = 711.70; // km
        final double alphaRoll = 0.05660; // deg
        return thetaRef + alphaRoll*(satelliteAltitude/1000.0 - hRef);
    }

    private double computeElevationAngle(final int subSwathIndex, final int burstIndex, final double slantRangeTime) {

        final double[] slantRangeTimeArray = subSwath[subSwathIndex - 1].apSlantRangeTime[burstIndex];
        final double[] elevationAngleArray = subSwath[subSwathIndex - 1].apElevationAngle[burstIndex];

        if (slantRangeTime < slantRangeTimeArray[0] || slantRangeTime > slantRangeTimeArray[slantRangeTimeArray.length - 1]) {
            return -1.0;
        }

        double slrt0 = 0.0, slrt1 = 0.0, theta0 = 0.0, theta1 = 0.0;
        for (int i = 0; i < slantRangeTimeArray.length; i++) {
            slrt1 = slantRangeTimeArray[i];
            theta1 = elevationAngleArray[i];
            if (slantRangeTime <= slrt1) {
                break;
            }
            slrt0 = slrt1;
            theta0 = theta1;

        }

        if (slrt0 == 0.0 || slrt1 == 0.0) {
            return -1.0;
        }

        final double lambda = (slantRangeTime - slrt0) / (slrt1 - slrt0);

        return (1 - lambda)*theta0 + lambda*theta1;
    }

    private void computeEAP(final double elevationAngle, final double rollSteeringAngle,
                            final EAPVector eapVector, final double[] eap) {

        final int i0 = (int)((elevationAngle - rollSteeringAngle) / eapVector.elevationAngleIncrement +
                (eapVector.count - 1) /2.0);

        final double elevationAngle0 = (i0 - (eapVector.count - 1) /2.0)*eapVector.elevationAngleIncrement +
                rollSteeringAngle;

        final int i1 = i0 + 1;
        final double eapI0 = eapVector.eapI[i0];
        final double eapI1 = eapVector.eapI[i1];
        final double eapQ0 = eapVector.eapQ[i0];
        final double eapQ1 = eapVector.eapQ[i1];

        final double lambda = (elevationAngle - elevationAngle0) / eapVector.elevationAngleIncrement;
        eap[0] = (1 - lambda)*eapI0 + lambda*eapI1;
        eap[1] = (1 - lambda)*eapQ0 + lambda*eapQ1;
    }


    public final static class EAPVector {
        public String swath;
        public String polarization;
        public double elevationAngleIncrement;
        public int count;
        public double[] eapI;
        public double[] eapQ;

        public EAPVector(final String swath, final String polarization, final double elevationAngleIncrement,
                         final double[] eap) {

            this.swath = swath;
            this.polarization = polarization;
            this.elevationAngleIncrement = elevationAngleIncrement;

            count = eap.length/2;
            this.eapI = new double[count];
            this.eapQ = new double[count];
            for (int i = 0; i < count; i++) {
                this.eapI[i] = eap[2*i];
                this.eapQ[i] = eap[2*i + 1];
            }
        }
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(EAPPhaseCorrectionOp.class);
        }
    }
}
