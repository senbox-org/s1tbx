/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.eo.Constants;

import java.awt.*;
import java.text.ParseException;
import java.util.*;
import java.util.List;

/**
 * De-Burst a WSS product
 */
@OperatorMetadata(alias = "DeburstWSS",
        category = "SAR Tools",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description="Debursts an ASAR WSS product")
public final class DeburstWSSOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = { SS1, SS2, SS3, SS4, SS5 }, defaultValue = SS1, label="Sub Swath:")
    private String subSwath = SS1;

    @Parameter(defaultValue = "false", label="Produce Intensities Only")
    private boolean produceIntensitiesOnly = false;
    @Parameter(defaultValue = "false", label="Mean Average Intensities")
    private boolean average = false;

    private final Vector<Integer> startLine = new Vector<Integer>(5);
    private static final double zeroThreshold = 1000;
    private static final double zeroThresholdSmall = 500;
    private LineTime[] lineTimes = null;
    private boolean lineTimesSorted = false;
    private int margin = 50; // edge of target band where not to write
    private double nodatavalue = 0;
    private double lineTimeInterval = 0;

    private final Map<Band, ComplexBand> bandMap = new HashMap<Band, ComplexBand>(5);

    private final static String SS1 = "SS1";
    private final static String SS2 = "SS2";
    private final static String SS3 = "SS3";
    private final static String SS4 = "SS4";
    private final static String SS5 = "SS5";

    private int subSwathNum;
    private int targetWidth;
    private int targetHeight;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public DeburstWSSOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {
        try {
            // check product type
            if (!sourceProduct.getProductType().equals("ASA_WSS_1P")) {
                throw new OperatorException("Source product is not an ASA_WSS_1P");
            }

            final int subSwathBandNum = getRealBandNumFromSubSwath(subSwath);
            subSwathNum = getSubSwathNumber(subSwath);

            getSourceMetadata();

            targetProduct = new Product(sourceProduct.getName() + "_" + subSwath,
                    sourceProduct.getProductType(),
                    targetWidth,
                    targetHeight);

            targetProduct.setPreferredTileSize(targetWidth, 50);

            final Band[] sourceBands = sourceProduct.getBands();

            if (produceIntensitiesOnly) {
                final Band tgtBand = targetProduct.addBand("Intensity_" + subSwath, ProductData.TYPE_FLOAT32);
                tgtBand.setUnit(Unit.INTENSITY);
                tgtBand.setNoDataValueUsed(true);
                tgtBand.setNoDataValue(nodatavalue);
                bandMap.put(tgtBand, new ComplexBand(sourceBands[subSwathBandNum], sourceBands[subSwathBandNum+1]));
            } else {
                final Band trgI = targetProduct.addBand("i_" +subSwath, sourceBands[subSwathBandNum].getDataType());
                trgI.setUnit(Unit.REAL);
                trgI.setNoDataValueUsed(true);
                trgI.setNoDataValue(nodatavalue);
                final Band trgQ = targetProduct.addBand("q_" +subSwath, sourceBands[subSwathBandNum+1].getDataType());
                trgQ.setUnit(Unit.IMAGINARY);
                trgQ.setNoDataValueUsed(true);
                trgQ.setNoDataValue(nodatavalue);
                bandMap.put(trgI, new ComplexBand(sourceBands[subSwathBandNum], sourceBands[subSwathBandNum+1]));
                ReaderUtils.createVirtualIntensityBand(targetProduct, trgI, trgQ, '_'+subSwath);
                ReaderUtils.createVirtualPhaseBand(targetProduct, trgI, trgQ, '_'+subSwath);
            }

            copyMetaData(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
            ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
            targetProduct.setStartTime(sourceProduct.getStartTime());
            targetProduct.setEndTime(sourceProduct.getEndTime());
            targetProduct.setDescription(sourceProduct.getDescription());

            createTiePointGrids();

            // update the metadata with the affect of the processing
            updateTargetProductMetadata();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Compute mean pixel spacing (in m).
     */
    private void getSourceMetadata() {
        final MetadataElement origRoot = AbstractMetadata.getOriginalProductMetadata(sourceProduct);
        final MetadataElement mppRootElem = origRoot.getElement("MAIN_PROCESSING_PARAMS_ADS");
        final MetadataElement mpp = mppRootElem.getElementAt(subSwathNum);

        targetHeight = mpp.getAttributeInt("num_output_lines") / 3;
        targetWidth = mpp.getAttributeInt("num_samples_per_line");

        if(!isBandReady(sourceProduct, subSwathNum)) {
            throw new OperatorException("Time codes for "+subSwath+" not ready yet. Please try again.");
        }
    }

    private static boolean isBandReady(final Product srcProduct, final int subSwathNum) {
        final MetadataElement imgRecElem = srcProduct.getMetadataRoot().getElement("Image Record");
        if(imgRecElem == null) return false;

        final MetadataElement bandElem = imgRecElem.getElement(srcProduct.getBandAt(subSwathNum*2).getName());
        if(bandElem == null) return false;

        final MetadataAttribute attrib = bandElem.getAttribute("t");
        return attrib != null;
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_output_lines, targetHeight);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_samples_per_line, targetWidth);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.SWATH, subSwath);
        if(produceIntensitiesOnly)
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.SAMPLE_TYPE, "DETECTED");

        final MetadataElement srcOrigRoot = AbstractMetadata.getOriginalProductMetadata(sourceProduct);
        final MetadataElement srcMPPRootElem = srcOrigRoot.getElement("MAIN_PROCESSING_PARAMS_ADS");
        final MetadataElement srcMPP = srcMPPRootElem.getElementAt(subSwathNum);

        final ProductData.UTC startTime = srcMPP.getAttributeUTC("first_zero_doppler_time", AbstractMetadata.NO_METADATA_UTC);
        final ProductData.UTC endTime = srcMPP.getAttributeUTC("last_zero_doppler_time", AbstractMetadata.NO_METADATA_UTC);
        absTgt.setAttributeUTC(AbstractMetadata.first_line_time, startTime);
        absTgt.setAttributeUTC(AbstractMetadata.last_line_time, endTime);
        lineTimeInterval = srcMPP.getAttributeDouble(AbstractMetadata.line_time_interval);
        absTgt.setAttributeDouble(AbstractMetadata.line_time_interval, lineTimeInterval);

        final MetadataElement tgtOrigRoot = AbstractMetadata.getOriginalProductMetadata(targetProduct);
        final MetadataElement tgtMppRootElem = tgtOrigRoot.getElement("MAIN_PROCESSING_PARAMS_ADS");
        tgtOrigRoot.removeElement(tgtMppRootElem);
        final MetadataElement tgtmds1RootElem = tgtOrigRoot.getElement("MDS1_SQ_ADS");
        tgtOrigRoot.removeElement(tgtmds1RootElem);

        final MetadataElement srcMDS1RootElem = srcOrigRoot.getElement("MAIN_PROCESSING_PARAMS_ADS");
        final MetadataElement srcMDS1 = srcMDS1RootElem.getElementAt(subSwathNum);

        final MetadataElement tgtMPP = srcMDS1.createDeepClone();
        tgtMPP.setName("MAIN_PROCESSING_PARAMS_ADS");
        tgtOrigRoot.addElement(tgtMPP);

        final MetadataElement tgtMDS1 = srcMPP.createDeepClone();
        tgtMDS1.setName("MDS1_SQ_ADS");
        tgtOrigRoot.addElement(tgtMDS1);

        targetProduct.setStartTime(startTime);
        targetProduct.setEndTime(endTime);

        updateOrbitStateVectors(absTgt, subSwathNum);
    }

    private static void updateOrbitStateVectors(final MetadataElement absTgt, final int subSwathNum) {

        final MetadataElement orbVectorsElem = absTgt.getElement(AbstractMetadata.orbit_state_vectors);
        if(subSwathNum == 0) {
            final int[] nums = { 1, 2, 3, 4, 5 };
            removeAndRenameOrbitStateVectors(orbVectorsElem, nums);
        } else if(subSwathNum == 1) {
            final int[] nums = { 6, 7, 8, 9, 10 };
            removeAndRenameOrbitStateVectors(orbVectorsElem, nums);
        } else if(subSwathNum == 2) {
            final int[] nums = { 11, 12, 13, 14, 15 };
            removeAndRenameOrbitStateVectors(orbVectorsElem, nums);
        } else if(subSwathNum == 3) {
            final int[] nums = { 16, 17, 18, 19, 20 };
            removeAndRenameOrbitStateVectors(orbVectorsElem, nums);
        } else if(subSwathNum == 4) {
            final int[] nums = { 21, 22, 23, 24, 25 };
            removeAndRenameOrbitStateVectors(orbVectorsElem, nums);
        }
    }

    private static void removeAndRenameOrbitStateVectors(final MetadataElement orbVectorsElem, final int[] nums) {
        int i = 1;
        for(int n=1; n <= 25; ++n) {
            final MetadataElement orbElem = orbVectorsElem.getElement(AbstractMetadata.orbit_vector+n);
            if(!contains(nums, n)) {
                orbVectorsElem.removeElement(orbElem);
            } else {
                orbElem.setName(AbstractMetadata.orbit_vector+i);
                ++i;
            }
        }
    }

    private static boolean contains(final int[] nums, final int n) {
        for(int i : nums) {
            if(i == n)
                return true;
        }
        return false;
    }

    private void createTiePointGrids() {

        final MetadataElement origRoot = AbstractMetadata.getOriginalProductMetadata(sourceProduct);
        final MetadataElement geolocRootElem = origRoot.getElement("GEOLOCATION_GRID_ADS");
        final MetadataElement[] geolocElems = geolocRootElem.getElements();
        final MetadataElement mainProcRootElem = origRoot.getElement("MAIN_PROCESSING_PARAMS_ADS");
        final MetadataElement[] mainProcElems = mainProcRootElem.getElements();

        Double lineTimeInterval = 0.0;
        for(MetadataElement mainProcElem : mainProcElems) {
            final String swathStr = mainProcElem.getAttributeString("swath_num");
            if(swathStr.equalsIgnoreCase(subSwath)) {
                lineTimeInterval = mainProcElem.getAttribute("line_time_interval").getData().getElemDouble();
                break;
            }
        }

        int subSamplingX = 1;
        final List<Double> time = new ArrayList<Double>(13);
        final List<Float> lats = new ArrayList<Float>(143);
        final List<Float> lons = new ArrayList<Float>(143);
        final List<Float> slant = new ArrayList<Float>(143);
        final List<Float> incidence = new ArrayList<Float>(143);

        for(MetadataElement geolocElem : geolocElems) {
            final String swathStr = geolocElem.getAttributeString("swath_number");
            if(swathStr.equalsIgnoreCase(subSwath)) {
                subSamplingX = geolocElem.getAttribute("ASAR_Geo_Grid_ADSR.sd/first_line_tie_points.samp_numbers")
                        .getData().getElemIntAt(1) - 1;

                final MetadataAttribute attrib = geolocElem.getAttribute("first_zero_doppler_time");
                if(attrib != null) {
                    final String timeStr = attrib.getData().getElemString();
                    double timeMJD = 0.0;
                    try {
                        timeMJD = ProductData.UTC.parse(timeStr).getMJD();
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("Unable to parse metadata attribute " + timeStr);
                    }
                    time.add(timeMJD*24.0*3600.0);
                }

                addTiePoints(geolocElem, "ASAR_Geo_Grid_ADSR.sd/first_line_tie_points.lats", lats);
                addTiePoints(geolocElem, "ASAR_Geo_Grid_ADSR.sd/first_line_tie_points.longs", lons);
                addTiePoints(geolocElem, "ASAR_Geo_Grid_ADSR.sd/first_line_tie_points.slant_range_times", slant);
                addTiePoints(geolocElem, "ASAR_Geo_Grid_ADSR.sd/first_line_tie_points.angles", incidence);
            }
        }

        final int subSamplingY = (int)((time.get(1) - time.get(0))/lineTimeInterval + 0.5);

        final int length = lats.size();
        final float[] latList = new float[length];
        final float[] lonList = new float[length];
        final float[] slantList = new float[length];
        final float[] incList = new float[length];
        for(int i=0; i < length; ++i) {
            latList[i] = lats.get(i) / (float)Constants.oneMillion;
            lonList[i] = lons.get(i) / (float)Constants.oneMillion;
            slantList[i] = slant.get(i);
            incList[i] = incidence.get(i);
        }

        final int gridWidth = 11;
        final int gridHeight = length / 11;
        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE,
                gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, latList);
        latGrid.setUnit(Unit.DEGREES);
        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE,
                gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, lonList, TiePointGrid.DISCONT_AT_180);
        lonGrid.setUnit(Unit.DEGREES);
        final TiePointGrid slantGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME,
                gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, slantList);
        slantGrid.setUnit(Unit.NANOSECONDS);
        final TiePointGrid incGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE,
                gridWidth, gridHeight, 0, 0,
                subSamplingX, subSamplingY, incList);
        incGrid.setUnit(Unit.DEGREES);

        targetProduct.addTiePointGrid(latGrid);
        targetProduct.addTiePointGrid(lonGrid);
        targetProduct.addTiePointGrid(slantGrid);
        targetProduct.addTiePointGrid(incGrid);

        targetProduct.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84));
    }

    private static void addTiePoints(final MetadataElement elem, final String tag, final List<Float> array) {
        final MetadataAttribute attrib = elem.getAttribute(tag);
        if(attrib != null) {
            if(attrib.getDataType() == ProductData.TYPE_FLOAT32) {
                final float[] fList = (float[])attrib.getData().getElems();
                for(float f : fList) {
                    array.add(f);
                }
            } else {
                final int[] iList = (int[])attrib.getData().getElems();
                for(int i : iList) {
                    array.add((float)i);
                }
            }
        }
    }

    private static int getSubSwathNumber(final String subSwath) {
        if(subSwath.equals(SS1))
            return 0;
        else if(subSwath.equals(SS2))
            return 1;
        else if(subSwath.equals(SS3))
            return 2;
        else if(subSwath.equals(SS4))
            return 3;
        return 4;
    }

    private static int getRealBandNumFromSubSwath(final String subSwath) {
        if(subSwath.equals(SS1))
            return 0;
        else if(subSwath.equals(SS2))
            return 2;
        else if(subSwath.equals(SS3))
            return 4;
        else if(subSwath.equals(SS4))
            return 6;
        return 8;
    }

    private static void copyMetaData(final MetadataElement source, final MetadataElement target) {
        for (final MetadataElement element : source.getElements()) {
            if (!element.getName().equals("Image Record"))
                target.addElement(element.createDeepClone());
        }
        for (final MetadataAttribute attribute : source.getAttributes()) {
            target.addAttribute(attribute.createDeepClone());
        }
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles The current tiles to be computed for each target band.
     * @param pm          A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Tile targetTileI = null, targetTileQ = null, targetTileIntensity = null;
        //final Rectangle targetRectangle = targetTile.getRectangle();
        //System.out.println("targetRect " + targetRectangle.x + " " + targetRectangle.y + " w "
        //        + targetRectangle.width + " h " + targetRectangle.height);

        try {
            ComplexBand cBand;
            if (produceIntensitiesOnly) {
                final Band tgtBand = targetProduct.getBandAt(0);
                targetTileIntensity = targetTiles.get(tgtBand);
                cBand = bandMap.get(tgtBand);
            } else {
                final Band tgtBandI = targetProduct.getBandAt(0);
                final Band tgtBandQ = targetProduct.getBandAt(1);
                targetTileI = targetTiles.get(tgtBandI);
                targetTileQ = targetTiles.get(tgtBandQ);
                cBand = bandMap.get(tgtBandI);
            }

            if (!lineTimesSorted) {
                sortLineTimes(sourceProduct, cBand.i);
            }

            final int maxY = targetRectangle.y + targetRectangle.height;
            final int maxX = targetRectangle.x + targetRectangle.width;
            //int targetLine = targetRectangle.y;
            //double startTime = targetLine * lineTimeInterval;

            //final double threshold = 0.000000139;
            final double threshold = 0.000000135;

            final double start = targetProduct.getStartTime().getMJD();
            final double end = targetProduct.getEndTime().getMJD();
            final double interval = (end-start)/ targetHeight;
            final Vector<Integer> burstLines = new Vector<Integer>(4);

            for(int y = targetRectangle.y; y < maxY; ++y) {
                double startTime = start + (y * interval);

                burstLines.clear();
                double min1 = Float.MAX_VALUE;
                double min2 = Float.MAX_VALUE;
                double min3 = Float.MAX_VALUE;
                int i1=0, i2=0, i3=0;
                for(int i=0; i < lineTimes.length; ++i) {
                    if (lineTimes[i].visited || lineTimes[i].time < 1)
                        continue;

                    double t = lineTimes[i].time;
                    final double diff = Math.abs(t - startTime);
                    if (diff < min1) {
                        if(min1 < min2) {
                            if(min2 < min3) {
                                min3 = min2;
                                i3 = i2;
                            }
                            min2 = min1;
                            i2 = i1;
                        }
                        min1 = diff;
                        i1 = i;
                    } else if(diff < min2) {
                        if(min2 < min3) {
                            min3 = min2;
                            i3 = i2;
                        }
                        min2 = diff;
                        i2 = i;
                    } else if(diff < min3) {
                        min3 = diff;
                        i3 = i;
                    }
                }

                burstLines.add(lineTimes[i1].line);
                setVisited(i1);
                burstLines.add(lineTimes[i2].line);
                setVisited(i2);
                burstLines.add(lineTimes[i3].line);
                setVisited(i3);

                if (!burstLines.isEmpty()) {

                    final boolean ok = deburstTile(burstLines, y, targetRectangle.x, maxX, cBand.i, cBand.q,
                            targetTileI, targetTileQ, targetTileIntensity);
                    if(!ok)
                        System.out.println("not ok "+y);
                }
            }

   /*         int i = 0;
            while (i < lineTimes.length) {
                if (lineTimes[i].visited || lineTimes[i].time < startTime) {
                    ++i;
                    continue;
                }

                burstLines.clear();
                burstLines.add(lineTimes[i].line);
                setVisited(i);

                int j = i + 1;
                while (j < lineTimes.length && burstLines.size() < 3) {
                    if (lineTimes[j].visited) {
                        ++j;
                        continue;
                    }

                    final double diff = Math.abs(lineTimes[j].time - lineTimes[i].time);
                    if (diff < threshold) {
                        burstLines.add(lineTimes[j].line);
                        setVisited(j);
                    }
                    ++j;
                }
                ++i;

                //System.out.println(targetLine+" found "+ burstLines.size() + " burstlines");

                if (!burstLines.isEmpty()) {

                    final boolean ok = deburstTile(burstLines, targetLine, targetRectangle.x, maxX, cBand.i, cBand.q,
                            targetTileI, targetTileQ, targetTileIntensity);
                    if(ok)
                        ++targetLine;
                }
                startTime = targetLine * lineTimeInterval;

                if(targetLine >= targetRectangle.y + targetRectangle.height)
                    break;
            }    */

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private synchronized void setVisited(final int i) {
        lineTimes[i].visited = true;
    }

    private synchronized void sortLineTimes(final Product srcProduct, final Band srcBand) {
        if(lineTimesSorted) return;

        final MetadataElement imgRecElem = srcProduct.getMetadataRoot().getElement("Image Record");
        final MetadataElement bandElem = imgRecElem.getElement(srcBand.getName());

        final MetadataAttribute attrib = bandElem.getAttribute("t");
        final double[] timeData = (double[])attrib.getData().getElems();
        lineTimes = new LineTime[timeData.length];

        for(int y=0; y < timeData.length; ++y) {
            lineTimes[y] = new LineTime(y, timeData[y]);
        }

        Arrays.sort(lineTimes, new LineTimeComparator());
        lineTimesSorted = true;
    }

    private boolean deburstTile(final Vector<Integer> burstLines, final int targetLine, final int startX, final int endX,
                              final Band srcBandI, final Band srcBandQ,
                              final Tile targetTileI, final Tile targetTileQ,
                              final Tile targetTileIntensity) {

        final Integer[] burstLineList = new Integer[burstLines.size()];
        burstLines.toArray(burstLineList);
        final int rowLength = endX - startX;
        final double[] peakLine = new double[rowLength];
        final double[] peakLineI = new double[rowLength];
        final double[] peakLineQ = new double[rowLength];
        final double[] sumLine = new double[rowLength];
        final int[] avgTotals = new int[rowLength];
        Arrays.fill(peakLine, -Float.MAX_VALUE);
        Arrays.fill(sumLine, 0.0);
        Arrays.fill(avgTotals, 0);
        double Ival, Qval, intensity;

        final Vector<short[]> srcDataListI = new Vector<short[]>(3);
        final Vector<short[]> srcDataListQ = new Vector<short[]>(3);
        final int widthMargin = targetWidth - margin;

        try {
            // get all burst lines
            getBurstLines(burstLineList, srcBandI, srcBandQ, startX, endX, srcDataListI, srcDataListQ);

            if(srcDataListI.isEmpty())
                return false;

            final int dataListSize = srcDataListI.size();
            // for all x peakpick or average from the bursts
            for (int x = startX, i = 0; x < endX; ++x, ++i) {

                for(int j=0; j < dataListSize; ++j) {

                    final short[] srcDataI = srcDataListI.get(j);
                    final short[] srcDataQ = srcDataListQ.get(j);

                    Ival = srcDataI[i];
                    Qval = srcDataQ[i];

                    intensity = (Ival * Ival) + (Qval * Qval);
                    if (intensity > peakLine[i]) {
                        peakLine[i] = intensity;
                        peakLineI[i] = Ival;
                        peakLineQ[i] = Qval;
                    }

                    if (average) {
                        if(!isInvalid(Ival, Qval, zeroThresholdSmall)) {
                            sumLine[i] += intensity;
                            avgTotals[i] += 1;
                        }
                    }
                }

                if(average && avgTotals[i] > 1)
                    sumLine[i] /= avgTotals[i];
            }

            if (produceIntensitiesOnly) {
                final ProductData data = targetTileIntensity.getDataBuffer();

                if (average) {

                    for (int x = startX, i = 0; x < endX; ++x, ++i) {
                        if(x < margin || x > widthMargin) {
                            data.setElemDoubleAt(targetTileIntensity.getDataBufferIndex(x, targetLine), nodatavalue);
                        } else {
                            data.setElemDoubleAt(targetTileIntensity.getDataBufferIndex(x, targetLine), sumLine[i]);
                        }
                    }
                } else {
                    for (int x = startX, i = 0; x < endX; ++x, ++i) {
                        if(peakLine[i] == -Float.MAX_VALUE) {
                            peakLine[i] = 0;
                            //System.out.println("uninitPeak " + i + " at " + targetLine);
                        }
                        if(x < margin || x > widthMargin) {
                            data.setElemDoubleAt(targetTileIntensity.getDataBufferIndex(x, targetLine), nodatavalue);
                        } else {
                            data.setElemDoubleAt(targetTileIntensity.getDataBufferIndex(x, targetLine), peakLine[i]);
                        }
                    }
                }
            } else {
                final ProductData dataI = targetTileI.getDataBuffer();
                final ProductData dataQ = targetTileQ.getDataBuffer();

                for (int x = startX, i = 0; x < endX; ++x, ++i) {
                    if(peakLine[i] == -Float.MAX_VALUE) {
                        peakLineI[i] = 0;
                        peakLineQ[i] = 0;
                        System.out.println("uninitPeak " + i + " at " + targetLine);
                    }
                    final int index = targetTileI.getDataBufferIndex(x, targetLine);
                    if(x < margin || x > widthMargin) {
                        dataI.setElemDoubleAt(index, nodatavalue);
                        dataQ.setElemDoubleAt(index, nodatavalue);
                    } else {
                        dataI.setElemDoubleAt(index, peakLineI[i]);
                        dataQ.setElemDoubleAt(index, peakLineQ[i]);
                    }
                }
            }
            return true;

        } catch (Exception e) {
            System.out.println("deburstTile " + e.toString());
        }
        return false;
    }

    private void getBurstLines(final Integer[] burstLineList, final Band srcBandI, final Band srcBandQ,
                               final int startX, final int endX,
                               final Vector<short[]> srcDataListI, final Vector<short[]> srcDataListQ) {
        Tile sourceRasterI, sourceRasterQ;
        final int srcBandHeight = srcBandI.getRasterHeight() - 1;
        final int srcBandWidth = srcBandI.getRasterWidth() - 1;

        for (Integer y : burstLineList) {
            if (y > srcBandHeight) continue;

            final Rectangle sourceRectangle = new Rectangle(startX, y, endX, 1);
            sourceRasterI = getSourceTile(srcBandI, sourceRectangle);
            final short[] srcDataI = (short[]) sourceRasterI.getRawSamples().getElems();
            sourceRasterQ = getSourceTile(srcBandQ, sourceRectangle);
            final short[] srcDataQ = (short[]) sourceRasterQ.getRawSamples().getElems();

            int invalidCount = 0;
            int total = 0;
            final int max = Math.min(srcBandWidth, srcDataI.length);
            for(int i=500; i < max; i+= 50) {
                if(isInvalid(srcDataI[i], srcDataQ[i], zeroThreshold))
                    ++invalidCount;
                ++total;
            }
            if(invalidCount / (float)total > 0.4)  {
                //System.out.println("skipping " + y);
                continue;
            }   

            srcDataListI.add(srcDataI);
            srcDataListQ.add(srcDataQ);
        }
    }

    private static void addToAverage(int i, double Ival, double Qval,
                                     final double[] sumLine, final int[] avgTotals) {
        if(!isInvalid(Ival, Qval, zeroThresholdSmall)) {
            sumLine[i] += (Ival * Ival) + (Qval * Qval);
            avgTotals[i] += 1;
        }
    }

    private static boolean isInvalid(final double i, final double q, final double threshold) {
        return i > -threshold && i < threshold && q > -threshold && q < threshold;
    }

    private final static class LineTime {
        boolean visited = false;
        final int line;
        final double time;

        LineTime(final int y, final double utc) {
            line = y;
            time = utc;
        }
    }

    private final static class LineTimeComparator implements Comparator<LineTime> {
        public int compare(LineTime a, LineTime b) {
            if (a.time < b.time) return -1;
            else if (a.time > b.time) return 1;
            return 0;
        }
    }

    private final static class ComplexBand {
        final Band i;
        final Band q;
        public ComplexBand(final Band iBand, final Band qBand) {
            i = iBand;
            q = qBand;
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(DeburstWSSOp.class);
        }
    }
}