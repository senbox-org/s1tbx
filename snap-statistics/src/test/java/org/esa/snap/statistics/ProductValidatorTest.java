package org.esa.snap.statistics;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProductValidatorTest {

    private final int oneSecond = 1000; // == 1000 milliseconds

    private ProductData.UTC startDate;
    private ProductData.UTC endDate;
    private List<BandConfiguration> bandConfigurations;
    private ProductValidator productValidator;
    private Logger loggerMock;
    private Product product;

    @Before
    public void setUp() throws Exception {
        loggerMock = mock(Logger.class);
        bandConfigurations = new ArrayList<>();
        startDate = ProductData.UTC.parse("2012-05-21 00:00:00", StatisticsOp.DATETIME_PATTERN);
        endDate = ProductData.UTC.parse("2012-11-08 00:00:00", StatisticsOp.DATETIME_PATTERN);
        productValidator = new ProductValidator(bandConfigurations, startDate, endDate, loggerMock);
        product = mock(Product.class);
        when(product.getSceneGeoCoding()).thenReturn(mock(GeoCoding.class));
        when(product.getStartTime()).thenReturn(startDate);
        when(product.getEndTime()).thenReturn(endDate);
    }

    @Test
    public void testIsValid_IfIsEntirelyInTimeRange() {
        //preparation

        //execution
        final boolean valid = productValidator.isValid(product);

        //verification
        assertEquals(true, valid);
        verifyNoMoreInteractions(loggerMock);
    }

    @Test
    public void testIsInvalid_IfProductDoesNotContainAGeoCoding() {
        //preparation
        when(product.getSceneGeoCoding()).thenReturn(null);
        when(product.getName()).thenReturn("No Geocoding");

        //execution
        final boolean valid = productValidator.isValid(product);

        //verification
        assertEquals(false, valid);
        verify(loggerMock).warning("Product skipped. The product 'No Geocoding' does not contain a geo coding.");
        verifyNoMoreInteractions(loggerMock);
    }

    @Test
    public void testIsInvalid_IfIsNotEntirelyInTimeRange_beforeTimeRange() {
        //preparation
        when(product.getStartTime()).thenReturn(before(startDate));
        when(product.getEndTime()).thenReturn(endDate);
        when(product.getName()).thenReturn("OutOfDateRange_before");

        //execution
        final boolean valid = productValidator.isValid(product);

        //verification
        assertEquals(false, valid);
        verify(loggerMock).warning("Product skipped. The product 'OutOfDateRange_before' is not inside the date range from 21-MAY-2012 00:00:00.000000 to 08-NOV-2012 00:00:00.000000");
        verifyNoMoreInteractions(loggerMock);
    }

    @Test
    public void testIsInvalid_IfIsNotEntirelyInTimeRange_afterTimeRange() {
        //preparation
        when(product.getStartTime()).thenReturn(startDate);
        when(product.getEndTime()).thenReturn(after(endDate));
        when(product.getName()).thenReturn("OutOfDateRange_after");

        //execution
        final boolean valid = productValidator.isValid(product);

        //verification
        assertEquals(false, valid);
        verify(loggerMock).warning("Product skipped. The product 'OutOfDateRange_after' is not inside the date range from 21-MAY-2012 00:00:00.000000 to 08-NOV-2012 00:00:00.000000");
        verifyNoMoreInteractions(loggerMock);
    }

    @Test
    public void testProductValidatorThatHasOnlyStartTime() throws Exception {
        //preparation
        productValidator = new ProductValidator(bandConfigurations, startDate, null, loggerMock);

        //execution
        //verification
        assertTrue(isValid(configureProductTimes(startDate, after(endDate))));
        assertTrue(isValid(configureProductTimes(startDate, before(endDate))));
        assertTrue(isValid(configureProductTimes(startDate, endDate)));
        assertTrue(isValid(configureProductTimes(startDate, null)));

        assertFalse(isValid(configureProductTimes(null, null)));
        assertFalse(isValid(configureProductTimes(null, endDate)));
        assertFalse(isValid(configureProductTimes(before(startDate), null)));
        assertFalse(isValid(configureProductTimes(before(startDate), endDate)));
    }

    @Test
    public void testProductValidatorThatHasOnlyEndTime() throws Exception {
        //preparation
        productValidator = new ProductValidator(bandConfigurations, null, endDate, loggerMock);

        //execution
        //verification
        assertTrue(isValid(configureProductTimes(null, endDate)));
        assertTrue(isValid(configureProductTimes(after(startDate), endDate)));
        assertTrue(isValid(configureProductTimes(before(startDate), endDate)));
        assertTrue(isValid(configureProductTimes(startDate, endDate)));

        assertFalse(isValid(configureProductTimes(null, null)));
        assertFalse(isValid(configureProductTimes(startDate, null)));
        assertFalse(isValid(configureProductTimes(null, after(endDate))));
        assertFalse(isValid(configureProductTimes(startDate, after(endDate))));
    }

    @Test
    public void testProductValidatorThatHasNoTimes() throws Exception {
        //preparation
        productValidator = new ProductValidator(bandConfigurations, null, null, loggerMock);

        //execution
        //verification
        assertTrue(isValid(configureProductTimes(null, endDate)));
        assertTrue(isValid(configureProductTimes(after(startDate), endDate)));
        assertTrue(isValid(configureProductTimes(before(startDate), endDate)));
        assertTrue(isValid(configureProductTimes(startDate, endDate)));

        assertTrue(isValid(configureProductTimes(null, null)));
        assertTrue(isValid(configureProductTimes(startDate, null)));
        assertTrue(isValid(configureProductTimes(null, after(endDate))));
        assertTrue(isValid(configureProductTimes(startDate, after(endDate))));
    }

    private boolean isValid(Product product) {
        return productValidator.isValid(product);
    }

    private Product configureProductTimes(ProductData.UTC startDate, ProductData.UTC endDate) {
        when(product.getStartTime()).thenReturn(startDate);
        when(product.getEndTime()).thenReturn(endDate);
        return product;
    }

    @Test
    public void testIsValid_IfProductHasAllBandsNeededInBandConfigurations() {
        //preparation
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.sourceBandName = "band-1";
        bandConfigurations.add(bandConfiguration);

        when(product.containsBand("band-1")).thenReturn(true);

        //execution
        final boolean valid = productValidator.isValid(product);

        //verification
        assertEquals(true, valid);
        verifyNoMoreInteractions(loggerMock);
    }

    @Test
    public void testIsInvalid_IfProductDoesNotContainAllBandsNeededInBandConfigurations() {
        //preparation
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.sourceBandName = "band-1";
        bandConfigurations.add(bandConfiguration);

        when(product.containsBand("band-1")).thenReturn(false);
        when(product.getName()).thenReturn("InvalidProduct");

        //execution
        final boolean valid = productValidator.isValid(product);

        //verification
        assertEquals(false, valid);
        verify(loggerMock).warning("Product skipped. The product 'InvalidProduct' does not contain the band 'band-1'");
        verifyNoMoreInteractions(loggerMock);
    }

    @Test
    public void testIsInvalid_IfProductCanNotResolveTheExpressionNeededInBandConfigurations() {
        //preparation
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.expression = "band_1 + 4";
        bandConfigurations.add(bandConfiguration);

        when(product.isCompatibleBandArithmeticExpression("band_1 + 4")).thenReturn(false);
        when(product.getName()).thenReturn("InvalidProduct");

        //execution
        final boolean valid = productValidator.isValid(product);

        //verification
        assertEquals(false, valid);
        verify(loggerMock).warning("Product skipped. The product 'InvalidProduct' can not resolve the band arithmetic expression 'band_1 + 4'");
        verifyNoMoreInteractions(loggerMock);
    }

    @Test
    public void testIsInvalid_IfProductAlreadyContainsBandWithExpressionName() {
        //preparation
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.expression = "band_1 + 4";
        bandConfigurations.add(bandConfiguration);
        when(product.isCompatibleBandArithmeticExpression("band_1 + 4")).thenReturn(true);
        when(product.getName()).thenReturn("InvalidProduct");
        when(product.containsBand("band_1_+_4")).thenReturn(true);

        //execution
        final boolean valid = productValidator.isValid(product);

        //verification
        assertEquals(false, valid);
        verify(loggerMock).warning("Product skipped. The product 'InvalidProduct' already contains a band 'band_1_+_4'");
        verifyNoMoreInteractions(loggerMock);
    }

    private ProductData.UTC before(ProductData.UTC date) {
        final long time = date.getAsDate().getTime() - oneSecond;
        return ProductData.UTC.create(new Date(time), 0);
    }

    private ProductData.UTC after(ProductData.UTC date) {
        final long time = date.getAsDate().getTime() + oneSecond;
        return ProductData.UTC.create(new Date(time), 0);
    }
}
