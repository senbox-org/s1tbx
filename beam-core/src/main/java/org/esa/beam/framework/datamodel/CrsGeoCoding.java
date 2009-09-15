package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.Ellipsoid;
import org.esa.beam.util.Debug;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.operation.AbstractCoordinateOperationFactory;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.resources.CRSUtilities;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class CrsGeoCoding extends AbstractGeoCoding {

    private final Rectangle2D imageBounds2D;
    private final AffineTransform imageToModel;

    private final MathTransform image2Base;
    private final MathTransform base2image;
    private final Datum datum;
    private final boolean crossingMeridianAt180;

    public CrsGeoCoding(final CoordinateReferenceSystem modelCRS,
                        final Rectangle2D imageBounds2D, 
                        final AffineTransform imageToModel) throws FactoryException,
                                                                                              TransformException {
        this.imageBounds2D = imageBounds2D;
        this.imageToModel = imageToModel;
        org.opengis.referencing.datum.Ellipsoid gtEllipsoid = CRS.getEllipsoid(getModelCRS());
        String ellipsoidName = gtEllipsoid.getName().getCode();
        Ellipsoid ellipsoid = new Ellipsoid(ellipsoidName, gtEllipsoid.getSemiMajorAxis(),
                                            gtEllipsoid.getSemiMinorAxis());
        org.opengis.referencing.datum.Datum gtDatum = CRSUtilities.getDatum(getModelCRS());
        String datumName = gtDatum.getName().getCode();
        this.datum = new Datum(datumName, ellipsoid, 0, 0, 0);

        MathTransform i2m = new AffineTransform2D(imageToModel);
        
        if (modelCRS instanceof DerivedCRS) {
            DerivedCRS derivedCRS = (DerivedCRS) modelCRS;
            CoordinateReferenceSystem baseCRS = derivedCRS.getBaseCRS();
            setBaseCRS(baseCRS);
        } else {
            setBaseCRS(DefaultGeographicCRS.WGS84);
        }
        setGridCRS(new DefaultDerivedCRS("Grid CS based on " + getBaseCRS().getName(),
                                         getBaseCRS(),
                                         i2m.inverse(),
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
        image2Base = mtFactory.createConcatenatedTransform(i2m, model2Base);
        base2image = image2Base.inverse();
        crossingMeridianAt180 = detect180MeridianCrossing();
    }

    @Override
    public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        final AffineTransform destTransform = new AffineTransform(getImageToModelTransform());
        Rectangle2D destBounds = new Rectangle2D.Double(0, 0, destScene.getRasterWidth(), destScene.getRasterHeight());

        if (subsetDef != null) {
            final Rectangle region = subsetDef.getRegion();
            double scaleX = subsetDef.getSubSamplingX();
            double scaleY = subsetDef.getSubSamplingY();
            if (region != null) {
                destTransform.translate(region.getX(), region.getY());
                destBounds.setRect(0, 0, region.getWidth() / scaleX, region.getHeight() / scaleY);
            }
            destTransform.scale(scaleX, scaleY);
        }

        try {
            destScene.setGeoCoding(new CrsGeoCoding(getModelCRS(), destBounds, destTransform));
        } catch (Exception e) {
            Debug.trace(e);
            return false;
        }
        return true;
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
        } catch (Exception ignored) {
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
        } catch (Exception ignored) {
            pixelPos.setInvalid();
        }
        return pixelPos;
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        return crossingMeridianAt180;
    }
    
    @Override
    public AffineTransform getImageToModelTransform() {
        return imageToModel;
    }

    @Override
    public String toString() {
        final String s = super.toString();
        return s + "\n\n" +
               "Model CRS:\n" + getModelCRS().toString() + "\n" +
               "Image To Model:\n" + imageToModel.toString();

    }

    private boolean detect180MeridianCrossing() throws TransformException, FactoryException {
        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(this.imageBounds2D, getImageCRS());
        referencedEnvelope = referencedEnvelope.transform(getBaseCRS(), true);
        final Rectangle2D.Double meridian180 = new Rectangle2D.Double(180 - 1.0e-3, 90, 2 * 1.0e-3, -180);
        final BoundingBox meridian180Envelop = new ReferencedEnvelope(meridian180, getBaseCRS());
        return referencedEnvelope.intersects(meridian180Envelop);
    }
}
