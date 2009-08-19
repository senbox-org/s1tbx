package org.esa.beam.gpf.common.reproject;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.AbstractGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Scene;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.Ellipsoid;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.operation.AbstractCoordinateOperationFactory;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.resources.CRSUtilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;

import java.awt.geom.AffineTransform;

public class GeotoolsGeoCoding extends AbstractGeoCoding {

    private final BeamGridGeometry gridGeometry;
    private final Datum datum;
    private MathTransform image2Base;
    private MathTransform base2image;

    public GeotoolsGeoCoding(BeamGridGeometry gridGeometry) throws FactoryException, NoninvertibleTransformException {
        this.gridGeometry = gridGeometry;

        org.opengis.referencing.datum.Ellipsoid gtEllipsoid = CRS.getEllipsoid(getModelCRS());
        String ellipsoidName = gtEllipsoid.getName().getCode();
        Ellipsoid ellipsoid = new Ellipsoid(ellipsoidName, gtEllipsoid.getSemiMajorAxis(),
                                            gtEllipsoid.getSemiMinorAxis());
        org.opengis.referencing.datum.Datum gtDatum = CRSUtilities.getDatum(getModelCRS());
        String datumName = gtDatum.getName().getCode();
        this.datum = new Datum(datumName, ellipsoid, 0, 0, 0);

        MathTransform imageToModel = new AffineTransform2D(gridGeometry.getImageToModel());
        
        final CoordinateReferenceSystem modelCRS = gridGeometry.getModelCRS();
        if (modelCRS instanceof DerivedCRS) {
            DerivedCRS derivedCRS = (DerivedCRS) modelCRS;
            CoordinateReferenceSystem baseCRS = derivedCRS.getBaseCRS();
            setBaseCRS(baseCRS);
        } else {
            setBaseCRS(DefaultGeographicCRS.WGS84);
        }
        setGridCRS(new DefaultDerivedCRS("Grid CS based on " + getBaseCRS().getName(),
                                         getBaseCRS(),
                                         imageToModel.inverse(),
                                         DefaultCartesianCS.DISPLAY));
        setModelCRS(modelCRS);
        
        MathTransform model2Base = CRS.findMathTransform(getModelCRS(), getBaseCRS());

        final CoordinateOperationFactory factory = ReferencingFactoryFinder.getCoordinateOperationFactory(null);
        final MathTransformFactory mtFactory;
        if (factory instanceof AbstractCoordinateOperationFactory) {
            mtFactory = ((AbstractCoordinateOperationFactory) factory).getMathTransformFactory();
        } else {
            mtFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        }
        image2Base = mtFactory.createConcatenatedTransform(imageToModel, model2Base);
        base2image = image2Base.inverse();
        
    }

    @Override
    public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canGetGeoPos() {
        return true;
    }

    @Override
    public boolean canGetPixelPos() {
        return true;
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }

    @Override
    public Datum getDatum() {
        return datum;
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        try {
            DirectPosition directPixelPos = new DirectPosition2D(pixelPos);
            DirectPosition directGeoPos = image2Base.transform(directPixelPos, null);
            geoPos.setLocation((float) directGeoPos.getOrdinate(1), (float) directGeoPos.getOrdinate(0));
        } catch (Exception e) {
            geoPos.setInvalid();
        }
        return geoPos;
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        try {
            DirectPosition directGeoPos = new DirectPosition2D(geoPos.getLon(), geoPos.getLat());
            DirectPosition directPixelPos = base2image.transform(directGeoPos, null);
            pixelPos.setLocation((float) directPixelPos.getOrdinate(0), (float) directPixelPos.getOrdinate(1));
        } catch (Exception e) {
            pixelPos.setInvalid();
        }
        return pixelPos;
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public AffineTransform getImageToModelTransform() {
        return gridGeometry.getImageToModel();
    }
}
