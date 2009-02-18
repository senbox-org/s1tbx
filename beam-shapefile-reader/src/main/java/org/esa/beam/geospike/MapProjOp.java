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
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.actions.GeoCodingMathTransform;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.processing.Operations;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.NoSuchIdentifierException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;

import java.awt.Rectangle;
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
        Envelope2D envelope = new Envelope2D(gridCRS, 0, 0, sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        Band sourceBand = sourceProduct.getBandAt(0);
        GridCoverage2D sourceCoverage = factory.create(sourceBand.getName(), sourceBand.getSourceImage(), envelope);
       
        CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
       
        GridCoverage2D targetCoverage = (GridCoverage2D) Operations.DEFAULT.resample(sourceCoverage, targetCRS);
        RenderedImage targetImage = targetCoverage.getRenderedImage();
        targetProduct = new Product("n","d",targetImage.getWidth(), targetImage.getHeight());
        Band targetBand = targetProduct.addBand(sourceBand.getName(), sourceBand.getDataType());
        targetBand.setSourceImage(targetImage);
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
