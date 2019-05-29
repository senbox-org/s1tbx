package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.DummyProductBuilder;
import org.esa.snap.core.util.DummyProductBuilder.GC;
import org.esa.snap.core.util.DummyProductBuilder.GCOcc;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.operation.matrix.GeneralMatrix;
import org.geotools.referencing.operation.transform.AbstractMathTransform;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.geotools.referencing.cs.DefaultCartesianCS.DISPLAY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Norman
 */
public class ProductSceneCrsTest {
    @Test
    public void testSceneCrsComparisons() throws Exception {
        Product product;
        DummyProductBuilder builder = new DummyProductBuilder();

        product = builder
                .gc(GC.TIE_POINTS)
                .gcOcc(GCOcc.UNIQUE).create();

        assertTrue(product.isSceneCrsASharedModelCrs());
        assertTrue(product.isSceneCrsEqualToModelCrsOf(product.getRasterDataNode("tpgrid_a")));
        assertTrue(product.isSceneCrsEqualToModelCrsOf(product.getRasterDataNode("band_a")));
        assertTrue(product.isSceneCrsEqualToModelCrsOf(product.getRasterDataNode("mask_c")));

        product = builder
                .gc(GC.MAP)
                .gcOcc(GCOcc.UNIQUE).create();

        assertTrue(product.isSceneCrsASharedModelCrs());
        assertTrue(product.isSceneCrsEqualToModelCrsOf(product.getRasterDataNode("tpgrid_a")));
        assertTrue(product.isSceneCrsEqualToModelCrsOf(product.getRasterDataNode("band_a")));
        assertTrue(product.isSceneCrsEqualToModelCrsOf(product.getRasterDataNode("mask_c")));

        product = builder
                .gc(GC.MAP)
                .gcOcc(GCOcc.VARIOUS).create();

        assertFalse(product.isSceneCrsASharedModelCrs());
        assertTrue(product.isSceneCrsEqualToModelCrsOf(product.getRasterDataNode("tpgrid_a")));
        assertTrue(product.isSceneCrsEqualToModelCrsOf(product.getRasterDataNode("band_a")));
        assertTrue(product.isSceneCrsEqualToModelCrsOf(product.getRasterDataNode("mask_c")));
        assertFalse(product.isSceneCrsEqualToModelCrsOf(product.getRasterDataNode("band_d")));

        product = builder
                .gc(GC.NONE).create();

        assertTrue(product.isSceneCrsASharedModelCrs());
        assertTrue(product.isSceneCrsEqualToModelCrsOf(product.getRasterDataNode("tpgrid_a")));
        assertTrue(product.isSceneCrsEqualToModelCrsOf(product.getRasterDataNode("band_a")));
        assertTrue(product.isSceneCrsEqualToModelCrsOf(product.getRasterDataNode("mask_c")));
    }

    @Test
    public void testObviousCase() throws Exception {
        MathTransform2D geoCodingTransform = new NonLinearGarbageTransform();
        DefaultDerivedCRS crs1 = new DefaultDerivedCRS("IMG1", WGS84, geoCodingTransform, DISPLAY);

        AffineTransform2D at1 = new AffineTransform2D(AffineTransform.getScaleInstance(0.5, 0.5));
        DefaultDerivedCRS crs2 = new DefaultDerivedCRS("IMG2", crs1, at1, DISPLAY);

        assertEquals(at1, CRS.findMathTransform(crs1, crs2));

        AffineTransform2D at2 = new AffineTransform2D(AffineTransform.getScaleInstance(0.5, 0.5));
        DefaultDerivedCRS crs3 = new DefaultDerivedCRS("IMG3", crs2, at2, DISPLAY);

        assertEquals(at2, CRS.findMathTransform(crs2, crs3));

        AffineTransform2D at3 = new AffineTransform2D(AffineTransform.getScaleInstance(0.25, 0.25));
        assertEquals(at3, CRS.findMathTransform(crs1, crs3));
    }

    @Ignore
    @Test
    public void testNotSoObviousCase() throws Exception {
        MathTransform2D geoCodingTransform = new NonLinearGarbageTransform();
        DefaultDerivedCRS crs1 = new DefaultDerivedCRS("IMG1", WGS84, geoCodingTransform, DISPLAY);

        AffineTransform2D at = new AffineTransform2D(AffineTransform.getScaleInstance(0.5, 0.5));
        MathTransform concatenatedTransform = ConcatenatedTransform.create(geoCodingTransform, at);
        DefaultDerivedCRS crs2 = new DefaultDerivedCRS("IMG2", WGS84, concatenatedTransform, DISPLAY);
        // I get "org.opengis.parameter.InvalidParameterValueException: Argument "info" should not be null."  :(

        MathTransform mathTransform = CRS.findMathTransform(crs1, crs2);
        assertEquals(at, mathTransform);
    }

    private static class NonLinearGarbageTransform extends AbstractMathTransform implements MathTransform2D {

        @Override
        public ParameterDescriptorGroup getParameterDescriptors() {
            return new DefaultParameterDescriptorGroup(getClass().getSimpleName(), new GeneralParameterDescriptor[]{
                    new DefaultParameterDescriptor<>("info", String.class, null, "")
            });
        }

        @Override
        public Matrix derivative(Point2D point2D) throws TransformException {
            return new GeneralMatrix(10);
        }

        @Override
        public Matrix derivative(DirectPosition directPosition) throws MismatchedDimensionException, TransformException {
            return new GeneralMatrix(10);
        }

        @Override
        public MathTransform2D inverse() throws NoninvertibleTransformException {
            return new NonLinearGarbageTransform();
        }

        @Override
        public int getSourceDimensions() {
            return 2;
        }

        @Override
        public int getTargetDimensions() {
            return 2;
        }

        @Override
        public DirectPosition transform(DirectPosition directPosition, DirectPosition directPosition1) throws MismatchedDimensionException, TransformException {
            return directPosition;
        }

        @Override
        public Point2D transform(Point2D point2D, Point2D point2D1) throws TransformException {
            return point2D;
        }

        @Override
        public void transform(double[] doubles, int i, double[] doubles1, int i1, int i2) throws TransformException {
        }

        @Override
        public void transform(float[] floats, int i, float[] floats1, int i1, int i2) throws TransformException {
        }

        @Override
        public void transform(float[] floats, int i, double[] doubles, int i1, int i2) throws TransformException {
        }

        @Override
        public void transform(double[] doubles, int i, float[] floats, int i1, int i2) throws TransformException {
        }

        @Override
        public Shape createTransformedShape(Shape shape) throws TransformException {
            return shape;
        }

        @Override
        public boolean isIdentity() {
            return false;
        }

        @Override
        public String toWKT() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass()) && super.equals(o) && getClass().equals(o.getClass());

        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + getClass().hashCode();
        }
    }


}
