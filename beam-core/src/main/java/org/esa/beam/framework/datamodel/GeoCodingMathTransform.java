package org.esa.beam.framework.datamodel;

import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.referencing.operation.transform.AbstractMathTransform;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class GeoCodingMathTransform extends AbstractMathTransform {

    private static final int DIMS = 2;
    private final GeoCoding geoCoding;
    private final Mode mode;
    private final T t;

    public enum Mode {

        P2G,
        G2P
    }

    public GeoCodingMathTransform(GeoCoding geoCoding, Mode mode) {
        this.geoCoding = geoCoding;
        this.mode = mode;
        t = mode == Mode.P2G ? new TP2G() : new TG2P();
    }

    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return new DefaultParameterDescriptorGroup(getClass().getSimpleName(), new GeneralParameterDescriptor[0]);

    }

    @Override
    public int getSourceDimensions() {
        return DIMS;
    }

    @Override
    public int getTargetDimensions() {
        return DIMS;
    }

    @Override
    public MathTransform inverse() throws NoninvertibleTransformException {
        return new GeoCodingMathTransform(geoCoding, mode == Mode.P2G ? Mode.G2P : Mode.P2G);
    }

    @Override
    public void transform(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts) throws TransformException {
        t.transform(geoCoding, srcPts, srcOff, dstPts, dstOff, numPts);
    }

    private interface T {

        void transform(GeoCoding geoCoding, double[] srcPts, int srcOff,
                       double[] dstPts, int dstOff,
                       int numPts) throws TransformException;
    }

    private static class TP2G implements T {

        @Override
        public void transform(GeoCoding geoCoding,
                              double[] srcPts, int srcOff,
                              double[] dstPts, int dstOff,
                              int numPts) throws TransformException {
            GeoPos geoPos = new GeoPos();
            PixelPos pixelPos = new PixelPos();
            for (int i = 0; i < numPts; i++) {
                final int firstIndex = (DIMS * i);
                final int secondIndex = firstIndex + 1;
                pixelPos.x = (float) srcPts[srcOff + firstIndex];
                pixelPos.y = (float) srcPts[srcOff + secondIndex];

                geoCoding.getGeoPos(pixelPos, geoPos);

                dstPts[dstOff + firstIndex] = geoPos.lon;
                dstPts[dstOff + secondIndex] = geoPos.lat;
            }
        }
    }

    private static class TG2P implements T {

        @Override
        public void transform(GeoCoding geoCoding,
                              double[] srcPts, int srcOff,
                              double[] dstPts, int dstOff,
                              int numPts) throws TransformException {
            GeoPos geoPos = new GeoPos();
            PixelPos pixelPos = new PixelPos();
            for (int i = 0; i < numPts; i++) {
                final int firstIndex = (DIMS * i);
                final int secondIndex = firstIndex + 1;
                geoPos.lon = (float) srcPts[srcOff + firstIndex];
                geoPos.lat = (float) srcPts[srcOff + secondIndex];

                geoCoding.getPixelPos(geoPos, pixelPos);

                dstPts[dstOff + firstIndex] = pixelPos.x;
                dstPts[dstOff + secondIndex] = pixelPos.y;
            }
        }
    }
}