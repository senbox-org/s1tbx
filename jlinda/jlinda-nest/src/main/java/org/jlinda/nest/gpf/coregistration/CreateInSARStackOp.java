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
package org.jlinda.nest.gpf.coregistration;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.FeatureCollectionClipper;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.StackUtils;
import org.esa.nest.gpf.TileIndex;
import org.jlinda.core.*;
import org.jlinda.core.Point;
import org.jlinda.nest.dat.coregistration.CreateInSARStackOpUI;

import java.awt.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The CreateStack operator with InSAR functionality.
 *
 */
@OperatorMetadata(alias = "CreateInSARStack",
        category = "InSAR\\InSAR Coregistration",
        authors = "Petar Marinkovic (with contributions of Jun Lu, Luis Veci)",
        copyright = "PPO.labs and European Space Agency",
        description = "Collocates two or more products based on their metadata and/or geo-codings.")
public class CreateInSARStackOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @Parameter(description = "The list of source bands.", alias = "masterBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Master Band")
    private String[] masterBandNames = null;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Slave Bands")
    private String[] slaveBandNames = null;

    private Product masterProduct = null;
    private final Band[] masterBands = new Band[2];

    @TargetProduct(description = "The target product which will use the master's grid.")
    private Product targetProduct = null;

    private String extent = MASTER_EXTENT;
    final static String MASTER_EXTENT = "Master";

    @Parameter(valueSet = {"ORBIT", "GCP"},
            defaultValue = "ORBIT",
            description = "Method to be used in computation of initial offset between master and slave",
            label = "Initial Offset Method")
    private String initialOffsetMethod = "ORBIT";

    public final static String INITIAL_OFFSET_GCP = "GCP";
    public final static String INITIAL_OFFSET_ORBIT = "ORBIT";


    private final Map<Band, Band> sourceRasterMap = new HashMap<Band, Band>(10);
    private final Map<Product, int[]> slaveOffsettMap = new HashMap<Product, int[]>(10);

    private boolean appendToMaster = false;
    private boolean productPixelSpacingChecked = false;

    @Override
    public void initialize() throws OperatorException {

        try {
            if(sourceProduct == null) {
                return;
            }

            if(sourceProduct.length < 2) {
                throw new OperatorException("Please select at least two source products");
            }

            for(final Product prod : sourceProduct) {
                if (prod.getGeoCoding() == null) {
                    throw new OperatorException(
                            MessageFormat.format("Product ''{0}'' has no geo-coding.", prod.getName()));
                }
            }

            if(masterBandNames == null || masterBandNames.length == 0 || getMasterProduct(masterBandNames[0]) == null) {
                final Product defaultProd = sourceProduct[0];
                if(defaultProd != null) {
                    final Band defaultBand = defaultProd.getBandAt(0);
                    if(defaultBand != null) {
                        if(defaultBand.getUnit() != null && defaultBand.getUnit().equals(Unit.REAL))
                            masterBandNames = new String[] { defaultProd.getBandAt(0).getName(),
                                    defaultProd.getBandAt(1).getName() };
                        else
                            masterBandNames = new String[] { defaultBand.getName() };
                    }
                }
                if(masterBandNames.length == 0) {
                    targetProduct = OperatorUtils.createDummyTargetProduct(sourceProduct);
                    return;
                }
            }

            masterProduct = getMasterProduct(masterBandNames[0]);
            if(masterProduct == null) {
                targetProduct = OperatorUtils.createDummyTargetProduct(sourceProduct);
                return;
            }

            appendToMaster = AbstractMetadata.getAbstractedMetadata(masterProduct).
                    getAttributeInt(AbstractMetadata.coregistered_stack, 0) == 1;
            final List<String> masterProductBands = new ArrayList<String>(masterProduct.getNumBands());

            final Band[] slaveBandList = getSlaveBands();
            if(masterProduct == null || slaveBandList.length == 0 || slaveBandList[0] == null) {
                targetProduct = OperatorUtils.createDummyTargetProduct(sourceProduct);
                return;
            }

            if(appendToMaster) {
                extent = MASTER_EXTENT;
            }

            targetProduct = new Product(masterProduct.getName(),
                    masterProduct.getProductType(),
                    masterProduct.getSceneRasterWidth(),
                    masterProduct.getSceneRasterHeight());

            OperatorUtils.copyProductNodes(masterProduct, targetProduct);

            if(appendToMaster) {
                // add all master bands
                for(Band b : masterProduct.getBands()) {
                    if(!(b instanceof VirtualBand)) {
                        final Band targetBand = new Band(b.getName(),
                                b.getDataType(),
                                targetProduct.getSceneRasterWidth(),
                                targetProduct.getSceneRasterHeight());
                        ProductUtils.copyRasterDataNodeProperties(b, targetBand);
                        targetBand.setSourceImage(b.getSourceImage());

                        masterProductBands.add(b.getName());
                        sourceRasterMap.put(targetBand, b);
                        targetProduct.addBand(targetBand);
                    }
                }
            }

            String suffix = "_mst";
            // add master bands first
            if(!appendToMaster) {
                for (final Band srcBand : slaveBandList) {
                    if(srcBand == masterBands[0] || (masterBands.length > 1 && srcBand == masterBands[1])) {
                        suffix = "_mst" + StackUtils.getBandTimeStamp(srcBand.getProduct());

                        final Band targetBand = new Band(srcBand.getName() + suffix,
                                srcBand.getDataType(),
                                targetProduct.getSceneRasterWidth(),
                                targetProduct.getSceneRasterHeight());
                        ProductUtils.copyRasterDataNodeProperties(srcBand, targetBand);
                        if(extent.equals(MASTER_EXTENT)) {
                            targetBand.setSourceImage(srcBand.getSourceImage());
                        }

                        masterProductBands.add(targetBand.getName());
                        sourceRasterMap.put(targetBand, srcBand);
                        targetProduct.addBand(targetBand);
                    }
                }
            }
            // then add slave bands
            int cnt = 1;
            if(appendToMaster) {
                for(Band trgBand : targetProduct.getBands()) {
                    final String name = trgBand.getName();
                    if(name.contains("slv"+cnt))
                        ++cnt;
                }
            }
            for (final Band srcBand : slaveBandList) {
                if(!(srcBand == masterBands[0] || (masterBands.length > 1 && srcBand == masterBands[1]))) {
                    if(srcBand.getUnit() != null && srcBand.getUnit().equals(Unit.IMAGINARY)) {
                    } else {
                        suffix = "_slv" + cnt++ + StackUtils.getBandTimeStamp(srcBand.getProduct());
                    }
                    final String tgtBandName = srcBand.getName() + suffix;

                    if(targetProduct.getBand(tgtBandName) == null) {
                        final Product srcProduct = srcBand.getProduct();
                        final Band targetBand = new Band(tgtBandName,
                                srcBand.getDataType(),
                                targetProduct.getSceneRasterWidth(),
                                targetProduct.getSceneRasterHeight());
                        ProductUtils.copyRasterDataNodeProperties(srcBand, targetBand);
                        if (srcProduct == masterProduct || srcProduct.isCompatibleProduct(targetProduct, 1.0e-3f)) {
                            targetBand.setSourceImage(srcBand.getSourceImage());
                        }

                        if(srcBand.getProduct() == masterProduct) {
                            masterProductBands.add(tgtBandName);
                        }

                        sourceRasterMap.put(targetBand, srcBand);
                        targetProduct.addBand(targetBand);
                    }
                }
            }

            // copy slave abstracted metadata
            copySlaveMetadata();

            StackUtils.saveMasterProductBandNames(targetProduct,
                    masterProductBands.toArray(new String[masterProductBands.size()]));
            saveSlaveProductNames(targetProduct, sourceRasterMap);

            // create temporary metadata
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
            absRoot.setAttributeInt("collocated_stack", 1);

            // copy GCPs if found to master band
            final ProductNodeGroup<Placemark> masterGCPgroup = masterProduct.getGcpGroup();
            if (masterGCPgroup.getNodeCount() > 0) {
                OperatorUtils.copyGCPsToTarget(masterGCPgroup, targetProduct.getGcpGroup(targetProduct.getBandAt(0)),
                        targetProduct.getGeoCoding());
            }

            if (initialOffsetMethod.equals(INITIAL_OFFSET_GCP)) {
                computeTargetSlaveCoordinateOffsets_GCP();
            }

            if (initialOffsetMethod.equals(INITIAL_OFFSET_ORBIT)) {
                computeTargetSlaveCoordinateOffsets_Orbits();
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void copySlaveMetadata() {
        final MetadataElement targetSlaveMetadataRoot = AbstractMetadata.getSlaveMetadata(targetProduct);
        for(Product prod : sourceProduct) {
            if(prod != masterProduct) {
                final MetadataElement slvAbsMetadata = AbstractMetadata.getAbstractedMetadata(prod);
                if(slvAbsMetadata != null) {
                    final String timeStamp = StackUtils.getBandTimeStamp(prod);
                    final MetadataElement targetSlaveMetadata = new MetadataElement(prod.getName()+timeStamp);
                    targetSlaveMetadataRoot.addElement(targetSlaveMetadata);
                    ProductUtils.copyMetadata(slvAbsMetadata, targetSlaveMetadata);
                }
            }
        }
    }

    private void saveSlaveProductNames(final Product targetProduct, final Map<Band, Band> sourceRasterMap) {

        for(Product prod : sourceProduct) {
            if(prod != masterProduct) {
                final String suffix = StackUtils.getBandTimeStamp(prod);
                final List<String> bandNames = new ArrayList<String>(10);
                for(Band tgtBand : sourceRasterMap.keySet()) {
                    final Band srcBand = sourceRasterMap.get(tgtBand);
                    final Product srcProduct = srcBand.getProduct();
                    if(srcProduct == prod) {
                        bandNames.add(tgtBand.getName());
                    }
                }
                final String prodName = prod.getName() + suffix;
                StackUtils.saveSlaveProductBandNames(targetProduct, prodName, bandNames.toArray(new String[bandNames.size()]));
            }
        }
    }

    private Product getMasterProduct(final String name) {
        final String masterName = getProductName(name);
        for(Product prod : sourceProduct) {
            if(prod.getName().equals(masterName)) {
                return prod;
            }
        }
        return null;
    }

    private Band[] getSlaveBands() throws OperatorException {
        final List<Band> bandList = new ArrayList<Band>(5);

        // add master band
        if(masterProduct == null) {
            throw new OperatorException("masterProduct is null");
        }
        if(masterBandNames.length > 2) {
            throw new OperatorException("Master band should be one real band or a real and imaginary band");
        }
        masterBands[0] = masterProduct.getBand(getBandName(masterBandNames[0]));
        if(!appendToMaster)
            bandList.add(masterBands[0]);

        final String unit = masterBands[0].getUnit();
        if(unit != null) {
            if (unit.contains(Unit.PHASE)) {
                throw new OperatorException("Phase band should not be selected for co-registration");
            } else if (unit.contains(Unit.IMAGINARY)) {
                throw new OperatorException("Real and imaginary master bands should be selected in pairs");
            } else if (unit.contains(Unit.REAL)) {
                if(masterBandNames.length < 2) {
                    if (!contains(masterBandNames, slaveBandNames[0])) {
                        throw new OperatorException("Real and imaginary master bands should be selected in pairs");
                    } else {
                        final int iBandIdx = masterProduct.getBandIndex(getBandName(masterBandNames[0]));
                        masterBands[1] = masterProduct.getBandAt(iBandIdx+1);
                        if(!masterBands[1].getUnit().equals(Unit.IMAGINARY))
                            throw new OperatorException("For complex products select a real and an imaginary band");
                        if(!appendToMaster)
                            bandList.add(masterBands[1]);
                    }
                } else {
                    final Product prod = getMasterProduct(masterBandNames[1]);
                    if(prod != masterProduct) {
                        throw new OperatorException("Please select master bands from the same product");
                    }
                    masterBands[1] = masterProduct.getBand(getBandName(masterBandNames[1]));
                    if(!masterBands[1].getUnit().equals(Unit.IMAGINARY))
                        throw new OperatorException("For complex products select a real and an imaginary band");
                    if(!appendToMaster)
                        bandList.add(masterBands[1]);
                }
            }
        }

        // add slave bands
        if(slaveBandNames == null || slaveBandNames.length == 0 || contains(masterBandNames, slaveBandNames[0])) {
            for(Product slvProduct : sourceProduct) {
                for(Band band : slvProduct.getBands()) {
                    if(band.getUnit() != null && band.getUnit().equals(Unit.PHASE))
                        continue;
                    if(band instanceof VirtualBand)
                        continue;
                    if(slvProduct == masterProduct && (band == masterBands[0] || band == masterBands[1] || appendToMaster))
                        continue;

                    bandList.add(band);
                }
            }
        } else {

            for(int i = 0; i < slaveBandNames.length; i++) {
                final String name = slaveBandNames[i];
                if(contains(masterBandNames, name)) {
                    throw new OperatorException("Please do not select the same band as master and slave");
                }
                final String bandName = getBandName(name);
                final String productName = getProductName(name);

                final Product prod = getProduct(productName, bandName);
                if(prod == null) continue;

                final Band band = prod.getBand(bandName);
                final String bandUnit = band.getUnit();
                if(bandUnit != null) {
                    if (bandUnit.contains(Unit.PHASE)) {
                        throw new OperatorException("Phase band should not be selected for co-registration");
                    } else if (bandUnit.contains(Unit.REAL) || bandUnit.contains(Unit.IMAGINARY)) {
                        if (slaveBandNames.length < 2) {
                            throw new OperatorException("Real and imaginary slave bands should be selected in pairs");
                        }
                        final String nextBandName = getBandName(slaveBandNames[i+1]);
                        final String nextBandProdName = getProductName(slaveBandNames[i+1]);
                        if (!nextBandProdName.contains(productName)){
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
        for(Product prod : sourceProduct) {
            if(prod.getName().equals(productName)) {
                if(prod.getBand(bandName) != null)
                    return prod;
            }
        }
        return null;
    }

    private static boolean contains(final String[] nameList, final String name) {
        for(String nameInList : nameList) {
            if(name.equals(nameInList))
                return true;
        }
        return false;
    }

    private static String getBandName(final String name) {
        if(name.contains("::"))
            return name.substring(0, name.indexOf("::"));
        return name;
    }

    private String getProductName(final String name) {
        if(name.contains("::"))
            return name.substring(name.indexOf("::")+2, name.length());
        return sourceProduct[0].getName();
    }

    private void computeTargetSlaveCoordinateOffsets_GCP() throws Exception {

        final GeoCoding targGeoCoding = targetProduct.getGeoCoding();
        final int targImageWidth = targetProduct.getSceneRasterWidth();
        final int targImageHeight = targetProduct.getSceneRasterHeight();

        final Geometry tgtGeometry = FeatureCollectionClipper.createGeoBoundaryPolygon(targetProduct);

        final PixelPos slvPixelPos = new PixelPos();
        final PixelPos tgtPixelPos = new PixelPos();
        final GeoPos slvGeoPos = new GeoPos();

        for (final Product slvProd : sourceProduct) {
            if(slvProd == masterProduct && extent.equals(MASTER_EXTENT)) {
                slaveOffsettMap.put(slvProd, new int[] {0,0});
                continue;
            }

            final GeoCoding slvGeoCoding = slvProd.getGeoCoding();
            final int slvImageWidth = slvProd.getSceneRasterWidth();
            final int slvImageHeight = slvProd.getSceneRasterHeight();

            boolean foundOverlapPoint = false;

            // test corners
            slvGeoCoding.getGeoPos(new PixelPos(10, 10), slvGeoPos);
            if(false) {// (pixelPosValid(targGeoCoding, slvGeoPos, tgtPixelPos, targImageWidth, targImageHeight)) {

                addOffset(slvProd, 0 - (int)tgtPixelPos.x, 0 - (int)tgtPixelPos.y);
                foundOverlapPoint = true;
            }
            if (false) {//!foundOverlapPoint) {
                slvGeoCoding.getGeoPos(new PixelPos(slvImageWidth-10, slvImageHeight-10), slvGeoPos);
                if (pixelPosValid(targGeoCoding, slvGeoPos, tgtPixelPos, targImageWidth, targImageHeight)) {

                    addOffset(slvProd, 0 - slvImageWidth - (int)tgtPixelPos.x, slvImageHeight - (int)tgtPixelPos.y);
                    foundOverlapPoint = true;
                }
            }

            if (!foundOverlapPoint) {
                final Geometry slvGeometry = FeatureCollectionClipper.createGeoBoundaryPolygon(slvProd);
                final Geometry intersect = tgtGeometry.intersection(slvGeometry);

                for(Coordinate c : intersect.getCoordinates()) {
                    getPixelPos((float)c.y, (float)c.x, slvGeoCoding, slvPixelPos);

                    if (slvPixelPos.isValid() && slvPixelPos.x >= 0 && slvPixelPos.x < slvImageWidth &&
                            slvPixelPos.y >= 0 && slvPixelPos.y < slvImageHeight) {

                        getPixelPos((float)c.y, (float)c.x, targGeoCoding, tgtPixelPos);
                        if (tgtPixelPos.isValid() && tgtPixelPos.x >= 0 && tgtPixelPos.x < targImageWidth &&
                                tgtPixelPos.y >= 0 && tgtPixelPos.y < targImageHeight) {

                            addOffset(slvProd, (int)slvPixelPos.x - (int)tgtPixelPos.x, (int)slvPixelPos.y - (int)tgtPixelPos.y);
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

            if (!foundOverlapPoint)  {
                throw new OperatorException("Product " + slvProd.getName() + " has no overlap with master product.");
            }
        }
    }

    private void computeTargetSlaveCoordinateOffsets_Orbits() throws Exception {

        // Note: This procedure will always compute some overlap

        // Similar as for GCPs but for every GCP use orbit information
        MetadataElement root = AbstractMetadata.getAbstractedMetadata(targetProduct);

        final int orbitDegree = 3;

        SLCImage metaMaster = new SLCImage(root);
        Orbit orbitMaster = new Orbit(root, orbitDegree);
        SLCImage metaSlave;
        Orbit orbitSlave;

        // Reference point in Master radar geometry
        Point tgtLP = metaMaster.getApproxRadarCentreOriginal();

        for (final Product slvProd : sourceProduct) {

            if(slvProd == masterProduct) {
                // if master is ref product put 0-es for offset
                slaveOffsettMap.put(slvProd, new int[] {0,0});
                continue;
            }

            // Slave metadata
            root = AbstractMetadata.getAbstractedMetadata(slvProd);
            metaSlave = new SLCImage(root);
            orbitSlave = new Orbit(root, orbitDegree);

            // (lp_master) & (master_orbit)-> (xyz_master) & (slave_orbit)-> (lp_slave)
            Point tgtXYZ = orbitMaster.lp2xyz(tgtLP, metaMaster);
            Point slvLP = orbitSlave.xyz2lp(tgtXYZ, metaSlave);

            // Offset: slave minus master
            Point offsetLP = slvLP.min(tgtLP);

            int offsetX = (int)Math.floor(offsetLP.x + .5);
            int offsetY = (int)Math.floor(offsetLP.y + .5);

            addOffset(slvProd, offsetX, offsetY);

        }
    }

    private static boolean pixelPosValid(final GeoCoding geoCoding, final GeoPos geoPos, final PixelPos pixelPos,
                                         final int width, final int height) {
        geoCoding.getPixelPos(geoPos, pixelPos);
        return (pixelPos.isValid() && pixelPos.x >= 0 && pixelPos.x < width &&
                pixelPos.y >= 0 && pixelPos.y < height);
    }

    private static void getPixelPos(final float lat, final float lon, final GeoCoding srcGeoCoding, final PixelPos pixelPos) {
        srcGeoCoding.getPixelPos(new GeoPos(lat, lon), pixelPos);
    }

    private void addOffset(final Product slvProd, final int offsetX, final int offsetY) {
        slaveOffsettMap.put(slvProd, new int[]{offsetX, offsetY});
    }

    @Override
    public void computeTile(final Band targetBand, final Tile targetTile, final ProgressMonitor pm) throws OperatorException {
        try {
            final Band sourceRaster = sourceRasterMap.get(targetBand);
            final Product srcProduct = sourceRaster.getProduct();
            final int srcImageWidth = srcProduct.getSceneRasterWidth();
            final int srcImageHeight = srcProduct.getSceneRasterHeight();

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

            final int[] offset = slaveOffsettMap.get(srcProduct);
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
            if(trgDataType == srcData.getType() &&
                    (trgDataType == ProductData.TYPE_INT16 || trgDataType == ProductData.TYPE_INT32)) {
                isInt = true;
            }

            for (int ty = ty0; ty < maxY; ++ty) {
                final int sy = ty + offset[1];
                final int trgOffset = trgIndex.calculateStride(ty);
                if(sy < 0 || sy >= srcImageHeight) {
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
                        if(isInt)
                            trgData.setElemIntAt(tx - trgOffset, srcData.getElemIntAt(sx - srcOffset));
                        else
                            trgData.setElemDoubleAt(tx - trgOffset, srcData.getElemDoubleAt(sx - srcOffset));
                    }
                }
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }


    private synchronized void checkProductPixelSpacings() throws OperatorException {

        if (productPixelSpacingChecked) {
            return;
        }
        try {

        } catch(Throwable e) {
            throw new OperatorException(e.getMessage());
        }

        productPixelSpacingChecked = true;
    }

    public static void checkPixelSpacing(final Product[] sourceProducts) throws Exception {
        double savedRangeSpacing = 0.0;
        double savedAzimuthSpacing = 0.0;
        for(final Product prod : sourceProducts) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(prod);
            if (absRoot == null) {
                throw new OperatorException(
                        MessageFormat.format("Product ''{0}'' has no abstract metadata.", prod.getName()));
            }

            final double rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
            final double azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
            double a = Math.abs(rangeSpacing - savedRangeSpacing);
            double b = Math.abs(azimuthSpacing - savedAzimuthSpacing);
            if(savedRangeSpacing > 0.0 && savedAzimuthSpacing > 0.0 &&
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

    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CreateInSARStackOp.class);
            setOperatorUI(CreateInSARStackOpUI.class);
        }
    }
}
