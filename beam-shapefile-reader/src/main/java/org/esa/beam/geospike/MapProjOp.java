/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.geospike;

import com.bc.ceres.core.ProgressMonitor;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.actions.GeoCodingMathTransform;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.processing.Operations;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;

import java.awt.image.RenderedImage;

/**
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
@OperatorMetadata(alias = "Mapproj",
                  internal = false)
public class MapProjOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
    
    @Parameter
    private String projectionName = "Geographic Lat/Lon";
    
//    @Parameter
//    private float pixelX;
//    @Parameter
//    private float pixelY;
//    @Parameter
//    private float easting;
//    @Parameter
//    private float northing;
//    @Parameter
//    private float pixelSizeX;
//    @Parameter
//    private float pixelSizeY;
    
//    private ProductProjectionBuilder productProjectionBuilder;
    
    @Override
    public void initialize() throws OperatorException {
//        MapProjection mapProjection = MapProjectionRegistry.getProjection(projectionName);
//        float orientation = 0.0f;
//        double defaultNoDataValue = MapInfo.DEFAULT_NO_DATA_VALUE;
//        if (sourceProduct.getGeoCoding() instanceof MapGeoCoding) {
//            MapInfo mapInfo = ((MapGeoCoding) getSourceProduct().getGeoCoding()).getMapInfo();
//            orientation = mapInfo.getOrientation();
//            defaultNoDataValue = mapInfo.getNoDataValue();
//        }
//        Datum datum = Datum.WGS_84;
//        MapInfo mapInfo = ProductUtils.createSuitableMapInfo(sourceProduct, mapProjection, orientation, defaultNoDataValue);
//        MapInfo mapInfo = new MapInfo(mapProjection, pixelX, pixelY, easting, northing, pixelSizeX, pixelSizeY, datum);
//        try {
//            productProjectionBuilder = new ProductProjectionBuilder(mapInfo);
//            targetProduct = productProjectionBuilder.readProductNodes(sourceProduct, null);
//        } catch (IOException e) {
//            throw new OperatorException(e);
//        }
        
        GeographicCRS baseCRS = DefaultGeographicCRS.WGS84;
        final GeoCoding geoCoding = sourceProduct.getGeoCoding();
        MathTransform baseToGridMathTransform = new GeoCodingMathTransform(geoCoding, GeoCodingMathTransform.Mode.G2P);
        CoordinateReferenceSystem gridCRS = new DefaultDerivedCRS("The grid CRS",
                                                                  baseCRS,
                                                                  baseToGridMathTransform,
                                                                  DefaultCartesianCS.DISPLAY);

        final GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
        Envelope2D sourceEnvelope = new Envelope2D(gridCRS, 0, 0, sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;

        // replace by GridGeometry computation
        Band testBand = sourceProduct.getBandAt(0);
        GridCoverage2D testSourceCoverage = factory.create(testBand.getName(), testBand.getSourceImage(), sourceEnvelope);
        GridCoverage2D testTargetCoverage = (GridCoverage2D) Operations.DEFAULT.resample(testSourceCoverage, targetCRS);
        RenderedImage testTargetImage = testTargetCoverage.getRenderedImage();
        // end replace
        
        targetProduct = new Product("projected_"+sourceProduct.getName(),
                                    "projection of: "+sourceProduct.getDescription(),
                                    testTargetImage.getWidth(), 
                                    testTargetImage.getHeight());
        addMetadataToProduct(targetProduct);
        addFlagCodingsToProduct(targetProduct);
        addIndexCodingsToProduct(targetProduct);
        
        
        try {
        for (Band sourceBand : sourceProduct.getBands()) {
            Band targetBand = targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
            RenderedImage sourceImage = sourceBand.getSourceImage();
            
            GridCoverage2D sourceCoverage = factory.create(sourceBand.getName(), sourceImage, sourceEnvelope);
            GridCoverage2D targetCoverage = (GridCoverage2D) Operations.DEFAULT.resample(sourceCoverage, targetCRS);
            RenderedImage targetImage = targetCoverage.getRenderedImage();
            targetBand.setSourceImage(targetImage);
            
            FlagCoding sourceFlagCoding = sourceBand.getFlagCoding();
            IndexCoding sourceIndexCoding = sourceBand.getIndexCoding();
            if (sourceFlagCoding != null) {
                String flagCodingName = sourceFlagCoding.getName();
                FlagCoding destFlagCoding = targetProduct.getFlagCodingGroup().get(flagCodingName);
                targetBand.setSampleCoding(destFlagCoding);
            } else if (sourceIndexCoding != null) {
                String indexCodingName = sourceIndexCoding.getName();
                IndexCoding destIndexCoding = targetProduct.getIndexCodingGroup().get(indexCodingName);
                targetBand.setSampleCoding(destIndexCoding);
            }
        }
        ProductUtils.copyBitmaskDefsAndOverlays(sourceProduct, targetProduct);
        copyPlacemarks(sourceProduct.getPinGroup(), targetProduct.getPinGroup(), PlacemarkSymbol.createDefaultPinSymbol());
        copyPlacemarks(sourceProduct.getGcpGroup(), targetProduct.getGcpGroup(), PlacemarkSymbol.createDefaultGcpSymbol());        
        
        }catch (Throwable e) {
            e.printStackTrace();
            // TODO: handle exception
        }
    }
    
    protected void addFlagCodingsToProduct(Product product) {
        final ProductNodeGroup<FlagCoding> flagCodingGroup = sourceProduct.getFlagCodingGroup();
        for (int i = 0; i < flagCodingGroup.getNodeCount(); i++) {
            FlagCoding sourceFlagCoding = flagCodingGroup.get(i);
            FlagCoding destFlagCoding = new FlagCoding(sourceFlagCoding.getName());
            destFlagCoding.setDescription(sourceFlagCoding.getDescription());
            cloneFlags(sourceFlagCoding, destFlagCoding);
            product.getFlagCodingGroup().add(destFlagCoding);
        }
    }

    protected void addIndexCodingsToProduct(Product product) {
        final ProductNodeGroup<IndexCoding> indexCodingGroup = sourceProduct.getIndexCodingGroup();
        for (int i = 0; i < indexCodingGroup.getNodeCount(); i++) {
            IndexCoding sourceIndexCoding = indexCodingGroup.get(i);
            IndexCoding destIndexCoding = new IndexCoding(sourceIndexCoding.getName());
            destIndexCoding.setDescription(sourceIndexCoding.getDescription());
            cloneIndexes(sourceIndexCoding, destIndexCoding);
            product.getIndexCodingGroup().add(destIndexCoding);
        }
    }
    
    protected void addMetadataToProduct(Product product) {
        cloneMetadataElementsAndAttributes(sourceProduct.getMetadataRoot(), product.getMetadataRoot(), 0);
    }
    
    protected void cloneFlags(FlagCoding sourceFlagCoding, FlagCoding destFlagCoding) {
        cloneMetadataElementsAndAttributes(sourceFlagCoding, destFlagCoding, 1);
    }

    protected void cloneIndexes(IndexCoding sourceFlagCoding, IndexCoding destFlagCoding) {
        cloneMetadataElementsAndAttributes(sourceFlagCoding, destFlagCoding, 1);
    }
    
    protected void cloneMetadataElementsAndAttributes(MetadataElement sourceRoot, MetadataElement destRoot, int level) {
        cloneMetadataElements(sourceRoot, destRoot, level);
        cloneMetadataAttributes(sourceRoot, destRoot);
    }

    protected void cloneMetadataElements(MetadataElement sourceRoot, MetadataElement destRoot, int level) {
        for (int i = 0; i < sourceRoot.getNumElements(); i++) {
            MetadataElement sourceElement = sourceRoot.getElementAt(i);
            if (level > 0) {
                MetadataElement element = new MetadataElement(sourceElement.getName());
                element.setDescription(sourceElement.getDescription());
                destRoot.addElement(element);
                cloneMetadataElementsAndAttributes(sourceElement, element, level + 1);
            }
        }
    }

    protected void cloneMetadataAttributes(MetadataElement sourceRoot, MetadataElement destRoot) {
        for (int i = 0; i < sourceRoot.getNumAttributes(); i++) {
            MetadataAttribute sourceAttribute = sourceRoot.getAttributeAt(i);
            destRoot.addAttribute(sourceAttribute.createDeepClone());
        }
    }
    
    private static void copyPlacemarks(ProductNodeGroup<Pin> sourcePlacemarkGroup,
                                       ProductNodeGroup<Pin> targetPlacemarkGroup, PlacemarkSymbol symbol) {
        final Pin[] placemarks = sourcePlacemarkGroup.toArray(new Pin[0]);
        for (Pin placemark : placemarks) {
            final Pin pin1 = new Pin(placemark.getName(), placemark.getLabel(),
                                     placemark.getDescription(), null, placemark.getGeoPos(),
                                     symbol);
            targetPlacemarkGroup.add(pin1);
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        // do nothing
    }
    
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MapProjOp.class);
        }
    }
    
    
}
