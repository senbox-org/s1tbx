package org.esa.beam.statistics;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.*;

public class ProductValidatorTest {

    private final int OneSecond = 1000; // == 1000 milliseconds

    private ProductData.UTC _startDate;
    private ProductData.UTC _endDate;
    private List<BandConfiguration> _bandConfigurations;
    private ProductValidator _productValidator;
    private Logger _loggerMock;
    private Product _product;

    @Before
    public void setUp() throws Exception {
        _loggerMock = mock(Logger.class);
        _bandConfigurations = new ArrayList<BandConfiguration>();
        _startDate = ProductData.UTC.parse("2012-05-21 00:00:00", StatisticsOp.DATETIME_PATTERN);
        _endDate = ProductData.UTC.parse("2012-11-08 00:00:00", StatisticsOp.DATETIME_PATTERN);
        _productValidator = new ProductValidator(_bandConfigurations, _startDate, _endDate, _loggerMock);
        _product = mock(Product.class);
        when(_product.getGeoCoding()).thenReturn(mock(GeoCoding.class));
        when(_product.getStartTime()).thenReturn(_startDate);
        when(_product.getEndTime()).thenReturn(_endDate);
    }

    @Test
    public void testIsValid_IfIsEntirelyInTimeRange() {
        //preparation

        //execution
        final boolean valid = _productValidator.isValid(_product);

        //verification
        assertEquals(true, valid);
        verifyNoMoreInteractions(_loggerMock);
    }

    @Test
    public void testIsInvalid_IfProductDoesNotContainAGeoCoding() {
        //preparation
        when(_product.getGeoCoding()).thenReturn(null);
        when(_product.getName()).thenReturn("No Geocoding");

        //execution
        final boolean valid = _productValidator.isValid(_product);

        //verification
        assertEquals(false, valid);
        verify(_loggerMock).info("Product skipped. The product 'No Geocoding' does not contain a geo coding.");
        verifyNoMoreInteractions(_loggerMock);
    }

    @Test
    public void testIsInvalid_IfIsNotEntirelyInTimeRange_beforeTimeRange() {
        //preparation
        when(_product.getStartTime()).thenReturn(smaller(_startDate));
        when(_product.getEndTime()).thenReturn(_endDate);
        when(_product.getName()).thenReturn("OutOfDateRange_before");

        //execution
        final boolean valid = _productValidator.isValid(_product);

        //verification
        assertEquals(false, valid);
        verify(_loggerMock).info("Product skipped. The product 'OutOfDateRange_before' is not inside the date range from: 21-MAY-2012 00:00:00.000000 to: 08-NOV-2012 00:00:00.000000");
        verifyNoMoreInteractions(_loggerMock);
    }

    @Test
    public void testIsInvalid_IfIsNotEntirelyInTimeRange_afterTimeRange() {
        //preparation
        when(_product.getStartTime()).thenReturn(_startDate);
        when(_product.getEndTime()).thenReturn(bigger(_endDate));
        when(_product.getName()).thenReturn("OutOfDateRange_after");

        //execution
        final boolean valid = _productValidator.isValid(_product);

        //verification
        assertEquals(false, valid);
        verify(_loggerMock).info("Product skipped. The product 'OutOfDateRange_after' is not inside the date range from: 21-MAY-2012 00:00:00.000000 to: 08-NOV-2012 00:00:00.000000");
        verifyNoMoreInteractions(_loggerMock);
    }

    @Test
    public void testIsValid_IfProductHasAllBandsNeededInBandConfigurations() {
        //preparation
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.sourceBandName = "band-1";
        _bandConfigurations.add(bandConfiguration);

        when(_product.containsBand("band-1")).thenReturn(true);

        //execution
        final boolean valid = _productValidator.isValid(_product);

        //verification
        assertEquals(true, valid);
        verifyNoMoreInteractions(_loggerMock);
    }

    @Test
    public void testIsInvalid_IfProductDoesNotContainAllBandsNeededInBandConfigurations() {
        //preparation
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.sourceBandName = "band-1";
        _bandConfigurations.add(bandConfiguration);

        when(_product.containsBand("band-1")).thenReturn(false);
        when(_product.getName()).thenReturn("InvalidProduct");

        //execution
        final boolean valid = _productValidator.isValid(_product);

        //verification
        assertEquals(false, valid);
        verify(_loggerMock).info("Product skipped. The product 'InvalidProduct' does not contain the band 'band-1'");
        verifyNoMoreInteractions(_loggerMock);
    }

    @Test
    public void testIsInvalid_IfProductCanNotResolveTheExpressionNeededInBandConfigurations() {
        //preparation
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.expression = "band_1 + 4";
        _bandConfigurations.add(bandConfiguration);

        when(_product.isCompatibleBandArithmeticExpression("band_1 + 4")).thenReturn(false);
        when(_product.getName()).thenReturn("InvalidProduct");

        //execution
        final boolean valid = _productValidator.isValid(_product);

        //verification
        assertEquals(false, valid);
        verify(_loggerMock).info("Product skipped. The product 'InvalidProduct' can not resolve the band arithmetic expression 'band_1 + 4'");
        verifyNoMoreInteractions(_loggerMock);
    }

    @Test
    public void testIsInvalid_IfProductAlreadyContainsBandWithExpressionName() {
        //preparation
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.expression = "band_1 + 4";
        _bandConfigurations.add(bandConfiguration);
        when(_product.isCompatibleBandArithmeticExpression("band_1 + 4")).thenReturn(true);
        when(_product.getName()).thenReturn("InvalidProduct");
        when(_product.containsBand("band_1_+_4")).thenReturn(true);

        //execution
        final boolean valid = _productValidator.isValid(_product);

        //verification
        assertEquals(false, valid);
        verify(_loggerMock).info("Product skipped. The product 'InvalidProduct' already contains a band 'band_1_+_4'");
        verifyNoMoreInteractions(_loggerMock);
    }

    private ProductData.UTC smaller(ProductData.UTC date) {
        final long time = date.getAsDate().getTime() - OneSecond;
        return ProductData.UTC.create(new Date(time), 0);
    }

    private ProductData.UTC bigger(ProductData.UTC date) {
        final long time = date.getAsDate().getTime() + OneSecond;
        return ProductData.UTC.create(new Date(time), 0);
    }
}
