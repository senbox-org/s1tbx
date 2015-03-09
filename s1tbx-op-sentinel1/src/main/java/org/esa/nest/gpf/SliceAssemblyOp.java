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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Term;
import com.bc.jexp.WritableNamespace;
import com.bc.jexp.impl.ParserImpl;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.dataop.barithm.RasterDataSymbol;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.OrbitStateVector;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.eo.Constants;
import org.esa.snap.gpf.InputProductValidator;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.TileIndex;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.esa.nest.dataio.sentinel1.Sentinel1Level1Directory.getListInEvenlySpacedGrid;

/**
 * Merges Sentinel-1 slice products
 */
@OperatorMetadata(alias = "SliceAssembly",
        category = "SAR Processing/SENTINEL-1",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Merges Sentinel-1 slice products")
public final class SliceAssemblyOp extends Operator {

    @SourceProducts
    private Product[] sourceProducts;
    @TargetProduct
    private Product targetProduct;

    // Only bands whose polarization is selected will be in the output product.
    @Parameter(description = "The list of polarisations", label = "Polarisations")
    private String[] selectedPolarisations;

    private MetadataElement absRoot = null;

    // The slice products will be in order in the array: 1st (top) slice is the 1st element in the array followed by
    // 2nd slice and so on.
    private Product[] sliceProducts;
    private Map<Band, BandLines[]> bandLineMap = new HashMap<>();

    // This is the raster width and height of the target product
    private int targetWidth = 0, targetHeight = 0;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public SliceAssemblyOp() {
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
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            for(Product srcProduct : sourceProducts) {
                final InputProductValidator validator = new InputProductValidator(srcProduct);
                validator.checkIfSentinel1Product();
                validator.checkProductType(new String[]{"SLC", "GRD"});
                validator.checkAcquisitionMode(new String[]{"SM","IW","EW"});
            }

            sliceProducts = determineSliceProducts();

            absRoot = AbstractMetadata.getAbstractedMetadata(sliceProducts[0]);

            if (selectedPolarisations == null || selectedPolarisations.length == 0) {
                final Sentinel1Utils su = new Sentinel1Utils(sliceProducts[0]);
                selectedPolarisations = su.getPolarizations();
            }

            createTargetProduct();

            updateTargetProductMetadata();

            determineBandStartEndTimes();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private Product[] determineSliceProducts() throws Exception {
        if(sourceProducts.length < 2) {
            throw new Exception("Slice assembly requires at least two consecutive slice products");
        }

        final TreeMap<Integer, Product> productSet = new TreeMap<>();
        for(Product srcProduct : sourceProducts) {
            final MetadataElement origMetaRoot = AbstractMetadata.getOriginalProductMetadata(srcProduct);
            final MetadataElement generalProductInformation = getGeneralProductInformation(origMetaRoot);
            if(!isSliceProduct(generalProductInformation)) {
                throw new Exception(srcProduct.getName() +" is not a slice product");
            }

            final int totalSlices = generalProductInformation.getAttributeInt("totalSlices");
            final int sliceNumber = generalProductInformation.getAttributeInt("sliceNumber");
            //System.out.println("SliceAssemblyOp.determineSliceProducts: totalSlices = " + totalSlices + "; slice product name = " + srcProduct.getName() + "; prod type = " + srcProduct.getProductType() + "; sliceNumber = " + sliceNumber);

            productSet.put(sliceNumber, srcProduct);
        }

        //check if consecutive
        Integer prev = productSet.firstKey();
        // Note that "The set's iterator returns the keys in ascending order".
        for(Integer i : productSet.keySet()) {
            if(!i.equals(prev)) {
                if(!prev.equals(i-1)) {
                    throw new Exception("Products are not consecutive slices");
                }
                prev = i;
            }
        }

        // Note that "If productSet makes any guarantees as to what order its elements
        // are returned by its iterator, toArray() must return the elements in
        // the same order".
        return productSet.values().toArray(new Product[productSet.size()]);
    }

    private static MetadataElement getGeneralProductInformation(final MetadataElement origMetaRoot) {
        final MetadataElement XFDU = origMetaRoot.getElement("XFDU");
        final MetadataElement metadataSection = XFDU.getElement("metadataSection");

        final MetadataElement metadataObject = findElementByID(metadataSection, "ID", "generalProductInformation");
        final MetadataElement metadataWrap = metadataObject.getElement("metadataWrap");
        final MetadataElement xmlData = metadataWrap.getElement("xmlData");
        MetadataElement generalProductInformation = xmlData.getElement("generalProductInformation");
        if (generalProductInformation == null)
            generalProductInformation = xmlData.getElement("standAloneProductInformation");
        return generalProductInformation;
    }

    private static boolean isSliceProduct(final MetadataElement generalProductInformation) {
        final String sliceProductFlag = generalProductInformation.getAttributeString("sliceProductFlag");
        return sliceProductFlag.equals("true");
    }

    private static MetadataElement findElementByID(final MetadataElement metadataSection, final String tag, final String id) {
        final MetadataElement[] metadataObjectList = metadataSection.getElements();

        for (MetadataElement metadataObject : metadataObjectList) {
            final String attrib = metadataObject.getAttributeString(tag, null);
            if (attrib.equals(id)) {
                return metadataObject;
            }
        }
        return null;
    }

    private void computeTargetWidthAndHeight() {
        // In GRD products, all the bands will have the same width, so the width of the product is equal to
        // the band width.
        // In SLC products, different bands may have different widths. The width of the product is equal to
        // maximum of the band widths.
        // So if a particular band should have the same width across different slice products, then all the
        // slice products should have the same product width.
        // But the slice products can have different widths, which implies that the width of a band
        // in one slice product can be different from the width of the same band in another slice product.

        // We assume that the the different-sized bands from the slice products are aligned to the left.
        // So for the product width, we can take the max among all slice products.
        // TODO Put a check here to check that the assumption is OK by looking at the slantRangeTime from
        // TODO Metadata > Original_Product_Metadata > annotation > s1...-00n.xml > product > imageAnnotation >
        // TODO imageInformation > slantRangeTime
        // TODO For GRD products, it should only be one value for the entire slice product. It is available in
        // TODO Abstracted_Metadata as well.
        // TODO For SLC products, we will need one value for each swath.

        for (Product srcProduct : sliceProducts) {
            if (targetWidth < srcProduct.getSceneRasterWidth())
                targetWidth = srcProduct.getSceneRasterWidth();
            targetHeight += srcProduct.getSceneRasterHeight();
        }
    }

    private void computeTargetBandWidthAndHeight(final String bandName, final Dimension dim) throws OperatorException {
        // See comments in computeTargetWidthAndHeight().
        // For band width, we take the max for that band among all slice products.
        // This is consistent with the assumption in computeTargetWidthAndHeight().
        for (Product srcProduct : sliceProducts) {
            final Band srcBand = srcProduct.getBand(bandName);
            if(srcBand == null) {
                throw new OperatorException(bandName +" not found in product "+srcProduct.getName());
            }

            dim.setSize(Math.max(dim.width, srcBand.getRasterWidth()), dim.height + srcBand.getRasterHeight());
        }
    }

    private void createTargetProduct() {
        computeTargetWidthAndHeight();

        final Product firstSliceProduct = sliceProducts[0];
        final Product lastSliceProduct = sliceProducts[sliceProducts.length-1];
        targetProduct = new Product(firstSliceProduct.getName(), firstSliceProduct.getProductType(), targetWidth, targetHeight);

        // We are creating each target band based on the source band in the first slice product only.
        final Band[] sourceBands = firstSliceProduct.getBands();
        for (Band srcBand : sourceBands) {
            boolean selectedPol = false;
            for (String pol : selectedPolarisations) {
                if (srcBand.getName().contains(pol))
                    selectedPol = true;
            }
            if(!selectedPol)
                continue;

            if (srcBand instanceof VirtualBand) {
                final VirtualBand sourceBand = (VirtualBand) srcBand;
                int destWidth = 0;
                int destHeight = 0;

                final Term term = createTerm(sourceBand.getExpression(), sliceProducts);
                final RasterDataSymbol[] refRasterDataSymbols = BandArithmetic.getRefRasterDataSymbols(term);
                for (RasterDataSymbol symbol : refRasterDataSymbols) {
                    String name = symbol.getName();
                    final Band trgBand = targetProduct.getBand(name);
                    if(trgBand != null) {
                        destWidth = trgBand.getRasterWidth();
                        destHeight = trgBand.getRasterHeight();
                        break;
                    }
                }

                final VirtualBand targetBand = new VirtualBand(sourceBand.getName(), sourceBand.getDataType(),
                        destWidth, destHeight, sourceBand.getExpression());

                ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                targetProduct.addBand(targetBand);
            } else {
                final Dimension dim = new Dimension(0, 0);
                computeTargetBandWidthAndHeight(srcBand.getName(), dim);
                final Band newBand = new Band(srcBand.getName(), srcBand.getDataType(), dim.width, dim.height);
                ProductUtils.copyRasterDataNodeProperties(srcBand, newBand);

                targetProduct.addBand(newBand);
            }
        }

        ProductUtils.copyMetadata(firstSliceProduct, targetProduct);
        ProductUtils.copyFlagCodings(firstSliceProduct, targetProduct);
        ProductUtils.copyMasks(firstSliceProduct, targetProduct);
        ProductUtils.copyVectorData(firstSliceProduct, targetProduct);
        ProductUtils.copyIndexCodings(firstSliceProduct, targetProduct);
        targetProduct.setStartTime(firstSliceProduct.getStartTime());
        targetProduct.setEndTime(lastSliceProduct.getEndTime());
        targetProduct.setDescription(firstSliceProduct.getDescription());

        // TODO still needs to do it for SLC
        createTiePointGridsForGRD();
        addGeocoding();
    }

    private Term createTerm(final String expression, final Product[] availableProducts) {
        WritableNamespace namespace = BandArithmetic.createDefaultNamespace(availableProducts, 0);
        final Term term;
        try {
            Parser parser = new ParserImpl(namespace, false);
            term = parser.parse(expression);
        } catch (ParseException e) {
            throw new OperatorException("Could not parse expression: " + expression, e);
        }
        return term;
    }

    private MetadataElement[] getGeoGridForGRD(final Product product) {

        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement annotationElem = origProdRoot.getElement("annotation");
        final MetadataElement imgElem = annotationElem.getElementAt(0); // For GRD product, same grid for all bands, so just take the 1st one
        final MetadataElement productElem = imgElem.getElement("product");
        final MetadataElement geolocationGrid = productElem.getElement("geolocationGrid");
        final MetadataElement geolocationGridPointList = geolocationGrid.getElement("geolocationGridPointList");
        return geolocationGridPointList.getElements();
    }

    private void createTiePointGridsForGRD() {

        // geolocationGridPointList has a count and a list of geolocationGridPoint(s).
        // These geolocationGridPoint(s) should form an MxN grid, M = number of rows and N = number of columns.
        // Each geolocationGridPoint contains line (i.e. row) and pixel (i.e. column).
        // The horizontal or vertical spacing may not be even.
        // But from line to line, the pixels are in the same location.
        // E.g A 4x6 grid for an image of width 24 and height 17:
        // 1st row: line = 0;	pixels =	0	5	10	15	20	23
        // 2nd row: line = 6;	pixels = 	0	5	10	15	20	23
        // 3rd row: line = 12;	pixels = 	0	5	10	15	20	23
        // 4th row: line = 16;  pixels = 	0	5	10	15	20	23
        // According to Table 6-88 of Product Specs v2.9, each geolocationGridPoint is a point within the image.
        // So we cannot have a point with line 17 or pixels 24.
        //
        // We assume that from slice to slice, M may differ but N must be the same. However, the pixel spacing
        // from slice to slice may not be the same.
        // So the next slice to the example above can be a 5x6 grid for an image of width 30 and height 20:
        // 1st row: line = 0;	pixels =	0	6	12	18	24	29
        // 2nd row: line = 5;	pixels = 	0	6	12	18	24	29
        // 3rd row: line = 10;	pixels = 	0	6	12	18	24	29
        // 4th row: line = 15;  pixels = 	0	6	12	18	24	29
        // 5th row: line = 19;  pixels = 	0	6	12	18	24	29
        //
        // We concatenate the two geolocationGridPointList(s) together to form a 9x6 grid for an image of width 30 and
        // height 37:
        // 1st row: line = 0;	pixels =	0	5	10	15	20	23
        // 2nd row: line = 6;	pixels = 	0	5	10	15	20	23
        // 3rd row: line = 12;	pixels = 	0	5	10	15	20	23
        // 4th row: line = 16;  pixels = 	0	5	10	15	20	23
        // 5th row: line = 17;	pixels =	0	6	12	18	24	29
        // 6th row: line = 22;	pixels = 	0	6	12	18	24	29
        // 7th row: line = 27;	pixels = 	0	6	12	18	24	29
        // 8th row: line = 32;  pixels = 	0	6	12	18	24	29
        // 9th row: line = 36;  pixels = 	0	6	12	18	24	29

        int geoGridLen = 0;
        final ArrayList<MetadataElement[]> geoGrids = new ArrayList<>();
        int i = 0;
        for (Product product : sliceProducts) {
            MetadataElement[]  geoGrid = getGeoGridForGRD(product);
            geoGridLen += geoGrid.length;
            geoGrids.add(i, geoGrid);
            i++;
        }

        //System.out.println("geoGridLen = " + geoGridLen);

        final double[] latList = new double[geoGridLen];
        final double[] lngList = new double[geoGridLen];
        final double[] incidenceAngleList = new double[geoGridLen];
        final double[] elevAngleList = new double[geoGridLen];
        final double[] rangeTimeList = new double[geoGridLen];
        final int[] x = new int[geoGridLen];
        final int[] y = new int[geoGridLen];

        // We assume here that all the slice products will have the same width in the geolocation grid.
        // That is the same number of points in each line.

        final int[] gridWidths = new int[sliceProducts.length];
        final int[] gridHeights = new int[sliceProducts.length];

        for (int j = 0; j < sliceProducts.length; j++) {
            gridWidths[j] = 0;
            gridHeights[j] = 0;
        }

        int gridHeight = 0;

        i = 0;
        int ptsInPrvSlices = 0;
        int heightOffset = 0;
        for (int j = 0; j < sliceProducts.length; j++) {

            if (j > 0) {
                heightOffset += sliceProducts[j-1].getSceneRasterHeight();
            }

            final MetadataElement[] geoGrid = geoGrids.get(j);

            for (MetadataElement ggPoint : geoGrid) {
                latList[i] = ggPoint.getAttributeDouble("latitude", 0);
                lngList[i] = ggPoint.getAttributeDouble("longitude", 0);
                incidenceAngleList[i] = ggPoint.getAttributeDouble("incidenceAngle", 0);
                elevAngleList[i] = ggPoint.getAttributeDouble("elevationAngle", 0);
                rangeTimeList[i] = ggPoint.getAttributeDouble("slantRangeTime", 0) * Constants.oneBillion; // s to ns

                x[i] = (int) ggPoint.getAttributeDouble("pixel", 0);
                if (x[i] == 0) {
                    // This means we are at the start of a new line
                    // gridWidths[j] will be updated to 0 at the 1st line.
                    // It will be updated to the correct value at the 2nd line.
                    if (gridWidths[j] == 0)
                        gridWidths[j] = i - ptsInPrvSlices;
                    ++gridHeights[j];
                }

                y[i] = (int) ggPoint.getAttributeDouble("line", 0) + heightOffset;

                ++i;
            }

            ptsInPrvSlices = i;

            gridHeight += gridHeights[j];
        }

        final int gridWidth = gridWidths[0];

        for (int w : gridWidths) {
            if (w != gridWidth) {
                throw new OperatorException("geolocation grids have different widths among slice products");
            }
        }

        //System.out.println("gridWidth = " + gridWidth + " gridHeight = " + gridHeight);

        final int newGridWidth = gridWidth;
        final int newGridHeight = gridHeight;
        if (geoGridLen != (newGridWidth * newGridHeight)) {
            throw new OperatorException("wrong number of geolocation grid points");
        }
        final float[] newLatList = new float[newGridWidth * newGridHeight];
        final float[] newLonList = new float[newGridWidth * newGridHeight];
        final float[] newIncList = new float[newGridWidth * newGridHeight];
        final float[] newElevList = new float[newGridWidth * newGridHeight];
        final float[] newslrtList = new float[newGridWidth * newGridHeight];
        final int sceneRasterWidth = targetProduct.getSceneRasterWidth();
        final int sceneRasterHeight = targetProduct.getSceneRasterHeight();
        final double subSamplingX = (double) sceneRasterWidth / (newGridWidth - 1);
        final double subSamplingY = (double) sceneRasterHeight / (newGridHeight - 1);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, latList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newLatList);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, lngList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newLonList);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, incidenceAngleList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newIncList);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, elevAngleList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newElevList);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, rangeTimeList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newslrtList);


        final TiePointGrid latGrid = new TiePointGrid(OperatorUtils.TPG_LATITUDE,
                newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newLatList);
        latGrid.setUnit(Unit.DEGREES);
        targetProduct.addTiePointGrid(latGrid);

        final TiePointGrid lonGrid = new TiePointGrid(OperatorUtils.TPG_LONGITUDE,
                newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newLonList, TiePointGrid.DISCONT_AT_180);
        lonGrid.setUnit(Unit.DEGREES);
        targetProduct.addTiePointGrid(lonGrid);

        final TiePointGrid incidentAngleGrid = new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE,
                newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newIncList);
        incidentAngleGrid.setUnit(Unit.DEGREES);
        targetProduct.addTiePointGrid(incidentAngleGrid);

        final TiePointGrid elevAngleGrid = new TiePointGrid(OperatorUtils.TPG_ELEVATION_ANGLE,
                newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newElevList);
        elevAngleGrid.setUnit(Unit.DEGREES);
        targetProduct.addTiePointGrid(elevAngleGrid);

        final TiePointGrid slantRangeGrid = new TiePointGrid(OperatorUtils.TPG_SLANT_RANGE_TIME,
                newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newslrtList);
        slantRangeGrid.setUnit(Unit.NANOSECONDS);
        targetProduct.addTiePointGrid(slantRangeGrid);
    }

    /*
    private void createTiePointGrids() {
        final Product firstSliceProduct = sliceProducts[0];

        final TiePointGrid[] tpgList = firstSliceProduct.getTiePointGrids();
        for(TiePointGrid tpg : tpgList) {
            final List<Float> newPoints = new ArrayList<>();

            final int gridWidth = tpg.getRasterWidth();
            final int gridHeight = tpg.getRasterHeight();
            final float[] points = tpg.getTiePoints();

            for(float f : points) {
                newPoints.add(f);
            }
            int newGridHeight = gridHeight;

            for(Product srcProduct : sliceProducts) {
                if(srcProduct == firstSliceProduct)
                    continue;

                final TiePointGrid tpg2 = srcProduct.getTiePointGrid(tpg.getName());
                final float[] points2 = tpg2.getTiePoints();

                for(int i=gridWidth; i < points2.length; ++i) {
                    newPoints.add(points2[i]);
                }
                newGridHeight += tpg2.getRasterHeight()-1;
            }

            final int subSamplingX = targetWidth / (gridWidth-1);
            final int subSamplingY = targetHeight / (newGridHeight-1);

            final float[] pointArray = new float[newPoints.size()];
            int i=0;
            for(Float f : newPoints) {
                pointArray[i++] = f;
            }

            final TiePointGrid newGrid = new TiePointGrid(
                    tpg.getName(), gridWidth, newGridHeight, 0, 0, subSamplingX, subSamplingY, pointArray);
            targetProduct.addTiePointGrid(newGrid);
        }
    }
    */

    private void addGeocoding() {
        final TiePointGrid latGrid = targetProduct.getTiePointGrid(OperatorUtils.TPG_LATITUDE);
        final TiePointGrid lonGrid = targetProduct.getTiePointGrid(OperatorUtils.TPG_LONGITUDE);

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);
        targetProduct.setGeoCoding(tpGeoCoding);
    }

    private String extractPol(final String filename) {
        return "";
    }

    private String extractImageNumber(final String filename) {

        final int dotIdx = filename.indexOf('.');
        return filename.substring(dotIdx-3, dotIdx);
    }

    private ProductData getStopTime(final Product product, final String imageNum) {

        //System.out.println("getStopTime for " + product.getName() + " imageNum = " + imageNum);

        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement calibration = origProdRoot.getElement("calibration");
        final MetadataElement[] calibrationElems = calibration.getElements();

        ProductData data = null;

        for (MetadataElement e : calibrationElems) {

            //System.out.println("getStopTime: " + e.getName());

            if (extractImageNumber(e.getName()).equals(imageNum)) {

                final MetadataElement calib = e.getElement("calibration");
                final MetadataElement adsHeader = calib.getElement("adsHeader");
                final MetadataAttribute stopTime = adsHeader.getAttribute("stopTime");
                //System.out.println("getStopTime: " + stopTime.getData().toString());

                data = stopTime.getData();
            }

        }

        return data;
    }

    MetadataElement getCalibrationVectorList(final Product product, final String imageNum) {

        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement calibration = origProdRoot.getElement("calibration");
        final MetadataElement[] calibrationElems = calibration.getElements();

        MetadataElement calibVectorList = null;

        for (MetadataElement e : calibrationElems) {

            if (extractImageNumber(e.getName()).equals(imageNum)) {

                final MetadataElement calib = e.getElement("calibration");
                calibVectorList = calib.getElement("calibrationVectorList");
            }

        }

        return calibVectorList;
    }

    private int getCalibrationPixelCount(final Product product, final String imageNum, final int[] pixelSpacing) {

        // TODO fill in pixelSpacing

        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement calibration = origProdRoot.getElement("calibration");
        final MetadataElement[] calibrationElems = calibration.getElements();

        int pixelCount = 0;

        for (MetadataElement e : calibrationElems) {
            //System.out.println("getCalibrationPixelCount: " + e.getName());
            if (extractImageNumber(e.getName()).equals(imageNum)) {
                final MetadataElement calib = e.getElement("calibration");
                final MetadataElement calibVectorList = calib.getElement("calibrationVectorList");
                final MetadataElement firstCalibVector = calibVectorList.getElementAt(0);
                final MetadataElement pixel = firstCalibVector.getElement("pixel");
                final MetadataAttribute count = pixel.getAttribute("count");
                //System.out.println("getCalibrationPixelCount: " + count.getData().toString());
                pixelCount = Integer.parseInt(count.getData().getElemString());
            }
        }

        return pixelCount;
    }

    private void concatenateCalibrationVectors(final MetadataElement targetCalibVectorList,
                                               final MetadataElement sliceCalibVectorList,
                                               final int startVectorIdx,
                                               final int lineOffset) {

        int idx = Integer.parseInt(targetCalibVectorList.getAttribute("count").getData().getElemString());
        final int numSliceLines = Integer.parseInt(sliceCalibVectorList.getAttribute("count").getData().getElemString());

        for (int i = startVectorIdx; i < numSliceLines; i++) {

            MetadataElement v = sliceCalibVectorList.getElementAt(i);
            MetadataElement newV = v.createDeepClone();
            final int newLine = Integer.parseInt(v.getAttributeString("line")) + lineOffset;
            newV.setAttributeString("line", Integer.toString(newLine));
            targetCalibVectorList.addElementAt(newV, idx);
            idx++;
        }

        targetCalibVectorList.setAttributeString("count", Integer.toString(idx));
    }

    private void updateCalibration() {

        final Product lastSliceProduct = sliceProducts[sliceProducts.length-1];

        // The calibration data in metadata in targetProduct at this point is copied from the 1st slice.
        // So we need to concatenate the calibration vectors from the slices to target.

        final MetadataElement targetOrigProdRoot = AbstractMetadata.getOriginalProductMetadata(targetProduct);
        final MetadataElement targetCalibration = targetOrigProdRoot.getElement("calibration");
        final MetadataElement[] targetCalibrationElems = targetCalibration.getElements();

        // loop through each s1...-nnn.xml where nnn is the image number
        for (MetadataElement target : targetCalibrationElems) {

            boolean isSelected = false;
            for (String pol : selectedPolarisations) {
                if (target.getName().toUpperCase().contains(pol)) {
                    isSelected = true;
                    break;
                }
            }

            if (!isSelected) {
                //System.out.println("remove calibration for " + target.getName());
                targetCalibration.removeElement(target);
                continue;
            }

            //System.out.println("update calibration for " + target.getName());

            final String imageNum = extractImageNumber(target.getName());
            final MetadataElement targetCalib = target.getElement("calibration");

            // Update stopTime in adsHeader
            final MetadataElement targetADSHeader = targetCalib.getElement("adsHeader");
            final ProductData lastSliceStopTime = getStopTime(lastSliceProduct, imageNum);
            AbstractMetadata.setAttribute(targetADSHeader, "stopTime", lastSliceStopTime.getElemString());

            // Update calibrationVectorList

            final MetadataElement targetCalibVectorList = targetCalib.getElement("calibrationVectorList");

            int numLines = Integer.parseInt(targetCalibVectorList.getAttribute("count").getData().getElemString());
            final int[] pixelSpacing = new int[1];
            int numPixels = getCalibrationPixelCount(targetProduct, imageNum, pixelSpacing);
            int height = sliceProducts[0].getSceneRasterHeight();
            //System.out.println("initial numLines = " + numLines + " numPixels = " + numPixels + " height = " + height);

            // Loop through 2nd to last slice products in order and concatenate the calibration vectors from each
            // slice to the bottom of target
            for (int i = 1; i < sliceProducts.length; i++) {

                final Product sliceProduct = sliceProducts[i];
                final int[] slicePixelSpacing = new int[1];
                final int sliceNumPixels = getCalibrationPixelCount(sliceProduct, imageNum, slicePixelSpacing);

                // TODO
                /*if (pixelSpacing != slicePixelSpacing) {
                    throw new OperatorException("slice products have different pixel spacing in calibration vectors");
                }*/

                final MetadataElement targetLastCalibVector = targetCalibVectorList.getElementAt(targetCalibVectorList.getNumElements()-1);
                int targetLastCalibVectorLine = Integer.parseInt(targetLastCalibVector.getAttributeString("line"));
                //System.out.println("targetLastCalibVectorLine = " + targetLastCalibVectorLine + " sliceNumPixels = " + sliceNumPixels);

                final MetadataElement sliceCalibVectorList = getCalibrationVectorList(sliceProduct, imageNum);
                final int sliceNumLines = Integer.parseInt(sliceCalibVectorList.getAttribute("count").getData().getElemString());

                final int topSliceWidth = sliceProducts[i-1].getSceneRasterWidth();
                final int bottomSliceWidth = sliceProducts[i].getSceneRasterWidth();

                int numLinesRemoved = 0;

                if (targetLastCalibVectorLine == height-1) {

                    concatenateCalibrationVectors(targetCalibVectorList, sliceCalibVectorList, 0, height);

                } else if (numPixels <= sliceNumPixels && topSliceWidth <= bottomSliceWidth) {

                    // Remove excess calibration vectors from target and keep all calibration vectors from bottom
                    // slice

                    int j;
                    for (j = numLines-1; j >= 0; j--) {
                        MetadataElement targetCalibVector = targetCalibVectorList.getElementAt(j);
                        final int targetCalibVectorLine = Integer.parseInt(targetCalibVector.getAttributeString("line"));
                        if (targetCalibVectorLine >= height) {
                            targetCalibVectorList.removeElement(targetCalibVector);
                        } else {
                            break;
                        }
                    }

                    numLinesRemoved = numLines - j - 1;
                    // Must update targetCalibVectorList count before calling concatenateCalibrationVectors()
                    targetCalibVectorList.setAttributeString("count", Integer.toString(numLines - numLinesRemoved));
                    concatenateCalibrationVectors(targetCalibVectorList, sliceCalibVectorList, 0, height);

                } else {

                    int j;

                    // Remove excess calibration vectors at the bottom in target metadata.
                    // "numLines" is thee current number of calibration vectors in the target metadata (with i slices assembled).
                    // "height" is the height of the image after assembling i slices.
                    // Say, height is 975 and there are calibration vectors at line 950, 1000, 1050, 1100. We can remove
                    // those for lines 1050 and 1100. There are slice products with such excess calibration vectors.
                    // They need to be removed before we concatenate the calibration vectors from the next slice.
                    for (j = numLines-1; j >= 0; j--) {
                        MetadataElement targetCalibVector = targetCalibVectorList.getElementAt(j);
                        final int targetCalibVectorLine = Integer.parseInt(targetCalibVector.getAttributeString("line"));
                        if (targetCalibVectorLine < height-1) {
                            // We need one calibration vector whose line is >= height-1 (height-1 is last line of image)
                            // so the j+1 is the last calibration vector we need to keep
                            break;
                        }
                        targetLastCalibVectorLine = targetCalibVectorLine;
                    }
                    // Want to keep j+1 as last calibration vector, start removing at j+2.
                    for (int k = j+2; k < numLines; k++) {
                        MetadataElement targetCalibVector = targetCalibVectorList.getElementAt(k);
                        targetCalibVectorList.removeElement(targetCalibVector);
                    }
                    targetCalibVectorList.setAttributeString("count", Integer.toString(j+2));
                    numLinesRemoved = numLines - (j+2);

                    // Since the slice we are concatenating to target is smaller in width, we want to skip the
                    // starting calibration vectors whose lines are <= the line of the last target calibration vector.
                    // "sliceNumLines" is the number of calibration vectors in the slice.
                    // Note that the line of each slice calibration vector has zero offset, so we have to add "height"
                    // to it before comparing with target.
                    for (j = 0; j < sliceNumLines; j++) {
                        final MetadataElement sliceCalibVector = sliceCalibVectorList.getElementAt(j);
                        final int sliceCalibVectorLine = Integer.parseInt(sliceCalibVector.getAttributeString("line"));
                        if (sliceCalibVectorLine + height > targetLastCalibVectorLine) {
                            // j is the first one we want to keep
                            break;
                        }
                    }
                    numLinesRemoved += j;

                    concatenateCalibrationVectors(targetCalibVectorList, sliceCalibVectorList, j, height);
                }

                numLines += (sliceNumLines - numLinesRemoved);
                targetCalibVectorList.setAttributeString("count", Integer.toString(numLines));
                numPixels = sliceNumPixels;
                height += sliceProduct.getSceneRasterHeight();
            }
        }

        //System.out.println("DONE updateCalibration");
    }

    private void updateTargetProductMetadata() throws Exception {

        // All the metadata has been copied from the 1st slice product to the assembled target product.
        // Now we want to update the metadata that should not be "included", but "merged" or concatenated".

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        final Product firstSliceProduct = sliceProducts[0];
        final Product lastSliceProduct = sliceProducts[sliceProducts.length-1];
        final MetadataElement absFirst = AbstractMetadata.getAbstractedMetadata(firstSliceProduct);
        final MetadataElement absLast = AbstractMetadata.getAbstractedMetadata(lastSliceProduct);

        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_line_time,
                absFirst.getAttributeUTC(AbstractMetadata.first_line_time));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_line_time,
                absLast.getAttributeUTC(AbstractMetadata.last_line_time));

        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_output_lines, targetHeight);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_samples_per_line, targetWidth);

        for(Band band : targetProduct.getBands()) {
            MetadataElement bandMeta = AbstractMetadata.getBandAbsMetadata(absTgt, band);

            AbstractMetadata.setAttribute(bandMeta, AbstractMetadata.first_line_time,
                    absFirst.getAttributeUTC(AbstractMetadata.first_line_time));
            AbstractMetadata.setAttribute(bandMeta, AbstractMetadata.last_line_time,
                    absLast.getAttributeUTC(AbstractMetadata.last_line_time));

            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_output_lines, band.getRasterHeight());
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_samples_per_line, band.getRasterWidth());
        }

        final List<OrbitStateVector> orbVectorList = new ArrayList<>();
        final List<AbstractMetadata.SRGRCoefficientList> srgrList = new ArrayList<>();
        final List<AbstractMetadata.DopplerCentroidCoefficientList> dopList = new ArrayList<>();
        for(Product srcProduct : sliceProducts) {
            final MetadataElement absSrc = AbstractMetadata.getAbstractedMetadata(srcProduct);

            // update orbit state vectors
            final OrbitStateVector[] orbs = AbstractMetadata.getOrbitStateVectors(absSrc);
            orbVectorList.addAll(Arrays.asList(orbs));

            // update srgr coeffs
            final AbstractMetadata.SRGRCoefficientList[] srgr = AbstractMetadata.getSRGRCoefficients(absSrc);
            srgrList.addAll(Arrays.asList(srgr));

            // update Doppler centroid coeffs
            final AbstractMetadata.DopplerCentroidCoefficientList[] dop = AbstractMetadata.getDopplerCentroidCoefficients(absSrc);
            dopList.addAll(Arrays.asList(dop));
        }

        AbstractMetadata.setOrbitStateVectors(absTgt, orbVectorList.toArray(new OrbitStateVector[orbVectorList.size()]));
        AbstractMetadata.setSRGRCoefficients(absTgt, srgrList.toArray(new AbstractMetadata.SRGRCoefficientList[srgrList.size()]));
        AbstractMetadata.setDopplerCentroidCoefficients(absTgt, dopList.toArray(new AbstractMetadata.DopplerCentroidCoefficientList[dopList.size()]));

        updateCalibration();

        //System.out.println("DONE updateTargetProductMetadata");
    }

    private void determineBandStartEndTimes() {
        for(Band targetBand : targetProduct.getBands()) {
            final List<BandLines> bandLineList = new ArrayList<>(sliceProducts.length);
            int height = 0;
            for (Product srcProduct : sliceProducts) {
                final Band srcBand = srcProduct.getBand(targetBand.getName());
                int start = height;
                height += srcBand.getRasterHeight();
                int end = height;
                bandLineList.add(new BandLines(srcBand, start, end));
            }
            final BandLines[] lines = bandLineList.toArray(new BandLines[bandLineList.size()]);
            bandLineMap.put(targetBand, lines);
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancellation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during computation of the target raster.
     */
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int tx0 = targetTileRectangle.x;
            final int ty0 = targetTileRectangle.y;
            final int maxY = ty0 + targetTileRectangle.height;
            final int maxX = tx0 + targetTileRectangle.width;

            final BandLines[] lines = bandLineMap.get(targetBand);
            final ProductData trgData = targetTile.getDataBuffer();
            //final int targetBandWidth = targetBand.getRasterWidth();
            final int targetBandWidth = maxX;

            final TileIndex trgIndex = new TileIndex(targetTile);
            final Rectangle srcRect = new Rectangle();

            //System.out.println("Do band = " + targetBand.getName() + ": tx0 = " + tx0 + " ty0 = " + ty0 + " maxX = " + maxX + " maxY = " + maxY);

            BandLines line = lines[0];
            for(int y=ty0; y < maxY; ++y) {
                boolean validLine = y >= line.start && y < line.end;
                if(!validLine) {
                    for(BandLines l : lines) {
                        if(y >= l.start && y < l.end) {
                            line = l;
                            validLine = true;
                            break;
                        }
                    }
                    if(!validLine) {
                        // should never get here
                        throw new OperatorException("line "+y+" not found in slice products");
                    }
                }

                final int yy = y-line.start;
                srcRect.setBounds(targetTileRectangle.x, yy, targetTileRectangle.width, 1);
                final Tile sourceRaster = getSourceTile(line.band, srcRect);

                final ProductData srcData = sourceRaster.getDataBuffer();
                final TileIndex srcIndex = new TileIndex(sourceRaster);
                trgIndex.calculateStride(y);
                srcIndex.calculateStride(yy);

                for (int x = tx0; x < maxX; ++x) {
                    trgData.setElemDoubleAt(trgIndex.getIndex(x), srcData.getElemDoubleAt(srcIndex.getIndex(x)));
                }
            }

            //System.out.println("DONE band = " + targetBand.getName() + ": tx0 = " + tx0 + " ty0 = " + ty0 + " maxX = " + maxX + " maxY = " + maxY);
        } catch (Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    private static class BandLines {
        final int start;
        final int end;
        final Band band;
        BandLines(final Band band, final int s, final int e) {
            this.band = band;
            this.start = s;
            this.end = e;
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
            super(SliceAssemblyOp.class);
        }
    }
}