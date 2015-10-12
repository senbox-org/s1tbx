package org.esa.snap.statistics.percentile.interpolated;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.Date;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Utils.class)
public class ProductValidatorTest {

    private String sourceBandName;
    private ProductData.UTC _timeRangeStart;
    private ProductData.UTC _timeRangeEnd;
    private Logger S_logger;
    private ProductValidator _productValidator;
    private Product M_product;
    private GeoCoding M_geoCoding;
    private Area _nonIntersectingArea;

    @Before
    public void setUp() throws Exception {
        sourceBandName = "sbn";

        _timeRangeStart = ProductData.UTC.parse("2012-05-21 00:00:00", TemporalPercentileOp.DATETIME_PATTERN);
        _timeRangeEnd = ProductData.UTC.parse("2012-07-08 00:00:00", TemporalPercentileOp.DATETIME_PATTERN);

        Area targetArea = new Area(new Rectangle(9, 51, 20, 15));
        Area intersectingArea = new Area(new Rectangle(3, 45, 20, 15));
        _nonIntersectingArea = new Area(new Rectangle(3, 45, 2, 1));
        PowerMockito.mockStatic(Utils.class);
        PowerMockito.when(Utils.createProductArea(any(Product.class))).thenReturn(intersectingArea);

        final Logger logger = Logger.getAnonymousLogger();
        S_logger = PowerMockito.spy(logger);

        _productValidator = new ProductValidator(sourceBandName, null, null, _timeRangeStart, _timeRangeEnd, targetArea, S_logger);

        M_geoCoding = PowerMockito.mock(GeoCoding.class);
        PowerMockito.when(M_geoCoding.canGetPixelPos()).thenReturn(true);

        final ProductData.UTC productStartTime = ProductData.UTC.parse("2012-05-22 00:00:00", TemporalPercentileOp.DATETIME_PATTERN);
        final ProductData.UTC productEndTime = ProductData.UTC.parse("2012-07-07 00:00:00", TemporalPercentileOp.DATETIME_PATTERN);

        M_product = PowerMockito.mock(Product.class);
        PowerMockito.when(M_product.getName()).thenReturn("ProductMock");
        PowerMockito.when(M_product.getSceneGeoCoding()).thenReturn(M_geoCoding);
        PowerMockito.when(M_product.getStartTime()).thenReturn(productStartTime);
        PowerMockito.when(M_product.getEndTime()).thenReturn(productEndTime);
        PowerMockito.when(M_product.containsBand(sourceBandName)).thenReturn(true);
    }

    @Test
    public void testValidProduct() {
        boolean result = _productValidator.isValid(M_product);

        assertEquals(true, result);
        verifyNoMoreInteractions(S_logger);
    }

    @Test
    public void testThatVerificationFailsIfProductHasNoGeoCoding() {
        PowerMockito.when(M_product.getSceneGeoCoding()).thenReturn(null);

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The product 'ProductMock' does not contain a geo coding.");
    }

    @Test
    public void testThatVerificationFailsIfTheGeoCodingCanNotGetPixelPositionFromGeoPos() {
        PowerMockito.when(M_geoCoding.canGetPixelPos()).thenReturn(false);

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The geo-coding of the product 'ProductMock' can not determine the pixel position from a geodetic position.");
    }

    @Test
    public void testThatVerificationFailsIfTheProductDoesNotContainAStartTime() {
        PowerMockito.when(M_product.getStartTime()).thenReturn(null);

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The product 'ProductMock' must contain start and end time.");
    }

    @Test
    public void testThatVerificationFailsIfTheProductDoesNotContainAnEndTime() {
        PowerMockito.when(M_product.getEndTime()).thenReturn(null);

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The product 'ProductMock' must contain start and end time.");
    }

    @Test
    public void testThatVerificationFailsIfTheProductCanNotHandleTheBandConfiguration() {
        PowerMockito.when(M_product.containsBand(sourceBandName)).thenReturn(false);

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The product 'ProductMock' does not contain the band 'sbn'.");
    }

    @Test
    public void testThatVerificationFailsIfTheProductStartsBeforeTimeRange() {
        final long timeRangeStartTime = _timeRangeStart.getAsDate().getTime();
        final Date beforeTime = new Date(timeRangeStartTime - 1);
        PowerMockito.when(M_product.getStartTime()).thenReturn(ProductData.UTC.create(beforeTime, 0));

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The product 'ProductMock' is not inside the date range from 21-MAY-2012 00:00:00.000000 to 08-JUL-2012 00:00:00.000000");
    }

    @Test
    public void testThatVerificationFailsIfTheProductEndsAfterTimeRange() {
        final long timeRangeEndTime = _timeRangeEnd.getAsDate().getTime();
        final Date afterTime = new Date(timeRangeEndTime + 1000);
        PowerMockito.when(M_product.getEndTime()).thenReturn(ProductData.UTC.create(afterTime, 0));

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The product 'ProductMock' is not inside the date range from 21-MAY-2012 00:00:00.000000 to 08-JUL-2012 00:00:00.000000");
    }

    @Test
    public void testThatVerificationFailsIfTheProductDoesNotIntersectTheTargetArea() {
        PowerMockito.when(Utils.createProductArea(any(Product.class))).thenReturn(_nonIntersectingArea);

        boolean result = _productValidator.isValid(M_product);

        assertEquals(false, result);
        verify(S_logger, times(1)).info("Product skipped. The product 'ProductMock' does not intersect the target product.");
    }
}
