package org.esa.nest.datamodel;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.nest.gpf.OperatorUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.geom.Rectangle2D;

/**
 * help create a CRS GeoCoding
 */
public class CRSGeoCodingHandler {

    private final CoordinateReferenceSystem targetCRS;
    private final CrsGeoCoding geoCoding;
    private final int targetWidth;
    private final int targetHeight;

    public CRSGeoCodingHandler(final Product sourceProduct, final String mapProjection,
                               final double pixelSpacingInDegree, final double pixelSpacingInMeter) throws Exception {

        targetCRS = MapProjectionHandler.getCRS(mapProjection);

        final OperatorUtils.ImageGeoBoundary imageGeoBoundary = OperatorUtils.computeImageGeoBoundary(sourceProduct);

        double pixelSizeX = pixelSpacingInMeter;
        double pixelSizeY = pixelSpacingInMeter;
        if (targetCRS.getName().getCode().equals("WGS84(DD)")) {
            pixelSizeX = pixelSpacingInDegree;
            pixelSizeY = pixelSpacingInDegree;
        }

        final Rectangle2D bounds = new Rectangle2D.Double();
        double lonMin = imageGeoBoundary.lonMin;
        double lonMax = imageGeoBoundary.lonMax;
        /*
        if(lonMin > 180)
            lonMin -= 360;
        if(lonMax > 180)
            lonMax -= 360;
        */
        bounds.setFrameFromDiagonal(lonMin, imageGeoBoundary.latMin, lonMax, imageGeoBoundary.latMax);
        final ReferencedEnvelope boundsEnvelope = new ReferencedEnvelope(bounds, DefaultGeographicCRS.WGS84);
        final ReferencedEnvelope targetEnvelope = boundsEnvelope.transform(targetCRS, true);
        targetWidth = org.esa.beam.util.math.MathUtils.floorInt(targetEnvelope.getSpan(0) / pixelSizeX);
        targetHeight = org.esa.beam.util.math.MathUtils.floorInt(targetEnvelope.getSpan(1) / pixelSizeY);
        geoCoding = new CrsGeoCoding(targetCRS,
                targetWidth,
                targetHeight,
                targetEnvelope.getMinimum(0),
                targetEnvelope.getMaximum(1),
                pixelSizeX, pixelSizeY);
    }

    public int getTargetWidth() {
        return targetWidth;
    }

    public int getTargetHeight() {
        return targetHeight;
    }

    public CoordinateReferenceSystem getTargetCRS() {
        return targetCRS;
    }

    public CrsGeoCoding getCrsGeoCoding() {
        return geoCoding;
    }
}
