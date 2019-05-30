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
package org.esa.s1tbx.insar.gpf.coregistration;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.s1tbx.insar.gpf.InSARStackOverview;
import org.esa.snap.core.dataio.ProductSubsetBuilder;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.VirtualBand;
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
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.ProductInformation;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.jlinda.core.Orbit;
import org.jlinda.core.Point;
import org.jlinda.core.SLCImage;

import java.awt.Rectangle;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The CreateStack operator.
 */
@OperatorMetadata(alias = "CreateStack",
        category = "Radar/Coregistration/Stack Tools",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Collocates two or more products based on their geo-codings.")
public class CreateStackOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @Parameter(description = "The list of source bands.", alias = "masterBands",
            rasterDataNodeType = Band.class, label = "Master Band")
    private String[] masterBandNames = null;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Slave Bands")
    private String[] slaveBandNames = null;

    private Product masterProduct = null;
    private final Band[] masterBands = new Band[2];

    @TargetProduct(description = "The target product which will use the master's grid.")
    private Product targetProduct = null;

    @Parameter(defaultValue = "NONE",
            description = "The method to be used when resampling the slave grid onto the master grid.",
            label = "Resampling Type")
    private String resamplingType = "NONE";
    private Resampling selectedResampling = null;

    @Parameter(valueSet = {MASTER_EXTENT, MIN_EXTENT, MAX_EXTENT},
            defaultValue = MASTER_EXTENT,
            description = "The output image extents.",
            label = "Output Extents")
    private String extent = MASTER_EXTENT;

    public final static String MASTER_EXTENT = "Master";
    public final static String MIN_EXTENT = "Minimum";
    public final static String MAX_EXTENT = "Maximum";

    public final static String INITIAL_OFFSET_GEOLOCATION = "Product Geolocation";
    public final static String INITIAL_OFFSET_ORBIT = "Orbit";

    @Parameter(valueSet = {INITIAL_OFFSET_ORBIT, INITIAL_OFFSET_GEOLOCATION},
            defaultValue = INITIAL_OFFSET_ORBIT,
            description = "Method for computing initial offset between master and slave",
            label = "Initial Offset Method")
    private String initialOffsetMethod = INITIAL_OFFSET_ORBIT;

    private final Map<Band, Band> sourceRasterMap = new HashMap<>(10);
    private final Map<Product, int[]> slaveOffsetMap = new HashMap<>(10);

    private boolean appendToMaster = false;
    private boolean productPixelSpacingChecked = false;

    private static final String PRODUCT_SUFFIX = "_Stack";

    @Override
    public void initialize() throws OperatorException {

        try {
            if (sourceProduct == null) {
                return;
            }

            if (sourceProduct.length < 2) {
                throw new OperatorException("Please select at least two source products");
            }

            for (final Product prod : sourceProduct) {
                final InputProductValidator validator = new InputProductValidator(prod);
                if(validator.isTOPSARProduct() && !validator.isDebursted()) {
                    throw new OperatorException("For S1 TOPS SLC products, TOPS Coregistration should be used");
                }

                if (prod.getSceneGeoCoding() == null) {
                    throw new OperatorException(
                            MessageFormat.format("Product ''{0}'' has no geo-coding", prod.getName()));
                }
            }

            if (masterBandNames == null || masterBandNames.length == 0 || getMasterProduct(masterBandNames[0]) == null) {
                final Product defaultProd = sourceProduct[0];
                if (defaultProd != null) {
                    final Band defaultBand = defaultProd.getBandAt(0);
                    if (defaultBand != null) {
                        if (defaultBand.getUnit() != null && defaultBand.getUnit().equals(Unit.REAL))
                            masterBandNames = new String[]{defaultProd.getBandAt(0).getName(),
                                    defaultProd.getBandAt(1).getName()};
                        else
                            masterBandNames = new String[]{defaultBand.getName()};
                    }
                }
                if (masterBandNames.length == 0) {
                    targetProduct = OperatorUtils.createDummyTargetProduct(sourceProduct);
                    return;
                }
            }

            masterProduct = getMasterProduct(masterBandNames[0]);
            if (masterProduct == null) {
                targetProduct = OperatorUtils.createDummyTargetProduct(sourceProduct);
                return;
            }

            appendToMaster = AbstractMetadata.getAbstractedMetadata(masterProduct).
                    getAttributeInt(AbstractMetadata.coregistered_stack, 0) == 1 ||
                    AbstractMetadata.getAbstractedMetadata(masterProduct).getAttributeInt("collocated_stack", 0) == 1;
            final List<String> masterProductBands = new ArrayList<>(masterProduct.getNumBands());

            final Band[] slaveBandList = getSlaveBands();
            if (masterProduct == null || slaveBandList.length == 0 || slaveBandList[0] == null) {
                targetProduct = OperatorUtils.createDummyTargetProduct(sourceProduct);
                return;
            }

            if (resamplingType.contains("NONE") && !extent.equals(MASTER_EXTENT)) {
                throw new OperatorException("Please select only Master extents when resampling type is None");
            }

            if (appendToMaster) {
                extent = MASTER_EXTENT;
            }

            switch (extent) {
                case MASTER_EXTENT:

                    targetProduct = new Product(masterProduct.getName() + PRODUCT_SUFFIX,
                                                masterProduct.getProductType(),
                                                masterProduct.getSceneRasterWidth(),
                                                masterProduct.getSceneRasterHeight());

                    ProductUtils.copyProductNodes(masterProduct, targetProduct);
                    break;
                case MIN_EXTENT:
                    determineMinExtents();
                    break;
                default:
                    determineMaxExtents();
                    break;
            }

            if (appendToMaster) {
                // add all master bands
                for (Band b : masterProduct.getBands()) {
                    if (!(b instanceof VirtualBand)) {
                        final Band targetBand = new Band(b.getName(),
                                                         b.getDataType(),
                                                         targetProduct.getSceneRasterWidth(),
                                                         targetProduct.getSceneRasterHeight());
                        masterProductBands.add(b.getName());
                        sourceRasterMap.put(targetBand, b);
                        targetProduct.addBand(targetBand);

                        ProductUtils.copyRasterDataNodeProperties(b, targetBand);
                        targetBand.setSourceImage(b.getSourceImage());
                    }
                }
            }

            String suffix = StackUtils.MST;
            // add master bands first
            if (!appendToMaster) {
                for (final Band srcBand : slaveBandList) {
                    if (srcBand.getProduct() == masterProduct) {
                        suffix = StackUtils.MST + StackUtils.createBandTimeStamp(srcBand.getProduct());
                        int dataType;
                        if (!extent.equals(MAX_EXTENT)) {
                            dataType = srcBand.getDataType();
                        } else {
                            dataType = ProductData.TYPE_FLOAT32;
                        }

                        final Band targetBand = new Band(srcBand.getName() + suffix,
                                                         dataType,
                                                         targetProduct.getSceneRasterWidth(),
                                                         targetProduct.getSceneRasterHeight());
                        masterProductBands.add(targetBand.getName());
                        sourceRasterMap.put(targetBand, srcBand);
                        targetProduct.addBand(targetBand);

                        ProductUtils.copyRasterDataNodeProperties(srcBand, targetBand);
                        if (extent.equals(MASTER_EXTENT)) {
                            targetBand.setSourceImage(srcBand.getSourceImage());
                        }

                        fixDependencies(targetBand, slaveBandList, suffix);
                    }
                }
            }
            // then add slave bands
            int cnt = 1;
            if (appendToMaster) {
                for (Band trgBand : targetProduct.getBands()) {
                    final String name = trgBand.getName();
                    if (name.contains(StackUtils.SLV + cnt))
                        ++cnt;
                }
            }
            for (final Band srcBand : slaveBandList) {
                if (srcBand.getProduct() != masterProduct) {
                    if (srcBand.getUnit() != null && srcBand.getUnit().equals(Unit.IMAGINARY)) {
                    } else {
                        suffix = StackUtils.SLV + cnt++ + StackUtils.createBandTimeStamp(srcBand.getProduct());
                    }
                    final String tgtBandName = srcBand.getName() + suffix;

                    if (targetProduct.getBand(tgtBandName) == null) {
                        final Product srcProduct = srcBand.getProduct();
                        int dataType;
                        if (resamplingType.contains("NONE")) {
                            dataType = srcBand.getDataType();
                        } else {
                            dataType = ProductData.TYPE_FLOAT32;
                        }

                        final Band targetBand = new Band(tgtBandName,
                                                         dataType,
                                                         targetProduct.getSceneRasterWidth(),
                                                         targetProduct.getSceneRasterHeight());
                        sourceRasterMap.put(targetBand, srcBand);
                        targetProduct.addBand(targetBand);

                        ProductUtils.copyRasterDataNodeProperties(srcBand, targetBand);
                        if (extent.equals(MASTER_EXTENT) && srcProduct.isCompatibleProduct(targetProduct, 1.0e-3f)) {
                            targetBand.setSourceImage(srcBand.getSourceImage());
                        }

                        fixDependencies(targetBand, slaveBandList, suffix);
                        
                        // Disable using of no data value in slave so that valid 0s will be used in the interpolation
                        srcBand.setNoDataValueUsed(false);
                    }
                }
            }

            // copy slave abstracted metadata
            copySlaveMetadata();

            StackUtils.saveMasterProductBandNames(targetProduct,
                                                  masterProductBands.toArray(new String[masterProductBands.size()]));
            StackUtils.saveSlaveProductNames(sourceProduct, targetProduct, masterProduct, sourceRasterMap);

            updateMetadata();

            // copy GCPs if found to master band
            final ProductNodeGroup<Placemark> masterGCPgroup = masterProduct.getGcpGroup();
            if (masterGCPgroup.getNodeCount() > 0) {
                OperatorUtils.copyGCPsToTarget(masterGCPgroup, GCPManager.instance().getGcpGroup(targetProduct.getBandAt(0)),
                                               targetProduct.getSceneGeoCoding());
            }

            if (!resamplingType.contains("NONE")) {
                selectedResampling = ResamplingFactory.createResampling(resamplingType);
                if(selectedResampling == null) {
                    throw new OperatorException("Resampling method "+ selectedResampling + " is invalid");
                }
            } else {
                if(initialOffsetMethod == null) {
                    initialOffsetMethod = INITIAL_OFFSET_ORBIT;
                }
                if (initialOffsetMethod.equals(INITIAL_OFFSET_GEOLOCATION)) {
                    computeTargetSlaveCoordinateOffsets_GCP();
                }
                if (initialOffsetMethod.equals(INITIAL_OFFSET_ORBIT)) {
                    computeTargetSlaveCoordinateOffsets_Orbits();
                }
            }

            // set non-elevation areas to no data value for the master bands using the slave bands
            DEMAssistedCoregistrationOp.setMasterValidPixelExpression(targetProduct, true);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private static void fixDependencies(final Band targetBand, final Band[] srcBandList, final String suffix) {
//        String validPixelExpression = targetBand.getValidPixelExpression();
//        if(validPixelExpression == null || validPixelExpression.isEmpty())
//            return;
//
//        for(Band srcBand : srcBandList) {
//            if(!validPixelExpression.contains(srcBand.getName() + suffix)) {
//                validPixelExpression = validPixelExpression.replaceAll(srcBand.getName(), srcBand.getName() + suffix);
//            }
//        }
//        targetBand.setValidPixelExpression(validPixelExpression);
    }

    private void updateMetadata() {
        final MetadataElement abstractedMetadata = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if(abstractedMetadata != null) {
            abstractedMetadata.setAttributeInt("collocated_stack", 1);
        }

        final MetadataElement inputElem = ProductInformation.getInputProducts(targetProduct);

        getBaselines(sourceProduct, targetProduct);

        for (Product srcProduct : sourceProduct) {
            if (srcProduct == masterProduct)
                continue;

            final MetadataElement slvInputElem = ProductInformation.getInputProducts(srcProduct);
            final MetadataAttribute[] slvInputProductAttrbList = slvInputElem.getAttributes();
            for (MetadataAttribute attrib : slvInputProductAttrbList) {
                final MetadataAttribute inputAttrb = AbstractMetadata.addAbstractedAttribute(inputElem, "InputProduct", ProductData.TYPE_ASCII, "", "");
                inputAttrb.getData().setElems(attrib.getData().getElemString());
            }
        }
    }

    public static void getBaselines(final Product[] sourceProduct, final Product targetProduct) {
        try {
            final MetadataElement abstractedMetadata = AbstractMetadata.getAbstractedMetadata(targetProduct);
            final MetadataElement baselinesElem = getBaselinesElem(abstractedMetadata);

            final InSARStackOverview.IfgStack[] stackOverview = InSARStackOverview.calculateInSAROverview(sourceProduct);

            for(InSARStackOverview.IfgStack stack : stackOverview) {
                final InSARStackOverview.IfgPair[] slaves = stack.getMasterSlave();
                //System.out.println("======");
                //System.out.println("Master: " + StackUtils.createBandTimeStamp(
                //        slaves[0].getMasterMetadata().getAbstractedMetadata().getProduct()).substring(1));

                final MetadataElement masterElem = new MetadataElement("Master: " + StackUtils.createBandTimeStamp(
                        slaves[0].getMasterMetadata().getAbstractedMetadata().getProduct()).substring(1));
                baselinesElem.addElement(masterElem);

                for (InSARStackOverview.IfgPair slave : slaves) {
                    //System.out.println("Slave: " + StackUtils.createBandTimeStamp(
                    //        slave.getSlaveMetadata().getAbstractedMetadata().getProduct()).substring(1) +
                    //        " perp baseline: " + slave.getPerpendicularBaseline() +
                    //        " temp baseline: " + slave.getTemporalBaseline());

                    final MetadataElement slaveElem = new MetadataElement("Slave: " + StackUtils.createBandTimeStamp(
                            slave.getSlaveMetadata().getAbstractedMetadata().getProduct()).substring(1));
                    masterElem.addElement(slaveElem);

                    addAttrib(slaveElem, "Perp Baseline", slave.getPerpendicularBaseline());
                    addAttrib(slaveElem, "Temp Baseline", slave.getTemporalBaseline());
                    addAttrib(slaveElem, "Modelled Coherence", slave.getCoherence());
                    addAttrib(slaveElem, "Height of Ambiguity", slave.getHeightAmb());
                    addAttrib(slaveElem, "Doppler Difference", slave.getDopplerDifference());
                }
                //System.out.println();
            }

        } catch (Exception e) {
            SystemUtils.LOG.warning("Unable to calculate baselines. " + e.getMessage());
        }
    }

    private static void addAttrib(final MetadataElement elem, final String tag, final double value) {
        final MetadataAttribute attrib = new MetadataAttribute(tag, ProductData.TYPE_FLOAT64);
        attrib.getData().setElemDouble(value);
        elem.addAttribute(attrib);
    }

    private static MetadataElement getBaselinesElem(final MetadataElement abstractedMetadata) {
        MetadataElement baselinesElem = abstractedMetadata.getElement("Baselines");
        if (baselinesElem == null) {
            baselinesElem = new MetadataElement("Baselines");
            abstractedMetadata.addElement(baselinesElem);
        }
        return baselinesElem;
    }

    private void copySlaveMetadata() {
        final MetadataElement targetSlaveMetadataRoot = AbstractMetadata.getSlaveMetadata(targetProduct.getMetadataRoot());
        for (Product prod : sourceProduct) {
            if (prod != masterProduct) {
                final MetadataElement slvAbsMetadata = AbstractMetadata.getAbstractedMetadata(prod);
                if (slvAbsMetadata != null) {
                    final String timeStamp = StackUtils.createBandTimeStamp(prod);
                    final MetadataElement targetSlaveMetadata = new MetadataElement(prod.getName() + timeStamp);
                    targetSlaveMetadataRoot.addElement(targetSlaveMetadata);
                    ProductUtils.copyMetadata(slvAbsMetadata, targetSlaveMetadata);
                }
            }
        }
    }

    private Product getMasterProduct(final String name) {
        final String masterName = getProductName(name);
        for (Product prod : sourceProduct) {
            if (prod.getName().equals(masterName)) {
                return prod;
            }
        }
        return null;
    }

    private Band[] getSlaveBands() throws OperatorException {
        final List<Band> bandList = new ArrayList<>(5);

        // add master band
        if (masterProduct == null) {
            throw new OperatorException("masterProduct is null");
        }
        if (masterBandNames.length > 2) {
            throw new OperatorException("Master band should be one real band or a real and imaginary band");
        }
        masterBands[0] = masterProduct.getBand(getBandName(masterBandNames[0]));
        if (!appendToMaster)
            bandList.add(masterBands[0]);

        final String unit = masterBands[0].getUnit();
        if (unit != null) {
            if (unit.contains(Unit.PHASE)) {
                throw new OperatorException("Phase band should not be selected for co-registration");
            } else if (unit.contains(Unit.IMAGINARY)) {
                throw new OperatorException("Real and imaginary master bands should be selected in pairs");
            } else if (unit.contains(Unit.REAL)) {
                if (masterBandNames.length < 2) {
                    if (!contains(masterBandNames, slaveBandNames[0])) {
                        throw new OperatorException("Real and imaginary master bands should be selected in pairs");
                    } else {
                        final int iBandIdx = masterProduct.getBandIndex(getBandName(masterBandNames[0]));
                        masterBands[1] = masterProduct.getBandAt(iBandIdx + 1);
                        if (!masterBands[1].getUnit().equals(Unit.IMAGINARY))
                            throw new OperatorException("For complex products select a real and an imaginary band");
                        if (!appendToMaster)
                            bandList.add(masterBands[1]);
                    }
                } else {
                    final Product prod = getMasterProduct(masterBandNames[1]);
                    if (prod != masterProduct) {
                        //throw new OperatorException("Please select master bands from the same product");
                    }
                    masterBands[1] = masterProduct.getBand(getBandName(masterBandNames[1]));
                    if (!masterBands[1].getUnit().equals(Unit.IMAGINARY))
                        throw new OperatorException("For complex products select a real and an imaginary band");
                    if (!appendToMaster)
                        bandList.add(masterBands[1]);
                }
            }
        }

        // add slave bands
        if (slaveBandNames == null || slaveBandNames.length == 0 || contains(masterBandNames, slaveBandNames[0])) {
            for (Product slvProduct : sourceProduct) {
                for (Band band : slvProduct.getBands()) {
                    if (band.getUnit() != null && band.getUnit().equals(Unit.PHASE))
                        continue;
                    if (band instanceof VirtualBand)
                        continue;
                    if (slvProduct == masterProduct && (band == masterBands[0] || band == masterBands[1] || appendToMaster))
                        continue;

                    bandList.add(band);
                }
            }
        } else {

            for (int i = 0; i < slaveBandNames.length; i++) {
                final String name = slaveBandNames[i];
                if (contains(masterBandNames, name)) {
                    throw new OperatorException("Please do not select the same band as master and slave");
                }
                final String bandName = getBandName(name);
                final String productName = getProductName(name);

                final Product prod = getProduct(productName, bandName);
                if (prod == null) continue;

                final Band band = prod.getBand(bandName);
                final String bandUnit = band.getUnit();
                if (bandUnit != null) {
                    if (bandUnit.contains(Unit.PHASE)) {
                        throw new OperatorException("Phase band should not be selected for co-registration");
                    } else if (bandUnit.contains(Unit.REAL) || bandUnit.contains(Unit.IMAGINARY)) {
                        if (slaveBandNames.length < 2) {
                            throw new OperatorException("Real and imaginary slave bands should be selected in pairs");
                        }
                        final String nextBandName = getBandName(slaveBandNames[i + 1]);
                        final String nextBandProdName = getProductName(slaveBandNames[i + 1]);
                        if (!nextBandProdName.contains(productName)) {
                            throw new OperatorException("Real and imaginary slave bands should be selected from the same product in pairs");
                        }
                        final Band nextBand = prod.getBand(nextBandName);
                        if ((bandUnit.contains(Unit.REAL) && !nextBand.getUnit().contains(Unit.IMAGINARY) ||
                                (bandUnit.contains(Unit.IMAGINARY) && !nextBand.getUnit().contains(Unit.REAL)))) {
                            throw new OperatorException("Real and imaginary slave bands should be selected in pairs");
                        }
                        bandList.add(band);
                        bandList.add(nextBand);
                        i++;
                    } else {
                        bandList.add(band);
                    }
                } else {
                    bandList.add(band);
                }
            }
        }
        return bandList.toArray(new Band[bandList.size()]);
    }

    private Product getProduct(final String productName, final String bandName) {
        for (Product prod : sourceProduct) {
            if (prod.getName().equals(productName)) {
                if (prod.getBand(bandName) != null)
                    return prod;
            }
        }
        return null;
    }

    private static boolean contains(final String[] nameList, final String name) {
        for (String nameInList : nameList) {
            if (name.equals(nameInList))
                return true;
        }
        return false;
    }

    private static String getBandName(final String name) {
        if (name.contains("::"))
            return name.substring(0, name.indexOf("::"));
        return name;
    }

    private String getProductName(final String name) {
        if (name.contains("::"))
            return name.substring(name.indexOf("::") + 2, name.length());
        return sourceProduct[0].getName();
    }

    /**
     * Minimum extents consists of the overlapping area
     */
    private void determineMinExtents() {

        Geometry tgtGeometry = FeatureUtils.createGeoBoundaryPolygon(masterProduct);

        for (final Product slvProd : sourceProduct) {
            if (slvProd == masterProduct) continue;

            final Geometry slvGeometry = FeatureUtils.createGeoBoundaryPolygon(slvProd);
            tgtGeometry = tgtGeometry.intersection(slvGeometry);
        }

        final GeoCoding mstGeoCoding = masterProduct.getSceneGeoCoding();
        final PixelPos pixPos = new PixelPos();
        final GeoPos geoPos = new GeoPos();
        final double mstWidth = masterProduct.getSceneRasterWidth();
        final double mstHeight = masterProduct.getSceneRasterHeight();

        double maxX = 0, maxY = 0;
        double minX = mstWidth;
        double minY = mstHeight;
        for (Coordinate c : tgtGeometry.getCoordinates()) {
            //System.out.println("geo "+c.x +", "+ c.y);
            geoPos.setLocation(c.y, c.x);
            mstGeoCoding.getPixelPos(geoPos, pixPos);
            //System.out.println("pix "+pixPos.x +", "+ pixPos.y);
            if (pixPos.isValid() && pixPos.x != -1 && pixPos.y != -1) {
                if (pixPos.x < minX) {
                    minX = Math.max(0, pixPos.x);
                }
                if (pixPos.y < minY) {
                    minY = Math.max(0, pixPos.y);
                }
                if (pixPos.x > maxX) {
                    maxX = Math.min(mstWidth, pixPos.x);
                }
                if (pixPos.y > maxY) {
                    maxY = Math.min(mstHeight, pixPos.y);
                }
            }
        }

        final ProductSubsetBuilder subsetReader = new ProductSubsetBuilder();
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeNames(masterProduct.getTiePointGridNames());

        subsetDef.setRegion((int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY));
        subsetDef.setSubSampling(1, 1);
        subsetDef.setIgnoreMetadata(false);

        try {
            targetProduct = subsetReader.readProductNodes(masterProduct, subsetDef);
            final Band[] bands = targetProduct.getBands();
            for (Band b : bands) {
                targetProduct.removeBand(b);
            }
        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    /**
     * Maximum extents consists of the overall area
     */
    private void determineMaxExtents() throws Exception {

        final OperatorUtils.SceneProperties scnProp = new OperatorUtils.SceneProperties();
        OperatorUtils.computeImageGeoBoundary(sourceProduct, scnProp);

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(masterProduct);
        double pixelSize = 1;
        if(absRoot != null) {
            final double rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
            final double azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
            pixelSize = Math.min(rangeSpacing, azimuthSpacing);
        }
        OperatorUtils.getSceneDimensions(pixelSize, scnProp);

        int sceneWidth = scnProp.sceneWidth;
        int sceneHeight = scnProp.sceneHeight;
        final double ratio = sceneWidth / (double)sceneHeight;
        long dim = (long) sceneWidth * (long) sceneHeight;
        while (sceneWidth > 0 && sceneHeight > 0 && dim > Integer.MAX_VALUE) {
            sceneWidth -= 1000;
            sceneHeight = (int)(sceneWidth / ratio);
            dim = (long) sceneWidth * (long) sceneHeight;
        }

        targetProduct = new Product(masterProduct.getName(),
                                    masterProduct.getProductType(),
                                    sceneWidth, sceneHeight);

        OperatorUtils.addGeoCoding(targetProduct, scnProp);
    }

    private void computeTargetSlaveCoordinateOffsets_GCP() {

        final GeoCoding targGeoCoding = targetProduct.getSceneGeoCoding();
        final int targImageWidth = targetProduct.getSceneRasterWidth();
        final int targImageHeight = targetProduct.getSceneRasterHeight();

        final Geometry tgtGeometry = FeatureUtils.createGeoBoundaryPolygon(targetProduct);

        final PixelPos slvPixelPos = new PixelPos();
        final PixelPos tgtPixelPos = new PixelPos();
        final GeoPos slvGeoPos = new GeoPos();

        for (final Product slvProd : sourceProduct) {
            if (slvProd == masterProduct && extent.equals(MASTER_EXTENT)) {
                slaveOffsetMap.put(slvProd, new int[]{0, 0});
                continue;
            }

            final GeoCoding slvGeoCoding = slvProd.getSceneGeoCoding();
            final int slvImageWidth = slvProd.getSceneRasterWidth();
            final int slvImageHeight = slvProd.getSceneRasterHeight();

            boolean foundOverlapPoint = false;

            // test corners
            slvGeoCoding.getGeoPos(new PixelPos(10, 10), slvGeoPos);
            if (false) {// (pixelPosValid(targGeoCoding, slvGeoPos, tgtPixelPos, targImageWidth, targImageHeight)) {

                addOffset(slvProd, 0 - (int) tgtPixelPos.x, 0 - (int) tgtPixelPos.y);
                foundOverlapPoint = true;
            }
            if (false) {//!foundOverlapPoint) {
                slvGeoCoding.getGeoPos(new PixelPos(slvImageWidth - 10, slvImageHeight - 10), slvGeoPos);
                if (pixelPosValid(targGeoCoding, slvGeoPos, tgtPixelPos, targImageWidth, targImageHeight)) {

                    addOffset(slvProd, 0 - slvImageWidth - (int) tgtPixelPos.x, slvImageHeight - (int) tgtPixelPos.y);
                    foundOverlapPoint = true;
                }
            }

            if (!foundOverlapPoint) {
                final Geometry slvGeometry = FeatureUtils.createGeoBoundaryPolygon(slvProd);
                final Geometry intersect = tgtGeometry.intersection(slvGeometry);

                for (Coordinate c : intersect.getCoordinates()) {
                    getPixelPos(c.y, c.x, slvGeoCoding, slvPixelPos);

                    if (slvPixelPos.isValid() && slvPixelPos.x >= 0 && slvPixelPos.x < slvImageWidth &&
                            slvPixelPos.y >= 0 && slvPixelPos.y < slvImageHeight) {

                        getPixelPos(c.y, c.x, targGeoCoding, tgtPixelPos);
                        if (tgtPixelPos.isValid() && tgtPixelPos.x >= 0 && tgtPixelPos.x < targImageWidth &&
                                tgtPixelPos.y >= 0 && tgtPixelPos.y < targImageHeight) {

                            addOffset(slvProd, (int) slvPixelPos.x - (int) tgtPixelPos.x, (int) slvPixelPos.y - (int) tgtPixelPos.y);
                            foundOverlapPoint = true;
                            break;
                        }
                    }
                }
            }

            //if(foundOverlapPoint) {
            //    final int[] offset = slaveOffsettMap.get(slvProd);
            //    System.out.println("offset x="+offset[0]+" y="+offset[1]);
            //}

            if (!foundOverlapPoint) {
                throw new OperatorException("Product " + slvProd.getName() + " has no overlap with master product.");
            }
        }
    }

    private void computeTargetSlaveCoordinateOffsets_Orbits() throws Exception {
        try {
            // Note: This procedure will always compute some overlap

            // Similar as for GCPs but for every GCP use orbit information
            if (!AbstractMetadata.hasAbstractedMetadata(targetProduct)) {
                throw new Exception("Orbit offset method is not support for product " + targetProduct.getName());
            }
            MetadataElement root = AbstractMetadata.getAbstractedMetadata(targetProduct);

            final int orbitDegree = 3;

            SLCImage metaMaster = new SLCImage(root, targetProduct);
            Orbit orbitMaster = new Orbit(root, orbitDegree);
            SLCImage metaSlave;
            Orbit orbitSlave;

            // Reference point in Master radar geometry
            Point tgtLP = metaMaster.getApproxRadarCentreOriginal();

            for (final Product slvProd : sourceProduct) {

                if (slvProd == masterProduct) {
                    // if master is ref product put 0-es for offset
                    slaveOffsetMap.put(slvProd, new int[]{0, 0});
                    continue;
                }

                // Slave metadata
                if (!AbstractMetadata.hasAbstractedMetadata(slvProd)) {
                    throw new Exception("Orbit offset method is not support for product " + slvProd.getName());
                }
                root = AbstractMetadata.getAbstractedMetadata(slvProd);
                metaSlave = new SLCImage(root, slvProd);
                orbitSlave = new Orbit(root, orbitDegree);

                // (lp_master) & (master_orbit)-> (xyz_master) & (slave_orbit)-> (lp_slave)
                Point tgtXYZ = orbitMaster.lp2xyz(tgtLP, metaMaster);
                Point slvLP = orbitSlave.xyz2lp(tgtXYZ, metaSlave);

                // Offset: slave minus master
                Point offsetLP = slvLP.min(tgtLP);

                int offsetX = (int) Math.floor(offsetLP.x + .5);
                int offsetY = (int) Math.floor(offsetLP.y + .5);

                addOffset(slvProd, offsetX, offsetY);

            }
        } catch (Exception e) {
            throw new IOException("Orbit offset method is not support for this product: "+e.getMessage());
        }
    }

    private static boolean pixelPosValid(final GeoCoding geoCoding, final GeoPos geoPos, final PixelPos pixelPos,
                                         final int width, final int height) {
        geoCoding.getPixelPos(geoPos, pixelPos);
        return (pixelPos.isValid() && pixelPos.x >= 0 && pixelPos.x < width &&
                pixelPos.y >= 0 && pixelPos.y < height);
    }

    private static void getPixelPos(final double lat, final double lon, final GeoCoding srcGeoCoding, final PixelPos pixelPos) {
        srcGeoCoding.getPixelPos(new GeoPos(lat, lon), pixelPos);
    }

    private void addOffset(final Product slvProd, final int offsetX, final int offsetY) {
        slaveOffsetMap.put(slvProd, new int[]{offsetX, offsetY});
    }

    @Override
    public void computeTile(final Band targetBand, final Tile targetTile, final ProgressMonitor pm) throws OperatorException {
        try {
            final Band sourceRaster = sourceRasterMap.get(targetBand);
            final Product srcProduct = sourceRaster.getProduct();
            final int srcImageWidth = srcProduct.getSceneRasterWidth();
            final int srcImageHeight = srcProduct.getSceneRasterHeight();

            if (resamplingType.contains("NONE")) { // without resampling

                if (!productPixelSpacingChecked) {
                    checkProductPixelSpacings();
                }

                final float noDataValue = (float) targetBand.getGeophysicalNoDataValue();
                final Rectangle targetRectangle = targetTile.getRectangle();
                final ProductData trgData = targetTile.getDataBuffer();
                final int tx0 = targetRectangle.x;
                final int ty0 = targetRectangle.y;
                final int tw = targetRectangle.width;
                final int th = targetRectangle.height;
                final int maxX = tx0 + tw;
                final int maxY = ty0 + th;

                final int[] offset = slaveOffsetMap.get(srcProduct);
                final int sx0 = Math.min(Math.max(0, tx0 + offset[0]), srcImageWidth - 1);
                final int sy0 = Math.min(Math.max(0, ty0 + offset[1]), srcImageHeight - 1);
                final int sw = Math.min(sx0 + tw - 1, srcImageWidth - 1) - sx0 + 1;
                final int sh = Math.min(sy0 + th - 1, srcImageHeight - 1) - sy0 + 1;
                final Rectangle srcRectangle = new Rectangle(sx0, sy0, sw, sh);
                final Tile srcTile = getSourceTile(sourceRaster, srcRectangle);
                final ProductData srcData = srcTile.getDataBuffer();

                final TileIndex trgIndex = new TileIndex(targetTile);
                final TileIndex srcIndex = new TileIndex(srcTile);

                boolean isInt = false;
                final int trgDataType = trgData.getType();
                if (trgDataType == srcData.getType() &&
                        (trgDataType == ProductData.TYPE_INT16 || trgDataType == ProductData.TYPE_INT32)) {
                    isInt = true;
                }

                for (int ty = ty0; ty < maxY; ++ty) {
                    final int sy = ty + offset[1];
                    final int trgOffset = trgIndex.calculateStride(ty);
                    if (sy < 0 || sy >= srcImageHeight) {
                        for (int tx = tx0; tx < maxX; ++tx) {
                            trgData.setElemDoubleAt(tx - trgOffset, noDataValue);
                        }
                        continue;
                    }
                    final int srcOffset = srcIndex.calculateStride(sy);
                    for (int tx = tx0; tx < maxX; ++tx) {
                        final int sx = tx + offset[0];

                        if (sx < 0 || sx >= srcImageWidth) {
                            trgData.setElemDoubleAt(tx - trgOffset, noDataValue);
                        } else {
                            if (isInt)
                                trgData.setElemIntAt(tx - trgOffset, srcData.getElemIntAt(sx - srcOffset));
                            else
                                trgData.setElemDoubleAt(tx - trgOffset, srcData.getElemDoubleAt(sx - srcOffset));
                        }
                    }
                }

            } else { // with resampling

                final Collocator col = new Collocator(this, srcProduct, targetProduct, targetTile.getRectangle());
                col.collocateSourceBand(sourceRaster, targetTile, selectedResampling);
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private synchronized void checkProductPixelSpacings() throws OperatorException {

        if (productPixelSpacingChecked) {
            return;
        }

        productPixelSpacingChecked = true;
    }

    public static void checkPixelSpacing(final Product[] sourceProducts) throws Exception {
        double savedRangeSpacing = 0.0;
        double savedAzimuthSpacing = 0.0;
        for (final Product prod : sourceProducts) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(prod);
            if (absRoot == null) {
                throw new OperatorException(
                        MessageFormat.format("Product ''{0}'' has no abstract metadata.", prod.getName()));
            }

            final double rangeSpacing = absRoot.getAttributeDouble(AbstractMetadata.range_spacing, 0);
            final double azimuthSpacing = absRoot.getAttributeDouble(AbstractMetadata.azimuth_spacing, 0);
            if(rangeSpacing == 0 || azimuthSpacing == 0)
                return;
            if (savedRangeSpacing > 0.0 && savedAzimuthSpacing > 0.0 &&
                    (Math.abs(rangeSpacing - savedRangeSpacing) > 0.05 ||
                            Math.abs(azimuthSpacing - savedAzimuthSpacing) > 0.05)) {
                throw new OperatorException("Resampling type cannot be NONE because pixel spacings" +
                                                    " are different for master and slave products");
            } else {
                savedRangeSpacing = rangeSpacing;
                savedAzimuthSpacing = azimuthSpacing;
            }
        }
    }

    // for unit test
    protected void setTestParameters(final String ext, final String offsetMethod) {
        this.extent = ext;
        this.initialOffsetMethod = offsetMethod;
    }

    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CreateStackOp.class);
        }
    }
}
