package org.esa.beam.statistics;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ProductLoopTest {

    private StatisticComputer _statisticComputerMock;
    private ProductLoop _productLoop;
    private ProductLoader _productLoaderMock;
    private Logger _loggerMock;
    private ProductData.UTC _startDate;
    private ProductData.UTC _endDate;
    private Product _validProductMock1;
    private Product _validProductMock2;

    @Before
    public void setUp() throws Exception {
        _statisticComputerMock = mock(StatisticComputer.class);
        _productLoaderMock = mock(ProductLoader.class);
        _loggerMock = mock(Logger.class);
        _startDate = ProductData.UTC.parse("22-MAR-2008 00:00:00");
        _endDate = ProductData.UTC.parse("15-SEP-2008 00:00:00");
        _validProductMock1 = createTimeValidProductMock(4, 3);
        _validProductMock2 = createTimeValidProductMock(7, 2);
        _productLoop = new ProductLoop(_productLoaderMock, _statisticComputerMock, _startDate, _endDate, _loggerMock);
    }

    @Test
    public void testThatAlreadyLoadedProductsShouldNotBeDisposedAfterStatisticComputation() {
        // preparation
        final Product[] loadedProducts = {_validProductMock1, _validProductMock2};

        // execution
        _productLoop.loop(loadedProducts, new File[0]);

        // verification
        final InOrder inOrder = inOrder(_statisticComputerMock);
        inOrder.verify(_statisticComputerMock).computeStatistic(same(_validProductMock1));
        inOrder.verify(_statisticComputerMock).computeStatistic(same(_validProductMock2));
        verify(_validProductMock1, never()).dispose();
        verify(_validProductMock2, never()).dispose();
    }

    @Test
    public void testThatLoadedProductsShouldBeDisposedAfterStatisticComputation() throws IOException {
        // preparation
        final File file1 = new File("1");
        final File file2 = new File("2");
        when(_productLoaderMock.loadProduct(file1)).thenReturn(_validProductMock1);
        when(_productLoaderMock.loadProduct(file2)).thenReturn(_validProductMock2);
        final File[] productFilesToLoad = {file1, file2};

        // execution
        _productLoop.loop(new Product[0], productFilesToLoad);

        // verification
        final InOrder inOrder = inOrder(_statisticComputerMock);
        inOrder.verify(_statisticComputerMock).computeStatistic(same(_validProductMock1));
        inOrder.verify(_statisticComputerMock).computeStatistic(same(_validProductMock2));
        verify(_validProductMock1, times(1)).dispose();
        verify(_validProductMock2, times(1)).dispose();
    }

    @Test
    public void testThatFilesWhichPointToAnAlreadyLoadedProductShouldNotBeOpenedTwice() throws IOException {
        // preparation
        final File file1 = _validProductMock1.getFileLocation();
        final File file2 = _validProductMock2.getFileLocation(); // points to productMock2
        when(_productLoaderMock.loadProduct(file1)).thenReturn(_validProductMock1);
        final Product[] alreadyLoadedProducts = {_validProductMock2};
        final File[] productFilesToLoad = {file1, file2};

        // execution
        _productLoop.loop(alreadyLoadedProducts, productFilesToLoad);

        // verification
        final InOrder inOrder = inOrder(_statisticComputerMock);
        inOrder.verify(_statisticComputerMock).computeStatistic(same(_validProductMock2));
        inOrder.verify(_statisticComputerMock).computeStatistic(same(_validProductMock1));
        verify(_productLoaderMock).loadProduct(eq(file1));
        verify(_validProductMock1, times(1)).dispose();
        verify(_validProductMock2, never()).dispose();
        verify(_validProductMock2, atLeastOnce()).getFileLocation();
    }

    @Test
    public void testThatOperatorExceptionOccursIfNoProductsAreComputed() {
        try {
            _productLoop.loop(new Product[0], new File[0]);
            fail("OperatorExceptionExpected");
        } catch (OperatorException expected) {
            assertEquals("No input products found.", expected.getMessage());
        }
    }

    @Test
    public void testThatFilesWhichCanNotBeOpenedAreLogged() throws IOException {
        //preparation
        final File file1 = _validProductMock1.getFileLocation();
        final File file2 = new File("No reader available"); // no reader found
        when(_productLoaderMock.loadProduct(file1)).thenReturn(_validProductMock1);
        when(_productLoaderMock.loadProduct(file2)).thenReturn(null); // no reader found
        final Product[] alreadyLoadedProducts = {};
        final File[] productFilesToLoad = {file1, file2};

        //execution
        _productLoop.loop(alreadyLoadedProducts, productFilesToLoad);

        //verification
        verify(_loggerMock).severe("Failed to read from 'No reader available' (not a data product or reader missing)");
        verify(_statisticComputerMock, times(1)).computeStatistic(_validProductMock1);
        verify(_productLoaderMock, times(2)).loadProduct(any(File.class));
        verifyNoMoreInteractions(_statisticComputerMock, _productLoaderMock, _loggerMock);
    }

    @Test
    public void testThatIOExceptionsAreLogged() throws IOException {
        //preparation
        final File file1 = _validProductMock1.getFileLocation();
        final File file2 = new File("Causes IO Exception"); // IO Exception
        when(_productLoaderMock.loadProduct(file1)).thenReturn(_validProductMock1);
        when(_productLoaderMock.loadProduct(file2)).thenThrow(new IOException()); // IO Exception
        final Product[] alreadyLoadedProducts = {};
        final File[] productFilesToLoad = {file1, file2};

        //execution
        _productLoop.loop(alreadyLoadedProducts, productFilesToLoad);

        //verification
        verify(_loggerMock).severe("Failed to read from 'Causes IO Exception' (not a data product or reader missing)");
        verify(_statisticComputerMock, times(1)).computeStatistic(_validProductMock1);
        verify(_productLoaderMock, times(2)).loadProduct(any(File.class));
        verifyNoMoreInteractions(_statisticComputerMock, _productLoaderMock, _loggerMock);
    }

    @Test
    public void testThatAlreadyLoadedProductsAreSkippedIfOutOfDateRange() {
        //preparation
        final Product productMockBefore = createProductMock(4, 2, true, false);
        final Product productMockAfter = createProductMock(4, 2, false, true);
        final Product[] alreadyLoadedProducts = {productMockBefore, productMockAfter, _validProductMock1};

        //execution
        _productLoop.loop(alreadyLoadedProducts, new File[0]);

        //verification
        final InOrder inOrder = inOrder(_loggerMock);
        inOrder.verify(_loggerMock).info("Product skipped. The product 'mock_loc_before_4_2' is not inside the date range from: 22-MAR-2008 00:00:00.000000 to: 15-SEP-2008 00:00:00.000000");
        inOrder.verify(_loggerMock).info("Product skipped. The product 'mock_loc_after_4_2' is not inside the date range from: 22-MAR-2008 00:00:00.000000 to: 15-SEP-2008 00:00:00.000000");
        verify(_statisticComputerMock).computeStatistic(_validProductMock1);
        verifyNoMoreInteractions(_statisticComputerMock);
        verify(productMockBefore, never()).dispose();
        verify(productMockAfter, never()).dispose();
        verify(_validProductMock1, never()).dispose();
    }

    @Test
    public void testThatProductsToBeLoadedAreSkippedIfOutOfDateRange() throws IOException {
        //preparation
        final File file1 = new File("before");
        final File file2 = new File("after");
        final File file3 = _validProductMock1.getFileLocation();
        final Product productMockBefore = createProductMock(4, 2, true, false);
        final Product productMockAfter = createProductMock(4, 2, false, true);
        when(_productLoaderMock.loadProduct(file1)).thenReturn(productMockBefore);
        when(_productLoaderMock.loadProduct(file2)).thenReturn(productMockAfter);
        when(_productLoaderMock.loadProduct(file3)).thenReturn(_validProductMock1);
        final Product[] alreadyLoadedProducts = {};
        final File[] productFilesToLoad = new File[]{file1, file2, file3};

        //execution
        _productLoop.loop(alreadyLoadedProducts, productFilesToLoad);

        //verification
        final InOrder inOrder = inOrder(_loggerMock);
        inOrder.verify(_loggerMock).info("Product skipped. The product 'mock_loc_before_4_2' is not inside the date range from: 22-MAR-2008 00:00:00.000000 to: 15-SEP-2008 00:00:00.000000");
        inOrder.verify(_loggerMock).info("Product skipped. The product 'mock_loc_after_4_2' is not inside the date range from: 22-MAR-2008 00:00:00.000000 to: 15-SEP-2008 00:00:00.000000");
        verify(_statisticComputerMock).computeStatistic(_validProductMock1);
        verifyNoMoreInteractions(_statisticComputerMock);
        verify(productMockBefore, times(1)).dispose();
        verify(productMockAfter, times(1)).dispose();
        verify(_validProductMock1, times(1)).dispose();
    }

    @Test
    public void testThatAlreadyLoadedProductsMightBeNull() throws IOException {
        //preparation
        File file = _validProductMock1.getFileLocation();
        when(_productLoaderMock.loadProduct(file)).thenReturn(_validProductMock1);

        //execution
        _productLoop.loop(null, new File[]{file});

        //verification
        verify(_statisticComputerMock).computeStatistic(_validProductMock1);
        verify(_validProductMock1, times(1)).dispose();
    }

    private Product createTimeValidProductMock(int startOffset, int numObservationDays) {
        return createProductMock(startOffset, numObservationDays, false, false);
    }

    private Product createProductMock(int startOffset, int numObservationDays, boolean before_startDate, boolean after_endDate) {
        final ProductData.UTC startTime = getStartTime(startOffset, numObservationDays, before_startDate, after_endDate);
        final ProductData.UTC endTime = new ProductData.UTC(startTime.getMJD() + numObservationDays);
        final File fileLocation = getFileLocation(startOffset, numObservationDays, before_startDate, after_endDate);

        final Product mock = mock(Product.class);
        when(mock.getStartTime()).thenReturn(startTime);
        when(mock.getEndTime()).thenReturn(endTime);
        when(mock.getFileLocation()).thenReturn(fileLocation);
        when(mock.getName()).thenReturn(fileLocation.getName());
        return mock;
    }

    private ProductData.UTC getStartTime(int startOffset, int numObservationDays, boolean before_startDate, boolean after_endDate) {
        final ProductData.UTC startTime;
        if (before_startDate) {
            startTime = new ProductData.UTC(_startDate.getMJD() - startOffset - numObservationDays);
        } else if (after_endDate) {
            startTime = new ProductData.UTC(_endDate.getMJD() + startOffset);
        } else {
            startTime = new ProductData.UTC(_startDate.getMJD() + startOffset);
        }
        return startTime;
    }

    private File getFileLocation(int startOffset, int numObservationDays, boolean before, boolean after) {
        final StringBuilder location = new StringBuilder();
        location.append("mock_loc_");
        if (before) {
            location.append("before_");
        } else if (after) {
            location.append("after_");
        }
        location.append(startOffset).append("_");
        location.append(numObservationDays);
        return new File(location.toString());
    }
}
