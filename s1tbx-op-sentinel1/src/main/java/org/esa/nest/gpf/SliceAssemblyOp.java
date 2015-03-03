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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
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
        // But it seems that the slice products can have different widths, which implies that the width of a band
        // in one slice product can be different from the width of the same band in another slice product.
        // TODO: For now, for the product width, we take the max among all slice products. This may not be
        // TODO: correct. It depends on how we have to align the different-sized bands from the slice products.
        // TODO: For now, we assume we align to the left, in which case taking the max is OK.
        for (Product srcProduct : sliceProducts) {
            if (targetWidth < srcProduct.getSceneRasterWidth())
                targetWidth = srcProduct.getSceneRasterWidth();
            targetHeight += srcProduct.getSceneRasterHeight();
        }
    }

    private void computeTargetBandWidthAndHeight(final String bandName, final Dimension dim) throws OperatorException {
        // See comments in computeTargetWidthAndHeight().
        // TODO: For now, for band width, we take the max for that band among all slice products.
        // TODO: This is consistent with the assumption in computeTargetWidthAndHeight().
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

        // We assume here that the slice products will have the same width in the geolocation grid.

        final int[] gridWidths = new int[sliceProducts.length];
        final int[] gridHeights = new int[sliceProducts.length];

        for (int j = 0; j < sliceProducts.length; j++) {
            gridWidths[j] = 0;
            gridHeights[j] = 0;
        }

        int gridHeight = 0;

        i = 0;
        int ptsInPrvSlices = 0;
        for (int j = 0; j < sliceProducts.length; j++) {

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
                    if (gridWidths[j] == 0) // Here we are implicitly assuming that the pixel horizontal spacing is assumed to be the same from line to line.
                        gridWidths[j] = i - ptsInPrvSlices;
                    ++gridHeights[j];
                }

                y[i] = (int) ggPoint.getAttributeDouble("line", 0);
                if (j > 0) {
                    // This is not the first slice
                    for (int k = 0; k < j; k++)
                        y[i] += sliceProducts[k].getSceneRasterHeight();
                }

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
                srcRect.setBounds(0, yy, targetBandWidth, 1);
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