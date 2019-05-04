package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.jai.SingleBandedSampleModel;
import org.junit.Test;

import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.Point;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.Raster;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Tonio Fincke
 */
public class DoubleDataAggregatorTest {

    @Test
    public void testAggregate_Mean_All_Fine() {
        final double[] srcData = {0., 1., 2., 3., 4., 5., 6., 7., 8., 9., 10., 11., 12., 13., 14., 15.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Mean meanDataAggregator = new DoubleDataAggregator.Mean();
        meanDataAggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(meanDataAggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{2.5, 4.5, 10.5, 12.5}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Mean_NaN_Is_No_Data_Value() {
        final double[] srcData = {0., 1., 2., 3., 4., 5., 6., 7., 8., 9., 10., 11., 12., 13., 14., 15.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Mean meanDataAggregator = new DoubleDataAggregator.Mean();
        meanDataAggregator.init(null, srcAccessor, destAccessor, Double.NaN);

        aggregate(meanDataAggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{2.5, 4.5, 10.5, 12.5}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Mean_No_Data() {
        final double[] srcData = {0., 1., -1., 3., 4., 5., -1., 7., 8., 9., -1., 11., 12., 13., 14., -1.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Mean meanDataAggregator = new DoubleDataAggregator.Mean();
        meanDataAggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(meanDataAggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{2.5, 5.0, 10.5, 12.5}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Mean_Nans_in_Data() {
        final double[] srcData = {0., 1., Double.NaN, 3., 4., 5., Double.NaN, 7., 8., 9.,
                Double.NaN, 11., 12., 13., 14., Double.NaN};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Mean meanDataAggregator = new DoubleDataAggregator.Mean();
        meanDataAggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(meanDataAggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{2.5, 5.0, 10.5, 12.5}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Median_All_Fine() {
        final double[] srcData = {0., 1., 2., 3., 4., 5., 6., 7., 8., 9., 10., 11., 12., 13., 14., 15.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Median aggregator = new DoubleDataAggregator.Median();
        aggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{2.5, 4.5, 10.5, 12.5}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Median_NaN_Is_No_Data_Value() {
        final double[] srcData = {0., 1., 2., 3., 4., 5., 6., 7., 8., 9., 10., 11., 12., 13., 14., 15.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Median aggregator = new DoubleDataAggregator.Median();
        aggregator.init(null, srcAccessor, destAccessor, Double.NaN);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{2.5, 4.5, 10.5, 12.5}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Median_No_Data() {
        final double[] srcData = {0., 1., -1., 3., 4., 5., -1., 7., 8., 9., -1., 11., 12., 13., 14., -1.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Median aggregator = new DoubleDataAggregator.Median();
        aggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{2.5, 5., 10.5, 12.5}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Median_Nans_in_Data() {
        final double[] srcData = {0., 1., Double.NaN, 3., 4., 5., Double.NaN, 7., 8., 9.,
                Double.NaN, 11., 12., 13., 14., Double.NaN};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Median aggregator = new DoubleDataAggregator.Median();
        aggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{2.5, 5., 10.5, 12.5}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Min_All_Fine() {
        final double[] srcData = {0., 1., 2., 3., 4., 5., 6., 7., 8., 9., 10., 11., 12., 13., 14., 15.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Min aggregator = new DoubleDataAggregator.Min();
        aggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{0., 2., 8., 10.}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Min_NaN_Is_No_Data_Value() {
        final double[] srcData = {0., 1., 2., 3., 4., 5., 6., 7., 8., 9., 10., 11., 12., 13., 14., 15.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Min aggregator = new DoubleDataAggregator.Min();
        aggregator.init(null, srcAccessor, destAccessor, Double.NaN);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{0., 2., 8., 10.}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Min_No_Data() {
        final double[] srcData = {0., 1., -1., 3., 4., 5., -1., 7., 8., 9., -1., 11., 12., 13., 14., -1.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Min aggregator = new DoubleDataAggregator.Min();
        aggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{0., 3., 8., 11.}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Min_Nans_in_Data() {
        final double[] srcData = {0., 1., Double.NaN, 3., 4., 5., Double.NaN, 7., 8., 9.,
                Double.NaN, 11., 12., 13., 14., Double.NaN};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Min aggregator = new DoubleDataAggregator.Min();
        aggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{0., 3., 8., 11.}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Max_All_Fine() {
        final double[] srcData = {0., 1., 2., 3., 4., 5., 6., 7., 8., 9., 10., 11., 12., 13., 14., 15.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Max aggregator = new DoubleDataAggregator.Max();
        aggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{5., 7., 13., 15.}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Max_NaN_Is_No_Data_Value() {
        final double[] srcData = {0., 1., 2., 3., 4., 5., 6., 7., 8., 9., 10., 11., 12., 13., 14., 15.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Max aggregator = new DoubleDataAggregator.Max();
        aggregator.init(null, srcAccessor, destAccessor, Double.NaN);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{5., 7., 13., 15.}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Max_No_Data() {
        final double[] srcData = {0., 1., -1., 3., 4., 5., -1., 7., 8., 9., -1., 11., 12., 13., 14., -1.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Max aggregator = new DoubleDataAggregator.Max();
        aggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{5., 7., 13., 14.}, destData, 1e-8);
    }

    @Test
    public void testAggregate_Max_Nans_in_Data() {
        final double[] srcData = {0., 1., Double.NaN, 3., 4., 5., Double.NaN, 7., 8., 9.,
                Double.NaN, 11., 12., 13., 14., Double.NaN};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.Max aggregator = new DoubleDataAggregator.Max();
        aggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{5., 7., 13., 14.}, destData, 1e-8);
    }

    @Test
    public void testAggregate_First_All_Fine() {
        final double[] srcData = {0., 1., 2., 3., 4., 5., 6., 7., 8., 9., 10., 11., 12., 13., 14., 15.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.First aggregator = new DoubleDataAggregator.First();
        aggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{0., 2., 8., 10.}, destData, 1e-8);
    }

    @Test
    public void testAggregate_First_NaN_Is_No_Data_Value() {
        final double[] srcData = {0., 1., 2., 3., 4., 5., 6., 7., 8., 9., 10., 11., 12., 13., 14., 15.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.First aggregator = new DoubleDataAggregator.First();
        aggregator.init(null, srcAccessor, destAccessor, Double.NaN);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{0., 2., 8., 10.}, destData, 1e-8);
    }

    @Test
    public void testAggregate_First_No_Data() {
        final double[] srcData = {0., 1., -1., 3., 4., 5., -1., 7., 8., 9., -1., 11., 12., 13., 14., -1.};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.First aggregator = new DoubleDataAggregator.First();
        aggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{0., -1., 8., -1.}, destData, 1e-8);
    }

    @Test
    public void testAggregate_First_Nans_in_Data() {
        final double[] srcData = {0., 1., Double.NaN, 3., 4., 5., Double.NaN, 7., 8., 9.,
                Double.NaN, 11., 12., 13., 14., Double.NaN};
        final RasterAccessor srcAccessor = createAccessor(4, 4, srcData);
        final RasterAccessor destAccessor = createAccessor(2, 2, new double[]{-2., -2., -2., -2.});
        final DoubleDataAggregator.First aggregator = new DoubleDataAggregator.First();
        aggregator.init(null, srcAccessor, destAccessor, -1.);

        aggregate(aggregator);

        final double[] destData = destAccessor.getDoubleDataArray(0);
        assertArrayEquals(new double[]{0., Double.NaN, 8., Double.NaN}, destData, 1e-8);
    }

    private void aggregate(DoubleDataAggregator aggregator) {
        aggregator.aggregate(0, 1, 0, 1, 4, 1, 1, 1, 1, 0);
        aggregator.aggregate(0, 1, 2, 3, 4, 1, 1, 1, 1, 1);
        aggregator.aggregate(2, 3, 0, 1, 4, 1, 1, 1, 1, 2);
        aggregator.aggregate(2, 3, 2, 3, 4, 1, 1, 1, 1, 3);
    }

    private RasterAccessor createAccessor(int width, int height, double[] data) {
        final SingleBandedSampleModel sampleModel = new SingleBandedSampleModel(DataBuffer.TYPE_DOUBLE, width, height);
        final DataBufferDouble dataBuffer = new DataBufferDouble(data, data.length);
        final Raster raster = Raster.createRaster(sampleModel, dataBuffer, new Point(0, 0));
        final RasterFormatTag tag = new RasterFormatTag(sampleModel, 1157);
        final ColorModel colorModel =
                ImageManager.createSingleBandedImageLayout(DataBuffer.TYPE_DOUBLE, width, height, width, height).
                        getColorModel(null);
        return new RasterAccessor(raster, raster.getBounds(), tag, colorModel);
    }

}